@echo off
setlocal
chcp 65001 >nul

echo === 1/3: Build jar (bo qua test) ===
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1

echo === 2/3: Chuan bi thu muc dong goi ===
if exist dist-installer rmdir /s /q dist-installer
mkdir target\jpackage-input
copy /y target\simplex-solver-1.0.0.jar target\jpackage-input\ >nul

echo === 3/3: Tao bo cai dat (.exe) co shortcut + logo ===
jpackage --type exe --input target\jpackage-input --name "SimplexSolver" ^
  --main-jar simplex-solver-1.0.0.jar ^
  --main-class vn.edu.haui.it6095.simplex.ui.Launcher ^
  --icon icon\simplex.ico ^
  --app-version 1.0.0 ^
  --description "Phan mem giai Quy hoach tuyen tinh bang phuong phap don hinh" ^
  --vendor "QuocAn" ^
  --win-shortcut --win-menu --win-menu-group "Simplex Solver" --win-dir-chooser ^
  --win-upgrade-uuid 6f1c2d7e-3b54-4a8e-9c1f-2d5a7e9b4c10 ^
  --dest dist-installer
if errorlevel 1 (
  echo.
  echo LOI: kiem tra da cai WiX Toolset va them vao PATH chua.
  exit /b 1
)

echo.
echo Xong! File cai dat nam trong thu muc dist-installer\
echo Gui file SimplexSolver-1.0.0.exe cho nguoi khac de cai.
