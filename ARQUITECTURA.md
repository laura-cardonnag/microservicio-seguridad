# 🏗️ ARQUITECTURA DEL MICROSERVICIO DE SEGURIDAD

## 📌 Información Clave del Proyecto

| Propiedad | Valor |
|-----------|-------|
| **Nombre del Proyecto** | microservicio-seguridad |
| **Puerto Principal** | 8181 |
| **Framework** | Spring Boot 4.0.3 |
| **Java Version** | 17 |
| **Base de Datos** | MongoDB Atlas |
| **BD Name** | db_seguridad |
| **Package Principal** | `com.lk.microservicio_seguridad` |
| **Build Tool** | Maven (mvnw) |
| **Estado** | Production-Ready |

---

## 📂 Estructura de Carpetas del Proyecto

```
C:\desarrolloBackend\microservicio-seguridad/
│
├── pom.xml                                  # Configuración Maven, dependencias
├── mvnw / mvnw.cmd                         # Maven Wrapper (ejecutar sin Maven instalado)
├── application-example.properties           # Ejemplo de configuración
│
├── src/main/
│   ├── java/com/lk/microservicio_seguridad/
│   │   ├── MicroservicioSeguridadApplication.java    # Main application class
│   │   │
│   │   ├── Configurations/                           # 🔧 Spring Beans & Configuración
│   │   │   ├── GlobalExceptionHandler.java           # Manejo global de excepciones
│   │   │   └── WebConfig.java                        # Configuración Web (CORS, interceptores)
│   │   │
│   │   ├── Controllers/                              # 🌐 REST Endpoints (10 controllers)
│   │   │   ├── AuthController.java                   # POST /api/public/auth/**
│   │   │   ├── UserController.java                   # GET /api/private/users**
│   │   │   ├── RoleController.java                   # GET /api/private/roles**
│   │   │   ├── PermissionController.java             # GET /api/private/permissions**
│   │   │   ├── UserRoleController.java               # N-N: Usuario ↔ Rol
│   │   │   ├── RolePermissionController.java         # N-N: Rol ↔ Permiso
│   │   │   ├── ProfileController.java                # User profiles
│   │   │   ├── SessionController.java                # Session management
│   │   │   ├── NotificationController.java           # Notifications
│   │   │   └── SecurityController.java               # Security operations
│   │   │
│   │   ├── Services/                                 # 🔄 Lógica de negocio (16 servicios)
│   │   │   ├── AuthService.java                      # Login, registro, reset contraseña
│   │   │   ├── UserService.java                      # CRUD usuarios
│   │   │   ├── RoleService.java                      # CRUD roles
│   │   │   ├── PermissionService.java                # CRUD permisos
│   │   │   ├── UserRoleService.java                  # Asignar roles a usuarios
│   │   │   ├── RolePermissionService.java            # Asignar permisos a roles
│   │   │   ├── SessionService.java                   # Gestión de sesiones
│   │   │   ├── ProfileService.java                   # Perfil de usuario
│   │   │   ├── SecurityService.java                  # Operaciones de seguridad
│   │   │   ├── JwtService.java                       # Generar/validar JWT tokens
│   │   │   ├── EncryptionService.java                # SHA256, hashing
│   │   │   ├── RecaptchaService.java                 # Validar reCAPTCHA v2
│   │   │   ├── NotificationService.java              # Enviar notificaciones
│   │   │   ├── NotificationServiceClient.java        # Cliente HTTP → puerto 5000
│   │   │   ├── ValidatorsService.java                # Validar roles/permisos en interceptor
│   │   │   └── RandomCodeService.java                # Generar códigos aleatorios
│   │   │
│   │   ├── Repositories/                             # 📦 Acceso a datos MongoDB (8 repos)
│   │   │   ├── UserRepository.java                   # find by email, CRUD
│   │   │   ├── RoleRepository.java                   # CRUD roles
│   │   │   ├── PermissionRepository.java             # find by url+method, CRUD
│   │   │   ├── UserRoleRepository.java               # find by userId, roleId
│   │   │   ├── RolePermissionRepository.java         # find by roleId, permissionId
│   │   │   ├── SessionRepository.java                # Session CRUD
│   │   │   ├── ProfileRepository.java                # Profile CRUD
│   │   │   └── PasswordResetTokenRepository.java     # Reset tokens CRUD
│   │   │
│   │   ├── Exceptions/                               # 🚨 Custom exceptions
│   │   │   └── RecaptchaValidationException.java     # reCAPTCHA error
│   │   │
│   │   ├── Interceptors/                             # 🔒 Middleware
│   │   │   └── SecurityInterceptor.java              # Valida JWT + permisos en cada request
│   │   │
│   │   └── models/                                   # 📋 Entity models (15 clases)
│   │       ├── User.java                             # @Document: usuario
│   │       ├── Role.java                             # @Document: rol
│   │       ├── Permission.java                       # @Document: permiso
│   │       ├── UserRole.java                         # @Document: relación N-N
│   │       ├── RolePermission.java                   # @Document: relación N-N
│   │       ├── Session.java                          # @Document: sesión
│   │       ├── PasswordResetToken.java               # @Document: token reset
│   │       ├── Profile.java                          # @Document: perfil usuario
│   │       ├── LoginRequest.java                     # DTO: email + password
│   │       ├── ForgotPasswordRequest.java            # DTO: email + recaptchaToken
│   │       ├── ResetPasswordRequest.java             # DTO: token + newPassword
│   │       ├── OAuthLoginRequest.java                # DTO: OAuth datos
│   │       ├── OAuthLoginResponse.java               # DTO: JWT + user info
│   │       ├── RecaptchaResponse.java                # DTO: respuesta Google
│   │       └── Verify2FARequest.java                 # DTO: sessionId + code
│   │
│   └── resources/
│       └── application.properties                     # 🔧 Configuración (BD, JWT, reCAPTCHA)
│
├── src/test/
│   └── java/com/lk/microservicio_seguridad/
│       └── MicroservicioSeguridadApplicationTests.java
│
├── target/                                  # Compilados (después de mvn clean install)
│   ├── classes/
│   ├── generated-sources/
│   └── maven-status/
│
├── PROYECTO_FUNCIONALIDAD.md               # 📄 Documentación completa (funcionalidades)
└── ARQUITECTURA.md                         # Este archivo
```

