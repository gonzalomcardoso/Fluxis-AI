# Integración con Gmail — Día 15

Estado: **fundación implementada; el flujo OAuth interactivo se completa en el Día 16**.

Este documento fija las decisiones de permisos, costes, retención y riesgos antes de
conectar una cuenta real de Gmail. El proyecto de Google Cloud, la pantalla de
consentimiento y el cliente OAuth son operaciones externas que debe crear y aprobar
el propietario de la cuenta. Nunca se deben pegar credenciales reales en el repositorio,
en la documentación, en issues o en el chat.

## Decisiones de seguridad

- OAuth 2.0 Authorization Code con PKCE en el backend. El flujo se implementará en el
  Día 16 con `state` de un solo uso, `code_verifier`, sesión autenticada y validación
  del `redirect_uri`.
- Los tokens nunca llegan al navegador ni se guardan en `localStorage`.
- La identidad estable de Google es el claim `sub` del ID token, guardado como
  `provider_account_id`. El correo se guarda solo como dato informativo y nunca se usa
  para identificar la cuenta.
- Los tokens se cifran antes de persistirlos con AES-256-GCM, IV aleatorio de 96 bits,
  tag de autenticación de 128 bits y datos asociados que ligan el ciphertext al usuario
  y al `sub` de Google. La clave se suministra mediante `APP_TOKEN_ENCRYPTION_KEY`,
  nunca se guarda en PostgreSQL ni en el repositorio.
- La tabla `connected_accounts` guarda únicamente ciphertext, versión de clave,
  metadatos de expiración y scopes concedidos. La API de estado no debe devolver tokens.
- El backend falla de forma cerrada si intenta cifrar o descifrar sin una clave válida.

## Creación del proyecto y consentimiento (acción del propietario)

La consola de Google cambia con frecuencia; los nombres pueden variar ligeramente.

1. Entrar en Google Cloud Console con la cuenta propietaria y crear un proyecto, por
   ejemplo `fluxis-ai-dev`. Usar proyectos separados para DEV, TEST y PROD.
2. En **APIs & Services > Library**, habilitar **Gmail API**.
3. Configurar **Google Auth Platform / OAuth consent screen**:
   - Tipo de usuario: **External**.
   - Estado de publicación: **Testing** mientras se desarrolla.
   - Nombre de la aplicación: `Fluxis AI`.
   - Correo de soporte y correo de contacto del desarrollador.
   - Dominio autorizado y enlaces de política de privacidad/terms cuando existan.
   - Añadir como **test users** solo cuentas sintéticas o controladas por el propietario.
4. En **Data access / Scopes**, añadir únicamente los scopes de la fase actual:
   - `openid`
   - `email`
   - `https://www.googleapis.com/auth/gmail.metadata`
5. En **Clients**, crear un cliente OAuth de tipo **Web application**:
   - Redirect URI de desarrollo:
     `http://localhost:8080/api/gmail/oauth/callback`.
   - Añadir una URI distinta y exacta para TEST y PROD cuando existan esos entornos.
     No usar comodines.
6. Descargar o copiar las credenciales directamente al gestor de secretos local. No
   crear `client_secret*.json` dentro del repositorio.
7. Generar una clave de cifrado de 32 bytes y configurarla como variable de entorno:

   ```bash
   openssl rand -base64 32
   ```

   En PowerShell se puede generar sin persistirla en un archivo:

   ```powershell
   $key = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($key)
   [Convert]::ToBase64String($key)
   ```

   Usar claves distintas en DEV, TEST y PROD. No reutilizar la clave de cifrado como
   contraseña, client secret ni clave de otro servicio.

Variables esperadas por el backend (los valores reales no se versionan):

| Variable | Uso |
|---|---|
| `GOOGLE_CLIENT_ID` | Identificador público del cliente OAuth |
| `GOOGLE_CLIENT_SECRET` | Secreto del cliente; solo backend |
| `GOOGLE_REDIRECT_URI` | URI exacta registrada en Google |
| `GOOGLE_OAUTH_SCOPES` | Lista de scopes de la fase activa |
| `APP_TOKEN_ENCRYPTION_KEY` | Clave base64 de 32 bytes para AES-256-GCM |
| `APP_TOKEN_ENCRYPTION_KEY_VERSION` | Versión lógica para rotación; empezar en `1` |

