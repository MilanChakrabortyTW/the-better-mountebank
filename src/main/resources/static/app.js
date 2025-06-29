document.addEventListener('DOMContentLoaded', function() {
    const impostersContainer = document.getElementById('imposters-container');
    const togglesContainer = document.getElementById('toggles-container');
    const saveButton = document.getElementById('saveButton');
    const resultContainer = document.getElementById('resultContainer');
    const mockPrefixElement = document.getElementById('mockPrefix');

    let imposters = [];
    let toggles = [];
    let selectedResponses = {};
    let customResponses = {};
    let toggleValues = {};

    const API_BASE = '/api/ui';

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

        Object.keys(groupedImposters).forEach(group => {
            const groupContainer = document.createElement('div');
            groupContainer.className = 'group-container';
            groupContainer.innerHTML = `<h3>${group}</h3>`;

            groupedImposters[group].forEach(imposter => {
                const imposterElement = document.createElement('div');
                imposterElement.className = 'imposter';
                imposterElement.innerHTML = `
                    <p>${imposter.specialName}</p>
                    <div class="response-options">
                        ${imposter.responses.map(response => `
                            <label class="radio-group">
                                <input type="radio" name="response-${imposter.specialName}" value="${response}" ${response === 'proxy' ? 'checked' : ''}>
                                ${response}
                            </label>
                        `).join('')}
                        <div class="custom-response-container" id="custom-response-${imposter.specialName}" style="display: none;">
                            <textarea class="custom-response-input" placeholder="Enter custom response here..."></textarea>
                        </div>
                    </div>
                    <button class="add-output-btn" id="add-output-${imposter.specialName}">Add Output</button>
                    <div class="additional-outputs" id="additional-outputs-${imposter.specialName}"></div>
                `;

                // Add event listener for radio buttons
                imposterElement.querySelectorAll(`input[name="response-${imposter.specialName}"]`).forEach(radio => {
                    radio.addEventListener('change', (event) => {
                        const customResponseContainer = document.getElementById(`custom-response-${imposter.specialName}`);
                        if (event.target.value === 'custom') {
                            customResponseContainer.style.display = 'block';
                        } else {
                            customResponseContainer.style.display = 'none';
                        }
                    });
                });

                // Add event listener for Add Output button
                const addOutputButton = imposterElement.querySelector(`#add-output-${imposter.specialName}`);
                addOutputButton.addEventListener('click', () => {
                    const additionalOutputsContainer = document.getElementById(`additional-outputs-${imposter.specialName}`);
                    const newOutput = document.createElement('div');
                    newOutput.className = 'additional-output';
                    newOutput.innerHTML = `
                        <div class="response-options">
                            ${imposter.responses.map(response => `
                                <label class="radio-group">
                                    <input type="radio" name="additional-response-${imposter.specialName}-${Date.now()}" value="${response}" ${response === 'proxy' ? 'checked' : ''}>
                                    ${response}
                                </label>
                            `).join('')}
                        </div>
                        <div class="custom-response-container" style="display: none;">
                            <textarea class="custom-response-input" placeholder="Enter additional custom response here..."></textarea>
                        </div>
                        <button class="remove-output-btn">Remove</button>
                    `;

                    // Add event listener for radio buttons in the new output
                    newOutput.querySelectorAll(`input[name^="additional-response-${imposter.specialName}"]`).forEach(radio => {
                        radio.addEventListener('change', (event) => {
                            const customResponseContainer = newOutput.querySelector('.custom-response-container');
                            if (event.target.value === 'custom') {
                                customResponseContainer.style.display = 'block';
                            } else {
                                customResponseContainer.style.display = 'none';
                            }
                        });
                    });

                    // Add event listener for Remove button
                    newOutput.querySelector('.remove-output-btn').addEventListener('click', () => {
                        newOutput.remove();
                    });

                    additionalOutputsContainer.appendChild(newOutput);
                });

                groupContainer.appendChild(imposterElement);
            });

            impostersContainer.appendChild(groupContainer);
        });
    }

    function renderToggles() {
        togglesContainer.innerHTML = '';

        toggles.forEach(toggle => {
            const toggleElement = document.createElement('div');
            toggleElement.className = 'toggle';
            toggleElement.innerHTML = `
                <p>${toggle.name}</p>
                <input type="checkbox" name="toggle-${toggle.name}" ${toggle.enabled ? 'checked' : ''}>
            `;
            togglesContainer.appendChild(toggleElement);
        });
    }

    saveButton.addEventListener('click', function() {
        const configRequest = {
            endpointConfigs: {},
            toggles: {}
        };

        imposters.forEach(imposter => {
            const outputs = [];

            // Collect the main response
            const selectedResponse = document.querySelector(`input[name="response-${imposter.specialName}"]:checked`).value;
            const customResponseInput = document.getElementById(`custom-response-${imposter.specialName}`)?.querySelector('textarea');
            outputs.push({
                type: selectedResponse,
                customResponse: selectedResponse === 'custom' ? customResponseInput?.value : null
            });

            const additionalOutputsContainer = document.getElementById(`additional-outputs-${imposter.specialName}`);
            additionalOutputsContainer.querySelectorAll('.additional-output').forEach(outputElement => {
                const additionalSelectedResponse = outputElement.querySelector('input[type="radio"]:checked').value;
                const additionalCustomResponseInput = outputElement.querySelector('.custom-response-input');
                outputs.push({
                    type: additionalSelectedResponse,
                    customResponse: additionalSelectedResponse === 'custom' ? additionalCustomResponseInput?.value : null
                });
            });

            configRequest.endpointConfigs[imposter.specialName] = outputs;
        });

        toggles.forEach(toggle => {
            const toggleCheckbox = document.querySelector(`input[name="toggle-${toggle.name}"]`);
            if (toggleCheckbox) {
                configRequest.toggles[toggle.name] = toggleCheckbox.checked;
            }
        });

        fetch(`${API_BASE}/config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(configRequest)
        })
        .then(response => response.json())
        .then(data => {
            resultContainer.classList.remove('hidden');
            mockPrefixElement.textContent = data.mockPrefix;
        })
        .catch(error => {
            console.error('Error saving configuration:', error);
            alert('Failed to save configuration. Please try again.');
        });
    });

    fetchImposters();
    fetchToggles();
});
