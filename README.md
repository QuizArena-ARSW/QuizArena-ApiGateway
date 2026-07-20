# QuizArena · API Gateway

Punto de entrada único al sistema. Enruta las peticiones REST a los
microservicios y valida el JWT en la entrada. Corre en el puerto 8080.

## Qué hace

- Recibe TODO el tráfico REST del frontend en el puerto 8080.
- Enruta cada camino al microservicio correcto:
  ```
  /api/auth/**       -> Servicio de Identidad
  /api/bancos/**     -> Servicio de Identidad
  /api/historial/**  -> Servicio de Identidad
  /api/amigos/**     -> Servicio de Identidad
  /api/salas/**      -> Servicio de Juego
  /ws-juego/**       -> Servicio de Juego  (WebSocket)
  /api/ia/**         -> Servicio de IA
  ```
- Valida el JWT antes de enrutar. Rutas públicas (sin token):
  `/api/auth/**`, `/ws-juego/**`, `/api/bancos/oficiales`.
- CORS centralizado aquí: ningún otro servicio debe tener `@CrossOrigin`
  (rompe con cabeceras duplicadas si lo hace).

## Requisitos

- Los otros microservicios corriendo (o accesibles por red):
  - Servicio de Identidad — `SERVICIO_IDENTIDAD_URL` (por defecto `localhost:8082`)
  - Servicio de Juego — `SERVICIO_JUEGO_URL` (por defecto `localhost:8081`)
  - Servicio de IA — `SERVICIO_IA_URL` (por defecto `localhost:8083`)
- `JWT_SECRET` **debe ser idéntica** a la de Identidad y el Servicio de
  Juego. No tiene valor por defecto: si falta, el Gateway no arranca (evita
  validar tokens con una clave pública conocida del repositorio).

## Cómo arrancarlo

Desde IntelliJ ejecuta `ApiGatewayApplication`, o:

```bash
mvn spring-boot:run
```

## Cómo probarlo

Todas las llamadas van al puerto 8080 (el Gateway), no a los puertos
internos de cada servicio.

### 1. Login (ruta pública, sin token)

```powershell
$r = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body '{"correo":"juan@mail.com","contrasena":"secreta123!"}'
$token = $r.token
```

### 2. Ruta protegida sin token → debe dar 401

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/bancos/mios"
```

### 3. Ruta protegida con token → debe funcionar

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/bancos/mios" -Headers @{Authorization="Bearer $token"}
```

Si los 3 casos se comportan así, el Gateway enruta y protege correctamente.

## Nota sobre el WebSocket

El WebSocket del juego (`/ws-juego`) se conecta **directo** al Servicio de
Juego (puerto 8081), no a través del Gateway. Decisión consciente: evita la
complejidad de enrutar SockJS por un proxy y reduce latencia en el tiempo
real. Solo el REST pasa por el Gateway.

## Nota sobre la validación doble de JWT

El JWT se valida en el Gateway **y** en Identidad/Juego (defensa en
profundidad). El Servicio de Juego lo valida de forma permisiva (nunca
rechaza la petición, solo identifica al usuario si el token es válido),
porque necesita saber quién crea una sala sin exigir sesión para jugar
como invitado.