---

## 🏛️ Capas de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (Controllers)              │
│  AuthController | UserController | RoleController | etc...      │
│  REST Endpoints: GET, POST, PUT, DELETE                         │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP Requests
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MIDDLEWARE & INTERCEPTION LAYER                 │
│  SecurityInterceptor → Valida JWT + Permisos en cada request    │
│  GlobalExceptionHandler → Maneja excepciones globales            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     BUSINESS LOGIC LAYER (Services)              │
│  AuthService | UserService | RoleService | JwtService | etc...  │
│  Lógica de negocio, validaciones, cálculos                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                 DATA ACCESS LAYER (Repositories)                 │
│  Spring Data MongoDB → Queries a base de datos                  │
│  UserRepository | RoleRepository | PermissionRepository | etc... │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER (MongoDB)                   │
│  Collections: users, roles, permissions, user_roles,            │
│  role_permissions, sessions, profiles, password_reset_tokens    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Modelos de Datos (MongoDB Collections)

### **ENTIDADES PRINCIPALES**

#### 1. **users** (Usuarios del Sistema)
```javascript
{
  _id: ObjectId,              // PK autogenerado
  name: String,               // Nombre completo
  email: String,              // Email único
  password: String,           // SHA256 encrypted
  createdAt: ISODate,
  updatedAt: ISODate,
  isActive: Boolean
}
```

#### 2. **roles** (Roles de Usuario)
```javascript
{
  _id: ObjectId,
  name: String,               // ej: "Admin", "Usuario", "Moderador"
  description: String,
  createdAt: ISODate
}
```

#### 3. **permissions** (Permisos/Recursos)
```javascript
{
  _id: ObjectId,
  url: String,               // ej: "/api/private/users/?"
  method: String,            // GET, POST, PUT, DELETE
  model: String,             // ej: "User", "Role"
  createdAt: ISODate
}
```

### **RELACIONES N-N**

#### 4. **user_roles** (Relación Usuario ↔ Rol)
```javascript
{
  _id: ObjectId,
  userId: String,            // FK → users._id
  roleId: String,            // FK → roles._id
  assignedAt: ISODate
}
```

#### 5. **role_permissions** (Relación Rol ↔ Permiso)
```javascript
{
  _id: ObjectId,
  roleId: String,            // FK → roles._id
  permissionId: String,      // FK → permissions._id
  assignedAt: ISODate
}
```

