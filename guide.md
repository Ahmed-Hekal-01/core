# Link Core Architecture Guide

This guide explains the architectural decisions, design patterns, and strict guidelines enforced in the `core` library. 

## 1. Explicit API Mode & Encapsulation
The library enforces Kotlin's **Explicit API Strict Mode**. This ensures that the exact boundary between the library (what the developer can see) and the internal plumbing (what is hidden) is strictly maintained.

* **`public`**: Used strictly for abstractions intended for the consumer (e.g., `ThemeController`, `RetrofitFactory`, `Resource`).
* **`internal`**: Used for concrete implementations and DI modules (e.g., `AppPreferences`, `FirestoreServiceImpl`, `NetworkModule`). This prevents polluting the app developer's IDE autocomplete and ensures they cannot bypass the public abstractions.

## 2. Presentation Layer: StateFlow Controllers
Jetpack Compose requires state that is immediately available in memory. The library provides three distinct controllers:
* `ThemeController`
* `LocaleController`
* `SessionController`

**The Architecture:**
1. The **Data Layer** (`AppPreferences`) reads from Jetpack DataStore on the disk and provides a standard Kotlin `Flow`.
2. The **Presentation Layer** Controllers wrap this `Flow` and convert it into a `StateFlow` using `.stateIn(..., SharingStarted.Eagerly)`.
3. `Eagerly` ensures the data is fetched immediately on app launch so the UI doesn't flicker on the first frame.
4. **Delegation**: The controllers provide `setTheme()` and `setLanguage()` methods. When called by the UI, the controller simply delegates the action down to the internal `AppPreferences` worker to save it to disk.

## 3. Firebase & Reified Extensions
The `FirestoreService` acts as a clean bridge to the Firebase SDK. 
Because `inline reified` functions cannot be placed inside an interface, the library uses a powerful two-step trick:
1. The `FirestoreService` interface defines `getAll(path, Class<T>)`.
2. A public `inline fun <reified T> FirestoreService.getAll(path)` extension function is provided.

This allows the consumer to write clean code (`getAll<User>("users")`) while maintaining a strict Dependency Injection architecture.

## 4. Network Layer
### The `RetrofitFactory` Clone Trick
When generating API services, the library uses `okHttpClient.newBuilder()`. This creates a lightweight clone of the `OkHttpClient`. 
This allows app developers to attach specific interceptors (like an Auth Token for a User API or an API Key for a Movie API) directly to a single service, preventing interceptors from leaking across different APIs globally.

### SafeApiCall & Manifest Merging
The `SafeApiCall` utility automatically checks network status before executing Retrofit requests. 
Instead of forcing the app developer to add `ACCESS_NETWORK_STATE` to their manifest, the `core` module declares it internally. The Android build system's **Manifest Merger** automatically deduplicates and includes this permission in the consumer app.

## 5. Domain Layer: Dispatcher Injection
To prevent flaky Unit Tests and `Main` thread crashes during JUnit tests, the library provides a `DispatcherProvider`.
App developers inject this interface into their ViewModels instead of hardcoding `Dispatchers.IO`. This allows them to easily swap in a `TestDispatcherProvider` during unit testing to execute coroutines synchronously.
