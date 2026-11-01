# core

[![](https://jitpack.io/v/Ahmed-Hekal-01/core.svg)](https://jitpack.io/#Ahmed-Hekal-01/core)

A reusable Android library that provides a full-stack foundation for modern Android applications. It ships batteries-included support for **MVI architecture**, **Retrofit networking**, **Firebase Auth & Firestore**, **DataStore preferences**, **theme/locale control**, and **session management** — all pre-wired with Hilt DI so you can start building features immediately.

---

## Requirements

| Tool | Version |
|---|---|
| Kotlin | 2.3.10 |
| Android Gradle Plugin | 9.1.1 |
| Min SDK | 24 |
| Compile SDK | 36 |
| Java | 17 |

---

## Installation

### 1. Add JitPack to your dependency resolution

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Ahmed-Hekal-01:core:1.3.0")
}
```

### 3. Apply the required plugins

```kotlin
plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
```

### 4. Enable Hilt in your Application class

```kotlin
@HiltAndroidApp
class MyApp : Application()
```

That's it — all Hilt modules provided by this library are automatically discovered.

---

## What's Included

| Module | What it gives you |
|---|---|
| **MVI** | `BaseViewModel<STATE, EVENT, ONE_TIME_ACTION>` — a full MVI base class |
| **Networking** | Pre-configured `OkHttpClient` + `Retrofit.Builder` + `SafeApiCall` |
| **Firebase Auth** | `AuthService` — login, register, logout, current user |
| **Firestore** | `FirestoreService` — add, getAll, delete, observe (real-time) |
| **DataStore** | `ThemeController`, `LocaleController` — persisted theme & language |
| **Session** | `SessionController` — first-launch flag and guest-mode state |
| **Error handling** | `NetworkError` + `Resource` sealed types + `StringWrapper` extensions |
| **Coroutines** | `DispatcherProvider` — testable dispatcher injection |

---

## Quick Examples

### MVI ViewModel

```kotlin
// 1. Define your contracts
data class HomeState(val isLoading: Boolean = false, val items: List<Item> = emptyList()) : State
sealed interface HomeEvent : Event { data object LoadItems : HomeEvent }
sealed interface HomeAction : OneTimeAction { data object NavigateToDetail : HomeAction }

// 2. Extend BaseViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val safeApiCall: SafeApiCall
) : BaseViewModel<HomeState, HomeEvent, HomeAction>() {

    override fun createInitialState() = HomeState()

    override fun onEvent(event: HomeEvent) = when (event) {
        HomeEvent.LoadItems -> loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = safeApiCall { myApi.getItems() }
            when (result) {
                is Resource.Success -> setState { copy(isLoading = false, items = result.data) }
                is Resource.Error   -> setState { copy(isLoading = false) }
            }
        }
    }
}
```

### Retrofit API Service

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val safeApiCall: SafeApiCall
) : ViewModel() {

    private val api = retrofitFactory.createService(
        serviceClass = MyApi::class.java,
        baseUrl = "https://api.example.com/",
        interceptors = listOf(AuthInterceptor())
    )
}
```

### Firebase Auth

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {
    fun login(email: String, password: String) {
        viewModelScope.launch {
            when (val result = authService.login(email, password)) {
                is Resource.Success -> { /* navigate */ }
                is Resource.Error   -> { /* show error */ }
            }
        }
    }
}
```

### Theme & Locale in Compose

```kotlin
@Composable
fun App(themeController: ThemeController, localeController: LocaleController) {
    val theme by themeController.currentTheme.collectAsState()
    val darkTheme = theme == AppTheme.DARK
    MyAppTheme(darkTheme = darkTheme) { /* content */ }
}
```

---

## Error Handling

All network calls return `Resource<T>`. Convert `NetworkError` to a UI-friendly string with:

```kotlin
val errorString = error.asStringWrapper().asString(context)
```

---

## License

```
MIT License — see LICENSE file for details.
```
