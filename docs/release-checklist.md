# Riptide — Release Checklist

Lista de verificación antes de subir un build a Google Play. Repetir cada release.

---

## 1. Versionado

- [ ] `versionCode` incrementado en `composeApp/build.gradle.kts` (Play rechaza valores ≤ existente).
- [ ] `versionName` actualizado siguiendo SemVer (`MAJOR.MINOR.PATCH`).
- [ ] Tag git creado: `git tag v$VERSION_NAME && git push --tags`.

## 2. Build release

```bash
./gradlew :composeApp:bundleRelease
```

- [ ] Bundle (`*.aab`) generado en `composeApp/build/outputs/bundle/release/`.
- [ ] APK universal de prueba generado: `./gradlew :composeApp:assembleRelease`.
- [ ] Firmado con `keystore.properties` (no debug keystore).

## 3. Smoke test sobre APK release firmado

Instalar el APK release (no debug) en un dispositivo limpio o tras `adb uninstall com.mnebot.riptide`:

```bash
adb install composeApp/build/outputs/apk/release/composeApp-release.apk
```

Comprobar manualmente:

- [ ] Splash screen aparece (logo Rising Currents sobre fondo azul océano).
- [ ] Onboarding se muestra (4 pasos) en primer lanzamiento.
- [ ] Login con Google funciona y guarda sesión.
- [ ] Crear bloque y tarea normal → aparece en TODAY.
- [ ] Crear tarea contable, prioritaria, con timer y con notas → todas se renderizan correctas en TaskCard.
- [ ] Completar tarea → criatura sale del lootbox / progresa.
- [ ] Pestaña POND: criaturas nadan, son tappables.
- [ ] Pestaña CALENDAR: navegar entre meses.
- [ ] Pestaña PROGRESS: gráfica + historial cargan.
- [ ] Swipe entre pestañas funciona; dots se actualizan; tap en dots navega.
- [ ] Resumen nocturno se programa (verificar en logs `adb logcat | grep RiptideSync` que SyncManager arranca).
- [ ] Notificación de tarea con hora se dispara a la hora programada (ajustar a +2 min para test).
- [ ] Widget Glance se puede añadir al launcher y muestra tareas.
- [ ] Live wallpaper se aplica desde Settings → Cuenta.

## 4. Sync end-to-end (2 dispositivos)

Mismo Google account en device A y device B:

- [ ] A crea tarea → en <30s aparece en B (con WiFi). Verificar timestamp en Settings → "Última sincronización hace…".
- [ ] B completa la tarea → cambio se refleja en A.
- [ ] A en modo avión modifica tarea, B online la modifica con texto distinto. A vuelve online → última escritura por timestamp gana, sin pérdida silenciosa.
- [ ] A borra tarea, B online — borrado se propaga a B.
- [ ] Si hay error en sync, en Settings → "Último error" lo muestra; y `adb logcat | grep RiptideSync` lo registra.

## 5. ProGuard / R8

- [ ] Build release no crashea (kotlinx-serialization, Ktor, Room, Glance funcionan tras minify).
- [ ] Si crashea, revisar `proguard-rules.pro` y añadir `-keep` necesario antes de re-build.

## 6. Listing en Play Console

- [ ] Título corto, descripción larga (ver `docs/store-listing-en.md` y `store-listing-es.md`).
- [ ] Iconos: 512×512 PNG (adaptive icon ya en `mipmap-anydpi-v26`).
- [ ] Feature graphic: 1024×500.
- [ ] Screenshots: mínimo 2 phone (preferible 8). Capturar pantalla en device real con: TODAY (con tareas), POND (con peces), Lootbox abriendo criatura, EcosystemScreen (rejilla), CALENDAR, PROGRESS.
- [ ] Privacy Policy URL pública (alojar `docs/privacy-policy.html` en GitHub Pages o equivalente).
- [ ] Data Deletion URL: misma página o sección dedicada — incluir explicación de cómo borrar cuenta (logout + endpoint `DELETE /auth/me` en backend si existe, o instrucciones por email).
- [ ] Categoría: Productividad.
- [ ] Edad recomendada: PEGI 3 / Everyone (sin contenido sensible).

## 7. Tests automatizados

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :backend:test
```

- [ ] Todos verdes.

## 8. Backend

- [ ] Endpoint sync producción accesible (`API_BASE_URL` en `local.properties` apunta al backend de release).
- [ ] PostgreSQL backup reciente.
- [ ] `JWT_SECRET` rotado si se ha filtrado.
- [ ] (Opcional) Aikido scan: `aikido_full_scan` en `/backend`.

## 9. Post-release

- [ ] Smoke test del build descargado de Play Internal Testing antes de promocionar a Production.
- [ ] Monitorizar tasa de crashes (Play Console → Vitals) en las primeras 24h.
- [ ] Bump `versionCode` para siguiente iteración inmediatamente al volver a `master`.

---

## Diferido a v1.1+

- Crashlytics integration (Firebase).
- iOS port.
- Tests adicionales para nuevos algoritmos (NightSummary scoring weighted, Recurrence advanced cases).
