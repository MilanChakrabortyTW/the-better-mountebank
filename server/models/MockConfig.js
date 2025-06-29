const mongoose = require('mongoose');

const OutputSchema = new mongoose.Schema({
  responseType: { type: String, default: 'proxy' },
  customResponse: { type: mongoose.Schema.Types.Mixed, default: null },
});

const EndpointConfigSchema = new mongoose.Schema({
  specialName: { type: String, required: true },
  outputs: {
    type: [OutputSchema],
    required: true,
    validate: v => Array.isArray(v) && v.length > 0,
  },
  callCount: { type: Number, default: 0 },
});

const MockConfigSchema = new mongoose.Schema({
  mockPrefix: { type: String, required: true, unique: true },
  configs: {
    type: Map,
    of: EndpointConfigSchema,
    default: {},
  },
  toggles: {
    type: Array,
    default: [],
  },
});

const MockConfig = mongoose.model('MockConfig', MockConfigSchema);

module.exports = MockConfig;

