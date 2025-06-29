document.addEventListener('DOMContentLoaded', function() {
    const impostersContainer = document.getElementById('imposters-container');
    const togglesContainer = document.getElementById('toggles-container');
    const saveButton = document.getElementById('saveButton');
    const resultContainer = document.getElementById('resultContainer');
    const mockPrefixElement = document.getElementById('mockPrefix');

    // State
    let imposters = [];
    let toggles = [];
    let selectedResponses = {};
    let customResponses = {};
    let toggleValues = {};

    // API base path
    const API_BASE = '/api/ui';

    // Fetch imposters from the backend
    function fetchImposters() {
        fetch(`${API_BASE}/imposters`)
            .then(response => response.json())
            .then(data => {
                imposters = data;
                renderImposters();
            })
            .catch(error => {
                console.error('Error fetching imposters:', error);
                impostersContainer.innerHTML = '<div class="error">Error loading imposters. Please try again.</div>';
            });
    }

    function fetchToggles() {
        fetch(`${API_BASE}/toggles`)
            .then(response => response.json())
            .then(data => {
                toggles = data;
                renderToggles();
            })
            .catch(error => {
                console.error('Error fetching toggles:', error);
                togglesContainer.innerHTML = '<div class="error">Error loading toggles. Please try again.</div>';
            });
    }

    function renderImposters() {
        impostersContainer.innerHTML = '';

        const groupedImposters = imposters.reduce((acc, imposter) => {
            if (!acc[imposter.group]) {
                acc[imposter.group] = [];
            }
            acc[imposter.group].push(imposter);
            return acc;
        }, {});

        Object.entries(groupedImposters).forEach(([group, groupImposters]) => {
            const groupDiv = document.createElement('div');
            groupDiv.className = 'imposter-group';

            const groupHeader = document.createElement('h3');
            groupHeader.className = 'group-header';
            groupHeader.textContent = group.toUpperCase();
            groupDiv.appendChild(groupHeader);

            groupImposters.forEach(imposter => {
                const imposterDiv = document.createElement('div');
                imposterDiv.className = 'imposter-item';

                const imposterName = document.createElement('div');
                imposterName.className = 'imposter-name';
                imposterName.textContent = `${imposter.method} ${imposter.pattern}`;
                imposterDiv.appendChild(imposterName);

                const responseOptions = document.createElement('div');
                responseOptions.className = 'response-options';

                if (!selectedResponses[imposter.specialName]) {
                    selectedResponses[imposter.specialName] = 'proxy';
                }

                imposter.responses.forEach(response => {
                    const radioGroup = document.createElement('div');
                    radioGroup.className = 'radio-group';

                    const radioInput = document.createElement('input');
                    radioInput.type = 'radio';
                    radioInput.name = `response-${imposter.specialName}`;
                    radioInput.id = `${imposter.specialName}-${response}`;
                    radioInput.value = response;
                    radioInput.checked = selectedResponses[imposter.specialName] === response;

                    radioInput.addEventListener('change', () => {
                        selectedResponses[imposter.specialName] = response;

                        const customContainer = document.getElementById(`custom-container-${imposter.specialName}`);
                        if (customContainer) {
                            if (response === 'custom') {
                                customContainer.classList.add('active');
                            } else {
                                customContainer.classList.remove('active');
                            }
                        }
                    });

                    const radioLabel = document.createElement('label');
                    radioLabel.htmlFor = `${imposter.specialName}-${response}`;
                    radioLabel.textContent = response;

                    radioGroup.appendChild(radioInput);
                    radioGroup.appendChild(radioLabel);
                    responseOptions.appendChild(radioGroup);
                });

                imposterDiv.appendChild(responseOptions);

                const customContainer = document.createElement('div');
                customContainer.className = 'custom-response-container';
                customContainer.id = `custom-container-${imposter.specialName}`;

                if (selectedResponses[imposter.specialName] === 'custom') {
                    customContainer.classList.add('active');
                }

                const customLabel = document.createElement('label');
                customLabel.htmlFor = `custom-response-${imposter.specialName}`;
                customLabel.textContent = 'Custom Response (JSON):';

                const customTextarea = document.createElement('textarea');
                customTextarea.className = 'custom-response-input';
                customTextarea.id = `custom-response-${imposter.specialName}`;
                customTextarea.placeholder = '{\n  "status": 200,\n  "body": { "message": "Custom response" },\n  "headers": { "Content-Type": "application/json" }\n}';

                if (customResponses[imposter.specialName]) {
                    customTextarea.value = customResponses[imposter.specialName];
                }

                customTextarea.addEventListener('input', (e) => {
                    customResponses[imposter.specialName] = e.target.value;
                });

                customContainer.appendChild(customLabel);
                customContainer.appendChild(customTextarea);
                imposterDiv.appendChild(customContainer);

                groupDiv.appendChild(imposterDiv);
            });

            impostersContainer.appendChild(groupDiv);
        });
    }

    function renderToggles() {
        togglesContainer.innerHTML = '';

        toggles.forEach(toggle => {
            if (toggleValues[toggle.name] === undefined) {
                toggleValues[toggle.name] = toggle.enabled;
            }

            const toggleDiv = document.createElement('div');
            toggleDiv.className = 'toggle-item';

            const toggleName = document.createElement('div');
            toggleName.className = 'toggle-name';
            toggleName.textContent = toggle.name;

            const toggleControls = document.createElement('div');
            toggleControls.className = 'toggle-controls';

            ['true', 'false'].forEach(value => {
                const radioGroup = document.createElement('div');
                radioGroup.className = 'radio-group';

                const radioInput = document.createElement('input');
                radioInput.type = 'radio';
                radioInput.name = `toggle-${toggle.name}`;
                radioInput.id = `${toggle.name}-${value}`;
                radioInput.value = value;
                radioInput.checked = toggleValues[toggle.name] === (value === 'true');

                radioInput.addEventListener('change', () => {
                    toggleValues[toggle.name] = value === 'true';
                });

                const radioLabel = document.createElement('label');
                radioLabel.htmlFor = `${toggle.name}-${value}`;
                radioLabel.textContent = value;

                radioGroup.appendChild(radioInput);
                radioGroup.appendChild(radioLabel);
                toggleControls.appendChild(radioGroup);
            });

            toggleDiv.appendChild(toggleName);
            toggleDiv.appendChild(toggleControls);
            togglesContainer.appendChild(toggleDiv);
        });
    }

    function saveConfiguration() {
        const endpointConfigs = {};

        Object.entries(selectedResponses).forEach(([specialName, responseType]) => {
            const outputs = [{
                type: responseType,
                customResponse: responseType === 'custom' ? parseCustomResponse(specialName) : null
            }];

            endpointConfigs[specialName] = outputs;
        });

        const requestData = {
            endpointConfigs: endpointConfigs,
            toggles: toggleValues
        };

        fetch(`${API_BASE}/config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
        .then(response => response.json())
        .then(data => {
            mockPrefixElement.textContent = data.mockPrefix;
            resultContainer.classList.remove('hidden');

            resultContainer.scrollIntoView({ behavior: 'smooth' });
        })
        .catch(error => {
            console.error('Error saving configuration:', error);
            alert('Error saving configuration. Please try again.');
        });
    }

    function parseCustomResponse(specialName) {
        const customResponseText = customResponses[specialName];
        if (!customResponseText) return null;

        try {
            return JSON.parse(customResponseText);
        } catch (error) {
            console.error('Invalid JSON in custom response:', error);
            return null;
        }
    }

    saveButton.addEventListener('click', saveConfiguration);

    fetchImposters();
    fetchToggles();
});
