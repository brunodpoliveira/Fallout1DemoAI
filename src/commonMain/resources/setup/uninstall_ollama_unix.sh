#!/bin/bash
echo "Starting OLLAMA uninstallation..."

# Remove the ollama service
echo "Removing Ollama service..."
sudo systemctl stop ollama
sudo systemctl disable ollama
sudo rm /etc/systemd/system/ollama.service

# Remove the ollama binary
echo "Removing Ollama binary..."
sudo rm "$(which ollama)"

# Remove downloaded models and service user/group
echo "Removing models and service user/group..."
sudo rm -r /usr/share/ollama
sudo userdel ollama
sudo groupdel ollama

echo "OLLAMA has been uninstalled."
