$ErrorActionPreference = 'Stop'
$codificacionAnterior = $OutputEncoding
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    New-Item -ItemType Directory -Force docker/demo-data | Out-Null
    $credencial = Get-Credential -UserName 'admin@gimnasio.com' -Message 'Ingresa las credenciales ADMIN del gimnasio'
    if ($null -eq $credencial) { throw 'Carga cancelada.' }
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    @{ email = $credencial.UserName; password = $credencial.GetNetworkCredential().Password } |
        ConvertTo-Json -Compress |
        docker compose --profile demo run --rm --no-deps -T demo node seed-demo.mjs --credentials-stdin
    if ($LASTEXITCODE -ne 0) { throw 'La carga demo no termino correctamente. Revisa el mensaje anterior.' }
    Write-Host 'Accesos de prueba guardados en docker/demo-data/accounts.json'
} finally {
    $OutputEncoding = $codificacionAnterior
    Remove-Variable credencial -ErrorAction SilentlyContinue
    Pop-Location
}
