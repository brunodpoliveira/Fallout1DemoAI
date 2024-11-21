#!/bin/bash

echo "Starting OLLAMA uninstallation..."

# Function to check if we're running with sudo
check_sudo() {
    if [ "$EUID" -ne 0 ]; then
        echo "Please run this script with sudo privileges."
        exit 1
    fi
}

# Function to stop all Ollama processes
stop_ollama() {
    echo "Stopping OLLAMA processes..."
    # Try graceful quit first
    osascript -e 'quit app "Ollama"' 2>/dev/null
    sleep 2
    # Force quit if still running
    pkill -9 ollama 2>/dev/null
    sleep 1
}

# Remove models before uninstalling
remove_models() {
    echo "Removing AI models..."
    if command -v ollama >/dev/null 2>&1; then
        ollama rm llama3 2>/dev/null
    fi
}

# Main uninstall for Mac (specific to zip installation)
uninstall_mac() {
    echo "Removing OLLAMA installation..."

    # Remove Application
    if [ -d "/Applications/Ollama.app" ]; then
        echo "Removing Ollama.app from Applications..."
        rm -rf "/Applications/Ollama.app"
    fi

    # Remove symbolic link
    if [ -L "/usr/local/bin/ollama" ]; then
        echo "Removing ollama command line tool..."
        rm -f "/usr/local/bin/ollama"
    fi

    # Remove application support files
    echo "Removing application support files..."
    rm -rf ~/Library/Application\ Support/Ollama

    # Remove caches
    echo "Removing caches..."
    rm -rf ~/Library/Caches/Ollama

    # Remove preferences
    echo "Removing preferences..."
    rm -f ~/Library/Preferences/com.ollama.Ollama.plist

    # Remove logs
    echo "Removing logs..."
    rm -rf ~/Library/Logs/Ollama
}

# Clean up user data
cleanup_data() {
    echo "Cleaning up user data..."
    rm -rf ~/.ollama
}

# Execute uninstallation
echo "Starting Ollama uninstallation process..."
check_sudo
stop_ollama
remove_models
uninstall_mac
cleanup_data

echo "OLLAMA has been uninstalled."
