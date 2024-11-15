#!/bin/bash

echo "Removing OLLAMA models and installation..."

# Remove models first
echo "Removing models..."
ollama rm llama3

# Stop OLLAMA service
echo "Stopping OLLAMA service..."
pkill ollama

if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS uninstall
    echo "Removing OLLAMA installation..."
    sudo rm -rf /usr/local/bin/ollama
    sudo rm -rf ~/.ollama
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux uninstall
    echo "Removing OLLAMA installation..."
    sudo systemctl stop ollama
    sudo systemctl disable ollama
    sudo rm -rf /usr/local/bin/ollama
    sudo rm -rf ~/.ollama
fi

echo "OLLAMA has been uninstalled."
