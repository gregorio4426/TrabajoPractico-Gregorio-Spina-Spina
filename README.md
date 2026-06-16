# 🏋️ GymApp API — Trabajo Práctico UTN

REST API para la gestión de un gimnasio, desarrollada con **Java 21** y **Spring Boot 3**.
Permite administrar alumnos, profesores, ejercicios y rutinas, incluyendo autenticación con **JWT** y autorización por roles (`ALUMNO` / `PROFESOR`).

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
- **Seguridad stateless** con JWT: cada request autenticado lleva el header `Authorization: Bearer <token>`
- **Autorización por rol** con `@PreAuthorize` en los controllers (`ALUMNO` / `PROFESOR`)

---

## 🗂️ Modelo de dominio

```
Alumno ←──── AlumnoController
  │
  ├── Nivel (enum: PRINCIPIANTE / INTERMEDIO / AVANZADO)
  ├── Objetivo (enum)
  ├── Profesores (ManyToMany)
  └── AsignacionesRutina (OneToMany)
         │
         └── Rutina ←── RutinaController
               │
               ├── Ejercicios (ManyToMany)
               └── Profesor (ManyToOne)
                     │
                     └── ProfesorController

Ejercicio ←── EjercicioController
  └── GrupoMuscular (enum)
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
| `GET` | `/api/alumnos` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/alumnos/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `PUT` | `/api/alumnos/{id}` | Actualizar alumno | ALUMNO |
| `POST` | `/api/alumnos/{alumnoId}/profesores/{profesorId}` | Asignar/elegir profesor | ALUMNO |

### Profesores `/api/profesores`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/profesores` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/profesores/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `PATCH` | `/api/profesores/{id}` | Actualizar profesor | PROFESOR |

### Ejercicios `/api/ejercicios`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/ejercicios` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/ejercicios/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `POST` | `/api/ejercicios` | Crear ejercicio | PROFESOR |
| `PUT` | `/api/ejercicios/{id}` | Actualizar ejercicio | PROFESOR |
| `DELETE` | `/api/ejercicios/{id}` | Eliminar ejercicio | PROFESOR |

### Rutinas `/api/rutinas`

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/rutinas` | Listar todas | ALUMNO, PROFESOR |
| `GET` | `/api/rutinas/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `POST` | `/api/rutinas` | Crear rutina | PROFESOR |
| `PUT` | `/api/rutinas/{id}` | Actualizar rutina | PROFESOR |
| `DELETE` | `/api/rutinas/{id}` | Eliminar rutina | PROFESOR |
| `POST` | `/api/rutinas/{rutinaId}/asignar/{alumnoId}` | Asignar rutina a alumno | PROFESOR |
| `GET` | `/api/rutinas/activa/alumno/{alumnoId}` | Rutina activa del alumno | ALUMNO, PROFESOR |
| `GET` | `/api/rutinas/historial/alumno/{alumnoId}` | Historial de rutinas del alumno | ALUMNO, PROFESOR |

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

Una vez levantada la aplicación (puerto por defecto `8080`), la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

El JSON de la especificación OpenAPI está en:

```
http://localhost:8080/v3/api-docs
```

### Cómo probar endpoints protegidos en Swagger

1. Ejecutar `POST /api/auth/register/alumno` o `register/profesor` (o usar un usuario ya existente).
2. Ejecutar `POST /api/auth/login` con esas credenciales y copiar el `token` de la respuesta.
3. Hacer clic en el botón **Authorize** (🔒) arriba a la derecha en Swagger.
4. Pegar `Bearer <token>` (con el prefijo `Bearer ` incluido) y confirmar.
5. A partir de ahí, todos los endpoints protegidos van a usar ese token automáticamente.

---

## 👥 Autores

- **Facundo Gregorio**
- **Nicolás Spina**
- **Tomas Spina**
