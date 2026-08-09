# Complete Developer Guide — `core` Library

> Version **1.1.1** · Min SDK 24 · Kotlin 2.3.10 · Hilt 2.59.2

This guide is the single authoritative reference for every public API, pattern, and integration point shipped by the `core` library. It is intended to be read end-to-end when onboarding, and used as a lookup reference during development.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Installation & Setup](#2-installation--setup)
3. [MVI — Model-View-Intent](#3-mvi--model-view-intent)
4. [Networking (Retrofit + OkHttp)](#4-networking-retrofit--okhttp)
5. [Error Handling — Resource & NetworkError](#5-error-handling--resource--networkerror)
6. [Firebase Authentication](#6-firebase-authentication)
7. [Firebase Firestore](#7-firebase-firestore)
8. [DataStore Preferences](#8-datastore-preferences)
9. [Theme Control](#9-theme-control)
10. [Locale Control](#10-locale-control)
11. [Session Management](#11-session-management)
12. [Coroutine Dispatchers](#12-coroutine-dispatchers)
13. [Presentation Utilities](#13-presentation-utilities)
14. [Hilt Dependency Graph](#14-hilt-dependency-graph)
15. [Constants Reference](#15-constants-reference)
16. [Full Package Map](#16-full-package-map)

---

## 1. Architecture Overview

The `core` library is designed around a **Clean Architecture + MVI** layering:

```
┌──────────────────────────────────────────────┐
│              Presentation Layer               │
│  BaseViewModel · MVI contracts · Controllers  │
├──────────────────────────────────────────────┤
│                Domain Layer                   │
│  Resource · NetworkError · DispatcherProvider │
├──────────────────────────────────────────────┤
│                 Data Layer                    │
│  RetrofitFactory · Firebase · DataStore       │
└──────────────────────────────────────────────┘
```

All components are singleton-scoped via Hilt and wired automatically — you only inject the interfaces you need.

---

## 2. Installation & Setup

### Step 1 — JitPack repository

`settings.gradle.kts` (root project):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add the dependency

`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Ahmed-Hekal-01:core:1.1.1")
}
```

### Step 3 — Apply Hilt + KSP plugins

Root `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
```

App `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}
```

### Step 4 — Annotate Application class

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

### Step 5 — Annotate Activities / Fragments

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

### Step 6 — Firebase setup (if using Auth / Firestore)

Add `google-services.json` to your `app/` directory and apply the Google Services plugin:

```kotlin
// root build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// app build.gradle.kts
plugins {
    id("com.google.gms.google-services")
}
```

---

## 3. MVI — Model-View-Intent

### 3.1 Contracts

Every screen is defined by three marker interfaces:

```kotlin
// com.link.core.presentation.mvi.State
interface State

// com.link.core.presentation.mvi.Event
interface Event

// com.link.core.presentation.mvi.OneTimeAction
interface OneTimeAction
```

- **State** — immutable UI state snapshot collected by Compose / Fragment
- **Event** — user intents dispatched to the ViewModel
- **OneTimeAction** — navigation commands or toasts that should fire exactly once

### 3.2 BaseViewModel

```kotlin
abstract class BaseViewModel<STATE : State, EVENT : Event, ONE_TIME_ACTION : OneTimeAction>
    : ViewModel()
```

| Member | Type | Description |
|---|---|---|
| `state` | `StateFlow<STATE>` | Current UI state; collect in Compose |
| `currentState` | `STATE` | Synchronous snapshot of `state.value` |
| `oneTimeAction` | `Flow<ONE_TIME_ACTION>` | Channel-backed single-shot stream |
| `sendEvent(event)` | `fun` | Dispatch an event from the UI |
| `setState { }` | `protected fun` | Reducer: `STATE.() -> STATE` |
| `sendOneTimeAction(action)` | `protected fun` | Emit a navigation / toast event |
| `createInitialState()` | `abstract fun` | Must return the first state |
| `onEvent(event)` | `abstract fun` | Handle every incoming event |

### 3.3 Step-by-step implementation

#### Define screen contracts

```kotlin
// State
data class ProductState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val error: NetworkError? = null
) : State

// Events
sealed interface ProductEvent : Event {
    data object LoadProducts : ProductEvent
    data class DeleteProduct(val id: String) : ProductEvent
}

// One-time actions
sealed interface ProductAction : OneTimeAction {
    data class ShowToast(val message: StringWrapper) : ProductAction
    data object NavigateBack : ProductAction
}
```

#### Implement the ViewModel

```kotlin
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val safeApiCall: SafeApiCall,
    private val productApi: ProductApi        // your own Retrofit API
) : BaseViewModel<ProductState, ProductEvent, ProductAction>() {

    override fun createInitialState() = ProductState()

    override fun onEvent(event: ProductEvent) {
        when (event) {
            ProductEvent.LoadProducts        -> loadProducts()
            is ProductEvent.DeleteProduct    -> delete(event.id)
        }
    }

    private fun loadProducts() = viewModelScope.launch {
        setState { copy(isLoading = true, error = null) }
        when (val result = safeApiCall { productApi.getProducts() }) {
            is Resource.Success -> setState { copy(isLoading = false, products = result.data) }
            is Resource.Error   -> {
                setState { copy(isLoading = false, error = result.error) }
                sendOneTimeAction(
                    ProductAction.ShowToast(result.error.asStringWrapper())
                )
            }
        }
    }

    private fun delete(id: String) = viewModelScope.launch {
        safeApiCall { productApi.deleteProduct(id) }
        loadProducts()
    }
}
```

#### Collect state in Compose

```kotlin
@Composable
fun ProductScreen(viewModel: ProductViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Collect one-time actions
    LaunchedEffect(Unit) {
        viewModel.oneTimeAction.collect { action ->
            when (action) {
                is ProductAction.ShowToast  -> showToast(action.message)
                ProductAction.NavigateBack  -> navController.popBackStack()
            }
        }
    }

    // Dispatch events
    Button(onClick = { viewModel.sendEvent(ProductEvent.LoadProducts) }) {
        Text("Load")
    }
    
    if (state.isLoading) CircularProgressIndicator()
    LazyColumn { items(state.products) { ProductRow(it) } }
}
```

---

## 4. Networking (Retrofit + OkHttp)

### 4.1 What the library provides

The library auto-configures and exposes (via Hilt) a shared `OkHttpClient` and `Retrofit.Builder` with:

- `kotlinx.serialization` JSON converter (`ignoreUnknownKeys = true`, `isLenient = true`, `encodeDefaults = true`)
- HTTP body logging interceptor (logs to `OkHttp` Logcat tag)
- 30 s connect / read / write timeouts

### 4.2 RetrofitFactory

```kotlin
class RetrofitFactory @Inject constructor(
    okHttpClient: OkHttpClient,
    retrofitBuilder: Retrofit.Builder
)
```

#### `createService<Service>`

```kotlin
fun <Service> createService(
    serviceClass: Class<Service>,
    baseUrl: String,
    interceptors: List<Interceptor> = emptyList()
): Service
```

Each call clones the base `OkHttpClient` (preventing interceptor leakage between APIs) and returns a fully configured Retrofit service.

#### Usage example

```kotlin
// Define your API
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    @POST("users")
    suspend fun createUser(@Body user: CreateUserRequest): UserDto
}

// Create the service in a Hilt module
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideUserApi(retrofitFactory: RetrofitFactory): UserApi =
        retrofitFactory.createService(
            serviceClass = UserApi::class.java,
            baseUrl = "https://api.myapp.com/v1/",
            interceptors = listOf(
                Interceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $TOKEN")
                            .build()
                    )
                }
            )
        )
}
```

### 4.3 SafeApiCall

```kotlin
interface SafeApiCall {
    suspend operator fun <T> invoke(apiCall: suspend () -> T): Resource<T>
}
```

`SafeApiCall` is an injectable function object that:
1. Checks network connectivity before calling the API
2. Maps HTTP errors to `NetworkError` automatically
3. Returns `Resource.Error(NetworkError.NoInternet)` when offline

```kotlin
// Inject and use
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val safeApiCall: SafeApiCall
) {
    suspend fun getUser(id: String): Resource<UserDto> = safeApiCall { api.getUser(id) }
}
```

### 4.4 HTTP error mapping

| HTTP code | `NetworkError` |
|---|---|
| 401 | `NetworkError.Unauthorized` |
| 403 | `NetworkError.Forbidden` |
| 404 | `NetworkError.NotFound(message)` |
| Other | `NetworkError.ServerError(code, message)` |
| No internet | `NetworkError.NoInternet` |
| Other exception | `NetworkError.Unknown` |

---

## 5. Error Handling — Resource & NetworkError

### 5.1 Resource

```kotlin
sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val error: NetworkError) : Resource<Nothing>
}
```

Use Kotlin `when` to exhaustively handle both cases:

```kotlin
when (val result = repository.getUser(id)) {
    is Resource.Success -> println(result.data.name)
    is Resource.Error   -> handleError(result.error)
}
```

### 5.2 NetworkError

```kotlin
sealed interface NetworkError {
    data object NoInternet : NetworkError
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data class NotFound(val message: String? = null) : NetworkError
    data class ServerError(val code: Int?, val message: String? = null) : NetworkError
    data object Unknown : NetworkError
}
```

### 5.3 Converting NetworkError to UI strings

```kotlin
// Extension — com.link.core.presentation.utils
fun NetworkError.asStringWrapper(): StringWrapper

// Then resolve to String with Context
val message: String = error.asStringWrapper().asString(context)
```

The built-in string resources provided for each error:

| Error | String resource |
|---|---|
| `NoInternet` | `R.string.no_internet_connection` |
| `Unauthorized` | `R.string.unauthorized_session` |
| `Forbidden` | `R.string.forbidden_access` |
| `NotFound` (no message) | `R.string.not_found` |
| `ServerError` / `Unknown` | `R.string.something_went_wrong` |

---

## 6. Firebase Authentication

### 6.1 AuthService interface

```kotlin
interface AuthService {
    val currentUserId: String?           // null if logged out
    val isLoggedIn: Boolean
    suspend fun login(email: String, password: String): Resource<Unit>
    suspend fun register(email: String, password: String): Resource<String>  // returns UID
    fun logout()
}
```

The implementation (`FirebaseAuthService`) is `@Singleton` and auto-bound by Hilt.

### 6.2 Login flow

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : BaseViewModel<LoginState, LoginEvent, LoginAction>() {

    override fun createInitialState() = LoginState()

    override fun onEvent(event: LoginEvent) = when (event) {
        is LoginEvent.Submit -> login(event.email, event.password)
    }

    private fun login(email: String, password: String) = viewModelScope.launch {
        setState { copy(isLoading = true) }
        when (val result = authService.login(email, password)) {
            is Resource.Success -> sendOneTimeAction(LoginAction.NavigateToHome)
            is Resource.Error   -> {
                setState { copy(isLoading = false, error = result.error.asStringWrapper()) }
            }
        }
    }
}
```

### 6.3 Registration flow

```kotlin
when (val result = authService.register(email, password)) {
    is Resource.Success -> {
        val uid = result.data   // Firebase UID — store in Firestore
        saveUserProfile(uid)
    }
    is Resource.Error -> showError(result.error)
}
```

### 6.4 Logout

```kotlin
// Synchronous — can be called from anywhere
authService.logout()
```

### 6.5 Checking auth state

```kotlin
if (authService.isLoggedIn) { /* ... */ }
val uid: String? = authService.currentUserId
```

---

## 7. Firebase Firestore

### 7.1 FirestoreService interface

```kotlin
interface FirestoreService {
    suspend fun <T : Any> add(path: String, docId: String? = null, item: T): Resource<Unit>
    suspend fun <T : Any> getAll(path: String, clazz: Class<T>): Resource<List<T>>
    suspend fun delete(path: String, docId: String): Resource<Unit>
    fun <T : Any> observe(path: String, clazz: Class<T>): Flow<List<T>>
}
```

### 7.2 Inline reified extensions (preferred API)

```kotlin
// Instead of passing Class<T> manually:
suspend inline fun <reified T : Any> FirestoreService.getAll(path: String): Resource<List<T>>
inline fun <reified T : Any> FirestoreService.observe(path: String): Flow<List<T>>
```

### 7.3 Examples

#### Add a document

```kotlin
// With auto-generated ID
firestoreService.add("users", item = User(name = "Alice", age = 30))

// With a specific document ID (e.g., Firebase Auth UID)
firestoreService.add("users", docId = uid, item = userProfile)
```

#### Read all documents

```kotlin
// Using reified extension (recommended)
when (val result = firestoreService.getAll<User>("users")) {
    is Resource.Success -> displayUsers(result.data)
    is Resource.Error   -> showError(result.error)
}
```

#### Delete a document

```kotlin
when (val result = firestoreService.delete("users", docId = userId)) {
    is Resource.Success -> showSuccess()
    is Resource.Error   -> showError(result.error)
}
```

#### Real-time observation

```kotlin
// In a ViewModel
val usersFlow: Flow<List<User>> = firestoreService.observe<User>("users")

// Collect in Compose
val users by usersFlow.collectAsState(initial = emptyList())
```

### 7.4 Data model requirements

Firestore objects must be **plain data classes with no-arg constructors** (or default values on all fields):

```kotlin
data class User(
    val name: String = "",
    val age: Int = 0,
    val email: String = ""
)
```

---

## 8. DataStore Preferences

The library internally manages all DataStore I/O through `AppPreferences` (internal) and surfaces it through the `ThemeController`, `LocaleController`, and `SessionController` public interfaces.

### Available enums

```kotlin
enum class AppTheme { LIGHT, DARK }
enum class AppLanguage { EN, AR }
```

---

## 9. Theme Control

### 9.1 ThemeController interface

```kotlin
interface ThemeController {
    val currentTheme: StateFlow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}
```

Inject via Hilt anywhere — it is a `@Singleton`.

### 9.2 Usage in Application / MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by themeController.currentTheme.collectAsState()
            MyAppTheme(darkTheme = theme == AppTheme.DARK) {
                AppNavigation()
            }
        }
    }
}
```

### 9.3 Toggling theme

```kotlin
// In a settings ViewModel
viewModelScope.launch {
    themeController.setTheme(
        if (themeController.currentTheme.value == AppTheme.DARK) AppTheme.LIGHT
        else AppTheme.DARK
    )
}
```

---

## 10. Locale Control

### 10.1 LocaleController interface

```kotlin
interface LocaleController {
    val currentLanguage: StateFlow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
}
```

### 10.2 Usage

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var localeController: LocaleController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            localeController.currentLanguage.collect { lang ->
                // Apply locale change — e.g., recreate activity or use AppCompatDelegate
                val locale = when (lang) {
                    AppLanguage.EN -> Locale.ENGLISH
                    AppLanguage.AR -> Locale("ar")
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
            }
        }
    }
}
```

### 10.3 Changing language

```kotlin
viewModelScope.launch {
    localeController.setLanguage(AppLanguage.AR)
}
```

---

## 11. Session Management

### 11.1 SessionController interface

```kotlin
interface SessionController {
    val isFirstLaunch: StateFlow<Boolean>
    val isGuest: StateFlow<Boolean>
    suspend fun setFirstLaunchDone()
    suspend fun setGuest(isGuest: Boolean)
}
```

### 11.2 First-launch onboarding gate

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionController: SessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isFirstLaunch by sessionController.isFirstLaunch.collectAsState()
            
            if (isFirstLaunch) {
                OnboardingScreen(
                    onFinish = {
                        lifecycleScope.launch { sessionController.setFirstLaunchDone() }
                    }
                )
            } else {
                MainNavigation()
            }
        }
    }
}
```

### 11.3 Guest mode

```kotlin
// Enable guest mode (skip login)
viewModelScope.launch { sessionController.setGuest(true) }

// Check if guest
val isGuest by sessionController.isGuest.collectAsState()
if (isGuest) showGuestBanner()

// Upgrade to full account
viewModelScope.launch { sessionController.setGuest(false) }
```

---

## 12. Coroutine Dispatchers

### 12.1 DispatcherProvider interface

```kotlin
interface DispatcherProvider {
    val io: CoroutineDispatcher      // Dispatchers.IO
    val main: CoroutineDispatcher    // Dispatchers.Main
    val default: CoroutineDispatcher // Dispatchers.Default
}
```

### 12.2 Why use it

Injecting `DispatcherProvider` instead of using `Dispatchers.IO` directly allows unit tests to swap in `TestCoroutineDispatcher` without modifying production code.

### 12.3 Usage in repositories

```kotlin
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val safeApiCall: SafeApiCall,
    private val dispatchers: DispatcherProvider
) {
    suspend fun getUser(id: String): Resource<UserDto> =
        withContext(dispatchers.io) {
            safeApiCall { api.getUser(id) }
        }
}
```

### 12.4 Usage in tests

```kotlin
class FakeDispatcherProvider : DispatcherProvider {
    override val io = StandardTestDispatcher()
    override val main = StandardTestDispatcher()
    override val default = StandardTestDispatcher()
}
```

---

## 13. Presentation Utilities

### 13.1 StringWrapper

Bridges the gap between string literals and Android string resources — use it in your MVI State/OneTimeAction to keep ViewModels context-free:

```kotlin
sealed class StringWrapper {
    data class FromString(val value: String) : StringWrapper()
    data class FromResource(val resId: Int, val args: List<Any> = emptyList()) : StringWrapper()
}
```

#### Create a StringWrapper

```kotlin
// From a literal string (e.g., an API error message)
StringWrapper.FromString("Invalid email format")

// From a resource ID
StringWrapper.FromResource(R.string.welcome_user, args = listOf(username))
```

#### Resolve to String in Composable / Fragment

```kotlin
// Extension function
fun StringWrapper.asString(context: Context): String

// Usage
val message = myStringWrapper.asString(context)
Text(text = myStringWrapper.asString(LocalContext.current))
```

### 13.2 NetworkError → StringWrapper

```kotlin
// Extension function
fun NetworkError.asStringWrapper(): StringWrapper

// Full chain
val uiMessage: String = resource.error.asStringWrapper().asString(context)
```

---

## 14. Hilt Dependency Graph

The following shows what Hilt provides automatically at the `SingletonComponent` scope:

```
SingletonComponent
│
├── NetworkModule
│   ├── HttpLoggingInterceptor
│   ├── Json (kotlinx.serialization)
│   ├── OkHttpClient
│   └── Retrofit.Builder
│
├── CoreDataModule
│   ├── ConnectivityManager
│   ├── SafeApiCall  ←─ CheckNetworkState + ValidateRemoteSource
│   ├── CheckNetworkState (→ CheckNetworkStateImpl)
│   └── ValidateRemoteSource (→ ValidateRemoteSourceImpl)
│
├── FirebaseModule
│   ├── FirebaseAuth
│   ├── FirebaseFirestore
│   ├── AuthService (→ FirebaseAuthService)
│   └── FirestoreService (→ FirestoreServiceImpl)
│
├── DataStoreModule
│   └── AppPreferences (→ AppPreferencesImpl)
│
├── ThemeModule
│   ├── ThemeController (→ ThemeControllerImpl)
│   └── LocaleController (→ LocaleControllerImpl)
│
├── SessionModule
│   └── SessionController (→ SessionControllerImpl)
│
└── CommonModule
    └── DispatcherProvider (→ DefaultDispatcherProvider)
```

> **Note**: `RetrofitFactory` is a regular `@Inject`-annotated class and does not appear in a `@Module` — Hilt creates it automatically wherever injected.

---

## 15. Constants Reference

```kotlin
// com.link.core.data.utils.Constants
object Constants {
    object OkHTTPConstants {
        const val CONNECT_TIMEOUT: Long = 30L   // seconds
        const val READ_TIMEOUT: Long    = 30L   // seconds
        const val WRITE_TIMEOUT: Long   = 30L   // seconds
    }
}
```

---

## 16. Full Package Map

```
com.link.core
├── data
│   ├── datastore
│   │   ├── AppPreferences          (internal interface)
│   │   ├── AppPreferencesImpl      (internal impl)
│   │   ├── AppTheme                (public enum: LIGHT, DARK)
│   │   ├── AppLanguage             (public enum: EN, AR)
│   │   └── di/DataStoreModule
│   ├── firebase
│   │   ├── auth
│   │   │   ├── AuthService         (public interface)
│   │   │   └── FirebaseAuthService (internal @Singleton impl)
│   │   ├── firestore
│   │   │   ├── FirestoreService    (public interface + reified extensions)
│   │   │   └── FirestoreServiceImpl (internal @Singleton impl)
│   │   └── di/FirebaseModule
│   ├── network
│   │   ├── CheckNetworkState       (internal fun interface)
│   │   ├── CheckNetworkStateImpl   (internal impl)
│   │   ├── RetrofitFactory         (public @Inject class)
│   │   ├── SafeApiCall             (public interface)
│   │   ├── ValidateRemoteSource    (internal interface + impl)
│   │   └── di
│   │       ├── CoreDataModule
│   │       └── NetworkModule
│   └── utils
│       └── Constants               (public object)
├── domain
│   ├── DispatcherProvider          (public interface)
│   ├── DefaultDispatcherProvider   (internal impl)
│   ├── models
│   │   ├── Resource                (public sealed interface: Success, Error)
│   │   └── NetworkError            (public sealed interface)
│   └── di/CommonModule
└── presentation
    ├── models
    │   └── StringWrapper           (public sealed class: FromString, FromResource)
    ├── mvi
    │   ├── State                   (public marker interface)
    │   ├── Event                   (public marker interface)
    │   ├── OneTimeAction           (public marker interface)
    │   └── BaseViewModel           (public abstract class)
    ├── session
    │   ├── SessionController       (public interface)
    │   ├── SessionControllerImpl   (internal @Singleton impl)
    │   └── di/SessionModule
    ├── theme
    │   ├── ThemeController         (public interface)
    │   ├── ThemeControllerImpl     (internal @Singleton impl)
    │   ├── LocaleController        (public interface)
    │   ├── LocaleControllerImpl    (internal @Singleton impl)
    │   └── di/ThemeModule
    └── utils
        ├── NetworkErrorExt         (NetworkError.asStringWrapper())
        └── StringWrapperExt        (StringWrapper.asString(context))
```

---

*For questions or contributions, open an issue or PR on [GitHub](https://github.com/Ahmed-Hekal-01/core).*
