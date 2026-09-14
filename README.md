# 🏋️ Simulacro — API REST Gestión de Gimnasio

REST API para la gestión integral de un gimnasio, desarrollada con **Java 21** y **Spring Boot 3**.
Permite administrar alumnos, profesores, ejercicios, rutinas y sesiones de entrenamiento, con autenticación **JWT** y autorización por roles.

---

## 🛠️ Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Data JPA / Hibernate | — |
| Spring Security + JWT (jjwt) | 0.11.5 |
| Spring Validation | — |
| MySQL | 8+ |
| Lombok | — |
| MapStruct | 1.6.3 |
| Springdoc OpenAPI (Swagger) | 2.8.5 |
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
- **Autorización por rol** con `@PreAuthorize` en los controllers
- **DataSeeder**: crea automáticamente un usuario ADMIN al levantar la aplicación por primera vez

---

## 🗂️ Modelo de dominio

```
Usuario
  └── Rol (enum: ADMIN / PROFESOR / ALUMNO)
  └── OneToOne → Alumno o Profesor

Alumno
  ├── Nivel (enum: PRINCIPIANTE / INTERMEDIO / AVANZADO)
  ├── Objetivo (enum)
  ├── Profesores (ManyToMany)
  ├── AsignacionesRutina (OneToMany)
  └── Sesiones (OneToMany)

Profesor
  └── Rutinas creadas (OneToMany)

Rutina
  ├── Profesor (ManyToOne)
  ├── Dias (OneToMany → DiaRutina)
  └── Asignaciones (OneToMany → AsignacionRutina)

DiaRutina
  ├── nombre (ej: "Día 1 - Pecho y Tríceps")
  ├── numeroOrden
  └── Ejercicios (OneToMany → DiaEjercicio)

DiaEjercicio
  ├── Ejercicio (ManyToOne)
  ├── seriesSugeridas
  ├── repsSugeridas
  └── pesoSugerido

Ejercicio
  └── GrupoMuscular (enum: PECHO / ESPALDA / PIERNAS / BRAZOS / HOMBROS / ABDOMINALES)

Sesion (entrenamiento de un alumno en un día de rutina)
  ├── Alumno (ManyToOne)
  ├── DiaRutina (ManyToOne)
  ├── fecha
  ├── completada
  └── Series (OneToMany)

Serie
  ├── DiaEjercicio (ManyToOne)
  ├── numeroSerie
  ├── repsHechas
  ├── pesoUsado
  └── completada
```

---

## 🔐 Autenticación y roles

Todos los endpoints (salvo `/api/auth/**` y Swagger) requieren un JWT válido en el header:

```
Authorization: Bearer <token>
```

### Roles

| Rol | Descripción |
|---|---|
| `ADMIN` | Creado automáticamente al iniciar la app. Puede registrar profesores. |
| `PROFESOR` | Gestiona ejercicios, rutinas y alumnos. |
| `ALUMNO` | Consulta rutinas asignadas y registra sus sesiones de entrenamiento. |

### Credenciales del ADMIN por defecto

```
Email:    admin@gimnasio.com
Password: Admin1234!
```

---

## 🚀 Endpoints

### Auth `/api/auth`

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/api/auth/register/alumno` | Registrar alumno | Público |
| `POST` | `/api/auth/register/profesor` | Registrar profesor | ADMIN |
| `POST` | `/api/auth/login` | Login (devuelve JWT) | Público |

### Alumnos `/api/alumnos`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/alumnos` | Listar todos | PROFESOR |
| `GET` | `/api/alumnos/{id}` | Obtener por ID | PROFESOR |
| `GET` | `/api/alumnos/me` | Ver mi perfil | ALUMNO |
| `PUT` | `/api/alumnos/me` | Actualizar mi perfil | ALUMNO |
| `POST` | `/api/alumnos/me/profesores/{profesorId}` | Asignarme un profesor | ALUMNO |

### Profesores `/api/profesores`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/profesores` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/profesores/{id}` | Obtener por ID | ALUMNO, PROFESOR |
| `GET` | `/api/profesores/me` | Ver mi perfil | PROFESOR |
| `PUT` | `/api/profesores/me` | Actualizar mi perfil | PROFESOR |

