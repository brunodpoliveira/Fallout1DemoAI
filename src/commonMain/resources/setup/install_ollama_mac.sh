#!/bin/bash
echo "Installing OLLAMA and required models..."

# Check OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    if ! command -v ollama &> /dev/null; then
        echo "Installing OLLAMA for macOS..."
        # Create temporary directory
        TEMP_DIR=$(mktemp -d)
        cd "$TEMP_DIR" || exit

        # Download and extract
        echo "Downloading Ollama..."
        curl -L -o Ollama.zip "https://ollama.com/download/Ollama-darwin.zip"

        echo "Extracting..."
        unzip Ollama.zip

        echo "Installing..."
        # Move to Applications folder
        sudo mv Ollama.app /Applications/

        # Create symbolic link for CLI access
        sudo ln -sf /Applications/Ollama.app/Contents/Resources/ollama /usr/local/bin/ollama

        # Cleanup
        cd - > /dev/null || exit
        rm -rf "$TEMP_DIR"

        echo "Installation completed."
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if ! command -v ollama &> /dev/null; then
        echo "Installing OLLAMA for Linux..."
        curl -fsSL https://ollama.ai/install.sh | sh
    fi
fi

# Start OLLAMA
echo "Starting OLLAMA service..."
# For macOS, we first ensure the app is running
if [[ "$OSTYPE" == "darwin"* ]]; then
    open -a Ollama
    sleep 3  # Give the app time to start
fi

ollama serve &

# Wait for service to start
sleep 5

# Download models
echo "Downloading roleplay model (llama3)..."
ollama pull llama3

echo "Setup complete! OLLAMA is running with both required models."
echo "You can now start the game and select 'Local LLaMA' in the options."
