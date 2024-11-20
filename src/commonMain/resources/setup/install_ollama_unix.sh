#!/bin/bash
echo "Installing OLLAMA and required models..."

# Install OLLAMA if not already installed
if ! command -v ollama &> /dev/null; then
    echo "Installing OLLAMA..."
    curl -fsSL https://ollama.com/install.sh | sh
fi

# Start OLLAMA service
echo "Starting OLLAMA service..."
ollama serve &

# Wait for service to start
sleep 5

# Download models
echo "Downloading roleplay model (llama3)..."
ollama pull llama3

echo "Setup complete! OLLAMA is running with both required models."
echo "You can now start the game and select 'Local LLaMA' in the options."