### Ejercicios `/api/ejercicios`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/ejercicios` | Listar todos | ALUMNO, PROFESOR |
| `GET` | `/api/ejercicios?nombre=press` | Filtrar por nombre | ALUMNO, PROFESOR |
| `GET` | `/api/ejercicios?grupoMuscular=PECHO` | Filtrar por grupo muscular | ALUMNO, PROFESOR |
| `POST` | `/api/ejercicios` | Crear ejercicio | PROFESOR |
| `PUT` | `/api/ejercicios/{id}` | Actualizar ejercicio | PROFESOR |
| `DELETE` | `/api/ejercicios/{id}` | Eliminar ejercicio | PROFESOR |

**Valores válidos para `grupoMuscular`:** `PECHO`, `ESPALDA`, `PIERNAS`, `BRAZOS`, `HOMBROS`, `ABDOMINALES`

### Rutinas `/api/rutinas`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/rutinas` | Listar todas | ALUMNO, PROFESOR |
| `GET` | `/api/rutinas/{id}` | Obtener por ID | PROFESOR |
| `POST` | `/api/rutinas` | Crear rutina con días y ejercicios | PROFESOR |
| `PUT` | `/api/rutinas/{id}` | Actualizar rutina (solo el creador) | PROFESOR |
| `DELETE` | `/api/rutinas/{id}` | Eliminar rutina (solo el creador) | PROFESOR |
| `POST` | `/api/rutinas/asignar/alumno/{alumnoId}/rutina/{rutinaId}` | Asignar rutina a alumno | PROFESOR |
| `GET` | `/api/rutinas/me/activa` | Mi rutina activa | ALUMNO |
| `GET` | `/api/rutinas/me/historial` | Mi historial de rutinas | ALUMNO |

#### Body para crear una rutina

```json
{
  "nombre": "Rutina Fuerza A",
  "descripcion": "Rutina de 3 días para ganar fuerza e hipertrofia.",
  "dias": [
    {
      "nombre": "Día 1 - Pecho y Tríceps",
      "numeroOrden": 1,
      "ejercicios": [
        {
          "ejercicioId": 1,
          "seriesSugeridas": 4,
          "repsSugeridas": 8,
          "pesoSugerido": 60.0
        }
      ]
    }
  ]
}
```

### Sesiones `/api/sesiones`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `POST` | `/api/sesiones` | Iniciar una sesión de entrenamiento | ALUMNO |
| `POST` | `/api/sesiones/{id}/series` | Registrar una serie dentro de la sesión | ALUMNO |
| `PUT` | `/api/sesiones/{id}/completar` | Marcar sesión como completada | ALUMNO |
| `GET` | `/api/sesiones/me/historial` | Ver mi historial de sesiones | ALUMNO |
| `GET` | `/api/sesiones/alumno/{alumnoId}/historial` | Ver historial de un alumno | PROFESOR |

#### Body para iniciar una sesión

```json
{
  "diaRutinaId": 1,
  "fecha": "2026-09-14"
}
```

#### Body para registrar una serie

```json
{
  "diaEjercicioId": 1,
  "numeroSerie": 1,
  "repsHechas": 8,
  "pesoUsado": 60.0
}
```

---

## 🔒 Reglas de negocio

- Un **alumno** solo puede ver y modificar sus propios datos. Acceder a datos de otro alumno devuelve `403`.
- Un **profesor** puede ver datos de cualquier alumno, pero solo puede editar/eliminar las rutinas que él mismo creó.
- Al asignar una nueva rutina a un alumno, la anterior se desactiva automáticamente.
- Solo el **ADMIN** puede registrar profesores.
- Un alumno solo puede registrar series en sus propias sesiones.

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

> Las tablas se crean automáticamente al levantar la app (`ddl-auto: update`).

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

Una vez levantada la aplicación:

```
http://localhost:8080/swagger-ui/index.html
```

### Cómo probar endpoints protegidos en Swagger

1. Ejecutar `POST /api/auth/login` con las credenciales del admin o de un usuario registrado.
2. Copiar el `token` de la respuesta.
3. Hacer clic en **Authorize** (🔒) arriba a la derecha.
4. Pegar `Bearer <token>` y confirmar.
5. Todos los endpoints protegidos usarán ese token automáticamente.

---

## 👥 Autores

- **Facundo Gregorio**
- **Nicolás Spina**
- **Tomás Spina**