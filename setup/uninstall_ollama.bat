@echo off
echo Removing OLLAMA models and installation...

:: Remove models
echo Removing models...
ollama rm llama3

:: Stop OLLAMA service
echo Stopping OLLAMA service...
taskkill /F /IM ollama.exe

:: Remove OLLAMA installation
echo Removing OLLAMA installation...
"%ProgramFiles%\Ollama\uninstall.exe" /S

:: Remove OLLAMA data
echo Removing OLLAMA data...
rd /s /q "%USERPROFILE%\.ollama"

echo OLLAMA has been uninstalled.
pause
