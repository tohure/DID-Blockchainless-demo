# DID Blockchainless Demo 🪪

Demo Android que implementa **Decentralized Identifiers (did:key)** sin blockchain, combinado con almacenamiento cifrado de **Verifiable Credentials** usando el Android Keystore y validación extra de StrongBox/TEE.

---

## ¿Qué hace la app?

| Módulo            | Qué hace                                                                                                          |
|-------------------|-------------------------------------------------------------------------------------------------------------------|
| **Identidad DID** | Genera claves `secp256k1`, registra el DID, solicita una VC mediante Proof JWT y **valida la presentación (VP)**. |
| **RSA Cifrado**   | Cifra credenciales (JSON o JWT) con `AES-256-GCM + RSA-2048 OAEP` usando el Android Keystore.                     |

---

## Requisitos

| Herramienta           | Versión          | Motivo                                                                                  |
|-----------------------|------------------|-----------------------------------------------------------------------------------------|
| **Android Studio**    | Meerkat 2024.3   | Versión mínima compatible con AGP 9.1                                                   |
| **JDK**               | 21               | Aprovecha virtual threads, records y mejoras de rendimiento en la JVM con Kotlin        |
| **Gradle**            | 9.3.1            | Wrapper incluido en el repo — no requiere instalación manual                            |
| **AGP**               | 9.1.0            | Android Gradle Plugin                                                                   |
| **Kotlin**            | 2.3              | Compilador K2, inferencia de tipos mejorada y Compose Multiplatform ready               |
| **Android minSdk**    | 27 (Android 8.1) | `AES-GCM` garantizado en hardware desde esta versión. StrongBox disponible desde API 28 |
| **Android targetSdk** | 36 (Android 16)  |                                                                                         |

---

## Configuración

### 1. Añade `BASE_URL` en `local.properties` (raíz del proyecto)

```properties
BASE_URL=https://tu-backend.com/
```

> Si esta variable no está definida, el build falla en tiempo de compilación con un error descriptivo.

### 2. Compila y ejecuta

```bash
./gradlew assembleDebug
```

---

## Flujo principal

```mermaid
graph TD
    A[Inicio App] --> B{Claves DID existen?}
    B -- No --> C["Generar par secp256k1"]
    B -- Sí --> D[Pantalla DID]
    
    D --> E[Registrar DID]
    E --> F[Solicitar Nonce]
    F --> G[Generar Proof JWT]
    G --> H["Enviar Proof (registerProof)"]
    H --> I["Recibir Credencial (VC JWT)"]
    I --> J["Cifrar y Guardar (RSA+AES)"]
    J --> K["Generar VP JWT"]
    K --> L["Validar VP en Backend"]
```

### Detalle del flujo DID

1.  **Generar DID**: Se crea un par de claves `secp256k1`. La privada se cifra con AES-GCM (clave en Keystore).
2.  **Registrar DID**: Se envía el DID y el email al backend (`/dids/register`).
3.  **Solicitar Nonce**: Se pide un desafío criptográfico (`/credentials/nonce`).
4.  **Proof JWT**: Se firma el nonce con la clave privada `secp256k1` (algoritmo ES256K).
5.  **Obtener Credencial**: Se envía el JWT (`/credentials/issue`) y se recibe una Verifiable Credential (VC).
6.  **Validar Presentación**: Se envuelve la VC en un **Verifiable Presentation (VP)** JWT y se envía al backend (`/credentials/verify`) para comprobar su validez.
7.  **Almacenamiento Seguro**: La VC se cifra automáticamente usando el módulo RSA y se guarda en `SharedPreferences`.

---

## Estructura del proyecto

