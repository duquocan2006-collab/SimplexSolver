@echo off
setlocal

echo === 1/3: Build jar (bo qua test) ===
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1

echo === 2/3: Chuan bi thu muc dong goi ===
if exist dist rmdir /s /q dist
mkdir target\jpackage-input
copy /y target\simplex-solver-1.0.0.jar target\jpackage-input\ >nul

echo === 3/3: jpackage ===
jpackage --type app-image --input target\jpackage-input --name SimplexSolver ^
  --main-jar simplex-solver-1.0.0.jar ^
  --main-class vn.edu.haui.it6095.simplex.ui.Launcher ^
  --icon icon\simplex.ico ^
  --dest dist
if errorlevel 1 exit /b 1

echo.
echo Xong! Chay thu: dist\SimplexSolver\SimplexSolver.exe
