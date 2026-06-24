# 🏋️ GymApp API — Trabajo Práctico UTN

REST API para la gestión de un gimnasio, desarrollada con **Java 21** y **Spring Boot 3**.
Permite administrar alumnos, profesores, ejercicios y rutinas, con autenticación **JWT** y autorización por roles (`ALUMNO` / `PROFESOR`).

---

## 🛠️ Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Data JPA | — |
| Spring Security + JWT (jjwt) | 0.11.5 |
| Spring Validation | — |
| MySQL | 8+ |
| Lombok | — |
| MapStruct | 1.6.3 |
| Springdoc OpenAPI (Swagger) | 2.6.0 |
| Maven | — |

---

## 📐 Arquitectura

```
Controller → Service → Repository → Base de datos (MySQL)
```

- **DTOs** separados por dirección (`request` / `response`)
- **MapStruct** para el mapeo entre entidades y DTOs
- **Manejo global de excepciones** con `@RestControllerAdvice`
- **Validaciones** con Bean Validation (`@Valid`) en todos los endpoints de escritura
- **Seguridad stateless** con JWT: cada request lleva el header `Authorization: Bearer <token>`
- **Autorización por rol** con `@PreAuthorize` en los controllers (`ALUMNO` / `PROFESOR`)
- **Ownership verification**: cada usuario solo puede operar sobre sus propios datos (IDOR protection)

---

## 🗂️ Modelo de dominio

```
Alumno ←──── AlumnoController
  │
  ├── Nivel (enum: PRINCIPIANTE / INTERMEDIO / AVANZADO)
  ├── Objetivo (enum)
  ├── FechaNacimiento → edad calculada automáticamente
  ├── Profesores (ManyToMany)
  └── AsignacionesRutina (OneToMany)
         │
         └── Rutina ←── RutinaController
               │
               ├── Ejercicios (ManyToMany)
               └── Profesor (ManyToOne) ← solo el profesor creador puede editar/eliminar

Ejercicio ←── EjercicioController
  └── GrupoMuscular (enum: PECHO / ESPALDA / PIERNAS / BRAZOS / HOMBROS / ABDOMINALES)

Usuario ──── Alumno o Profesor (OneToOne)
  └── Rol (enum: ALUMNO / PROFESOR)
```

---

## 🔐 Autenticación

Todos los endpoints (salvo `/api/auth/**` y Swagger) requieren un JWT válido en el header:

```
Authorization: Bearer <token>
```

### Auth `/api/auth`

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/api/auth/register/alumno` | Registrar alumno | Público |
| `POST` | `/api/auth/register/profesor` | Registrar profesor | Público |
| `POST` | `/api/auth/login` | Login (devuelve JWT) | Público |

---

## 🚀 Endpoints

### Alumnos `/api/alumnos`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/alumnos` | Listar todos | PROFESOR |
| `GET` | `/api/alumnos/{id}` | Obtener por ID | PROFESOR |
| `PUT` | `/api/alumnos/{id}` | Actualizar alumno por ID (uso futuro ADMIN) | PROFESOR |
| `GET` | `/api/alumnos/me` | Ver mi perfil | ALUMNO |
| `PUT` | `/api/alumnos/me` | Actualizar mi perfil | ALUMNO |
| `POST` | `/api/alumnos/{alumnoId}/profesores/{profesorId}` | Asignar profesor (por ID) | ALUMNO |
| `POST` | `/api/alumnos/me/profesores/{profesorId}` | Asignarme un profesor | ALUMNO |

> Los endpoints `/{id}` son para uso administrativo (PROFESOR). El alumno opera siempre sobre `/me`.

### Profesores `/api/profesores`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/profesores` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/profesores/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `PATCH` | `/api/profesores/{id}` | Actualizar profesor por ID (uso futuro ADMIN) | PROFESOR |
| `GET` | `/api/profesores/me` | Ver mi perfil | PROFESOR |
| `PUT` | `/api/profesores/me` | Actualizar mi perfil | PROFESOR |

