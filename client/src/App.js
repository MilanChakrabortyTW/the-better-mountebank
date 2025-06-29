import React, { useEffect, useState } from "react";
import axios from "axios";
import "./App.css";

const DEFAULT_PROXY = "https://some-proxy";

const OutputTypeRadio = ({
  outputType,
  checked,
  onChange,
  proxyTarget,
  showCustomInput,
  customValue,
  onCustomChange,
}) => (
  <div className="bm-radio-row">
    <label className="bm-radio-label">
      <input
        type="radio"
        name="outputType"
        value={outputType}
        checked={checked}
        onChange={onChange}
      />
      {outputType === "proxy"
        ? `Proxy (${proxyTarget || DEFAULT_PROXY})`
        : outputType.charAt(0).toUpperCase() + outputType.slice(1)}
    </label>
    {outputType === "custom" && checked && (
      <textarea
        className="bm-custom-input"
        placeholder={"{\n  \"status\": 200,\n  \"body\": {}\n}"}
        value={customValue}
        onChange={onCustomChange}
        rows={5}
      />
    )}
  </div>
);

const OutputBlock = ({
  output,
  availableTypes,
  proxyTarget,
  onChange,
  onCustomChange,
  onRemove,
  index,
  total,
}) => (
  <div className="bm-output-block">
    <div className="bm-output-header">
      <span>Output {index + 1}</span>
      {total > 1 && (
        <button className="bm-remove-btn" onClick={onRemove} title="Remove this output">×</button>
      )}
    </div>
    <div className="bm-radio-group">
      {availableTypes.map((type) => (
        <OutputTypeRadio
          key={type}
          outputType={type}
          checked={output.responseType === type}
          onChange={() => onChange(type)}
          proxyTarget={proxyTarget}
          showCustomInput={type === "custom"}
          customValue={output.customResponse || ""}
          onCustomChange={onCustomChange}
        />
      ))}
    </div>
  </div>
);

const EndpointSection = ({ endpoint, config, setConfig }) => {
  const outputs = config.outputs || [
    { responseType: endpoint.responses[0], customResponse: "" },
  ];

  const handleOutputTypeChange = (idx, type) => {
    const newOutputs = outputs.map((o, i) =>
      i === idx ? { responseType: type, customResponse: "" } : o
    );
    setConfig({ ...config, outputs: newOutputs });
  };

  const handleCustomChange = (idx, e) => {
    const newOutputs = outputs.map((o, i) =>
      i === idx ? { ...o, customResponse: e.target.value } : o
    );
    setConfig({ ...config, outputs: newOutputs });
  };

  const handleAddOutput = () => {
    setConfig({ ...config, outputs: [...outputs, { responseType: endpoint.responses[0], customResponse: "" }] });
  };

  const handleRemoveOutput = (idx) => {
    setConfig({ ...config, outputs: outputs.filter((_, i) => i !== idx) });
  };

  return (
    <div className="bm-endpoint-section">
      <div className="bm-endpoint-header">
        <div>
          <span className="bm-endpoint-regex">{endpoint.regex.toString()}</span>
          <span className="bm-endpoint-special">({endpoint.specialName})</span>
        </div>
        <button className="bm-add-btn" onClick={handleAddOutput} title="Add Output">+</button>
      </div>
      {outputs.map((output, idx) => (
        <OutputBlock
          key={idx}
          output={output}
          availableTypes={[...endpoint.responses, "proxy", "custom"]}
          proxyTarget={endpoint.proxyTarget}
          onChange={(type) => handleOutputTypeChange(idx, type)}
          onCustomChange={(e) => handleCustomChange(idx, e)}
          onRemove={() => handleRemoveOutput(idx)}
          index={idx}
          total={outputs.length}
        />
      ))}
    </div>
  );
};

function App() {
  const [endpoints, setEndpoints] = useState([]);
  const [configs, setConfigs] = useState({});
  const [lanPrefix, setLanPrefix] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    axios.get("/imposters").then((res) => {
      setEndpoints(res.data);
      const initialConfigs = {};
      res.data.forEach((ep) => {
        initialConfigs[ep.specialName] = { outputs: [{ responseType: ep.responses[0], customResponse: "" }] };
      });
      setConfigs(initialConfigs);
    });
  }, []);

  const handleConfigChange = (specialName, newConfig) => {
    setConfigs((prev) => ({ ...prev, [specialName]: newConfig }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await axios.post("/config/new", { configs });
      setLanPrefix(res.data.lanPrefix);
    } catch (e) {
      alert("Failed to save config");
    }
    setSaving(false);
  };

  return (
    <div className="bm-root">
      <header className="bm-header">
        <span className="bm-title">The Better Mountebank</span>
        <span className="bm-by">By Milan</span>
      </header>
      <main className="bm-main">
        {endpoints.map((ep) => (
          <EndpointSection
            key={ep.specialName}
            endpoint={ep}
            config={configs[ep.specialName] || { outputs: [] }}
            setConfig={(newConfig) => handleConfigChange(ep.specialName, newConfig)}
          />
        ))}
        <button className="bm-save-btn" onClick={handleSave} disabled={saving}>
          {saving ? "Saving..." : "Save Config & Get LanPrefix"}
        </button>
        {lanPrefix && (
          <div className="bm-lanprefix">LanPrefix: <b>{lanPrefix}</b></div>
        )}
      </main>
    </div>
  );
}

export default App;
