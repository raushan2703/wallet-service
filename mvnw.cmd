@echo off
rem Maven Wrapper script for Windows
setlocal

set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6

if not exist "%MAVEN_HOME%" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_HOME%'"
    del "%TEMP%\maven.zip"
)

for /r "%MAVEN_HOME%" %%f in (mvn.cmd) do set MAVEN_BIN=%%f

if "%MAVEN_BIN%"=="" (
    echo Error: Maven binary not found
    exit /b 1
)

"%MAVEN_BIN%" %*
