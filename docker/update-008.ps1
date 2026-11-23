$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
function Invoke-DockerStep([scriptblock] $Action) {
    & $Action
    if ($LASTEXITCODE -ne 0) { throw 'El comando Docker fallo. No continuar sin revisar el error anterior.' }
}
try {
    $backupName = 'gimnasio-antes-008-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql'
    New-Item -ItemType Directory -Force docker/backups | Out-Null
    Invoke-DockerStep { docker compose stop frontend backend }
    Invoke-DockerStep { docker compose exec -T db sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces -r /tmp/antes-008.sql $MYSQL_DATABASE' }
    Invoke-DockerStep { docker compose cp db:/tmp/antes-008.sql "docker/backups/$backupName" }
    if ((Get-Item "docker/backups/$backupName").Length -eq 0) { throw 'El respaldo esta vacio. No se aplico la migracion.' }
    Invoke-DockerStep { docker compose cp simulacro_nombreAlumno/src/main/resources/db/mysql/008_medicion_y_personalizacion.sql db:/tmp/008_medicion_y_personalizacion.sql }
    Invoke-DockerStep { docker compose exec -T db sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot $MYSQL_DATABASE < /tmp/008_medicion_y_personalizacion.sql' }
    Write-Host "Migracion 008 aplicada. Respaldo guardado en docker/backups/$backupName"
    Invoke-DockerStep { docker compose up -d --build --wait --wait-timeout 300 }
    Write-Host 'IMPULSO actualizado. Abrir http://localhost:8088 y recargar el navegador.'
} finally {
    Pop-Location
}