`.env.example` contiene únicamente marcadores vacíos o valores locales no secretos.
Los archivos `.env*` reales están ignorados por Git y requieren consentimiento explícito
del propietario para modificarse.

## Scopes mínimos y consentimiento incremental

| Fase | Scope | Para qué | Cuándo se solicita |
|---|---|---|---|
| Base | `openid` | Identidad OAuth | Primera conexión |
| Base | `email` | Mostrar el correo conectado | Primera conexión |
| Base | `https://www.googleapis.com/auth/gmail.metadata` | Listar IDs, labels, cabeceras y fechas sin cuerpos ni adjuntos | Primera conexión, Día 17 |
| Acciones | `https://www.googleapis.com/auth/gmail.modify` | Aplicar/quitar labels y archivar mensajes | Solo tras opt-in explícito a acciones, Día 19 |
| Borradores | `https://www.googleapis.com/auth/gmail.compose` | Crear borradores para revisión manual | Solo si hace falta en el Día 26 |

Reglas:

- No se solicita `https://mail.google.com/`, `gmail.readonly` ni `gmail.send` en la
  fase inicial.
- Google no ofrece un scope que permita exclusivamente cambiar labels/archivar sin
  ampliar capacidades de lectura. `gmail.modify` es el mínimo documentado para modificar
  mensajes, pero es un scope amplio. Por eso se pide en una autorización incremental y
  solo cuando el usuario active las acciones de organización.
- Aunque un scope permita más operaciones, el backend aplicará una allowlist: solo
  añadir/quitar labels y quitar `INBOX` para archivar. Nunca llamará a operaciones de
  envío, borrado permanente ni borrado de la cuenta.
- Los scopes realmente concedidos se guardan en `granted_scopes`; si Google concede
  menos de lo solicitado, la aplicación debe mostrar el estado y no asumir permisos.
- No se solicita consentimiento "offline" hasta que el flujo OAuth del Día 16 pueda
  almacenar y usar el refresh token de forma segura.

### Texto orientativo de consentimiento

> Fluxis AI necesita acceso de solo metadatos a Gmail para identificar mensajes,
> etiquetas y fechas. No lee ni modifica el cuerpo de los correos en esta fase. Las
> acciones de organización se pedirán por separado y siempre requerirán tu aprobación.

## Modelo persistido

La migración `V15__create_connected_accounts.sql` crea `connected_accounts`:

- una conexión Google por usuario (`UNIQUE (user_id, provider)`);
- `provider_account_id` con el `sub` de Google;
- `access_token_encrypted` y `refresh_token_encrypted` como `TEXT` con ciphertext;
- `granted_scopes TEXT[]`, expiración, estado y versión de clave;
- `status = CONNECTED | REVOKED`;
- al desconectar se vacían los ciphertext y se registra `revoked_at`; la fila se conserva
  para trazabilidad mientras no se borre el usuario;
- `ON DELETE CASCADE` cuando se elimina la cuenta de Fluxis AI.

El token de acceso y el refresh token se descifran solo en memoria dentro del backend,
para la llamada concreta a Gmail. `OAuthTokens` no debe serializarse desde un
controlador.

## Retención y ciclo de vida

- Los tokens se conservan mientras la conexión esté activa y sean necesarios para
  sincronizar o ejecutar acciones aprobadas.
- Al desconectar: revocar el acceso en Google (Día 16), vaciar ambos ciphertext,
  establecer `status = REVOKED` y `revoked_at`. Si la revocación remota falla, se
  registra el incidente y se reintenta; los tokens locales se eliminan igualmente.
- Al eliminar la cuenta de Fluxis AI, la FK con `ON DELETE CASCADE` elimina la conexión.
- Los tokens no se incluyen en logs, respuestas HTTP, trazas, métricas ni mensajes de
  error. Los refresh tokens no se escriben en el frontend.
- La retención de mensajes y metadatos de correo se definirá en el Día 17. La política
  inicial debe minimizar metadatos, permitir borrado por usuario y no conservar cuerpos
  que no sean necesarios para una sugerencia aprobada.
- Las copias de seguridad de PostgreSQL deben cifrarse y protegerse con la misma
  criticidad que la base de datos; la clave de cifrado de tokens no debe incluirse en
  el mismo backup.

### Rotación de clave

