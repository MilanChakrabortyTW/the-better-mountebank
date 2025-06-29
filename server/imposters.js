const bpm = require('./bpm/api');

const imposters = [
  {
    regex: new RegExp('^/api/v1/bpm$'),
    method: 'POST',
    specialName: 'bpm',
    responses: bpm.responses,
    group: 'bpm',
    proxyTarget: 'https://some-proxy',
    apiModule: bpm,
  },
];

const getImposterByUrl = (url, method) => {
  return imposters.find(i => {
    try {
      return i.regex.test(url) && (!i.method || i.method === method);
    } catch (e) {
      return false;
    }
  });
};

const getSpecialNameByUrl = (url) => {
  const imposter = getImposterByUrl(url);
  return imposter ? imposter.specialName : null;
};

const getImpostersForApi = () =>
  imposters.map(({ regex, ...rest }) => ({
    regex: regex.toString(),
    ...rest,
  }));

module.exports = {
  imposters,
  getImposterByUrl,
  getSpecialNameByUrl,
  getImpostersForApi,
};
