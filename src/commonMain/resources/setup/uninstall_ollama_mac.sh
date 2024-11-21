#!/bin/bash

echo "Starting OLLAMA uninstallation..."

# Prompt for sudo at the beginning to avoid interruptions later
echo "Requesting sudo access..."
if ! sudo -v; then
    echo "Error: Sudo access is required to proceed with the uninstallation."
    exit 1
fi

# Check architecture (Intel or Apple Silicon)
ARCH=$(uname -m)
if [[ "$ARCH" == "x86_64" ]]; then
    echo "Detected Intel processor. Proceeding with Intel-specific uninstallation."
elif [[ "$ARCH" == "arm64" ]]; then
    echo "Detected Apple Silicon processor. Proceeding with Apple Silicon-specific uninstallation."
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

# Function to stop all Ollama processes
stop_ollama() {
    echo "Stopping OLLAMA processes..."

    # Try graceful quit first
    osascript -e 'quit app "Ollama"' 2>/dev/null || echo "Warning: Unable to quit Ollama gracefully."
    sleep 2

    # Force quit if still running
    if pgrep -x "Ollama" > /dev/null; then
        echo "Forcing Ollama to quit..."
        sudo pkill -9 "Ollama" 2>/dev/null || echo "Warning: Unable to force quit Ollama."
    else
        echo "No active Ollama processes found."
    fi
}

# Function to remove models before uninstalling
remove_models() {
    echo "Removing AI models..."
    if command -v ollama >/dev/null 2>&1; then
        echo "Found ollama CLI. Removing models..."
        ollama rm llama3 2>/dev/null || echo "Warning: Unable to remove model llama3 (it may not exist)."
    else
        echo "Ollama CLI not found. Skipping model removal."
    fi
}

# Function to uninstall OLLAMA application and files
uninstall_mac() {
    echo "Removing OLLAMA installation..."

    # Remove Application
    if [ -d "/Applications/Ollama.app" ]; then
        echo "Removing Ollama.app from Applications..."
        if sudo rm -rf "/Applications/Ollama.app"; then
            echo "Ollama.app successfully removed."
        else
            echo "Error: Failed to remove /Applications/Ollama.app. Ensure you provide sudo access."
        fi
    else
        echo "Ollama.app not found in /Applications."
    fi

    # Remove symbolic link
    if [ -L "/usr/local/bin/ollama" ]; then
        echo "Removing ollama command line tool..."
        if sudo rm -f "/usr/local/bin/ollama"; then
            echo "Command line tool removed successfully."
        else
            echo "Error: Failed to remove /usr/local/bin/ollama. Ensure you provide sudo access."
        fi
    else
        echo "ollama command line tool not found."
    fi

    # Remove application support files
    echo "Removing application support files..."
    if [ -d ~/Library/Application\ Support/Ollama ]; then
        rm -rf ~/Library/Application\ Support/Ollama || echo "Error: Failed to remove ~/Library/Application Support/Ollama."
    fi

    # Remove caches
    echo "Removing caches..."
    if [ -d ~/Library/Caches/Ollama ]; then
        rm -rf ~/Library/Caches/Ollama || echo "Error: Failed to remove ~/Library/Caches/Ollama."
    fi

    # Remove preferences
    echo "Removing preferences..."
    if [ -f ~/Library/Preferences/com.ollama.Ollama.plist ]; then
        rm -f ~/Library/Preferences/com.ollama.Ollama.plist || echo "Error: Failed to remove ~/Library/Preferences/com.ollama.Ollama.plist."
    fi

    # Remove logs
    echo "Removing logs..."
    if [ -d ~/Library/Logs/Ollama ]; then
        rm -rf ~/Library/Logs/Ollama || echo "Error: Failed to remove ~/Library/Logs/Ollama."
    fi
}

# Function to clean up user data
cleanup_data() {
    echo "Cleaning up user data..."
    if [ -d ~/.ollama ]; then
        rm -rf ~/.ollama || echo "Error: Failed to remove ~/.ollama."
    else
        echo "No user data found in ~/.ollama."
    fi
}

# Ensure Ollama is completely stopped after uninstallation
finalize_stop() {
    echo "Finalizing process termination..."
    if pgrep -x "Ollama" > /dev/null; then
        echo "Forcing remaining Ollama processes to quit..."
        sudo pkill -9 "Ollama" 2>/dev/null || echo "Warning: Unable to force quit remaining Ollama processes."
    else
        echo "No remaining Ollama processes detected."
    fi
}

# Execute uninstallation steps
echo "Starting Ollama uninstallation process..."
stop_ollama
remove_models
uninstall_mac
cleanup_data
finalize_stop

echo "OLLAMA has been uninstalled successfully."
echo "Closing terminal in 10 seconds..."
for i in {10..1}; do
    echo "Uninstalled successfully. Closing terminal in $i..."
    sleep 1
done
osascript -e 'tell application "Terminal" to close first window' & exit
