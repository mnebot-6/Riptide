# Deploy backend en Railway — Guía paso a paso

## Prerrequisitos

- Cuenta en [railway.app](https://railway.app) (login con GitHub)
- Repo Riptide en GitHub (o subir solo `/backend`)

---

## Paso 1: Crear proyecto en Railway

1. Ir a [railway.app/new](https://railway.app/new)
2. Click **"Deploy from GitHub Repo"**
3. Seleccionar el repo `Riptide`
4. Railway detectará el Dockerfile. **IMPORTANTE:** configurar el Root Directory a `backend` en Settings → General → Root Directory

---

## Paso 2: Añadir PostgreSQL

1. En el dashboard del proyecto, click **"+ New"** → **"Database"** → **"PostgreSQL"**
2. Railway crea la DB automáticamente
3. Click en el servicio PostgreSQL → **"Variables"** → copiar `DATABASE_URL`
   - Formato: `postgresql://user:pass@host:port/railway`

---

## Paso 3: Configurar variables de entorno

En el servicio del backend (no en la DB), ir a **Variables** y añadir:

| Variable | Valor | Notas |
|---|---|---|
| `DATABASE_URL` | *(referencia a la DB)* | Click "Add Reference" → seleccionar la variable de PostgreSQL |
| `JWT_SECRET` | *(generar uno)* | Mínimo 32 caracteres. Generar con: `openssl rand -base64 48` |
| `GOOGLE_CLIENT_ID` | `tu-web-client-id.apps.googleusercontent.com` | El mismo que usas en la app Android |
| `CORS_ALLOWED_HOSTS` | *(tu dominio Railway)* | Ejemplo: `riptide-backend-production.up.railway.app` |

**Para generar JWT_SECRET** (ejecutar en terminal):
```bash
openssl rand -base64 48
```

**Nota:** `PORT` lo inyecta Railway automáticamente. No hace falta configurarlo.

**Nota:** `DATABASE_URL` se parsea automáticamente — ya no necesitas `DATABASE_USER` ni `DATABASE_PASSWORD` por separado.

---

## Paso 4: Deploy

Railway hace deploy automático al detectar las variables. Si no:

1. Ir a **Deployments** → **"Deploy"**
2. Esperar a que el build termine (2-4 minutos la primera vez)
3. Verificar en los logs que aparezca el puerto de escucha

---

## Paso 5: Generar dominio público

1. Ir a **Settings** → **Networking** → **"Generate Domain"**
2. Railway te da una URL tipo: `riptide-backend-production.up.railway.app`
3. **Actualizar `CORS_ALLOWED_HOSTS`** con este dominio exacto

---

## Paso 6: Verificar

```bash
# Health check
curl https://TU-DOMINIO.up.railway.app/health
# Debe devolver: {"status":"ok"}
```

---

## Paso 7: Conectar la app Android

En `local.properties` (para builds de producción):

```properties
API_BASE_URL=https://TU-DOMINIO.up.railway.app
```

O crear un `local.properties` de release con la URL de producción.

---

## Paso 8: Configurar Google OAuth para producción

1. Ir a [Google Cloud Console](https://console.cloud.google.com/) → Credentials
2. En tu OAuth Client ID existente, añadir la URL de Railway a **Authorized redirect URIs** si es necesario
3. Verificar que el `GOOGLE_CLIENT_ID` en Railway coincide con el de la app

---

## Monitoreo

- **Logs:** Dashboard Railway → tu servicio → "Logs" (tiempo real)
- **Métricas:** Dashboard → "Metrics" (CPU, RAM, red)
- **Health:** Railway pinga `/health` automáticamente (configurado en `railway.toml`)

---

## Costes

Railway Starter plan: **$5/mes de crédito gratis**.

Consumo estimado para Riptide (uso personal):
- Backend: ~$1-2/mes (siempre encendido, bajo tráfico)
- PostgreSQL: ~$1-2/mes (poca data)
- **Total: dentro del crédito gratis** para uso personal

Si creces más allá de los $5: plan Pro a $20/mes con $10 de crédito incluido.

---

## Troubleshooting

| Problema | Solución |
|---|---|
| Build falla | Verificar que Root Directory = `backend` |
| `Connection refused` a la DB | Usar `DATABASE_URL` referenciado, no copiado manualmente |
| JWT error al iniciar | `JWT_SECRET` debe tener 32+ caracteres y no ser el placeholder |
| CORS bloqueado | Añadir dominio exacto a `CORS_ALLOWED_HOSTS` (sin `https://`) |
| App no arranca | Revisar logs — probablemente falta alguna variable de entorno |
