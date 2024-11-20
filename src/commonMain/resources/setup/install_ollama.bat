@echo off
echo Installing OLLAMA and required models...

:: Check if OLLAMA is installed
where ollama >nul 2>nul
if %errorlevel% neq 0 (
    echo OLLAMA not found. Downloading installer...
    curl -L -o OllamaSetup.exe https://ollama.com/download/OllamaSetup.exe
    echo Running installer...
    start /wait OllamaSetup.exe
    del OllamaSetup.exe
)

:: Start OLLAMA service
echo Starting OLLAMA service. Please wait...
start "" ollama serve

:: Wait for service to start
timeout /t 10 /nobreak

:: Download models
echo Downloading model (llama3)...
ollama pull llama3

echo Setup complete! OLLAMA is running with both required models.
echo You can now start the game and select 'Local LLaMA' in the options.
pause
