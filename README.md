# auth-users

Microservicio de **usuarios, autenticación con JWT y pago simulado premium** del foro de recomendaciones de libros.

Forma parte de un sistema de dos microservicios independientes, cada uno con su propio repositorio y su propio esquema Oracle:

| Microservicio | Puerto | Esquema Oracle | Responsabilidad |
|---|---|---|---|
| **auth-users** (este repo) | 8081 | `USUARIOS_APP` | Usuarios, roles, login (emite el JWT) y pago premium |
| gestor-biblioteca | 8080 | `FORO_APP` | Foro: publicaciones de libros y comentarios (valida el JWT) |

## Tecnologías

- Java 17 y Spring Boot 4.1.2 (Web, Data JPA, Validation, Security)
- JJWT 0.12.6 (tokens firmados con HS256)
- BCrypt para las contraseñas
- Oracle Autonomous Database (conexión con wallet)
- Lombok

## Cómo ejecutar

La wallet de Oracle (`src/main/resources/wallet/`) y la configuración (`application.properties`) vienen incluidas en el repositorio, así que no hace falta configurar nada.

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8081`.

Al arrancar, si no existe ningún ADMIN, la aplicación crea uno automáticamente (`config/AdminInicial`) con las credenciales de `admin.email` / `admin.password`.

## Usuarios de prueba

Están cargados en la base de datos. Para recrearlos se ejecuta la parte DML de [`script_bd.sql`](script_bd.sql) conectado como `USUARIOS_APP` (o como ADMIN con `ALTER SESSION SET CURRENT_SCHEMA = USUARIOS_APP;`).

| Email | Contraseña | Rol | Premium | Sirve para probar |
|---|---|---|---|---|
| `admin@biblioteca.cl` | `Admin2026!` | ADMIN | sí | Administración de usuarios |
| `premium@biblioteca.cl` | `Premium2026!` | USUARIO | sí | Usuario con premium (puede publicar en el foro) |
| `usuario@biblioteca.cl` | `Usuario2026!` | USUARIO | no | Accesos denegados (403) y el pago simulado |

## Roles y seguridad

- **ADMIN**: administra todos los usuarios.
- **USUARIO**: solo puede ver y editar sus propios datos, y no puede cambiarse el rol.
- **premium**: atributo de cualquier usuario. Se activa con el pago simulado y en el foro habilita publicar.

El login devuelve un **JWT válido por 1 hora** con estos claims:

```json
{ "sub": "5", "email": "...", "nombre": "...", "rol": "USUARIO", "premium": false, "iat": ..., "exp": ... }
```

Las rutas protegidas exigen el header:

```
Authorization: Bearer <token>
```

El token se valida en cada request (`JwtAuthFilter`), sin sesión en el servidor (`STATELESS`). Los permisos se controlan con `@PreAuthorize`.

| Código | Significado |
|---|---|
| `401` | Sin token, token inválido o expirado, o credenciales incorrectas |
| `403` | Token válido, pero sin permisos para esa acción |

## Endpoints

### Autenticación (públicos)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/auth/registro` | `nombre`, `apellido`, `email`, `password` | `201` usuario creado (rol USUARIO, premium false) |
| POST | `/auth/login` | `email`, `password` | `200` `{ token, tipo: "Bearer", usuario }` |

### Usuarios

| Método | Ruta | Quién puede | Respuesta |
|---|---|---|---|
| GET | `/usuarios/me` | Cualquier usuario autenticado | `200` sus propios datos |
| GET | `/usuarios` | Solo ADMIN | `200` lista de usuarios |
| GET | `/usuarios/{id}` | ADMIN o el propio usuario | `200` |
| POST | `/usuarios` | Solo ADMIN (puede elegir el rol) | `201` |
| PUT | `/usuarios/{id}` | ADMIN o el propio usuario (solo ADMIN cambia el rol) | `200` |
| DELETE | `/usuarios/{id}` | Solo ADMIN (borra también sus pagos) | `204` |

### Pagos (usuario autenticado)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/pagos/premium` | `201` `{ pago, sesion }` con un **token nuevo** · `409` si ya es premium |
| GET | `/pagos` | `200` pagos del usuario autenticado |

En los endpoints de pagos y en `/usuarios/me`, el id del usuario se obtiene del token y no de la URL. Así nadie puede pagar ni consultar datos a nombre de otro.

## Pago simulado

- No se envían datos de tarjeta: `POST /pagos/premium` **sin body** simula la compra.
- El monto lo fija el servidor (`premium.precio=2000`, CLP). El cliente no puede modificarlo.
- Si se aprueba, el usuario queda con `premium = true` y la respuesta trae un **JWT nuevo**. El token anterior sigue diciendo `premium: false` hasta que expira, por lo que hay que reemplazarlo por el nuevo.

## Errores

Todas las respuestas de error tienen el mismo formato:

```json
{ "status": 404, "mensaje": "El usuario no existe", "detalles": [], "timestamp": "2026-10-31T18:00:00" }
```

| Código | Cuándo |
|---|---|
| `400` | Validación fallida (`detalles` indica el campo), JSON mal formado o parámetro con tipo inválido |
| `401` | No autenticado o credenciales incorrectas |
| `403` | Sin permisos |
| `404` | Usuario o ruta inexistente |
| `405` | Método HTTP no permitido |
| `409` | Email ya registrado o usuario ya premium |
| `500` | Error inesperado (sin detalles internos) |

## Flujo de prueba sugerido (Postman)

1. `POST /auth/login` con `usuario@biblioteca.cl` → copiar el `token`.
2. `GET /usuarios/me` con el token → `200` con sus datos.
3. `GET /usuarios` → `403` (no es ADMIN).
4. `PUT /usuarios/{su id}` enviando `"rol": "ADMIN"` → `403` (no puede subirse el rol).
5. `GET /usuarios` sin token → `401`.
6. `POST /pagos/premium` → `201`. La respuesta trae `sesion.token`, un token nuevo con `premium: true` (se puede revisar en [jwt.io](https://jwt.io)).
7. `POST /pagos/premium` otra vez → `409`.
8. `GET /pagos` → `200` con el pago realizado.
9. `POST /auth/login` con `admin@biblioteca.cl` → `GET /usuarios` → `200`.

## Base de datos

[`script_bd.sql`](script_bd.sql) contiene:

- **DDL** de las tablas `usuarios` y `pagos` (FK `pagos.usuario_id → usuarios.id`). Hibernate las crea con `ddl-auto=update`, así que el DDL queda como referencia.
- **DML** con los 3 usuarios de prueba (contraseñas hasheadas con BCrypt) y el pago de la usuaria premium.

## Estructura

```
src/main/java/com/duoc/auth_users/
├── config/        SecurityConfig, AdminInicial
├── controller/    AuthController, UsuarioController, PagoController
├── dto/           Records de request y response
├── exception/     Excepciones propias y GlobalExceptionHandler
├── model/         Usuario, Pago, Rol
├── repository/    UsuarioRepository, PagoRepository
├── security/      JwtService, JwtAuthFilter, UsuarioAutenticado
└── service/       Interfaces e implementaciones (Auth, Usuario, Pago)
```