### **COLECCIONES DE SOPORTE**

#### 6. **sessions** (Sesiones Activas)
```javascript
{
  _id: ObjectId,
  userId: String,            // FK → users._id
  token: String,             // JWT Token
  code2FA: String,           // Código 2FA temporal
  expiration: ISODate,       // Expiración de sesión
  failedAttempts: Number,    // Intentos fallidos 2FA
  createdAt: ISODate
}
```

#### 7. **password_reset_tokens** (Recuperación Contraseña)
```javascript
{
  _id: UUID,                 // UUID único
  userId: String,            // FK → users._id
  email: String,             // Email para auditoría
  token: String,             // Token único (UNIQUE index)
  createdAt: ISODate,
  expiresAt: ISODate,        // NOW + 30 minutos
  used: Boolean,             // ¿Fue utilizado?
  usedAt: ISODate
}
```

#### 8. **profiles** (Perfiles de Usuario - Opcional)
```javascript
{
  _id: ObjectId,
  userId: String,            // FK → users._id
  phone: String,
  address: String,
  birthDate: ISODate,
  photoUrl: String,          // NO persistido de OAuth
  createdAt: ISODate,
  updatedAt: ISODate
}
```

---

## 🔄 Relaciones Entre Entidades

```
┌──────────────┐
│    users     │  (1)
└──────┬───────┘
       │
       │ (1:N)
       │
       ▼
┌──────────────────┐
│  user_roles      │  (Junction Table)
└──────┬───────────┘
       │
       │ (N:1)
       │
       ▼
┌──────────────┐
│    roles     │  (1)
└──────┬───────┘
       │
       │ (1:N)
       │
       ▼
┌──────────────────────┐
│ role_permissions     │  (Junction Table)
└──────┬───────────────┘
       │
       │ (N:1)
       │
       ▼
┌──────────────┐
│ permissions  │  (1)
└──────────────┘

Flujo de Autorización:
Usuario → UserRole → Rol → RolePermission → Permiso → Endpoint Access
```

---

## 🔌 Dependencias del Proyecto (pom.xml)

```xml
<!-- CORE SPRING BOOT -->
spring-boot-starter-data-mongodb   → ORM MongoDB
spring-boot-starter-webmvc         → Web controllers, REST

<!-- AUTHENTICATION & SECURITY -->
jjwt-api (0.12.3)                  → JWT generation
jjwt-impl (0.12.3)                 → JWT implementation
jjwt-jackson (0.12.3)              → JWT serialization

<!-- UTILITIES -->
lombok                             → @Data, @Autowired, @Service
spring-boot-starter-mail           → Envío de emails (SMTP)

<!-- DATABASE -->
spring-data-mongodb                → Queries, repositories
mongodb-driver-sync                → Driver MongoDB

<!-- EXTERNAL APIs -->
Google reCAPTCHA v2 API             → Validación anti-bots
```

---

## 🌐 Flujos de Comunicación

### **Flujo 1: Request de Cliente → Backend**

```
┌─────────────┐
│   Cliente   │  GET /api/private/users
│  (Frontend) │  Authorization: Bearer eyJhbGci...
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────┐
│  Spring Boot (8181)                  │
│                                      │
│  1. SecurityInterceptor preHandle()  │
│     - Extrae JWT del header          │
│     - Valida JWT signature           │
│     - Obtiene usuario del JWT        │
│     - Normaliza URL                  │
│     - Busca permiso en BD            │
│     - Valida roles + permisos        │
│     → 403 si no tiene acceso         │
│                                      │
│  2. UserController.getAll()          │
│     - Llama a UserService           │
│                                      │
│  3. UserService.getAllUsers()        │
│     - Llama a UserRepository         │
│                                      │
│  4. UserRepository.findAll()         │
│     - Query a MongoDB                │
│                                      │
│  5. MongoDB                          │
│     - Retorna lista de usuarios      │
│                                      │
│  6. Response → Cliente               │
│     HTTP 200 + JSON                  │
└──────────────────────────────────────┘
```

### **Flujo 2: Login con 2FA**

