@echo off
rem compile the back end and start the menu (windows)
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\tokentally\app\TokenTallyApp.java
if errorlevel 1 exit /b 1
java -cp out tokentally.app.TokenTallyApp %*
