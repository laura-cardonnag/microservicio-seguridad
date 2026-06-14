# 📋 Documentación Completa del Proyecto: Microservicio de Seguridad

**Fecha de documentación:** 2026-05-27  
**Versión del Proyecto:** 0.0.1-SNAPSHOT  
**Stack Tecnológico:** Spring Boot 4.0.3, Java 17, MongoDB, JWT, reCAPTCHA v2

---

## 📑 Tabla de Contenidos

1. [Visión General](#visión-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Tecnologías Utilizadas](#tecnologías-utilizadas)
4. [Funcionalidades Implementadas](#funcionalidades-implementadas)
5. [Funcionalidades NO Implementadas](#funcionalidades-no-implementadas)
6. [Estructuras de Base de Datos](#estructuras-de-base-de-datos)
7. [Endpoints de la API](#endpoints-de-la-api)
8. [Flujos de Autenticación](#flujos-de-autenticación)
9. [Sistemas de Seguridad](#sistemas-de-seguridad)
10. [Problemas Conocidos](#problemas-conocidos)
11. [Recomendaciones](#recomendaciones)

---

## 🎯 Visión General

El **Microservicio de Seguridad** es un backend especializado en gestión de autenticación, autorización y control de acceso para un sistema de transporte (KALA Buses). 

**Objetivo Principal:** Centralizar toda la lógica de seguridad, permitir que usuarios se autentiquen mediante múltiples métodos y controlar qué recursos pueden acceder según sus roles y permisos.

**Puerto de Ejecución:** `8181`  
**Base de Datos:** MongoDB Atlas (cluster0)  
**Base de Datos:** `db_seguridad`

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Angular)                        │
│                        (Puerto 4200)                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
    ┌───────────▼──────────────┐  ┌──────▼────────────────┐
    │  Microservicio Seguridad │  │  Microservicio       │
    │  (Puerto 8181)           │  │  Notificaciones      │
    │  - Auth                  │  │  (Puerto 5000)       │
    │  - Roles/Permisos        │  │  - Email             │
    │  - JWT Tokens            │  │  - Verificación 2FA  │
    │  - 2FA                   │  │  - Password Reset    │
    │  - OAuth                 │  │  - Códigos           │
    └───────────┬──────────────┘  └──────┬────────────────┘
                │                        │
                └────────────┬───────────┘
                             │
                ┌────────────▼────────────┐
                │    MongoDB Atlas        │
                │    (db_seguridad)       │
                └─────────────────────────┘
```

---

## 🛠️ Tecnologías Utilizadas

### Backend
| Tecnología | Versión | Propósito |
|-----------|---------|----------|
| Spring Boot | 4.0.3 | Framework principal |
| Java | 17 | Lenguaje de programación |
| Spring Data MongoDB | Latest | ORM para MongoDB |
| JWT (jjwt) | 0.12.3 | Generación y validación de tokens |
| Lombok | Latest | Reducción de código boilerplate |
| Bcrypt | Latest | Encriptación de contraseñas |
| reCAPTCHA v2 | Google API | Validación anti-bots |
| Spring Web MVC | Latest | Controladores REST |

### Infraestructura
- **Base de Datos:** MongoDB Atlas
- **Servidor:** Apache Tomcat (incluido en Spring Boot)
- **Protocolo:** HTTP/HTTPS
- **Puerto Principal:** 8181

---

## ✅ Funcionalidades Implementadas

### 1. **Autenticación Tradicional**

#### 1.1 Registro de Usuarios
- **Endpoint:** `POST /api/public/auth/register`
- **Descripción:** Permite que nuevos usuarios se registren en el sistema
- **Validaciones:**
  - Email único (no puede existir otro usuario con el mismo email)
  - Contraseña encriptada con SHA256
  - Asignación automática del rol "Ciudadano"
- **Proceso:**
  1. Valida que el email no exista
  2. Encripta la contraseña
  3. Crea el usuario en BD
  4. Asigna rol ciudadano por defecto
  5. Envía email de bienvenida
- **Response:** Usuario creado con ID, email, nombre

#### 1.2 Login con Autenticación de Dos Factores (2FA)
- **Endpoint:** `POST /api/public/auth/login`
- **Descripción:** Autentica usuarios con email, contraseña y reCAPTCHA
- **Flujo de Autenticación:**
  1. **Validación Inicial:**
     - Verifica que email y contraseña no sean nulos
     - Valida reCAPTCHA token con Google (score mínimo: 0.5)
  
  2. **Búsqueda de Usuario:**
     - Busca usuario por email en BD
     - Compara contraseña (SHA256)
  
  3. **Generación de Código 2FA:**
     - Genera código de 6 dígitos aleatorio
     - Lo guarda en una sesión temporal (válida 10 minutos)
     - Envía el código por email
  
  4. **Response:**
     ```json
     {
       "success": "true",
       "message": "Código de verificación enviado a tu correo",
       "sessionId": "UUID-de-sesion",
       "maskedEmail": "cri***@***.com",
       "expiresAt": 1234567890,
       "attemptsRemaining": 3
     }
     ```

#### 1.3 Verificación de 2FA
- **Endpoint:** `POST /api/public/auth/verify-2fa`
- **Descripción:** Valida el código 2FA recibido por email
- **Parámetros:**
  - `sessionId`: ID de la sesión temporal
  - `code2FA`: Código de 6 dígitos recibido
- **Validaciones:**
  - Sesión existe y no ha expirado
  - Código coincide exactamente
  - Máximo 3 intentos fallidos
- **Response (Exitoso):**
  ```json
  {
    "success": "true",
    "message": "Verificación 2FA exitosa",
    "userId": "UUID-usuario",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```

#### 1.4 Reenvío de Código 2FA
- **Endpoint:** `POST /api/public/auth/resend-2fa`
- **Descripción:** Reenvia un nuevo código si el anterior expiró
- **Parámetros:** `sessionId`
- **Response:** Nueva sesión con código generado

#### 1.5 Cancelación de 2FA
- **Endpoint:** `POST /api/public/auth/cancel-2fa`
- **Descripción:** Cancela la autenticación en progreso
- **Parámetros:** `sessionId`

---

### 2. **Autenticación OAuth 2.0**

#### 2.1 Login con OAuth (Google, GitHub, Microsoft)
- **Endpoint:** `POST /api/public/auth/oauth-login`
- **Descripción:** Sincroniza autenticación de Firebase con el backend
- **Providers Soportados:**
  - Google
  - GitHub
  - Microsoft
- **Request:**
  ```json
  {
    "email": "user@gmail.com",
    "name": "Juan Pérez",
    "photoUrl": "https://googleusercontent.com/photo.jpg",
    "provider": "google",
    "firebaseToken": "token-de-firebase"
  }
  ```
- **Lógica:**
  1. Valida campos requeridos (email, name, provider)
  2. Busca usuario existente por email
  3. Si existe: actualiza nombre si cambió
  4. Si NO existe: crea usuario nuevo con contraseña aleatoria
  5. Genera JWT token
  6. Retorna token + información del usuario
- **Response:**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "id": "UUID-usuario",
    "email": "user@gmail.com",
    "name": "Juan Pérez",
    "photoUrl": "https://googleusercontent.com/photo.jpg"
  }
  ```
- **Nota Importante:** `photoUrl` NO se almacena en BD, solo se retorna del request

---

### 3. **Recuperación de Contraseña**

#### 3.1 Solicitar Reset de Contraseña
- **Endpoint:** `POST /api/public/auth/forgot-password`
- **Descripción:** Inicia proceso de recuperación de contraseña
- **Request:**
  ```json
  {
    "email": "user@example.com",
    "recaptchaToken": "token-de-google"
  }
  ```
- **Validaciones:**
  - Email no vacío
  - reCAPTCHA válido
- **Lógica:**
  1. Valida reCAPTCHA con Google
  2. Busca usuario por email (SIN revelar si existe)
  3. Si existe:
     - Genera UUID único para reset
     - Guarda token en tabla `password_reset_tokens`
     - Token válido por 30 minutos
     - Llama a microservicio de notificaciones
     - Envía email con link de reset
  4. Response SIEMPRE igual (por seguridad, incluso si email NO existe)
- **Response:**
  ```json
  {
    "message": "Si el email existe en nuestros registros, recibirá instrucciones de recuperación en su bandeja de entrada."
  }
  ```
- **Tabla `password_reset_tokens`:**
  | Campo | Tipo | Descripción |
  |-------|------|-------------|
  | id | UUID | Clave primaria |
  | user_id | String (FK) | Referencia a usuario |
  | email | String | Email para auditoría |
  | token | String (UNIQUE) | Token del reset |
  | created_at | Date | Timestamp de creación |
  | expires_at | Date | Expira en 30 minutos |
  | used | Boolean | ¿Fue utilizado? |
  | used_at | Date | Cuándo se utilizó |

#### 3.2 Restablecer Contraseña
- **Endpoint:** `POST /api/public/auth/reset-password`
- **Descripción:** Cambia la contraseña usando el token
- **Request:**
  ```json
  {
    "token": "uuid-token-xxxxx",
    "newPassword": "nuevaContraseña123"
  }
  ```
- **Validaciones:**
  - Token existe en BD
  - Token NO ha expirado (expires_at > NOW)
  - Token NO fue usado (used = false)
  - Nueva contraseña >= 8 caracteres
- **Lógica:**
  1. Busca token en BD
  2. Valida todas las condiciones
  3. Busca usuario asociado al token
  4. Encripta nueva contraseña (SHA256)
  5. Actualiza password en usuario
  6. Marca token como usado (used=true, used_at=NOW)
  7. Envía email de confirmación (opcional)
  8. Invalida otros tokens del mismo usuario (opcional)
- **Response:**
  ```json
  {
    "message": "Contraseña restablecida exitosamente. Puede iniciar sesión con su nueva contraseña."
  }
  ```
- **Códigos de Error:**
  - `400` - Token inválido, expirado o ya fue usado
  - `400` - Contraseña muy corta
  - `500` - Error interno

---

### 4. **Gestión de Roles y Permisos**

#### 4.1 Estructura de Relaciones N-N

**Relación 1: Usuario → Rol (UserRole)**
```
Usuario (1) ──→ (N) UserRole ←── (1) Rol
```
- Permite que un usuario tenga múltiples roles
- Tabla: `user_roles` (no relacional en MongoDB, embebido en documentos)

**Relación 2: Rol → Permiso (RolePermission)**
```
Rol (1) ──→ (N) RolePermission ←── (1) Permiso
```
- Permite que un rol tenga múltiples permisos
- Un permiso puede ser asignado a múltiples roles

**Flujo de Autorización:**
```
Usuario ─→ [UserRoles] ─→ Roles ─→ [RolePermissions] ─→ Permisos
                                                              ↓
                                         ¿Endpoint URL + Método está permitido?
```

#### 4.2 CRUD de Roles
- **Crear Rol:** `POST /api/private/roles`
- **Listar Roles:** `GET /api/private/roles`
- **Obtener Rol:** `GET /api/private/roles/{id}`
- **Actualizar Rol:** `PUT /api/private/roles/{id}`
- **Eliminar Rol:** `DELETE /api/private/roles/{id}`

#### 4.3 CRUD de Permisos
- **Crear Permiso:** `POST /api/private/permissions`
  - Parámetros: `url`, `method`, `model`
  - Ejemplo: URL=`/api/private/users/?`, Método=`GET`
- **Listar Permisos:** `GET /api/private/permissions`
- **Obtener Permiso:** `GET /api/private/permissions/{id}`
- **Actualizar Permiso:** `PUT /api/private/permissions/{id}`
- **Eliminar Permiso:** `DELETE /api/private/permissions/{id}`

#### 4.4 Asignación de Roles a Usuarios
- **Asignar Rol:** `POST /api/private/user-role/user/{userId}/role/{roleId}`
- **Obtener Roles del Usuario:** `GET /api/private/user-role/user/{userId}`
- **Eliminar Rol del Usuario:** `DELETE /api/private/user-role/user/{userId}/role/{roleId}`

#### 4.5 Asignación de Permisos a Roles
- **Asignar Permiso:** `POST /api/private/role-permission/role/{roleId}/permission/{permissionId}`
- **Obtener Permisos del Rol:** `GET /api/private/role-permission/role/{roleId}`
- **Eliminar Permiso del Rol:** `DELETE /api/private/role-permission/role/{roleId}/permission/{permissionId}`

---

### 5. **Gestión de Usuarios**

#### 5.1 CRUD de Usuarios
- **Listar Usuarios:** `GET /api/private/users`
- **Obtener Usuario:** `GET /api/private/users/{id}`
- **Actualizar Usuario:** `PUT /api/private/users/{id}`
- **Eliminar Usuario:** `DELETE /api/private/users/{id}`

#### 5.2 Campos del Usuario
| Campo | Tipo | Requerido | Descripción |
|-------|------|----------|-------------|
| id | String | SÍ | UUID automático |
| name | String | SÍ | Nombre completo |
| email | String | SÍ | Email único |
| password | String | SÍ | Encriptado SHA256 |

---

### 6. **Gestión de Perfiles de Usuario**

#### 6.1 CRUD de Perfiles
- **Crear Perfil:** `POST /api/private/profile`
- **Obtener Perfil:** `GET /api/private/profile/{userId}`
- **Actualizar Perfil:** `PUT /api/private/profile/{userId}`
- **Eliminar Perfil:** `DELETE /api/private/profile/{userId}`

#### 6.2 Campos del Perfil (Propuesto)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| userId | String | FK a Usuario |
| photoUrl | String | Foto de perfil (solo lectura) |
| phone | String | Teléfono |
| address | String | Dirección |
| birthDate | Date | Fecha de nacimiento |

---

### 7. **Gestión de Sesiones**

#### 7.1 CRUD de Sesiones
- **Crear Sesión:** `POST /api/private/sessions` (Interno)
- **Obtener Sesión:** `GET /api/private/sessions/{id}`
- **Listar Sesiones:** `GET /api/private/sessions`
- **Actualizar Sesión:** `PUT /api/private/sessions/{id}`
- **Eliminar Sesión:** `DELETE /api/private/sessions/{id}`

#### 7.2 Campos de Sesión
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | String | UUID automático |
| userId | String | FK a Usuario |
| token | String | JWT Token |
| code2FA | String | Código 2FA temporal |
| expiration | Date | Expiración de sesión |
| failedAttempts | Integer | Intentos fallidos 2FA |
| createdAt | Date | Timestamp creación |

---

### 8. **Seguridad con JWT**

#### 8.1 Generación de Tokens
- **Servicio:** `JwtService`
- **Algoritmo:** HS256
- **Secret Key:** Configurado en `application.properties`
- **Expiration:** 1 hora (configurable)
- **Claims Incluidos:**
  - `sub` (subject): Email del usuario
  - `iat` (issued at): Timestamp de creación
  - `exp` (expiration): Timestamp de expiración

#### 8.2 Validación de Tokens
- **Ubicación:** Encabezado `Authorization: Bearer <token>`
- **Validación:** Se realiza en cada request
- **Acciones si es inválido:**
  - Token expirado → Respuesta 401
  - Token corrupto → Respuesta 401
  - Token ausente → Acceso a rutas públicas permitido

#### 8.3 Rutas Públicas vs Privadas
- **Públicas:** `/api/public/**`
  - No requieren JWT
  - Incluyen: login, register, forgot-password, reset-password, oauth-login
- **Privadas:** `/api/private/**`
  - Requieren JWT válido
  - Requieren permisos en rol

---

### 9. **Validación de reCAPTCHA v2**

#### 9.1 Integración con Google
- **Proveedor:** Google reCAPTCHA v2
- **Secret Key:** Guardada en `application.properties`
- **Endpoints donde se usa:**
  - `/api/public/auth/login` (action: "login")
  - `/api/public/auth/forgot-password` (action: "forgot_password")

#### 9.2 Validación
- **Parámetro:** `recaptchaToken` en el request
- **Validación con Google:** POST a `https://www.google.com/recaptcha/api/siteverify`
- **Score Mínimo:** 0.5
- **Errores Manejados:**
  - Token expirado
  - Token inválido
  - Action no coincide
  - Score bajo (< 0.5)

---

### 10. **Encriptación de Contraseñas**

#### 10.1 Algoritmo SHA256
- **Servicio:** `EncryptionService`
- **Método:** `convertSHA256(password)`
- **Uso:**
  - Registro de usuarios
  - Login (comparación)
  - Reset de contraseña
  - OAuth (contraseña aleatoria para nuevos usuarios)

#### 10.2 Contraseña para OAuth
- Para nuevos usuarios OAuth, se genera contraseña aleatoria:
  ```java
  String randomPassword = UUID.randomUUID().toString();
  String hashedPassword = encryptionService.convertSHA256(randomPassword);
  ```

---

### 11. **Notificaciones y Emails**

#### 11.1 Envío de Emails
- **Servicio:** Microservicio independiente en puerto 5000
- **Protocolo:** HTTP REST
- **Método:** POST

#### 11.2 Tipos de Emails Enviados

**A) Código de Verificación 2FA**
- **Endpoint:** `/api/enviar-codigo-verificacion`
- **Parámetros Requeridos:**
  ```json
  {
    "nombre": "Juan Pérez",
    "destinatario": "juan@example.com",
    "remitente": "noreply@sistema.com",
    "asunto": "Tu código de verificación",
    "codigo": "123456"
  }
  ```

**B) Email de Bienvenida (Registro)**
- **Endpoint:** `/api/enviar-html` o similar
- **Contenido:** Bienvenida personalizada

**C) Email de Recuperación de Contraseña**
- **Endpoint:** `/api/enviar-cambio-contraseña`
- **Parámetros Requeridos:**
  ```json
  {
    "nombre": "Juan Pérez",
    "destinatario": "juan@example.com",
    "remitente": "noreply@sistema.com",
    "asunto": "Restablecer tu contraseña",
    "urlCambio": "http://localhost:4200/reset-password?token=xxx",
    "tiempoValidez": "30 minutos"
  }
  ```

#### 11.3 Manejo de Errores
- Si el servicio de notificaciones falla, el backend **no falla** la solicitud
- Se registra en logs pero se retorna éxito al usuario (por seguridad)
- El usuario no debe saber si hubo error de email

---

### 12. **Sistema de Validación de Permisos (Interceptor)**

#### 12.1 Funcionamiento del SecurityInterceptor
- **Ubicación:** `SecurityInterceptor.java`
- **Activación:** Cada request que llegue
- **Flujo:**
  1. Verifica si es request OPTIONS (CORS) → Permitido
  2. Extrae JWT del encabezado
  3. Obtiene usuario del JWT
  4. Normaliza la URL del request
  5. Busca permiso en BD
  6. Verifica roles del usuario
  7. Verifica RolePermissions
  8. Retorna 403 si no tiene acceso

#### 12.2 Normalización de URLs
- **Propósito:** Convertir URLs con parámetros dinámicos en formato estándar
- **Patrón:** Reemplaza:
  - `{userId}` → `?`
  - UUIDs → `?`
  - ObjectIds MongoDB → `?`
  - Números → `?`
- **Ejemplo:**
  ```
  Original: /api/private/user-role/user/41e20eb1-c851-42fa-ba4d-957a5b223946
  Normalizada: /api/private/user-role/user/?
  ```
- **Problema Actual:** La normalización convierte `/api/private/user-role/user/{userId}` en `/api/private/user-role/user/?`, pero en BD se guardó con `?` literal
- **Estado:** ⚠️ FUNCIONA pero hay inconsistencias en cómo se almacenan los permisos

---

### 13. **Logging y Debugging**

#### 13.1 Niveles de Log
- **INFO:** Operaciones normales (login exitoso, token generado, etc.)
- **WARN:** Situaciones anormales pero recuperables (login fallido, token expirado)
- **ERROR:** Errores graves (BD inaccesible, fallo crítico)

#### 13.2 Logs Implementados
- Login y autenticación
- Verificación 2FA (con comparación de códigos)
- Validación de reCAPTCHA
- Reset de contraseña
- Llamadas a microservicio de notificaciones
- Validación de permisos
- Creación/actualización de sesiones

---

## ❌ Funcionalidades NO Implementadas

### 1. **Refresh Token**
- ❌ No existe endpoint `/api/public/auth/refresh-token`
- ❌ Los JWT tokens expiran y requieren volver a hacer login
- **Recomendación:** Implementar refresh tokens con expiración más larga

### 2. **Logout Explícito**
- ❌ No existe endpoint `/api/public/auth/logout`
- ❌ El logout se hace solo en frontend (eliminando JWT)
- **Recomendación:** Implementar lista negra de tokens (token blacklist)

### 3. **Multi-Sesión**
- ⚠️ Parcialmente: No se valida si hay múltiples sesiones activas del mismo usuario
- **Recomendación:** Permitir/bloquear múltiples sesiones simultáneas

### 4. **Rate Limiting**
- ❌ No existe limitación de intentos de login
- ❌ No existe limitación de solicitudes a forgot-password
- **Recomendación:** Implementar rate limiting por IP o email

### 5. **Autenticación con Contraseña Temporal**
- ❌ No existe cambio de contraseña en primer login
- ❌ No existe validación de contraseña temporal

### 6. **Cambio de Contraseña Autenticado**
- ❌ No existe endpoint para que usuario logueado cambie su contraseña
- ⚠️ Solo existe reset de contraseña olvidada

### 7. **Recuperación de Cuenta**
- ❌ No existe validación de preguntas de seguridad
- ❌ No existe verificación mediante teléfono
- **Nota:** Solo existe reset por email

### 8. **Bloqueo de Cuenta**
- ❌ No existe bloqueo automático por múltiples intentos fallidos
- ❌ No existe desbloqleo de cuenta

### 9. **Auditoría Completa**
- ⚠️ Logging existe pero sin tabla de auditoría en BD
- **Recomendación:** Crear tabla audit_logs con todas las operaciones sensibles

### 10. **Gestión de Tokens**
- ❌ No existe endpoint para ver tokens activos
- ❌ No existe opción de revocar token específico
- ❌ No existe "cerrar sesiones en otros dispositivos"

### 11. **Autenticación Biométrica**
- ❌ No existe soporte para huella dactilar
- ❌ No existe soporte para reconocimiento facial

### 12. **Two-Factor Authentication Alternativo**
- ⚠️ Solo SMS/Email está implementado
- ❌ No existe Google Authenticator/TOTP
- ❌ No existe autenticación biométrica

### 13. **Single Sign-On (SSO)**
- ⚠️ OAuth está implementado pero solo para registro/login
- ❌ No existe sincronización SSO completa entre microservicios

### 14. **Permisos Granulares**
- ⚠️ Permisos por URL/Método están implementados
- ❌ No existe permisos por atributo (field-level permissions)
- ❌ No existe permisos temporales

### 15. **Historial de Acceso**
- ❌ No existe endpoint para ver historial de login del usuario
- ❌ No existe detección de intentos de acceso sospechosos

### 16. **Verificación de Email**
- ⚠️ No existe validación que el email es real durante registro
- ❌ No existe "confirmar email" antes de activar cuenta

### 17. **API Keys**
- ❌ No existe sistema de API Keys para acceso de terceros
- ❌ No existe rate limiting por API Key

### 18. **OAuth Avanzado**
- ⚠️ OAuth básico existe (Google, GitHub, Microsoft)
- ❌ No existe OAuth para "compartir datos entre apps"
- ❌ No existe consentimiento de permisos (scopes)

### 19. **Sesión Remota**
- ❌ No existe validación de "dispositivo confiable"
- ❌ No existe notificación de nuevo login en otro dispositivo
- ❌ No existe bloqueo por cambio de IP sospechosa

### 20. **Migración de Datos**
- ❌ No existe migración de usuarios de otro sistema
- ❌ No existe import/export de usuarios

---

## 📊 Estructuras de Base de Datos

### 1. **Colección: users**
```javascript
{
  _id: ObjectId,
  name: String,
  email: String (unique),
  password: String (SHA256),
  createdAt: Date,
  updatedAt: Date,
  isActive: Boolean
}
```

### 2. **Colección: roles**
```javascript
{
  _id: ObjectId,
  name: String,
  description: String,
  createdAt: Date
}
```

### 3. **Colección: permissions**
```javascript
{
  _id: ObjectId,
  url: String,
  method: String (GET, POST, PUT, DELETE),
  model: String,
  createdAt: Date
}
```

### 4. **Colección: user_roles**
```javascript
{
  _id: ObjectId,
  userId: String (FK → users),
  roleId: String (FK → roles),
  assignedAt: Date
}
```

### 5. **Colección: role_permissions**
```javascript
{
  _id: ObjectId,
  roleId: String (FK → roles),
  permissionId: String (FK → permissions),
  assignedAt: Date
}
```

### 6. **Colección: sessions**
```javascript
{
  _id: ObjectId,
  userId: String (FK → users),
  token: String (JWT),
  code2FA: String,
  expiration: Date,
  failedAttempts: Integer,
  createdAt: Date
}
```

### 7. **Colección: password_reset_tokens**
```javascript
{
  _id: UUID,
  userId: String (FK → users),
  email: String,
  token: String (unique),
  createdAt: Date,
  expiresAt: Date,
  used: Boolean,
  usedAt: Date
}
```

### 8. **Colección: profiles** (Opcional)
```javascript
{
  _id: ObjectId,
  userId: String (FK → users),
  phone: String,
  address: String,
  birthDate: Date,
  photoUrl: String (NO persistido de OAuth),
  createdAt: Date,
  updatedAt: Date
}
```

---

## 🔌 Endpoints de la API

### Autenticación Pública

| Método | Endpoint | Descripción | Require JWT |
|--------|----------|-------------|------------|
| POST | `/api/public/auth/register` | Registrar usuario | ❌ |
| POST | `/api/public/auth/login` | Iniciar login con 2FA | ❌ |
| POST | `/api/public/auth/verify-2fa` | Verificar código 2FA | ❌ |
| POST | `/api/public/auth/resend-2fa` | Reenviar código 2FA | ❌ |
| POST | `/api/public/auth/cancel-2fa` | Cancelar sesión 2FA | ❌ |
| POST | `/api/public/auth/oauth-login` | Login OAuth (Google, GitHub, Microsoft) | ❌ |
| POST | `/api/public/auth/forgot-password` | Solicitar reset de contraseña | ❌ |
| POST | `/api/public/auth/reset-password` | Cambiar contraseña con token | ❌ |

### Usuarios (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/users` | Listar usuarios | ✅ | ✅ |
| GET | `/api/private/users/{id}` | Obtener usuario | ✅ | ✅ |
| PUT | `/api/private/users/{id}` | Actualizar usuario | ✅ | ✅ |
| DELETE | `/api/private/users/{id}` | Eliminar usuario | ✅ | ✅ |

### Roles (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/roles` | Listar roles | ✅ | ✅ |
| POST | `/api/private/roles` | Crear rol | ✅ | ✅ |
| GET | `/api/private/roles/{id}` | Obtener rol | ✅ | ✅ |
| PUT | `/api/private/roles/{id}` | Actualizar rol | ✅ | ✅ |
| DELETE | `/api/private/roles/{id}` | Eliminar rol | ✅ | ✅ |

### Permisos (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/permissions` | Listar permisos | ✅ | ✅ |
| POST | `/api/private/permissions` | Crear permiso | ✅ | ✅ |
| GET | `/api/private/permissions/{id}` | Obtener permiso | ✅ | ✅ |
| PUT | `/api/private/permissions/{id}` | Actualizar permiso | ✅ | ✅ |
| DELETE | `/api/private/permissions/{id}` | Eliminar permiso | ✅ | ✅ |

### Relaciones Usuario-Rol (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/user-role/user/{userId}` | Obtener roles del usuario | ✅ | ✅ |
| POST | `/api/private/user-role/user/{userId}/role/{roleId}` | Asignar rol a usuario | ✅ | ✅ |
| DELETE | `/api/private/user-role/user/{userId}/role/{roleId}` | Quitar rol al usuario | ✅ | ✅ |

### Relaciones Rol-Permiso (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/role-permission/role/{roleId}` | Obtener permisos del rol | ✅ | ✅ |
| POST | `/api/private/role-permission/role/{roleId}/permission/{permissionId}` | Asignar permiso a rol | ✅ | ✅ |
| DELETE | `/api/private/role-permission/role/{roleId}/permission/{permissionId}` | Quitar permiso al rol | ✅ | ✅ |

### Perfil (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/profile/{userId}` | Obtener perfil | ✅ | ✅ |
| PUT | `/api/private/profile/{userId}` | Actualizar perfil | ✅ | ✅ |
| POST | `/api/private/profile` | Crear perfil | ✅ | ✅ |
| DELETE | `/api/private/profile/{userId}` | Eliminar perfil | ✅ | ✅ |

### Sesiones (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| GET | `/api/private/sessions` | Listar sesiones | ✅ | ✅ |
| GET | `/api/private/sessions/{id}` | Obtener sesión | ✅ | ✅ |
| POST | `/api/private/sessions` | Crear sesión | ✅ | ✅ |
| PUT | `/api/private/sessions/{id}` | Actualizar sesión | ✅ | ✅ |
| DELETE | `/api/private/sessions/{id}` | Eliminar sesión | ✅ | ✅ |

### Notificaciones (Privado)

| Método | Endpoint | Descripción | Require JWT | Require Permiso |
|--------|----------|-------------|------------|-----------------|
| POST | `/api/private/notification/send` | Enviar notificación | ✅ | ✅ |

---

## 🔄 Flujos de Autenticación

### Flujo 1: Login Tradicional con 2FA

```
┌─────────────────────┐
│  Usuario en Login   │
│  (Frontend)         │
└──────────┬──────────┘
           │
           ├─► Ingresa: email, contraseña
           │
           ├─► Realiza reCAPTCHA
           │
           ├─► POST /api/public/auth/login
           │
┌──────────▼──────────┐
│  Backend            │
│  (Verificación)     │
└──────────┬──────────┘
           │
           ├─► Valida reCAPTCHA con Google
           │   ✓ Score > 0.5
           │   ✓ Action = "login"
           │
           ├─► Busca usuario por email
           │   ✓ Encuentra usuario
           │
           ├─► Compara SHA256(contraseña)
           │   ✓ Coincide
           │
           ├─► Genera código 2FA (6 dígitos)
           │
           ├─► Crea sesión temporal
           │   • code2FA: "123456"
           │   • expiration: NOW + 10 minutos
           │   • failedAttempts: 0
           │
           ├─► Llama microservicio notificaciones
           │   POST http://localhost:5000/api/enviar-codigo-verificacion
           │
           ├─► Envía email con código
           │
           └─► Retorna sessionId
               (SIN JWT aún)
               
┌──────────────────┐
│  Usuario          │
│  (Recibe email)   │
└──────────┬───────┘
           │
           ├─► Recibe email con código
           │
           ├─► Ingresa código en formulario
           │
           ├─► POST /api/public/auth/verify-2fa
           │   {
           │     sessionId: "xxx",
           │     code2FA: "123456"
           │   }
           │
┌──────────▼──────────┐
│  Backend            │
│  (Verificación 2FA) │
└──────────┬──────────┘
           │
           ├─► Busca sesión por sessionId
           │   ✓ Encontrada
           │
           ├─► Valida que NO expiró
           │   ✓ expiration > NOW
           │
           ├─► Compara código
           │   ✓ code2FA = "123456"
           │
           ├─► Valida intentos
           │   ✓ failedAttempts < 3
           │
           ├─► Genera JWT Token
           │   (HS256, expira en 1 hora)
           │
           ├─► Actualiza sesión
           │   • token: "eyJhbGci..."
           │   • expiration: NOW + 1 hora
           │   • failedAttempts: 0
           │
           └─► Retorna JWT + info usuario
               {
                 token: "eyJhbGci...",
                 userId: "xxx",
                 email: "user@example.com"
               }

┌──────────────────┐
│  Usuario          │
│  (Logueado)       │
└──────────────────┘
           │
           ├─► Almacena JWT en localStorage
           │
           └─► Ahora puede acceder a /api/private/**
               (Enviando: Authorization: Bearer <token>)
```

### Flujo 2: OAuth Login (Google/GitHub/Microsoft)

```
┌─────────────────────┐
│  Usuario en Login   │
│  (Frontend)         │
└──────────┬──────────┘
           │
           ├─► Hace clic "Login con Google"
           │
           ├─► Firebase SDK abre ventana OAuth
           │
           ├─► Usuario autoriza en Google
           │
           ├─► Firebase retorna:
           │   {
           │     email: "user@gmail.com",
           │     name: "Juan Pérez",
           │     photoUrl: "https://googleusercontent.com/...",
           │     provider: "google",
           │     firebaseToken: "..."
           │   }
           │
           ├─► POST /api/public/auth/oauth-login
           │
┌──────────▼──────────┐
│  Backend            │
│  (OAuth Processing) │
└──────────┬──────────┘
           │
           ├─► Valida campos requeridos
           │   ✓ email, name, provider
           │
           ├─► Valida provider
           │   ✓ google, github, microsoft
           │
           ├─► Busca usuario por email
           │
           ├─► CASO 1: Usuario EXISTE
           │   ├─► Actualiza nombre si cambió
           │   └─► Obtiene usuario de BD
           │
           ├─► CASO 2: Usuario NO existe
           │   ├─► Genera contraseña aleatoria
           │   ├─► Encripta SHA256
           │   ├─► Crea usuario en BD
           │   └─► Asigna rol ciudadano
           │
           ├─► Genera JWT Token
           │
           └─► Retorna:
               {
                 token: "eyJhbGci...",
                 id: "uuid-usuario",
                 email: "user@gmail.com",
                 name: "Juan Pérez",
                 photoUrl: "https://googleusercontent.com/..."
               }

┌──────────────────┐
│  Usuario          │
│  (Logueado)       │
└──────────────────┘
           │
           ├─► Almacena JWT en localStorage
           │
           ├─► Almacena photoUrl (frontend solo)
           │   (NO se persistió en BD)
           │
           └─► Acceso a /api/private/** con JWT
```

### Flujo 3: Recuperación de Contraseña Olvidada

```
┌──────────────────────┐
│  Usuario (Olvido PW) │
│  (Frontend)          │
└──────────┬───────────┘
           │
           ├─► Hace clic "¿Olvidaste tu contraseña?"
           │
           ├─► Ingresa email
           │
           ├─► Realiza reCAPTCHA
           │
           ├─► POST /api/public/auth/forgot-password
           │   {
           │     email: "user@example.com",
           │     recaptchaToken: "token"
           │   }
           │
┌──────────▼──────────────┐
│  Backend                │
│  (Reset Initialization) │
└──────────┬──────────────┘
           │
           ├─► Valida reCAPTCHA
           │   ✓ Score > 0.5
           │   ✓ Action = "forgot_password"
           │
           ├─► Busca usuario por email
           │
           ├─► CASO 1: Email NO existe
           │   ├─► NO revela esto
           │   └─► Retorna mensaje genérico
           │
           ├─► CASO 2: Email EXISTE
           │   ├─► Genera UUID único
           │   ├─► Crea PasswordResetToken
           │   │   • token: "uuid-xxx"
           │   │   • userId: "usuario-id"
           │   │   • email: "user@example.com"
           │   │   • createdAt: NOW
           │   │   • expiresAt: NOW + 30 minutos
           │   │   • used: false
           │   │
           │   ├─► Llama servicio notificaciones
           │   │   POST http://localhost:5000/api/enviar-cambio-contraseña
           │   │   {
           │   │     nombre: "Juan",
           │   │     destinatario: "user@example.com",
           │   │     remitente: "noreply@sistema.com",
           │   │     asunto: "Restablecer contraseña",
           │   │     urlCambio: "http://localhost:4200/reset-password?token=uuid-xxx",
           │   │     tiempoValidez: "30 minutos"
           │   │   }
           │   │
           │   └─► Envía email
           │
           └─► SIEMPRE retorna mensaje genérico
               (igual para ambos casos)
               {
                 message: "Si el email existe, recibirá instrucciones..."
               }

┌─────────────────────┐
│  Usuario (email)    │
│  (Recibe correo)    │
└──────────┬──────────┘
           │
           ├─► Recibe email con link
           │   http://localhost:4200/reset-password?token=uuid-xxx
           │
           ├─► Hace clic en link
           │
           ├─► Frontend abre formulario
           │
           ├─► Ingresa nueva contraseña
           │
           ├─► POST /api/public/auth/reset-password
           │   {
           │     token: "uuid-xxx",
           │     newPassword: "nuevaPass123"
           │   }
           │
┌──────────▼──────────────┐
│  Backend                │
│  (Reset Execution)      │
└──────────┬──────────────┘
           │
           ├─► Valida campos
           │   ✓ token presente
           │   ✓ newPassword >= 8 caracteres
           │
           ├─► Busca token en BD
           │
           ├─► CASO 1: Token NO existe
           │   └─► Error 400: "Token inválido o expirado"
           │
           ├─► CASO 2: Token expiró
           │   ├─► (expiresAt < NOW)
           │   └─► Error 400: "Token expirado"
           │
           ├─► CASO 3: Token ya fue usado
           │   ├─► (used = true)
           │   └─► Error 400: "Token ya fue utilizado"
           │
           ├─► CASO 4: Token VÁLIDO
           │   ├─► Busca usuario asociado
           │   ├─► Encripta nueva contraseña (SHA256)
           │   ├─► Actualiza password en usuario
           │   ├─► Marca token como usado
           │   │   • used: true
           │   │   • usedAt: NOW
           │   ├─► Llama servicio notificaciones (opcional)
           │   │   (Confirmar cambio de contraseña)
           │   └─► Retorna éxito
           │
           └─► Response:
               {
                 message: "Contraseña restablecida. Puede iniciar sesión."
               }

┌─────────────────────┐
│  Usuario (logueado) │
└─────────────────────┘
           │
           ├─► Puede hacer login con nueva contraseña
           │
           └─► Acceso restaurado ✅
```

### Flujo 4: Validación de Permisos en Request

```
┌─────────────────────┐
│  Cliente            │
│  (Frontend)         │
└──────────┬──────────┘
           │
           ├─► GET /api/private/users
           │   Authorization: Bearer eyJhbGci...
           │
┌──────────▼──────────────┐
│  Spring Web             │
│  (SecurityInterceptor)  │
└──────────┬──────────────┘
           │
           ├─► preHandle() ejecuta
           │
           ├─► ¿Es request OPTIONS? (CORS)
           │   ✓ SÍ → Permitir
           │   ✗ NO → Continuar
           │
           ├─► Extrae token del header
           │   Authorization: Bearer eyJhbGci...
           │
           ├─► Llama ValidatorsService.validationRolePermission()
           │
┌──────────▼──────────────┐
│  ValidatorsService      │
│  (Validación)           │
└──────────┬──────────────┘
           │
           ├─► getUser() del token JWT
           │   • Decodifica JWT
           │   • Extrae email del claim "sub"
           │   • Busca usuario en BD
           │   • Retorna Usuario
           │
           ├─► Normaliza URL
           │   Original: /api/private/users/41e20eb1...
           │   Normalizada: /api/private/users/?
           │
           ├─► getPermission(url, method)
           │   • Busca en BD: permission.url = "/api/private/users/?"
           │   • Busca en BD: permission.method = "GET"
           │   • Retorna Permission
           │
           ├─► Obtiene roles del usuario
           │   • Busca en UserRole table
           │   • userId = usuario.id
           │   • Retorna lista de roles
           │
           ├─► POR CADA ROL:
           │   ├─► Busca RolePermission
           │   │   roleId = rol.id
           │   │   permissionId = permission.id
           │   │
           │   ├─► ¿Existe RolePermission?
           │   │   ✓ SÍ → Retorna TRUE (permitido)
           │   │   ✗ NO → Continúa con siguiente rol
           │
           └─► NINGÚN rol tiene permiso
               └─► Retorna FALSE (denegado)

┌──────────▼──────────────┐
│  Spring Web             │
│  (Response)             │
└──────────┬──────────────┘
           │
           ├─► ¿Tiene acceso? (resultado validación)
           │
           ├─► SÍ → Permite request
           │   • Ejecuta controller
           │   • Retorna 200
           │
           ├─► NO → Deniega request
           │   • response.setStatus(403 FORBIDDEN)
           │   • response.write("Acceso denegado")
           │   • Retorna 403

┌─────────────────────┐
│  Cliente            │
│  (Frontend)         │
└─────────────────────┘
           │
           ├─► 200 OK
           │   • Datos del usuario
           │
           └─► 403 FORBIDDEN
               • "Acceso denegado"
```

---

## 🔐 Sistemas de Seguridad

### 1. **Encriptación de Contraseñas**
- ✅ Implementado: SHA256
- ⚠️ Mejor práctica: Bcrypt o Argon2
- **Mejora Recomendada:** Migrar a Bcrypt

### 2. **JWT Tokens**
- ✅ Implementado: HS256 (HMAC-SHA256)
- ✅ Secret key configurado en properties
- ⚠️ No hay refresh tokens
- ⚠️ No hay token blacklist

### 3. **reCAPTCHA v2**
- ✅ Implementado: Validación de score
- ✅ Score mínimo: 0.5
- ✅ Validación de action
- ⚠️ No hay rate limiting por IP

### 4. **2FA (Two Factor Authentication)**
- ✅ Implementado: Código por email
- ✅ Código 6 dígitos aleatorio
- ✅ Válido 10 minutos
- ✅ Máximo 3 intentos
- ⚠️ No existe Google Authenticator/TOTP

### 5. **CORS (Cross-Origin Resource Sharing)**
- ⚠️ Estado: Parcialmente configurado
- **Recomendación:** Revisar WebConfig.java

### 6. **Rate Limiting**
- ❌ No implementado
- **Recomendación:** Implementar usando Redis

### 7. **Validación de Datos**
- ✅ Validación de email
- ✅ Validación de contraseña (longitud mínima)
- ✅ Validación de campos requeridos
- ⚠️ No hay validación de formato de email

### 8. **Logging y Monitoreo**
- ✅ Logs en INFO, WARN, ERROR
- ⚠️ No hay métricas de sistema
- ⚠️ No hay alertas de seguridad

---

## ⚠️ Problemas Conocidos

### 1. **Normalización de URLs con Parámetros Dinámicos**
- **Problema:** El interceptor normaliza `/api/private/user-role/user/{userId}` a `/api/private/user-role/user/?`
- **Causa:** Reemplazo de UUIDs y parámetros dinámicos
- **Impacto:** Los permisos deben guardarse con `?` en la BD
- **Estado:** 🟡 FUNCIONA pero con inconsistencias
- **Error en logs:**
  ```
  ❌ PERMISO NO ENCONTRADO en BD para: /api/private/user-role/user/? GET
  ```
- **Solución:** Asegurar que todos los permisos se guardan con `?` literal

### 2. **Email No Llega**
- **Problema:** Frontend muestra error 500 al enviar reset de contraseña
- **Causa:** Formato incorrecto del payload al microservicio de notificaciones
- **Último error:**
  ```
  Faltan campos requeridos: nombre, destinatario, remitente, asunto, urlCambio, tiempoValidez
  ```
- **Estado:** 🟢 RESUELTO (verificar backend.log)
- **Mejora:** Agregar validación de formato en NotificationServiceClient

### 3. **photoUrl en OAuth**
- **Problema:** La URL de foto se envía pero no se persiste en BD
- **Comportamiento:** Se retorna en response pero solo desde el request
- **Estado:** 🟢 INTENCIONADO (por diseño de seguridad)

### 4. **Sesiones Múltiples**
- **Problema:** Un usuario puede tener múltiples sesiones activas
- **Impacto:** No hay validación si ya está logueado en otro lugar
- **Estado:** ⚠️ REQUIERE IMPLEMENTACIÓN

### 5. **Tokens Expirados No Se Revocan**
- **Problema:** Después que expira, el token no se invalida explícitamente
- **Impacto:** Requiere implementar token blacklist
- **Estado:** ⚠️ REQUIERE IMPLEMENTACIÓN

### 6. **Error 500 en Endpoints de Rol-Permiso**
- **Problema:** Algunos requests retornan 500
- **Última mención:** `/api/private/role-permission/role/{roleId}/permission/{permissionId}`
- **Estado:** ⚠️ REQUIERE DEBUGGING

### 7. **CORS Issues**
- **Problema:** Frontend obtiene errores de CORS en algunos requests
- **Última mención:** `Cross-Origin-Opener-Policy` bloquea `window.close()`
- **Estado:** ⚠️ REQUIERE CONFIGURACIÓN ADICIONAL

### 8. **reCAPTCHA Action Name Validation**
- **Problema:** El nombre de acción debe cumplir patron `[A-Za-z/_]`
- **Error:** "El nombre de la acción no es válido"
- **Status:** 🟡 PARCIALMENTE SOLUCIONADO (requiere revisar frontend)

---

## 📈 Recomendaciones

### Corto Plazo (Urgente)

1. **Validar Todas las Rutas con Permisos**
   - Auditoria: Verificar que todos los permisos se guardaron con URL normalizada
   - Test: Hacer requests a cada endpoint privado y verificar acceso

2. **Agregar Pruebas Unitarias**
   - AuthServiceTests
   - ValidatorsServiceTests
   - JwtServiceTests

3. **Mejorar Logging**
   - Agregar identificadores únicos (traceId) a cada request
   - Logs estructurados (JSON) para facilitar parsing
   - Agregar timestamp de microsegundos

4. **Documentación de API**
   - Generar Swagger/OpenAPI
   - Incluir ejemplos de request/response

### Mediano Plazo (1-2 meses)

1. **Implementar Refresh Tokens**
   - Crear tabla refresh_tokens
   - Endpoint POST /api/public/auth/refresh-token
   - Expiración más larga (7 días)

2. **Agregar Logout Explícito**
   - Endpoint POST /api/public/auth/logout
   - Token blacklist o sesión invalidada

3. **Rate Limiting**
   - Por IP: Máx 5 login/hora
   - Por email: Máx 3 forgot-password/hora
   - Usar Redis para almacenar contadores

4. **Auditoría Completa**
   - Crear tabla audit_logs
   - Registrar: usuario, acción, timestamp, IP, resultado
   - Generar reportes de acceso

5. **Migrar de SHA256 a Bcrypt**
   - Crear migration script
   - Encriptar contraseñas existentes
   - Implementar Bcrypt en AuthService

### Largo Plazo (3+ meses)

1. **OAuth Avanzado**
   - Agregar scopes de permisos
   - Implementar consentimiento del usuario
   - Soportar más providers

2. **Autenticación Biométrica**
   - WebAuthn/FIDO2
   - Soporte para huella dactilar
   - Reconocimiento facial

3. **Single Sign-On (SSO)**
   - SAML 2.0
   - OpenID Connect
   - Sincronización entre microservicios

4. **Multi-Sesión Controlada**
   - Limit: 1 o N sesiones por usuario
   - "Cerrar otras sesiones"
   - Notificación de nuevo login

5. **Historial de Acceso**
   - Endpoint GET /api/private/users/{id}/login-history
   - Detección de anomalías
   - Alertas de acceso sospechoso

---

## 🎓 Conclusiones

### ✅ Lo Que Funciona Bien

1. **Autenticación 2FA Robusta**
   - Flujo seguro con código por email
   - Validación de intentos
   - Expiración de sesión

2. **Gestión de Roles y Permisos**
   - Modelo N-N bien implementado
   - Validación en interceptor
   - Granularidad por URL y método

3. **OAuth Básico**
   - Integración con Firebase
   - Múltiples providers
   - Creación automática de usuarios

4. **Recuperación de Contraseña**
   - Flujo seguro con token único
   - Integración con servicio de notificaciones
   - Respuestas genéricas (sin revelar usuarios)

5. **Encriptación**
   - JWT tokens secure
   - SHA256 para contraseñas
   - Validación de reCAPTCHA

### ⚠️ Lo Que Necesita Mejora

1. Refresh tokens
2. Token blacklist/logout explícito
3. Rate limiting
4. Auditoría en BD
5. Migración a Bcrypt
6. Pruebas automatizadas
7. Documentación API
8. CORS configuration
9. Error handling más consistente
10. Validación de datos más estricta

### 🚀 Siguiente Paso

Según el contexto, el profesor probablemente pedirá:
- **Una nueva relación N-N** (diferente de User-Role y Role-Permission)
- Posibles opciones:
  - Usuarios y Departamentos
  - Usuarios y Proyectos
  - Roles y Funcionalidades
  - Usuarios y Perfiles de Seguridad

**Recomendación para sustentación:** Preparar el flujo paso a paso de cómo se implementaría una nueva relación N-N desde cero (Models → Repositories → Services → Controllers).

---

**Documento generado:** 27 de Mayo de 2026  
**Versión:** 1.0  
**Estado:** Completo