```
┌─────────────┐
│   Usuario   │ POST /api/public/auth/login
│  (Frontend) │ {email, password, recaptchaToken}
└──────┬──────┘
       │
       ▼
┌──────────────────────────┐
│  Backend (8181)          │
│                          │
│  1. AuthController       │
│     POST /login          │
│                          │
│  2. AuthService.login()  │
│     - Valida reCAPTCHA   │  → Google API
│     - Busca usuario      │  → MongoDB
│     - Compara SHA256     │
│     - Genera 2FA code    │
│     - Crea sesión        │  → MongoDB
│                          │
│  3. Envía email 2FA      │  → Puerto 5000
│     HTTP POST http://localhost:5000/api/enviar-codigo-verificacion
│     {nombre, destinatario, codigo}
│                          │
│  4. Response al cliente  │
│     {sessionId, expiresAt}
└──────────────────────────┘

┌─────────────┐
│   Usuario   │ POST /api/public/auth/verify-2fa
│  (Frontend) │ {sessionId, code2FA}
└──────┬──────┘
       │
       ▼
┌──────────────────────────┐
│  Backend (8181)          │
│                          │
│  1. AuthController       │
│     POST /verify-2fa     │
│                          │
│  2. AuthService.verify() │
│     - Busca sesión       │  → MongoDB
│     - Valida no expiró   │
│     - Compara código     │
│     - Genera JWT         │
│     - Actualiza sesión   │  → MongoDB
│                          │
│  3. Response al cliente  │
│     {token, userId, email}
└──────────────────────────┘
```

### **Flujo 3: Reset de Contraseña**

```
┌─────────────┐
│   Usuario   │ POST /api/public/auth/forgot-password
│  (Frontend) │ {email, recaptchaToken}
└──────┬──────┘
       │
       ▼
┌──────────────────────────────┐
│  Backend (8181)              │
│                              │
│  AuthService.forgotPassword()│
│  - Valida reCAPTCHA          │  → Google
│  - Busca usuario por email   │  → MongoDB
│  - Genera token UUID         │
│  - Guarda reset token        │  → MongoDB
│                              │
│  - Envía email con link      │  → Puerto 5000
│    POST /api/enviar-cambio-contraseña
│    {nombre, destinatario, urlCambio, tiempoValidez}
│                              │
│  - Response genérica         │
│    (no revela si existe)     │
└──────────────────────────────┘

┌─────────────┐
│   Usuario   │ POST /api/public/auth/reset-password
│  (Frontend) │ {token, newPassword}
└──────┬──────┘
       │
       ▼
┌──────────────────────────────┐
│  Backend (8181)              │
│                              │
│  AuthService.resetPassword() │
│  - Busca token en BD         │  → MongoDB
│  - Valida no expiró          │
│  - Valida no fue usado       │
│  - Encripta nueva password   │
│  - Actualiza usuario         │  → MongoDB
│  - Marca token como usado    │  → MongoDB
│                              │
│  - Response: Éxito           │
└──────────────────────────────┘
```

### **Flujo 4: OAuth Login**

```
┌─────────────┐
│   Usuario   │ Click "Login con Google"
│  (Frontend) │
└──────┬──────┘
       │
       ▼
┌────────────────────────┐
│  Firebase Auth         │
│  (Google)              │
│                        │
│  Usuario autoriza      │
│  Firebase retorna:     │
│  {email, name, photo}  │
└──────┬─────────────────┘
       │
       ▼
┌──────────────────────────────┐
│  Backend (8181)              │
│                              │
│  POST /api/public/auth/oauth-login
│  {email, name, provider}     │
│                              │
│  AuthService.oauthLogin()    │
│  - Busca usuario por email   │  → MongoDB
│  - Si NO existe:             │
│    • Genera contraseña random│
│    • Encripta SHA256         │
│    • Crea usuario            │  → MongoDB
│    • Asigna rol ciudadano    │  → MongoDB
│  - Genera JWT                │
│  - Retorna token + user      │
└──────────────────────────────┘
```

---

## 🔐 Seguridad en Capas

