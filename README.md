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
- Docker, GitHub Actions y AWS (ECR, EC2, Systems Manager, Secrets Manager y S3)

## Cómo ejecutar

Las credenciales **no están en el repositorio**: se leen de variables de entorno. En local, la app las toma de un archivo `.env` en la raíz (`spring.config.import=optional:file:.env[.properties]`).

1. Descargar la wallet de Oracle Cloud (**Database connection → Download wallet**) y descomprimirla en la carpeta `wallet/` del proyecto.
2. Copiar `.env.example` a `.env` y completar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`. `JWT_SECRET` debe ser **el mismo** que en gestor-biblioteca.
3. Ejecutar:

```bash
./mvnw spring-boot:run
```

`.env` y `wallet/` están en `.gitignore`: nunca se suben a git.

La API queda disponible en `http://localhost:8081`.

Al arrancar, si no existe ningún ADMIN, la aplicación crea uno automáticamente (`config/AdminInicial`) con las credenciales de `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## Tests

```bash
./mvnw test
```

Los 51 tests usan **H2 en memoria** (perfil `test`), así que no necesitan Oracle, la wallet ni el `.env`. Cubren los servicios (registro, login, usuarios, pagos y recuperación de contraseña), los controladores con sus códigos HTTP, las 6 reglas de contraseña y las clases de la wallet. El workflow de GitHub Actions los ejecuta antes de construir la imagen: si alguno falla, no se despliega.

## Docker

```bash
docker compose up --build
```

`docker-compose.yml` construye la imagen con el `Dockerfile` (Temurin 17), usa el mismo `.env` y monta `./wallet` en `/app/wallet` en modo solo lectura. Para comprobar que levantó:

```bash
curl http://localhost:8081/actuator/health   # {"status":"UP"}
```

Actuator expone **solo** `/actuator/health`, que el despliegue usa como health check.

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
| POST | `/auth/recuperar` | `email` | `200` `{ mensaje, codigoDemo, minutosVigencia }` (ver abajo) |
| POST | `/auth/restablecer` | `email`, `codigo`, `nuevaPassword` | `204` · `400` si el código es inválido, expiró o ya se usó |

### Reglas de contraseña

Se aplican en el registro, al crear o editar un usuario y al restablecer la contraseña. Cada regla incumplida aparece como un elemento de `detalles` en el `400`:

1. Mínimo 8 caracteres.
2. Máximo 20 caracteres.
3. Al menos una mayúscula y una minúscula.
4. Al menos un número.
5. Al menos un carácter especial.
6. Sin espacios.

### Recuperar contraseña (simulado)

1. `POST /auth/recuperar` genera un código de **6 dígitos** válido por **15 minutos** y lo guarda hasheado (BCrypt). Como no se envían correos, el código se devuelve en `codigoDemo` para simular el email. Si el email no existe, la respuesta es la misma pero con `codigoDemo: null`.
2. `POST /auth/restablecer` con el email, el código y la nueva contraseña. El código es de **un solo uso**.

### Usuarios

| Método | Ruta | Quién puede | Respuesta |
|---|---|---|---|
| GET | `/usuarios/me` | Cualquier usuario autenticado | `200` sus propios datos |
| GET | `/usuarios` | Solo ADMIN | `200` lista de usuarios |
| GET | `/usuarios/{id}` | ADMIN o el propio usuario | `200` |
| POST | `/usuarios` | Solo ADMIN (puede elegir el rol) | `201` |
| PUT | `/usuarios/{id}` | ADMIN o el propio usuario (solo ADMIN cambia el rol). `password` es **opcional**: si no se envía, se conserva | `200` |
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

## Colección Postman

En Postman: **Import** → [`postman/auth-users.postman_collection.json`](postman/auth-users.postman_collection.json).

- No necesita environment: la URL (`baseUsuarios`), los tokens y los ids se guardan como **variables de la colección**.
- Las carpetas están numeradas (login → registro → usuarios → pagos → limpieza); se ejecutan en orden o con **Run collection**.
- Cada request tiene un test con el código esperado. El pago premium se prueba con un usuario recién registrado, y la carpeta final lo elimina, así que la colección se puede ejecutar varias veces sin cambiar los usuarios de prueba.

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

## Despliegue en AWS

El microservicio corre como contenedor en una EC2 compartida con el otro microservicio y el front. Nginx publica las rutas en el puerto 80:

```
EC2 · Nginx :80
  ├─ /auth, /usuarios, /pagos      → auth-users        127.0.0.1:8081
  ├─ /publicaciones, /comentarios  → gestor-biblioteca 127.0.0.1:8080
  └─ /                             → front Angular     127.0.0.1:4000
