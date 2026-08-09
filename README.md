# Link Core Library

The `core` module is a shared Android library providing a foundational Clean Architecture setup, networking tools, MVI architecture base classes, and standard utility components for Android applications.

## 📦 Installation & Setup

Since this library is published locally, follow these steps to use it in another project on your machine.

### 1. Publish to Maven Local
Before using the library in another project, you must build and publish it to your local Maven repository. Navigate to this `core` project in your terminal and run:
```bash
./gradlew publishToMavenLocal
```
This will publish the library to `~/.m2/repository/com/link/core/1.1.0/`.

### 2. Add Dependency in Consumer App
In the target application where you want to use the library, ensure `mavenLocal()` is included in your `settings.gradle.kts` or project-level `build.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal() // <-- Add this
    }
}
```

Then, add the dependency to your app-level `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.link:core:1.1.0")
}
```

## 🚀 Core Features & Usage

### 1. UI Controllers (`ThemeController`, `LocaleController`, `SessionController`)
The library provides dedicated, public controllers that expose clean `StateFlow`s for Jetpack Compose, hiding the underlying DataStore implementation.

**Example Usage:**
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeController: ThemeController,
    private val localeController: LocaleController,
    private val sessionController: SessionController
) : ViewModel() {

    val currentTheme = themeController.currentTheme // StateFlow<AppTheme>
    val isGuest = sessionController.isGuest // StateFlow<Boolean>

    fun toggleTheme() {
        viewModelScope.launch { themeController.setTheme(AppTheme.DARK) }
    }
}
```

### 2. MVI Architecture (`BaseViewModel`)
The library provides a powerful `BaseViewModel` that strictly enforces the Model-View-Intent pattern using Kotlin Coroutines and Flows.

**Example Usage:**
```kotlin
// 1. Define your State, Events, and Actions
data class ProfileState(val isLoading: Boolean = false, val name: String = "") : State
sealed class ProfileEvent : Event {
    object LoadProfile : ProfileEvent()
}
sealed class ProfileAction : OneTimeAction {
    data class ShowToast(val message: String) : ProfileAction()
}

// 2. Extend the BaseViewModel
@HiltViewModel
class ProfileViewModel @Inject constructor() : BaseViewModel<ProfileState, ProfileEvent, ProfileAction>() {
    
    override fun createInitialState() = ProfileState()

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadData()
        }
    }

    private fun loadData() {
        // Update State
        setState { copy(isLoading = true) }
        
        // Send a one-time UI action (e.g. navigation or toast)
        sendOneTimeAction(ProfileAction.ShowToast("Loading Profile!"))
    }
}
```

### 3. Networking & `RetrofitFactory`
The library provides an abstracted way to create Retrofit services without leaking OkHttp interceptors.

**Example Usage:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MyApiModule {

    @Provides
    @Singleton
    fun provideMyApiService(retrofitFactory: RetrofitFactory): MyApiService {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer TOKEN")
                .build()
            chain.proceed(request)
        }
        
        return retrofitFactory.createService(
            serviceClass = MyApiService::class.java,
            baseUrl = "https://api.my-domain.com/",
            interceptors = listOf(authInterceptor)
        )
    }
}
```

### 4. Safe API Calls & Error Handling
Use the `SafeApiCall` function to safely execute API calls. It is automatically provided by Hilt, checks network connectivity, and maps HTTP exceptions to domain `NetworkError`s.

```kotlin
class MyRepositoryImpl @Inject constructor(
    private val api: MyApiService,
    private val safeApiCall: SafeApiCall
) {

    suspend fun fetchProfile(): Resource<Profile> {
        return safeApiCall { api.getProfile() }
    }
}
```
You can then seamlessly map the `Resource<T>` to your UI state, and use `NetworkErrorExt.asStringWrapper()` to easily display localized errors to the user.

For a deeper dive into the architectural design and Kotlin features used, see [guide.md](guide.md).

## 🛠 Dependency Injection (Hilt)
This library uses Hilt. Its internal implementations (like `FirebaseAuthService` or `FirestoreServiceImpl`) are bound to their respective interfaces (`AuthService`, `FirestoreService`) inside the library's internal DI modules.
When you import this library, Hilt will automatically pick up these modules, so you can simply `@Inject` the interfaces directly into your consuming app's classes!