```
┌────────────────────────────────────┐
│  1. TRANSPORT LAYER                │
│  - HTTPS (en producción)           │
│  - TLS 1.2+                        │
└────────────────────────────────────┘
                │
                ▼
┌────────────────────────────────────┐
│  2. AUTHENTICATION LAYER           │
│  - JWT Tokens (HS256)              │
│  - 2FA (código por email)          │
│  - OAuth (Firebase)                │
│  - reCAPTCHA v2 (Google)           │
└────────────────────────────────────┘
                │
                ▼
┌────────────────────────────────────┐
│  3. AUTHORIZATION LAYER            │
│  - SecurityInterceptor valida:     │
│    • Token válido y no expirado    │
│    • Usuario existe                │
│    • URL normalizada               │
│    • Permiso existe                │
│    • Usuario tiene rol con permiso │
│  - Retorna 403 si acceso denegado  │
└────────────────────────────────────┘
                │
                ▼
┌────────────────────────────────────┐
│  4. DATA LAYER                     │
│  - Contraseñas SHA256              │
│  - Tokens únicos (UUID)            │
│  - Índices para búsquedas seguras  │
│  - UNIQUE constraints              │
└────────────────────────────────────┘
```

---

## 🚀 Flujo Completo de Startup

```
1. JVM Start
   └─► Carga Spring Boot

2. Main Application Class
   └─► MicroservicioSeguridadApplication.java

3. Spring Boot Initialization
   └─► @SpringBootApplication en main class
       ├─► Component Scanning
       │   ├─► Controllers (10)
       │   ├─► Services (16)
       │   ├─► Repositories (8)
       │   └─► Configuraciones
       │
       ├─► Conecta a MongoDB
       │   └─► mongodb+srv://user:pass@cluster0.mongodb.net/db_seguridad
       │
       ├─► Registra Interceptores
       │   └─► SecurityInterceptor en WebConfig
       │
       └─► Inicia servidor Tomcat
           └─► Escucha en puerto 8181

4. Ready
   └─► Backend listo para recibir requests
       - /api/public/** → Sin autenticación
       - /api/private/** → Requiere JWT + Permisos
```

---

## 📡 Comunicación Externa

### **Integraciones HTTP**

```
┌────────────────┐
│  Backend       │
│  (8181)        │
└────────┬───────┘
         │
    ┌────┴────┬──────────────┬──────────────┐
    │          │              │              │
    ▼          ▼              ▼              ▼
┌────────┐ ┌─────────┐ ┌────────────┐ ┌─────────┐
│MongoDB │ │Google   │ │Notificaciones│Frontend │
│Atlas   │ │reCAPTCHA│ │(Python 5000)│(Angular)│
│        │ │API      │ │             │(4200)   │
└────────┘ └─────────┘ └────────────┘ └─────────┘

MongoDB:
- Conexión: mongodb+srv://user:pass@cluster0.mongodb.net
- Database: db_seguridad
- Operaciones: CRUD, Queries, Indexing

Google reCAPTCHA:
- Endpoint: https://www.google.com/recaptcha/api/siteverify
- Method: POST
- Validación de token + score

Notificaciones (5000):
- Endpoints:
  • /api/enviar-codigo-verificacion (2FA)
  • /api/enviar-cambio-contraseña (Reset)
  • /api/enviar-html (General)
- Method: POST
- Formato: JSON

Frontend (4200):
- Angular SPA
- Consume endpoints /api/public y /api/private
- Almacena JWT en localStorage
- Envía JWT en Authorization header
```

---

## 🔑 Configuración Crítica (application.properties)

```properties
# Servidor
server.port=8181
spring.application.name=microservicio-seguridad

# MongoDB
spring.mongodb.uri=mongodb+srv://user:pass@cluster0.mongodb.net/?appName=Cluster0
spring.mongodb.database=db_seguridad

# JWT
jwt.secret=mySuperSecretKeyThatIsAtLeast32CharactersLongForJWT
jwt.expiration=3600000  # 1 hora en milisegundos

# reCAPTCHA
recaptcha.secret-key=6Lcw15EsAAAAANwp0pfJnct1ATpCj7A-iMUCmGxF
recaptcha.min-score=0.5

# Email SMTP (para notificaciones internas)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=app-password
spring.mail.from=noreply@sistema.com

# Frontend URL
app.frontend.url=http://localhost:4200

# Microservicio Notificaciones
notification.verification-url=http://localhost:5000/api/enviar-codigo-verificacion
notification.sender-email=tu-email@gmail.com
```

---

## 📊 Resumen de Componentes

### **10 Controllers**
- AuthController (autenticación)
- UserController (usuarios CRUD)
- RoleController (roles CRUD)
- PermissionController (permisos CRUD)
- UserRoleController (relación N-N)
- RolePermissionController (relación N-N)
- ProfileController (perfiles)
- SessionController (sesiones)
- NotificationController (notificaciones)
- SecurityController (operaciones seguridad)

