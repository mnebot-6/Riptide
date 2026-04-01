# Riptide — Setup Guide (Backend + Sync)

## Prerequisitos

- PostgreSQL 16+ instalado localmente
- Google Cloud Console con proyecto configurado
- Android Studio con emulador (o dispositivo fisico)

---

## 1. PostgreSQL

**Instalacion:** [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)

```sql
-- En psql (SQL Shell):
CREATE USER riptide WITH PASSWORD 'riptide';
CREATE DATABASE riptide OWNER riptide;
```

**Datos de conexion (dev):**
| Campo | Valor |
|-------|-------|
| Host | localhost |
| Port | 5432 |
| Database | riptide |
| User | riptide |
| Password | riptide |

---

## 2. Google Cloud Console

**Proyecto:** `riptide-492016`
**Console:** https://console.cloud.google.com/apis/credentials?project=riptide-492016

### OAuth consent screen
- Tipo: External
- Test users: tu email de Gmail

### Client IDs necesarios

| Tipo | Uso | Donde se configura |
|------|-----|--------------------|
| Web application | Backend verifica tokens + App pide ID token | `backend/.env` (GOOGLE_CLIENT_ID) + `local.properties` (GOOGLE_CLIENT_ID) |
| Android | Permite sign-in desde la app | Solo existe en la Console (no se usa en codigo) |

**Android Client ID config:**
- Package: `com.mnebot.riptide`
- SHA-1 debug: obtener con `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
- Para release: usar SHA-1 del keystore de firma

> Los valores reales de los Client IDs estan en `backend/.env` (no versionado).

---

## 3. Backend

### Configurar variables de entorno

Copia `backend/.env.example` como `backend/.env` y rellena los valores.

### Ejecutar (PowerShell)

```powershell
cd backend

# Cargar variables del .env manualmente:
Get-Content .env | Where-Object { $_ -match '^\w' } | ForEach-Object {
    $key, $val = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($key, $val, 'Process')
}

.\gradlew.bat run
```

O directamente:
```powershell
cd backend
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/riptide"
$env:DATABASE_USER = "riptide"
$env:DATABASE_PASSWORD = "riptide"
$env:JWT_SECRET = "riptide-dev-secret-at-least-32-characters-long"
$env:GOOGLE_CLIENT_ID = "tu-web-client-id.apps.googleusercontent.com"
.\gradlew.bat run
```

Cuando veas `Responding at http://0.0.0.0:8080` esta listo.

### Verificar

```powershell
curl http://localhost:8080/health
# -> { "status": "ok" }
```

---

## 4. App Android

### local.properties

```properties
sdk.dir=C\:\\Users\\TU_USER\\AppData\\Local\\Android\\Sdk

# Sync config
API_BASE_URL=http://10.0.2.2:8080
GOOGLE_CLIENT_ID=tu-web-client-id.apps.googleusercontent.com
```

> `10.0.2.2` es el alias del emulador para `localhost` del host.
> Para dispositivo fisico en la misma red, usa la IP local del PC (e.g. `192.168.1.50`).

### Build & Install

```powershell
.\gradlew.bat installDebug
```

---

## 5. Probar sync

1. Asegurate de que el backend esta corriendo
2. Abre la app en el emulador
3. Drawer -> "Iniciar sesion con Google"
4. Selecciona tu cuenta de test
5. Deberia aparecer tu nombre y "Sincronizado" en el drawer

### Verificar datos en PostgreSQL

```sql
-- En psql:
\c riptide
SELECT id, email FROM users;
SELECT id, name FROM work_blocks;
SELECT id, title, is_completed FROM day_tasks LIMIT 10;
```

---

## Notas

- **Sin cuenta:** la app funciona 100% offline, el sync es opcional
- **Token refresh:** los JWT duran 24h, el refresh token 30 dias
- **Sync automatico:** cada 1h via WorkManager + 5s despues de cada mutacion
- **Primera sync:** `InitialSyncPreparer` stampa datos pre-existentes antes del primer push
- **Para produccion:** deploy backend a Railway/Render, cambiar JWT_SECRET, crear Client ID de release con SHA-1 del keystore de firma
