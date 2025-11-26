# Script de build complet pour genj
# Compile en mode release et crée le package ZIP

param(
    [string]$ConfigPath = "Cargo.toml"
)

Write-Host "[*] Build genj en mode release avec package ZIP..." -ForegroundColor Cyan

# Compiler en mode release
Write-Host "[*] Compilation en cours..." -ForegroundColor Yellow
cargo build --release

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] La compilation a echoue" -ForegroundColor Red
    exit 1
}

Write-Host "[+] Compilation reussie" -ForegroundColor Green

# Exécuter le binaire de création de package
Write-Host "[*] Creation du package ZIP..." -ForegroundColor Yellow
cargo run --release --bin build-package

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] La creation du package a echoue" -ForegroundColor Red
    exit 1
}

Write-Host "[+] Build termine avec succes !" -ForegroundColor Green
