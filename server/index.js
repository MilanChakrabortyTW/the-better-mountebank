require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const path = require('path');
const { createProxyMiddleware } = require('http-proxy-middleware');
const MockConfig = require('./models/MockConfig');
const Counter = require('./models/Counter');
const fs = require('fs');
const { imposters, getImposterByUrl } = require('./imposters');

const app = express();
const PORT = process.env.PORT || 5000;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://admin:admin@localhost:27017/';

mongoose.connect(MONGO_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  user: process.env.MONGO_USER,
  pass: process.env.MONGO_PASS,
})
    .then(() => console.log('MongoDB connected'))
    .catch((err) => console.error('MongoDB connection error:', err));

app.use(express.static(path.join(__dirname, '../client/build')));
app.use(express.json());

app.post('/config', async (req, res) => {
  const { configs } = req.body;
  const counter = await Counter.findByIdAndUpdate(
      'mockPrefix',
      { $inc: { seq: 1 } },
      { upsert: true, returnDocument: 'after' }
  );
  const mockPrefix = `BMB_MOCK_${String(counter.seq).padStart(6, '0')}`;
  const config = new MockConfig({ mockPrefix, configs });
  await config.save();
  res.json({ mockPrefix });
});

app.put('/config/:mockPrefix', async (req, res) => {
  const { mockPrefix } = req.params;
  const { configs } = req.body;
  let config = await MockConfig.findOne({ mockPrefix });
  if (!config) {
    return res.status(404).json({ error: 'Config not found for mockPrefix' });
  }
  config.configs = configs;
  await config.save();
  res.json({ mockPrefix });
});

const unleashProxy = createProxyMiddleware({
  target: 'http://unleash.unleash:4242',
  changeOrigin: true,
});

app.use('/unleash/api/client/features', async (req, res, next) => {
  try {
    const lanPrefix = req.header('X-VO-LAN');
    let config = null;
    if (lanPrefix) {
      config = await MockConfig.findOne({ lanPrefix });
    }
    if (!lanPrefix || !config || !Array.isArray(config.toggles)) {
      return unleashProxy(req, res, next);
    }
    return res.json({ version: 1, features: config.toggles });
  } catch (e) {
    console.error('Error in /unleash/api/client/features:', e);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

const getOutputForCall = (endpointConfig) => {
  console.log('[getOutputForCall] endpointConfig:', endpointConfig);
  const callIndex = endpointConfig.callCount || 0;
  const outputIndex = Math.min(callIndex, endpointConfig.outputs.length - 1);
  return { output: endpointConfig.outputs[outputIndex], callIndex };
};

const updateCallCount = async (config, endpointConfig, specialName, callIndex) => {
  console.log('[updateCallCount] specialName:', specialName, 'callIndex:', callIndex);
  endpointConfig.callCount = callIndex + 1;
  config.configs.set(specialName, endpointConfig);
  config.markModified('configs');
  await config.save();
};

const sendStaticResponse = (res, imposter, output) => {
  try {
    const filePath = path.join(__dirname, imposter.group, 'responses', `${output.responseType}.json`);
    console.log('[sendStaticResponse] filePath:', filePath);
    if (fs.existsSync(filePath)) {
      const data = fs.readFileSync(filePath, 'utf-8');
      const parsed = JSON.parse(data);
      if (parsed.headers) {
        Object.entries(parsed.headers).forEach(([key, value]) => res.setHeader(key, value));
      }
      return res.status(parsed.status || 200).json(parsed.body !== undefined ? parsed.body : parsed);
    } else {
      console.warn('[sendStaticResponse] File does not exist:', filePath);
    }
  } catch (e) {
    console.error('[sendStaticResponse] Error reading static response:', e);
  }
  return false;
};

app.all('/{*any}', async (req, res, next) => {
  if (
      req.path.startsWith('/unleash/api/client/features') ||
      req.path.startsWith('/static/') ||
      req.path === '/' ||
      req.path.startsWith('/api')
  ) {
    return next();
  }

  const headerMockPrefix = req.header('X-VO-MOCK');
  let config = null;

  if (headerMockPrefix) {
    const prefixMatch = headerMockPrefix.match(/^[A-Z]+_MOCK_\d{6}/);
    if (prefixMatch) {
      config = await MockConfig.findOne({ mockPrefix: prefixMatch[0] });
    }
  }

  const method = req.method;
  const imposter = getImposterByUrl(req.path, method);

  if (!imposter) {
    if (config) {
      const specialName = 'not_found';
      let endpointConfig = config.configs.get(specialName) || {
        outputs: [{ responseType: 'not_found' }],
        callCount: 0,
      };
      await updateCallCount(config, endpointConfig, specialName, endpointConfig.callCount || 0);
    }
    return res.status(404).json({ error: 'Imposter not found for this path and method' });
  }

  const specialName = imposter.specialName;
  const endpointConfig = specialName && config ? config.configs.get(specialName) : undefined;
  const { output, callIndex } = endpointConfig
      ? getOutputForCall(endpointConfig)
      : { output: null, callIndex: 0 };

  if (output && !imposter.responses.includes(output.responseType)) {
    await updateCallCount(config, endpointConfig, specialName, callIndex);
    return res.status(404).json({ error: `ResponseType '${output.responseType}' not allowed for this imposter` });
  }

  if (output && output.responseType === 'custom' && output.customResponse) {
    await updateCallCount(config, endpointConfig, specialName, callIndex);
    return res.json(output.customResponse);
  }

  if (output && output.responseType === 'proxy') {
    await updateCallCount(config, endpointConfig, specialName, callIndex);
    return proxyToDefault(req, res, imposter);
  }

  if (
      output &&
      output.responseType &&
      output.responseType !== 'proxy' &&
      output.responseType !== 'custom'
  ) {
    await updateCallCount(config, endpointConfig, specialName, callIndex);
    const sent = sendStaticResponse(res, imposter, output);
    if (sent) return;
    else {
      return res.status(404).json({
        error: `Static response file '${output.responseType}.json' not found for group '${imposter.group}'`,
      });
    }
  }

  if (config && endpointConfig) {
    await updateCallCount(config, endpointConfig, specialName, callIndex);
  }

  return res.status(404).json({ error: 'No valid responseType or output found for this endpoint' });
});

const proxyToDefault = (req, res, imposter) => {
  let proxyTarget = 'https://some-proxy';
  if (imposter && imposter.proxyTarget) {
    proxyTarget = imposter.proxyTarget;
  }

  createProxyMiddleware({
    target: proxyTarget,
    changeOrigin: true,
  })(req, res);
};

const serverListen = () => {
  console.log(`Server running on port ${PORT}`);
};

if (require.main === module) {
  app.listen(PORT, serverListen);
}

module.exports = app;
