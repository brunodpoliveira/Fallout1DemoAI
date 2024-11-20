#!/bin/bash

echo "Starting OLLAMA uninstallation..."

# Check if we're running with sudo
if [ "$EUID" -ne 0 ]; then
    echo "Please run this script with sudo privileges."
    exit 1
fi

# Check if this is Linux
if [[ "$OSTYPE" != "linux-gnu"* ]]; then
    echo "This script is for Linux only. For macOS, please use the alternative uninstall script."
    exit 1
fi

echo "Stopping and removing Ollama service..."
systemctl stop ollama
systemctl disable ollama
rm /etc/systemd/system/ollama.service

echo "Removing Ollama binary..."
rm "$(which ollama)"

echo "Removing downloaded models and Ollama service user/group..."
rm -r /usr/share/ollama
userdel ollama
groupdel ollama

echo "OLLAMA has been uninstalled following official uninstallation procedure."
