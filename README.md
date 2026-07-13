# QuizArena - API Gateway (Fase 3)

Punto de entrada unico al sistema. Enruta las peticiones a los microservicios
y valida el JWT en la entrada. Corre en el puerto 8080.

## Que hace

- Recibe TODO el trafico del frontend en el puerto 8080.
- Enruta cada camino al microservicio correcto:
    /api/auth/**       -> Servicio de Identidad (8082)
    /api/bancos/**     -> Servicio de Identidad (8082)
    /api/historial/**  -> Servicio de Identidad (8082)
    /api/salas/**      -> Servicio de Juego (8081)
    /ws-juego/**       -> Servicio de Juego (8081)  [WebSocket]
- Valida el JWT antes de enrutar (rutas publicas: /api/auth y /ws-juego).

## Requisitos

- Los tres servicios deben estar corriendo:
    - Api Gateway       : 8080  (este)
    - Servicio de Juego : 8081
    - Servicio de Identidad : 8082
- La clave JWT del Gateway (application.yml) debe ser IGUAL a la de Identidad.

## Como arrancarlo

Desde IntelliJ ejecuta ApiGatewayApplication, o:

    mvn spring-boot:run

## Como probarlo

La idea: ahora puedes hacer TODAS las llamadas al puerto 8080 (el Gateway) en
vez de a los puertos internos, y el Gateway las reenvia.

### 1. Registro / login (ruta publica, sin token) - via Gateway (8080)

    $r = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body '{"correo":"juan@mail.com","contrasena":"secreta123"}'
    $token = $r.token

Si esto funciona, el Gateway esta enrutando bien a Identidad.

### 2. Ruta protegida SIN token -> debe dar 401

    Invoke-RestMethod -Uri "http://localhost:8080/api/bancos?materia=Arquitectura"

Debe rechazar con 401 (el Gateway bloquea porque falta el token).

### 3. Ruta protegida CON token -> debe funcionar

    Invoke-RestMethod -Uri "http://localhost:8080/api/bancos?materia=Arquitectura" -Headers @{Authorization="Bearer $token"}

Debe devolver los bancos (el Gateway valido el token y enruto a Identidad).

Si los 3 casos se comportan asi, el Gateway funciona: enruta y protege.

## Nota sobre el WebSocket

El Gateway puede enrutar el WebSocket (/ws-juego). Si al conectar el frontend
por el Gateway hay problemas con SockJS, la alternativa valida es conectar el
WebSocket directo al Servicio de Juego (8081) y dejar solo el REST por el
Gateway; es una decision comun para el trafico en tiempo real.

## Nota sobre la validacion doble

Por ahora el JWT se valida en el Gateway Y en Identidad (defensa en profundidad).
En una evolucion se puede centralizar solo en el Gateway y que los servicios
internos confien en el.
