# Gemini API Compose Starter

Jetpack Compose chat app for MAD Assignment 1. Users send prompts to Gemini (`gemini-3.6-flash`) by text or voice. Chat history and a few preferences survive app restarts.

This document is for branch `N101-Assignment1`.

## What changed

### Chat UI
- Material 3 conversation layout with user/Gemini bubbles, empty state, and an in-list loading bubble.
- Adaptive layout from `WindowSizeClass` (compact / medium / expanded, including landscape).
- Follows light/dark theme, with optional dynamic colour on Android 12+.
- Snackbar errors with a Retry action when a Gemini request fails.
- Network calls run on `Dispatchers.IO`, not the main thread.

### Voice input
- Microphone button launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` through `rememberLauncherForActivityResult`.
- `RECORD_AUDIO` is requested first when needed.
- Spoken text fills the prompt. If **Auto-send voice prompts** is on, the app sends it immediately.

### Persistence
- **Room** stores the conversation in `gemini_chat.db` so history is still there after a restart.
- **Preferences DataStore** stores:
  - auto-send voice prompts
  - dynamic colour
- Overflow menu: auto-send, dynamic colour, clear chat history.

### API key
- The key is **not** committed. It is read from `local.properties` at build time, then encrypted into Android Keystore-backed storage on first launch.

## Where to place the API key

1. Open [Google AI Studio](https://aistudio.google.com/apikey) and create a Gemini API key.
2. In the **project root** (same folder as `settings.gradle.kts`), create or edit `local.properties`.
3. Add this line (no quotes):

```properties
GEMINI_API_KEY=your_key_here
```

`local.properties` is listed in `.gitignore`. Do not put the key in source, `BuildConfig` comments, screenshots, or git.

You can also set a `GEMINI_API_KEY` environment variable. Gradle uses that only if the property is missing from `local.properties`.

After changing the key, **Rebuild** the app (`Build > Rebuild Project`, or `gradlew.bat :app:assembleDebug`). The value is copied into `BuildConfig.GEMINI_API_KEY` at compile time. If the key is empty, the chat screen shows a configuration error and send is disabled.

The emulator needs internet (Wi-Fi on) or Gemini requests will fail.

## Encryption flow

The plaintext key is only used at build time and briefly in memory after decrypt. What is stored on the device is ciphertext.

```text
local.properties  (GEMINI_API_KEY, git-ignored)
        |
        |  Gradle reads it while compiling
        v
BuildConfig.GEMINI_API_KEY
        |
        |  MainActivity on first launch / when the configured key changed
        v
ApiKeyManager.saveApiKey()
        |
        |  AES-256-GCM with a key that never leaves Android Keystore
        v
SharedPreferences ("secure_api_storage")
  - encrypted_api_key  (Base64 ciphertext)
  - api_key_iv         (Base64 GCM IV)
```

**Save**

1. `MainActivity` reads any previously decrypted key from `ApiKeyManager`.
2. It compares that with `BuildConfig.GEMINI_API_KEY`.
3. If the configured key is non-empty and different, it calls `saveApiKey()`.
4. `ApiKeyManager` loads or creates an AES-256 key in the Android Keystore (`GeminiApiKeyEncryptionKey`, GCM, no padding).
5. The plaintext is encrypted with `AES/GCM/NoPadding`.
6. Ciphertext and IV are Base64-encoded and written to private SharedPreferences.

**Load**

1. `getApiKey()` reads the stored ciphertext and IV.
2. The Keystore secret key decrypts them (`GCMParameterSpec`, 128-bit tag).
3. The decrypted string is passed into `GeminiRepositoryImpl` for REST calls (`x-goog-api-key`).
4. If nothing is stored and `BuildConfig` is blank, the UI shows that `GEMINI_API_KEY` is missing.

Clearing app data removes SharedPreferences. The Keystore key may remain; the next launch with a key in `local.properties` encrypts and stores it again.

## Running the app

1. Open the project in Android Studio.
2. Add `GEMINI_API_KEY` to `local.properties` as above.
3. Use an emulator or device with API 26+ and internet.
4. Run the `app` configuration.

From the project root in PowerShell:

```powershell
.\gradlew.bat :app:installDebug
```

## Running the tests

Dependencies are already in `app/build.gradle.kts`:

- Unit tests: JUnit 4 and `kotlinx-coroutines-test`
- Instrumented / Compose tests: AndroidX JUnit, Espresso, and `androidx.compose.ui:ui-test-junit4` (`createComposeRule()`)

### From Android Studio

- **Unit tests** (`ChatViewModel` with a fake `GeminiRepository`): open a class under `app/src/test/` and use **Run**.
- **Compose UI tests** (`createComposeRule()` on the chat screen): open a class under `app/src/androidTest/`, start an emulator, then **Run**.

You can also use the Gradle tool window: `:app` → `test` / `connectedDebugAndroidTest`.

### From the command line

Unit tests (local JVM, no device):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Instrumented and Compose UI tests (emulator or device must be running):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

HTML reports:

- Unit: `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumented: `app/build/reports/androidTests/connected/index.html`

`ChatViewModel` tests should use a fake `GeminiRepository` so they never call the real Gemini API. Compose tests should set `ChatScreen` state directly and assert empty, loading, and message UI without needing a key.

## Project layout

| Path | Role |
| --- | --- |
| `app/src/main/java/.../ui/chat/` | Chat screen, ViewModel, UI state |
| `app/src/main/java/.../data/GeminiRepositoryImpl.kt` | Gemini REST client (`Dispatchers.IO`) |
| `app/src/main/java/.../data/local/` | Room database, DAO, history repository |
| `app/src/main/java/.../data/prefs/` | Preferences DataStore |
| `app/src/main/java/.../security/ApiKeyManager.kt` | Keystore AES-GCM wrap for the API key |
| `app/src/test/` | Unit tests |
| `app/src/androidTest/` | Instrumented and Compose UI tests |
| `local.properties` | SDK path and `GEMINI_API_KEY` (not in git) |
