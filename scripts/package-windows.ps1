$ErrorActionPreference = "Stop"

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Version = "1.0.0"
$AppName = "app-javafx-test"
$JarName = "gestion-dossiers-desktop-$Version.jar"
$InputDir = Join-Path $ProjectDir "target\jpackage-input"
$OutputDir = Join-Path $ProjectDir "target\dist\windows"
$JarPath = Join-Path $ProjectDir "target\$JarName"
$IconPath = Join-Path $ProjectDir "icon\ic_1.ico"

Push-Location $ProjectDir
try {
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        throw "La compilation Maven a échoué."
    }

    if (-not (Test-Path $JarPath)) {
        throw "JAR compilé introuvable : $JarPath"
    }
    if (-not (Test-Path $IconPath)) {
        throw "Icône Windows introuvable : $IconPath"
    }
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
        throw "WiX Toolset 3 doit être installé et candle.exe accessible dans le PATH pour créer un EXE."
    }

    Remove-Item $InputDir, $OutputDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item $InputDir, $OutputDir -ItemType Directory -Force | Out-Null
    Copy-Item $JarPath $InputDir

    jpackage `
        --type exe `
        --name $AppName `
        --app-version $Version `
        --vendor "PRMP" `
        --input $InputDir `
        --main-jar $JarName `
        --main-class com.ministere.dossiers.AppMain `
        --icon $IconPath `
        --dest $OutputDir `
        --win-menu `
        --win-shortcut `
        --win-dir-chooser `
        --win-per-user-install
    if ($LASTEXITCODE -ne 0) {
        throw "La création de l'installateur Windows a échoué."
    }

    Write-Host "Installateur Windows créé dans $OutputDir"
}
finally {
    Pop-Location
}