```

La plantilla de CloudFormation de la EC2 y la guía completa paso a paso están en el repo del front: [`DESPLIEGUE.md`](https://github.com/sebcorva/gestor-libros-front-aws/blob/main/DESPLIEGUE.md).

### Qué pasa en cada push a `main`

El workflow [`.github/workflows/publish-ecr.yml`](.github/workflows/publish-ecr.yml) (*Test, publish and deploy auth-users to EC2*) tiene dos jobs:

1. **Test y publicación:** corre `./mvnw test`, construye la imagen y la sube a ECR como `auth-users:<sha del commit>`. Si el repositorio de ECR no existe, lo crea.
2. **Despliegue por Systems Manager (SSM):** sin SSH, ejecuta [`ec2/deploy-api.sh`](ec2/deploy-api.sh) dentro de la EC2. El script:
   - lee las credenciales desde Secrets Manager;
   - descarga la imagen nueva y reemplaza el contenedor `auth-users`, publicado solo en `127.0.0.1:8081`;
   - espera a que `/actuator/health` responda `UP`. Si no lo logra, muestra los errores del contenedor y **restaura el contenedor anterior** (rollback).

   Después, [`ec2/deploy-to-instance.sh`](ec2/deploy-to-instance.sh) hace un smoke test desde fuera: `GET http://<IP>/usuarios/me` sin token debe responder **401**.

También se puede lanzar a mano desde **Actions → Run workflow**.

### Credenciales en AWS

En AWS no se usa `.env`. Las credenciales viven en el secreto **`libros/auth-users`** de Secrets Manager (tipo clave/valor):

| Clave | Valor |
|---|---|
| `DB_URL` | Alias del `tnsnames.ora` |
| `DB_USERNAME` | `USUARIOS_APP` |
| `DB_PASSWORD` | Contraseña de `USUARIOS_APP` |
| `JWT_SECRET` | Secreto Base64 de 32 bytes o más, **idéntico** al de `libros/gestor-biblioteca` |
| `ADMIN_EMAIL` | Email del admin inicial |
| `ADMIN_PASSWORD` | Contraseña del admin inicial (cumple las 6 reglas) |

`DB_URL` puede ser solo el **alias** del `tnsnames.ora` (por ejemplo `midb_high`): `OracleJdbcUrl` lo completa como `jdbc:oracle:thin:@<alias>?TNS_ADMIN=/app/wallet`.

### Wallet desde S3

La wallet tampoco va en la imagen. Al arrancar, `OracleWalletBootstrap` descarga el ZIP desde S3 (`ORACLE_WALLET_S3_BUCKET` / `ORACLE_WALLET_S3_KEY`) y lo descomprime en `/app/wallet`. Si esas variables no existen, como en local, no hace nada y se usa la carpeta `./wallet`.

### Configuración en GitHub

En **Settings → Secrets and variables → Actions**:

| Tipo | Nombre | Valor |
|---|---|---|
| Secret | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Credenciales de la sesión de AWS Academy (vencen con la sesión) |
| Variable | `AWS_REGION` | `us-east-1` |
| Variable | `EC2_INSTANCE_ID` | Output `InstanceId` del stack |
| Variable | `ORACLE_CREDENTIALS_SECRET_ARN` | ARN completo de `libros/auth-users` |
| Variable | `ORACLE_WALLET_S3_BUCKET` | Output `WalletBucketName` del stack (no el nombre del stack) |
| Variable | `ORACLE_WALLET_S3_KEY` | Output `WalletObjectKey` (por ejemplo `Wallet.zip`) |

Este servicio se despliega **primero**: el front y las pruebas de punta a punta necesitan el login.

### Probar el despliegue

La colección Postman sirve igual contra AWS: en las variables de la colección cambia `baseUsuarios` por `http://<IP pública de la EC2>`.

## Estructura

```
src/main/java/com/duoc/auth_users/
├── OracleWalletBootstrap, OracleJdbcUrl   Wallet desde S3 y URL JDBC
├── config/        SecurityConfig, AdminInicial
├── controller/    AuthController, UsuarioController, PagoController
├── dto/           Records de request y response
├── exception/     Excepciones propias y GlobalExceptionHandler
├── model/         Usuario, Pago, Rol
├── repository/    UsuarioRepository, PagoRepository
├── security/      JwtService, JwtAuthFilter, UsuarioAutenticado
└── service/       Interfaces e implementaciones (Auth, Usuario, Pago)
```

```
Dockerfile, docker-entrypoint.sh, docker-compose.yml   Imagen y ejecución local con Docker
.github/workflows/publish-ecr.yml                      Test → ECR → despliegue en EC2
ec2/deploy-api.sh, ec2/deploy-to-instance.sh           Despliegue dentro de la EC2 y smoke test
```

## Créditos

El `Dockerfile`, `docker-entrypoint.sh`, el workflow de GitHub Actions, los scripts de `ec2/` y las clases `OracleWalletBootstrap` y `OracleJdbcUrl` (con sus tests) se basan en el repositorio del profesor [cvalverd/pet-api-oracle](https://github.com/cvalverd/pet-api-oracle), adaptados a este microservicio.