```
app/src/main/java/dev/tohure/didblockchainlessdemo/
├── SecureCredentialsApp.kt          # Application: registra BouncyCastle al inicio
│
├── crypto/
│   ├── CryptoManager.kt             # RSA-2048 + AES-256-GCM para cifrado de VCs
│   ├── KeystoreHelper.kt            # Lógica compartida: StrongBox/TEE y niveles de seguridad
│   └── SecurityLevel.kt             # Enum: STRONGBOX / TEE / SOFTWARE / UNKNOWN
│
├── did/
│   ├── DIDKeyManager.kt             # Genera secp256k1, deriva DID, firma ES256K
│   ├── ProofJWTBuilder.kt           # Construye el Proof JWT (solicitud de credencial)
│   ├── VpJWTBuilder.kt              # Construye el VP JWT (presentación de credencial)
│   └── JwtExtensions.kt             # Utilidades compartidas (Base64Url, etc.)
│
├── data/
│   ├── model/                       # Modelos de datos (Request/Response)
│   ├── network/
│   │   ├── CredentialApi.kt         # Retrofit: Endpoints para DID y Credenciales
│   │   └── NetworkClient.kt         # OkHttp + timeouts + logging
│   └── repository/CredentialRepository.kt
│
├── storage/CredentialStore.kt       # SharedPreferences cifradas: guarda el payload AES-GCM
│
├── utils/
│   ├── AppLogger.kt                 # Logging centralizado (tags: tohure-*)
│   └── ValidationUtils.kt           # Validaciones de JSON y JWT
│
└── ui/
    ├── navigation/AppNavigation.kt  # NavHost: HOME → DID → RSA
    ├── screens/                     # Pantallas (HomeScreen, DidScreen, RsaScreen)
    ├── components/                  # Composables reutilizables (StatusBar, Sections, Tabs)
    ├── viewmodel/
    │   ├── DidViewModel.kt          # Lógica de negocio DID (Generación, Proof, VP)
    │   ├── RsaViewModel.kt          # Lógica RSA y Cifrado
    │   ├── DidUiState.kt            # Estado inmutable de la pantalla DID
    │   └── RsaUiState.kt            # Estado inmutable de la pantalla RSA
```

---

## Logging y Depuración

La aplicación cuenta con un sistema de logging centralizado (`AppLogger`) que permite filtrar fácilmente los eventos y errores en Logcat.

**Tags principales:**
- `tohure-did-vm`: Errores en la lógica de vista DID.
- `tohure-rsa-vm`: Errores en la lógica de vista RSA.
- `tohure-repository`: Errores de red y repositorio.
- `tohure-did-key`: Errores criptográficos en DIDKeyManager.
- `tohure-keystore`: Errores de acceso al Android Keystore.

---

## Seguridad — consideraciones importantes

### Claves DID (secp256k1 - 256 bits)

- **Por qué 256 bits?**: ECC (Curva Elíptica) es más eficiente. 256 bits en ECC equivalen a ~3072 bits en RSA.
- La clave privada **nunca sale del dispositivo en texto plano**.
- Se cifra con `AES-256-GCM` usando una `SecretKey` del Android Keystore como wrap key.
- Se borra de RAM con `fill(0)` inmediatamente después de usarse.

### Cifrado de VCs (RSA-2048)

- **Por qué 2048 bits?**: Es el estándar mínimo seguro para RSA hoy en día.
- Esquema **híbrido**: RSA-OAEP cifra una clave AES efímera → AES-GCM cifra el payload.
- **Validación Inteligente**: El sistema detecta si el input es un JSON o un JWT antes de cifrar.

### BouncyCastle en Android

Android incluye un `BC` provider recortado. La app lo reemplaza al inicio para soportar `secp256k1` correctamente.

### URL del backend

La `BASE_URL` se maneja vía `local.properties` para no exponerla en el repositorio.

---

## Endpoints del backend

| Método | Ruta                  | Descripción                                       |
|--------|-----------------------|---------------------------------------------------|
| `POST` | `/dids/register`      | Registra el DID y el Client ID (email)            |
| `GET`  | `/credentials/nonce`  | Obtiene un nonce vinculado al DID                 |
| `POST` | `/credentials/issue`  | Envía el Proof JWT y recibe la Credencial (VC)    |
| `GET`  | `/credentials`        | Obtiene metadatos de credenciales asociadas       |
| `POST` | `/credentials/verify` | Valida un Verifiable Presentation (VP) JWT        |
| `GET`  | `/credentials/{id}`   | Descarga una credencial específica (Legacy flow)  |

---

## Stack tecnológico

| Librería                 | Uso                                           |
|--------------------------|-----------------------------------------------|
| **Jetpack Compose**      | UI declarativa moderna                        |
| **Android Keystore**     | Almacenamiento seguro respaldado por hardware |
| **BouncyCastle**         | Criptografía avanzada (secp256k1, ES256K)     |
| **Retrofit + OkHttp**    | Networking robusto con timeouts               |
| **Kotlin Serialization** | Parseo JSON eficiente                         |
| **Coroutines + Flow**    | Manejo asíncrono y estado reactivo            |

---

## Contribuciones y contacto

¿Tienes dudas, encontraste un bug o quieres proponer una mejora? No dudes en:

- Abrir un **Issue** en el repositorio
- Enviar un **Pull Request** — todas las contribuciones son bienvenidas

También puedes encontrarme en mis redes como **@tohure**:

[![Twitter](https://img.shields.io/badge/Twitter-@tohure-1DA1F2?logo=twitter&logoColor=white)](https://twitter.com/tohure_)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-@tohure-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/tohure)
[![Medium](https://img.shields.io/badge/Medium-@tohure-000000?logo=medium&logoColor=white)](https://medium.com/@tohure)
