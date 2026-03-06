# DID Blockchainless Demo 🪪

Demo Android que implementa **Decentralized Identifiers (did:key)** sin blockchain, combinado con almacenamiento cifrado de **Verifiable Credentials** usando el Android Keystore y validación extra de StrongBox/TEE.

---

## ¿Qué hace la app?

| Módulo            | Qué hace                                                                                            |
| ----------------- | --------------------------------------------------------------------------------------------------- |
| **Identidad DID** | Genera un par de claves `secp256k1`, deriva un `did:key` y firma un Proof JWT con ese DID           |
| **RSA Cifrado**   | Cifra credenciales verificables (JSON) con `AES-256-GCM + RSA-2048 OAEP` usando el Android Keystore |

---

## Requisitos

| Herramienta           | Versión          | Motivo                                                                                  |
| --------------------- | ---------------- | --------------------------------------------------------------------------------------- |
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
BASE_URL=https://tu-backend.azurecontainerapps.io/
```

> Si esta variable no está definida, el build falla en tiempo de compilación con un error descriptivo.

### 2. Compila y ejecuta

```bash
./gradlew assembleDebug
```

---

## Flujo principal

```
App startup
  └─ SecureCredentialsApp.onCreate()
       ├─ setupBouncyCastle()   ← reemplaza el BC recortado de Android por el completo (secp256k1)
       └─ DIDKeyManager.generateKeysIfNeeded()

Pantalla Home
  ├─ [Identidad DID]  → DidScreen
  │     ├─ Genera par secp256k1 (clave privada cifrada con AES-GCM en prefs)
  │     ├─ Deriva did:key:z... (multicodec 0xe7,0x01 + pubkey comprimido + base58btc)
  │     └─ Solicita nonce al backend → firma Proof JWT (ES256K, RFC 6979)
  │
  └─ [RSA Cifrado]    → RsaScreen
        ├─ Genera par RSA-2048 en Android Keystore (StrongBox → TEE → Software)
        ├─ Cifra JSON: RSA-OAEP encripta clave AES efímera → AES-GCM cifra el payload
        └─ Descifra y muestra el JSON original
```

---

## Estructura del proyecto

```
app/src/main/java/.../
├── SecureCredentialsApp.kt          # Application: registra BouncyCastle al inicio
│
├── crypto/
│   ├── CryptoManager.kt             # RSA-2048 + AES-256-GCM para cifrado de VCs
│   ├── KeystoreHelper.kt            # Abstracción StrongBox → TEE → Software
│   └── SecurityLevel.kt             # Enum: STRONGBOX / TEE / SOFTWARE / UNKNOWN
│
├── did/
│   ├── DIDKeyManager.kt             # Genera secp256k1, deriva DID, firma ES256K
│   └── ProofJWTBuilder.kt           # Construye el JWT de prueba (header+payload+firma)
│
├── data/
│   ├── model/                       # VerifiableCredential, NonceResponse
│   ├── network/
│   │   ├── CredentialApi.kt         # Retrofit: GET /credentials/nonce?holder_did=...
│   │   └── NetworkClient.kt         # OkHttp + timeouts + logging condicional
│   └── repository/CredentialRepository.kt
│
├── storage/CredentialStore.kt       # SharedPreferences cifradas: guarda el payload AES-GCM
│
└── ui/
    ├── navigation/AppNavigation.kt  # NavHost: HOME → DID → RSA
    ├── screens/
    │   ├── HomeScreen.kt            # Menú principal con 2 botones
    │   ├── DidScreen.kt             # Pantalla de identidad DID + Proof JWT
    │   └── RsaScreen.kt             # Pantalla de cifrado RSA + credencial VC
    ├── components/                  # Composables reutilizables (SharedComponents, secciones)
    ├── viewmodel/
    │   ├── CredentialViewModel.kt         # Estado central + launchCrypto()
    │   ├── CredentialViewModelDid.kt      # Extension: generateDIDKeys, deleteDIDKeys
    │   ├── CredentialViewModelRsa.kt      # Extension: generateRsaKeys, encrypt, decrypt
    │   └── CredentialViewModelNetwork.kt  # Extension: requestCredentialWithNonce, fetchAndEncrypt
    └── CredentialUiState.kt         # Estado inmutable de la UI
```

---

## Seguridad — consideraciones importantes

### Claves DID (secp256k1)

- La clave privada **nunca sale del dispositivo en texto plano**
- Se cifra con `AES-256-GCM` usando una `SecretKey` del Android Keystore como wrap key
- Se borra de RAM con `fill(0)` inmediatamente después de usarse para firmar
- La clave pública comprimida (33 bytes) se guarda en `SharedPreferences` en hex

### Cifrado de VCs (RSA-2048)

- Esquema **híbrido**: RSA-OAEP cifra una clave AES efímera → AES-GCM cifra el payload
- Preferencia de hardware: **StrongBox** > TEE > Software (fallback automático)
- El nivel de seguridad real se muestra en la UI (🔒 StrongBox / 🛡 TEE / ⚠️ Software)

### BouncyCastle en Android

Android incluye un `BC` provider recortado sin `secp256k1`. La app lo reemplaza al inicio:

```kotlin
Security.removeProvider("BC")
Security.addProvider(BouncyCastleProvider())
```

Esto se hace en `SecureCredentialsApp.onCreate()`, una sola vez, antes de cualquier operación criptográfica.

### URL del backend

La `BASE_URL` **no debe subirse al repositorio**. Usa `local.properties` (ya en `.gitignore`) o variables de entorno en CI/CD.

---

## Endpoint del backend

| Método | Ruta                 | Parámetro            | Descripción                                        |
| ------ | -------------------- | -------------------- | -------------------------------------------------- |
| `GET`  | `/credentials/nonce` | `holder_did` (query) | Devuelve un nonce de un solo uso para el Proof JWT |

### Ejemplo de Proof JWT generado

El JWT tiene 3 partes separadas por `.`. La sección **Payload (Decoded)** de la app muestra el JSON descifrado:

```json
{
	"iss": "did:key:zQ3sh...",
	"aud": "https://tu-backend/",
	"nonce": "abc123",
	"iat": 1741223400,
	"type": "VerifiableCredential"
}
```

---

## Stack tecnológico

| Librería                     | Uso                                         |
| ---------------------------- | ------------------------------------------- |
| Jetpack Compose + Navigation | UI declarativa y navegación entre pantallas |
| Android Keystore             | Almacenamiento seguro de claves RSA y AES   |
| BouncyCastle 1.83            | secp256k1, ECDSA (ES256K), SHA-256          |
| Retrofit 3 + OkHttp 5        | Cliente HTTP para el backend                |
| Kotlin Serialization         | Serialización JSON                          |
| AndroidViewModel + StateFlow | Arquitectura MVVM sin memory leaks          |

---

## Contribuciones y contacto

¿Tienes dudas, encontraste un bug o quieres proponer una mejora? No dudes en:

- Abrir un **Issue** en el repositorio
- Enviar un **Pull Request** — todas las contribuciones son bienvenidas

También puedes encontrarme en mis redes como **@tohure**:

[![Twitter](https://img.shields.io/badge/Twitter-@tohure-1DA1F2?logo=twitter&logoColor=white)](https://twitter.com/tohure_)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-@tohure-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/tohure)
[![Medium](https://img.shields.io/badge/Medium-@tohure-000000?logo=medium&logoColor=white)](https://medium.com/@tohure)