`encryption_key_version` permite una rotación futura: se descifra con la versión
anterior, se vuelve a cifrar con la nueva y se actualiza la fila. Una rotación debe ser
un procedimiento aprobado, probado con datos sintéticos y con ventana de mantenimiento.
Si se sospecha exposición de la clave: parar el flujo Gmail, revocar tokens en Google,
rotar la clave, forzar reconexión y documentar el incidente sin reproducir secretos.

## Costes

- Gmail API y OAuth no tienen un cargo directo documentado por llamada. Están sujetos a
  cuotas y límites de Google; el backend debe tratar `429` con backoff y no asumir
  capacidad ilimitada.
- Un proyecto de Google Cloud sin recursos de pago no debería generar factura. No
  habilitar facturación ni otros servicios de Google sin una decisión explícita.
- El estado **Testing** de la pantalla de consentimiento está limitado a usuarios de
  prueba y los refresh tokens pueden caducar a los 7 días. Es aceptable para desarrollo,
  pero obliga a reconectar y no sirve como configuración de producción.
- Publicar para usuarios externos puede exigir verificación de Google y una revisión de
  seguridad por el uso de scopes sensibles. Hay que presupuestar tiempo, no solo dinero.
- Los costes de infraestructura, PostgreSQL, backups y el proveedor de IA son
  independientes. El presupuesto de IA (`AI_MONTHLY_BUDGET_EUR`) no cubre Gmail.
- La verificación de cuotas, precios y límites vigentes es una tarea de revisión antes
  de PROD; las cifras de esta tabla no deben tratarse como garantía contractual.

## Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Robo de la base de datos | Uso de Gmail por un tercero | AES-256-GCM, clave fuera de la base, rotación, mínimo privilegio, revocación |
| Clave de cifrado en logs o repositorio | Descifrado masivo | Gestor de secretos, `.gitignore`, revisión de logs, nunca imprimir valores |
| Scope demasiado amplio | Exposición innecesaria del correo | `gmail.metadata` inicial, autorización incremental, allowlist de acciones |
| Token en el navegador | Robo por XSS/extensión | Tokens solo backend; cookies `HttpOnly`; sin localStorage |
| CSRF en el callback OAuth | Vinculación de una cuenta ajena | `state` de un solo uso, PKCE, sesión autenticada y validación estricta (Día 16) |
| Mezcla de identidades | Acceso a la cuenta equivocada | Usar `sub`, comparar con la conexión existente y rechazar reemplazos silenciosos |
| Refresh token ausente o caducado | Sincronización detenida | Estado visible, reconexión guiada, no borrar el refresh anterior al refrescar |
| Cuota o caída de Gmail | Fallos parciales | Backoff, límites, idempotencia, reintentos y errores observables |
| Prompt injection en correos | Acciones no autorizadas por IA | Correo como dato no confiable, aprobación humana y ejecución allowlisted |
| Borrado accidental | Pérdida de datos del usuario | No usar scopes de borrado; archivar = quitar `INBOX`; confirmación explícita |
| Backup con datos sensibles | Exposición por copia | Cifrado de backups, acceso restringido y retención definida |
| Consentimiento en Testing | Tokens de corta duración y límite de usuarios | Reautenticación esperada; plan de verificación antes de producción |

## Verificación del Día 15

Automatizada:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify
```

Cubre:

- migración Flyway `V15` y restricciones de `connected_accounts`;
- round-trip AES-GCM y fallo cerrado sin clave;
- IV aleatorio y autenticación de datos asociados;
- que el servicio persiste ciphertext y no los tokens en claro;
- descifrado solo para operaciones internas;
- borrado de tokens al desconectar;
- lista de scopes base sin `gmail.send`, `gmail.readonly` ni acceso total.

Manual, pendiente de que el propietario lo ejecute en Google Cloud:

- [ ] Proyecto DEV creado y Gmail API habilitada.
- [ ] Pantalla de consentimiento en Testing con los tres scopes base.
- [ ] Cliente OAuth Web con la redirect URI exacta.
- [ ] Cuenta de prueba añadida.
- [ ] Variables configuradas fuera de Git y clave distinta de otros entornos.
- [ ] Ningún secreto aparece en `git status`, logs ni documentación.

El Día 16 añadirá el flujo OAuth, el estado de conexión, la desconexión con revocación y
la comprobación de que la cuenta conectada es la esperada.
