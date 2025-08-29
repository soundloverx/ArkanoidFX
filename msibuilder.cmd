@echo off
setlocal EnableExtensions EnableDelayedExpansion

echo Extracting project.version from pom.xml via PowerShell
for /f "usebackq delims=" %%v in (`
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$xml=[xml](Get-Content 'pom.xml');" ^
    "$ns=New-Object System.Xml.XmlNamespaceManager($xml.NameTable);" ^
    "$ns.AddNamespace('m',$xml.DocumentElement.NamespaceURI);" ^
    "$n=$xml.SelectSingleNode('/m:project/m:version',$ns);" ^
    "if ($n -ne $null) { $n.InnerText }"
`) do set "APP_VERSION=%%v"

if not defined APP_VERSION (
  echo Failed to obtain project.version from pom.xml
  exit /b 1
)

echo Generating list of modules from dependencies
for /f "delims=" %%m in ('
  jdeps --multi-release 21 --ignore-missing-deps --print-module-deps ^
        --class-path target\lib\* ^
        target\arkanoidfx.jar
') do set MODS=%%m

echo Linking modules
jlink ^
  --module-path "%JAVA_HOME%jmods;%FXJMODS%;target/lib" ^
  --add-modules %MODS%,javafx.base,javafx.graphics,javafx.controls,javafx.media ^
  --strip-debug --no-header-files --no-man-pages --compress=zip-9 ^
  --output build\runtime

echo Isolating the necessary files
set INPUT_DIR=build\pkg-input
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%" || exit /b 1

copy /y target\arkanoidfx.jar "%INPUT_DIR%" >nul
robocopy target\lib "%INPUT_DIR%\lib" /e >nul

echo Packaging installer
jpackage ^
  --type msi ^
  --name "ArkanoidFX" ^
  --vendor "Gamesti" ^
  --app-version ""%APP_VERSION%"" ^
  --win-upgrade-uuid 34de898e-53de-4cf1-8e34-a48d2b62dd5d ^
  --input "%INPUT_DIR%" ^
  --main-jar arkanoidfx.jar ^
  --main-class org.overb.arkanoidfx.ArkanoidApp ^
  --runtime-image build\runtime ^
  --win-menu ^
  --win-shortcut ^
  --win-per-user-install


rmdir /s /q build
echo Installer package created: ArkanoidFX-%APP_VERSION%.msi
