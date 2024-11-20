@echo off
echo Starting OLLAMA uninstallation...

:: Locate and run the uninstaller
set "UNINSTALLER=%LOCALAPPDATA%\Programs\Ollama\unins000.exe"

if exist "%UNINSTALLER%" (
    echo Found Ollama uninstaller. Starting uninstallation...
    start /wait "" "%UNINSTALLER%"
    echo.
    echo OLLAMA has been uninstalled.
) else (
    echo Ollama uninstaller not found at: %UNINSTALLER%
    echo Please check if Ollama is installed correctly.
)

pause