### **16 Services**
- AuthService, UserService, RoleService, PermissionService
- UserRoleService, RolePermissionService
- SessionService, ProfileService, SecurityService
- JwtService, EncryptionService, RecaptchaService
- NotificationService, NotificationServiceClient
- ValidatorsService, RandomCodeService

### **8 Repositories**
- UserRepository, RoleRepository, PermissionRepository
- UserRoleRepository, RolePermissionRepository
- SessionRepository, ProfileRepository, PasswordResetTokenRepository

### **8 MongoDB Collections**
- users, roles, permissions
- user_roles, role_permissions
- sessions, password_reset_tokens, profiles

### **4 Configuraciones**
- GlobalExceptionHandler (excepciones)
- WebConfig (CORS, interceptores)
- Spring Boot Configuration
- MongoDB Configuration

---

## ✨ Stack Tecnológico Completo

```
Frontend:
  - Angular (SPA)
  - TypeScript
  - RxJS (Observables)
  - Bootstrap / Material
  - Firebase Auth

Backend:
  - Spring Boot 4.0.3
  - Java 17
  - Spring Data MongoDB
  - JWT (jjwt)
  - Lombok (decoradores)
  - Maven (build)

Database:
  - MongoDB Atlas
  - Collections (no relacional)
  - Índices optimizados

External Services:
  - Google reCAPTCHA v2
  - Gmail SMTP
  - Microservicio Notificaciones (Python)
  - Firebase Auth

Infrastructure:
  - Tomcat (embedded en Spring Boot)
  - REST API (HTTP)
  - JSON (data format)
  - CORS (cross-origin)
```

---

## 🎯 Puntos de Entrada (Endpoints Base)

```
PÚBLICOS (sin JWT):
  POST   /api/public/auth/register                    → Registrar
  POST   /api/public/auth/login                       → Login con 2FA (paso 1)
  POST   /api/public/auth/verify-2fa                  → Verificar 2FA (paso 2)
  POST   /api/public/auth/resend-2fa                  → Reenviar código
  POST   /api/public/auth/cancel-2fa                  → Cancelar sesión
  POST   /api/public/auth/oauth-login                 → Login OAuth
  POST   /api/public/auth/forgot-password             → Solicitar reset
  POST   /api/public/auth/reset-password              → Cambiar contraseña

PRIVADOS (requieren JWT + permiso):
  GET    /api/private/users                           → Listar usuarios
  POST   /api/private/roles                           → Crear rol
  GET    /api/private/permissions                     → Listar permisos
  POST   /api/private/user-role/user/{id}/role/{rid}  → Asignar rol
  GET    /api/private/role-permission/role/{rid}      → Obtener permisos
  ... (más endpoints privados)
```

---

## 📋 Información para Otros IAs

**CONTEXTO RÁPIDO:**

1. **¿Qué es?** Microservicio de autenticación, autorización y control de acceso para sistema de transporte.

2. **¿Dónde está?** `C:\desarrolloBackend\microservicio-seguridad`

3. **¿Cómo se ejecuta?** 
   ```bash
   mvn clean install
   mvn spring-boot:run
   # O: mvnw.cmd spring-boot:run
   ```
   Escucha en puerto 8181

4. **¿Cómo autenticarse?**
   - LOGIN: POST /api/public/auth/login → obtener sessionId → POST /api/public/auth/verify-2fa → obtener JWT
   - OAUTH: POST /api/public/auth/oauth-login → obtener JWT directamente
   - El JWT se envía en header: `Authorization: Bearer <token>`

5. **¿Cómo verificar acceso?** 
   - SecurityInterceptor valida en cada request
   - Usuario → Rol → Permiso → Acceso

6. **BD:** MongoDB Atlas (db_seguridad)

7. **Dependencias externas:**
   - Google reCAPTCHA v2
   - Microservicio Notificaciones (puerto 5000)
   - Firebase Auth (frontend)

8. **¿Qué NO tiene implementado?**
   - Refresh tokens
   - Logout explícito
   - Rate limiting
   - Auditoría en BD
   - Token blacklist

---

**Documento Generado:** 27 de Mayo de 2026  
**Versión:** 1.0  
**Tipo:** Arquitectura del Sistema  
**Audiencia:** Otros IAs / Desarrolladores nuevos

