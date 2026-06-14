# 🏋️ GymApp API — Trabajo Práctico UTN

REST API para la gestión de un gimnasio, desarrollada con **Java 21** y **Spring Boot 4**.  
Permite administrar alumnos, profesores, ejercicios y rutinas, incluyendo asignación y seguimiento de rutinas activas por alumno.

---

## 🛠️ Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Data JPA | — |
| Spring Validation | — |
| MySQL | 8+ |
| Lombok | — |
| MapStruct | 1.6.3 |
| Springdoc OpenAPI (Swagger) | 2.8.8 |
| Maven | — |

---

## 📐 Arquitectura

El proyecto sigue una arquitectura en capas estándar:

```
Controller → Service → Repository → Base de datos (MySQL)
```

- **DTOs** separados por dirección (`request` / `response`)
- **MapStruct** para el mapeo entre entidades y DTOs
- **Manejo global de excepciones** con `@RestControllerAdvice`
- **Validaciones** con Bean Validation (`@Valid`) en todos los endpoints de escritura

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

## 🚀 Endpoints

### Alumnos `/api/alumnos`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/alumnos` | Registrar alumno |
| `GET` | `/api/alumnos` | Listar todos |
| `GET` | `/api/alumnos/{id}` | Obtener por ID |
| `PUT` | `/api/alumnos/{id}` | Actualizar alumno |
| `POST` | `/api/alumnos/{alumnoId}/profesores/{profesorId}` | Asignar profesor a alumno |

### Profesores `/api/profesores`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/profesores` | Registrar profesor |
| `GET` | `/api/profesores` | Listar todos |
| `GET` | `/api/profesores/{id}` | Obtener por ID |
| `PUT` | `/api/profesores/{id}` | Actualizar profesor |

### Ejercicios `/api/ejercicios`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/ejercicios` | Crear ejercicio |
| `GET` | `/api/ejercicios` | Listar todos |
| `GET` | `/api/ejercicios/{id}` | Obtener por ID |
| `PUT` | `/api/ejercicios/{id}` | Actualizar ejercicio |
| `DELETE` | `/api/ejercicios/{id}` | Eliminar ejercicio |

### Rutinas `/api/rutinas`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/rutinas` | Crear rutina |
| `GET` | `/api/rutinas` | Listar todas |
| `PUT` | `/api/rutinas/{id}` | Actualizar rutina |
| `DELETE` | `/api/rutinas/{id}` | Eliminar rutina |
| `POST` | `/api/rutinas/asignar` | Asignar rutina a alumno |
| `GET` | `/api/rutinas/activa/alumno/{alumnoId}` | Obtener rutina activa del alumno |
| `GET` | `/api/rutinas/historial/alumno/{alumnoId}` | Historial de rutinas del alumno |

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

## 📄 Documentación (Swagger)

Una vez levantada la aplicación, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 👥 Autores

- **Facundo Gregorio**
- **Nicolás Spina**
- **Tomas Spina ** 
