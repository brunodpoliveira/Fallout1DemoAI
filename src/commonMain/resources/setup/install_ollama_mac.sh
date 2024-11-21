#!/bin/bash
echo "Installing OLLAMA and required models..."

# Prompt for sudo upfront
echo "Requesting sudo access..."
if ! sudo -v; then
    echo "Error: Sudo access is required to proceed with the installation."
    exit 1
fi

# Check OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    if ! command -v ollama &> /dev/null; then
        echo "Installing OLLAMA for macOS..."
        # Create temporary directory
        TEMP_DIR=$(mktemp -d)
        cd "$TEMP_DIR" || exit 1

        # Download and extract
        echo "Downloading Ollama..."
        if ! curl -L -o Ollama.zip "https://ollama.com/download/Ollama-darwin.zip"; then
            echo "Error: Failed to download Ollama.zip"
            exit 1
        fi

        echo "Extracting..."
        if ! unzip Ollama.zip; then
            echo "Error: Failed to extract Ollama.zip"
            exit 1
        fi

        echo "Installing..."
        # Move to Applications folder
        if ! sudo mv Ollama.app /Applications/; then
            echo "Error: Failed to move Ollama.app to /Applications/"
            exit 1
        fi

        # Create symbolic link for CLI access
        if ! sudo ln -sf /Applications/Ollama.app/Contents/Resources/ollama /usr/local/bin/ollama; then
            echo "Error: Failed to create symbolic link for Ollama CLI."
            exit 1
        fi

        # Cleanup
        cd - > /dev/null || exit 1
        rm -rf "$TEMP_DIR"

        echo "Installation completed."
    else
        echo "OLLAMA already installed."
    fi
else
    echo "Unsupported OS: $OSTYPE"
    exit 1
fi

# Force stop any running Ollama processes
echo "Ensuring no conflicting OLLAMA processes are running..."
if pgrep -f Ollama.app > /dev/null; then
    echo "Stopping existing OLLAMA processes..."
    pkill -f Ollama.app || echo "Warning: Unable to stop some OLLAMA processes."
    sleep 2
fi

# Start Ollama application
echo "Starting OLLAMA service..."
open -a /Applications/Ollama.app
sleep 5

# Wait for Ollama to fully initialize
for i in {1..12}; do
    if pgrep -f Ollama.app > /dev/null; then
        echo "OLLAMA application has started."
        break
    fi
    echo "Waiting for OLLAMA application to start... ($i/12)"
    sleep 5
done

if ! pgrep -f Ollama.app > /dev/null; then
    echo "Error: Failed to start Ollama application."
    exit 1
fi

# Download models
echo "Downloading roleplay model (llama3)..."
if ! ollama pull llama3; then
    echo "Error: Failed to download roleplay model llama3. Forcing service restart and retrying..."
    # Force restart and retry
    pkill -f Ollama.app || echo "Warning: Unable to stop some OLLAMA processes."
    open -a /Applications/Ollama.app
    sleep 10
    if ! ollama pull llama3; then
        echo "Error: Retried and failed to download roleplay model llama3."
        exit 1
    fi
fi

echo "Setup complete! OLLAMA is running with both required models."
echo "You can now start the game and select 'Local LLaMA' in the options."
echo "Closing terminal in 10 seconds..."
for i in {10..1}; do
    echo "Installation completed successfully. Closing terminal in $i..."
    sleep 1
done
osascript -e 'tell application "Terminal" to close first window' & exit