### Ejercicios `/api/ejercicios`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/ejercicios` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/ejercicios?grupoMuscular=PECHO` | Filtrar por grupo muscular | ALUMNO, PROFESOR |
| `POST` | `/api/ejercicios` | Crear ejercicio | PROFESOR |
| `PUT` | `/api/ejercicios/{id}` | Actualizar ejercicio | PROFESOR |
| `DELETE` | `/api/ejercicios/{id}` | Eliminar ejercicio | PROFESOR |

**Valores válidos para `grupoMuscular`:** `PECHO`, `ESPALDA`, `PIERNAS`, `BRAZOS`, `HOMBROS`, `ABDOMINALES`

### Rutinas `/api/rutinas`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/rutinas` | Listar todas | ALUMNO, PROFESOR |
| `GET` | `/api/rutinas/{id}` | Obtener por ID | PROFESOR |
| `POST` | `/api/rutinas` | Crear rutina (se asigna al profesor autenticado) | PROFESOR |
| `PUT` | `/api/rutinas/{id}` | Actualizar rutina (solo el creador) | PROFESOR |
| `DELETE` | `/api/rutinas/{id}` | Eliminar rutina (solo el creador) | PROFESOR |
| `POST` | `/api/rutinas/asignar/alumno/{alumnoId}/rutina/{rutinaId}` | Asignar rutina a alumno | PROFESOR |
| `GET` | `/api/rutinas/activa/alumno/{alumnoId}` | Rutina activa de un alumno por ID | ALUMNO*, PROFESOR |
| `GET` | `/api/rutinas/historial/alumno/{alumnoId}` | Historial de rutinas por ID | ALUMNO*, PROFESOR |
| `GET` | `/api/rutinas/me/activa` | Mi rutina activa | ALUMNO |
| `GET` | `/api/rutinas/me/historial` | Mi historial de rutinas | ALUMNO |

> *Un alumno solo puede consultar su propio `alumnoId`. Intentar consultar el de otro devuelve `403 Forbidden`.

---

## 🔒 Seguridad — reglas de negocio

- Un **alumno** solo puede ver y modificar sus propios datos (`/me`). Acceder a datos de otro alumno por ID devuelve `403`.
- Un **profesor** puede ver y modificar datos de cualquier alumno, pero solo puede editar/eliminar las rutinas que él mismo creó.
- La **edad** se calcula automáticamente a partir de la `fechaNacimiento` al registrarse o actualizar el perfil.
- Al asignar una nueva rutina activa a un alumno, la anterior se desactiva automáticamente.

---

## ⚙️ Configuración

### Requisitos previos

- Java 21
- MySQL 8+
- Maven

### Base de datos

Crear la base de datos en MySQL antes de levantar la aplicación:

```sql
CREATE DATABASE APPgimnasioBD;
```

### `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/APPgimnasioBD
    username: root
    password: 1234
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

> Modificar `username` y `password` según tu entorno local.

### Levantar el proyecto

```bash
./mvnw spring-boot:run
```

---

## 📄 Documentación interactiva (Swagger)

Una vez levantada la aplicación, la documentación está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

El JSON de la especificación OpenAPI está en:

```
http://localhost:8080/v3/api-docs
```

### Cómo probar endpoints protegidos en Swagger

1. Ejecutar `POST /api/auth/register/alumno` o `register/profesor`.
2. Ejecutar `POST /api/auth/login` y copiar el `token` de la respuesta.
3. Hacer clic en el botón **Authorize** (🔒) arriba a la derecha.
4. Pegar `Bearer <token>` (con el prefijo `Bearer ` incluido) y confirmar.
5. A partir de ahí todos los endpoints protegidos usan ese token automáticamente.

---


## 👥 Autores

- **Facundo Gregorio**
- **Nicolás Spina**
- **Tomas Spina**
