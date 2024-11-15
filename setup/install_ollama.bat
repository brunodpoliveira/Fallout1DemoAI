@echo off
echo Installing OLLAMA and required models...
# install_ollama.bat (Windows)

:: Check if OLLAMA is installed
where ollama >nul 2>nul
if %errorlevel% neq 0 (
    echo OLLAMA not found. Downloading installer...
    curl -L -o ollama-installer.exe https://ollama.ai/download/windows
    echo Running installer...
    start /wait ollama-installer.exe
    del ollama-installer.exe
)

:: Start OLLAMA service
echo Starting OLLAMA service...
start "" ollama serve

:: Wait for service to start
timeout /t 5 /nobreak

:: Download models
echo Downloading model (llama3)...
ollama pull llama3

echo Setup complete! OLLAMA is running with both required models.
echo You can now start the game and select 'Local LLaMA' in the options.
pause
