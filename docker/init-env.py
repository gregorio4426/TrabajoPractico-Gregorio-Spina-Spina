#!/usr/bin/env python3
"""Create private local Compose configuration; never overwrite an existing .env."""
import base64
import os
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
path = root / '.env'
values = {
    'MYSQL_PASSWORD': secrets.token_hex(32),
    'MYSQL_ROOT_PASSWORD': secrets.token_hex(32),
    'GYM_JWT_SECRET_BASE64': base64.b64encode(secrets.token_bytes(48)).decode(),
    'GYM_BOOTSTRAP_ADMIN_ENABLED': 'true',
    'GYM_BOOTSTRAP_ADMIN_EMAIL': 'admin@localhost.test',
    'GYM_BOOTSTRAP_ADMIN_PASSWORD': secrets.token_urlsafe(32),
    'GYM_BIND_ADDRESS': '127.0.0.1',
    'GYM_HTTP_PORT': '8088',
    'GYM_CORS_ALLOWED_ORIGINS': '',
}
try:
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('Ya existe .env. No se sobrescribió. Conservá sus claves para el volumen existente.')
with os.fdopen(fd, 'w') as stream:
    stream.write('# Configuración privada generada localmente. No versionar.\n')
    stream.writelines(f'{key}={value}\n' for key, value in values.items())
print('Se creó .env con claves aleatorias. Consultá allí el email y contraseña iniciales, sin compartirlos.')
