#!/bin/bash
echo "Installing OLLAMA and required models..."

# Check OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    if ! command -v ollama &> /dev/null; then
        echo "Installing OLLAMA for macOS..."
        curl -fsSL https://ollama.com/download/Ollama-darwin.zip | sh
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if ! command -v ollama &> /dev/null; then
        echo "Installing OLLAMA for Linux..."
        curl -fsSL https://ollama.com/install.sh | sh
    fi
fi

# Start OLLAMA
echo "Starting OLLAMA service..."
ollama serve &

# Wait for service to start
sleep 5

# Download models
echo "Downloading roleplay model (llama3)..."
ollama pull llama3

echo "Setup complete! OLLAMA is running with both required models."
echo "You can now start the game and select 'Local LLaMA' in the options."
