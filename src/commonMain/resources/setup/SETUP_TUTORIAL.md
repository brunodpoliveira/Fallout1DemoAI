# Local LLM Setup Instructions

This game supports both online (OpenAI) and local (OLLAMA) text generation. To use the local option, follow these instructions:

## System Requirements
* Memory (RAM): At least 16GB
* Graphics Card (GPU): NVIDIA with CUDA support, 8GB or more VRAM (RTX 3000 series and better)
* Storage: At least 8GB free space

## Automatic Installation

### Windows
1. Double-click `install_ollama.bat`
2. Follow any prompts from the installer
3. Wait for the script to complete (it will download two models, which may take some time)

### Linux/Mac
1. Open Terminal
2. Navigate to the game directory
3. Run: `chmod +x install_ollama.sh`
4. Run: `./install_ollama.sh`
5. Enter your password if prompted

## Manual Installation
If the automatic scripts don't work, you can install manually:

1. Download OLLAMA from https://ollama.ai/download
2. Install and start OLLAMA
3. Open terminal/command prompt
4. Run these commands:
   ```
   ollama pull mistral
   ollama pull llama3
   ```

## Troubleshooting
* If installation fails, ensure you have admin/sudo privileges
* Check that your firewall isn't blocking OLLAMA
* Verify your system meets the minimum requirements
* For Windows users: Make sure Windows Defender isn't blocking the installer

## Support
If you encounter issues:
1. Check the game logs (enable in Options menu)
2. Visit our GitHub issues page and open a new issue
3. Join the GNUGRAF Telegram channel for community support

Remember: You can always use the online (OpenAI) option if local setup doesn't work for your system.
