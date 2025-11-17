# Dependency Injection Setup

## Overview

This document describes the **Manual Dependency Injection** setup for the **authentication section**. We use a simple, clean DI pattern without external libraries for maximum compatibility with support libraries (pre-AndroidX).

> **📖 See Also:**
> - [WHITEBOARD_DI_SETUP.md](WHITEBOARD_DI_SETUP.md) - Dependency Injection setup for the whiteboard section
> 
> Both sections use the same DI pattern and share the `DependencyContainer` and `ViewModelFactory`.

## Sections Using DI

- ✅ **Auth Section** - LoginViewModel, AuthRepository (documented in this file)
- ✅ **Whiteboard Section** - WhiteboardViewModel, PageManager, ExportManager, ImportManager (see WHITEBOARD_DI_SETUP.md)

## Why Manual DI?

- ✅ **Zero external dependencies** - No third-party libraries needed
- ✅ **Full compatibility** - Works with all Android SDK versions and support libraries
- ✅ **Simple and transparent** - Easy to understand and debug
- ✅ **Lightweight** - No runtime overhead from DI frameworks
- ✅ **Type-safe** - Compile-time checks for all dependencies
- ✅ **No reflection** - Better performance and ProGuard compatibility

## Architecture

### Core Components

#### 1. **DependencyContainer** (`di/DependencyContainer.kt`)

Centralized container for managing application dependencies:

```kotlin
class DependencyContainer(private val applicationContext: Context) {

    // Singletons with lazy initialization
    val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }

    val apiService: ApiService by lazy {
        ApiClient.createApiService()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            prefs = userPreferences,
            apiService = apiService
        )
    }
}
```

#### 2. **ViewModelFactory** (`di/ViewModelFactory.kt`)

Factory for creating ViewModels with dependencies:

```kotlin
class ViewModelFactory(
    private val application: Application,
    private val dependencies: DependencyContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    application = application,
                    authRepository = dependencies.authRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}
```

#### 3. **MoodleApplication** (`MoodleApplication.kt`)

Application class that initializes the dependency container:

```kotlin
class MoodleApplication : Application() {

    lateinit var dependencyContainer: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        dependencyContainer = DependencyContainer(applicationContext)
    }

    companion object {
        fun getDependencyContainer(application: Application): DependencyContainer {
            return (application as MoodleApplication).dependencyContainer
        }
    }
}
```

Registered in `AndroidManifest.xml`:

```xml
<application
    android:name=".MoodleApplication"
    ...>
```

## Refactored Components

### 1. AuthRepository

**Before:**

```kotlin
class AuthRepository(private val prefs: UserPreferences) {
    suspend fun login(...) {
        val apiService = ApiClient.createApiService() // Manual creation
        ...
    }
}
```

**After:**

```kotlin
class AuthRepository(
    private val prefs: UserPreferences,
    private val apiService: ApiService  // Injected via constructor
) {
    suspend fun login(...) {
        // Uses injected apiService
        val response = apiService.login(request).execute()
        ...
    }
}
```

### 2. LoginViewModel

**Before:**

```kotlin
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    init {
        authRepository = AuthRepository(UserPreferences(application)) // Manual creation
    }
}
```

**After:**

```kotlin
class LoginViewModel(
    application: Application,
    private val authRepository: AuthRepository  // Injected via constructor
) : AndroidViewModel(application) {
    // Clean initialization - no manual dependency creation
}
```

### 3. LoginActivity

**Before:**

```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
    }
}
```

**After:**

```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get dependency container and create ViewModel with factory
        val dependencies = (application as MoodleApplication).dependencyContainer
        val factory = ViewModelFactory(application, dependencies)
        viewModel = ViewModelProviders.of(this, factory).get(LoginViewModel::class.java)
        ...
    }
}
```

## Benefits Achieved

### ✅ Single Responsibility Principle

Each class has one clear responsibility:

- **DependencyContainer** - Manages dependency creation and lifecycle
- **ViewModelFactory** - Creates ViewModels with dependencies
- **UserPreferences** - Handles data persistence
- **ApiService** - Manages network requests
- **AuthRepository** - Implements auth business logic
- **LoginViewModel** - Manages UI state

### ✅ Dependency Inversion Principle

- High-level modules (ViewModels, Activities) don't depend on low-level modules (Repositories, Services)
- Both depend on abstractions (interfaces/constructor parameters)
- Dependencies flow inward through constructor injection

### ✅ Open/Closed Principle

- Easy to extend with new dependencies without modifying existing code
- Add new ViewModels to the factory without changing the factory structure
- Add new repositories to the container without changing existing ones

### ✅ Don't Repeat Yourself (DRY)

- Single instance of each dependency across the app (singletons)
- No duplicate dependency creation code
- Centralized configuration in DependencyContainer

### ✅ Testability

Dependencies can be easily mocked for unit testing:

```kotlin
// Create test dependencies
val mockApiService = mock<ApiService>()
val mockPreferences = mock<UserPreferences>()

// Create test container
val testContainer = DependencyContainer(testContext).apply {
    // Override dependencies if needed
}

// Test with mocked dependencies
val repository = AuthRepository(mockPreferences, mockApiService)
```

### ✅ No Tight Coupling

- `AuthRepository` doesn't create `ApiService` - receives it via constructor
- `LoginViewModel` doesn't create `AuthRepository` - receives it via constructor
- Easy to swap implementations without modifying consumers

### ✅ Type Safety

- All dependencies are resolved at compile time
- No runtime reflection or string-based lookups
- IDE autocomplete works perfectly

## Dependency Lifecycle

### Singleton Pattern (Lazy Initialization)

```kotlin
val userPreferences: UserPreferences by lazy {
    UserPreferences(applicationContext)
}
```

- Created once when first accessed
- Shared across the entire application
- Lives as long as the Application instance

### ViewModel Lifecycle

```kotlin
viewModel = ViewModelProviders.of(this, factory).get(LoginViewModel::class.java)
```

- Created by ViewModelFactory
- Survives configuration changes (rotation, etc.)
- Cleared when Activity/Fragment is permanently destroyed

## Adding New Dependencies

### Step 1: Add to DependencyContainer

```kotlin
class DependencyContainer(private val applicationContext: Context) {

    // Existing dependencies...

    // New dependency - example: Analytics Service
    val analyticsService: AnalyticsService by lazy {
        AnalyticsService(applicationContext)
    }

    // New repository using existing dependencies
    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            prefs = userPreferences,
            apiService = apiService
        )
    }
}
```

### Step 2: Update ViewModelFactory (if needed)

```kotlin
class ViewModelFactory(
    private val application: Application,
    private val dependencies: DependencyContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(application, dependencies.authRepository) as T
            }
            // Add new ViewModel
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(application, dependencies.profileRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
```

### Step 3: Use in Activity/Fragment

```kotlin
class ProfileActivity : AppCompatActivity() {
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dependencies = (application as MoodleApplication).dependencyContainer
        val factory = ViewModelFactory(application, dependencies)
        viewModel = ViewModelProviders.of(this, factory).get(ProfileViewModel::class.java)
    }
}
```

## Accessing Dependencies Directly

For non-ViewModel classes that need dependencies:

```kotlin
class SomeService(context: Context) {
    private val dependencies = (context.applicationContext as MoodleApplication).dependencyContainer

    fun doSomething() {
        val user = dependencies.userPreferences.getUsername()
        val result = dependencies.authRepository.isLoggedIn()
    }
}
```

## Migration Checklist for Other Sections

When adding DI to other sections (e.g., Whiteboard):

1. ✅ **Identify dependencies** - List all services, repositories, and utilities needed
2. ✅ **Add to DependencyContainer** - Create lazy properties for new dependencies
3. ✅ **Update constructors** - Modify classes to receive dependencies via constructor
4. ✅ **Update ViewModelFactory** - Add new ViewModel creation cases
5. ✅ **Update Activities** - Use ViewModelFactory to create ViewModels
6. ✅ **Remove manual creation** - Delete any `= ApiClient.create()` or `new Repository()` calls
7. ✅ **Test** - Verify all dependencies are properly injected

## Best Practices

### ✅ Constructor Injection

Always use constructor injection for dependencies:

```kotlin
// Good
class MyRepository(
    private val apiService: ApiService,
    private val prefs: UserPreferences
) {
    // Dependencies are clear and testable
}

// Bad
class MyRepository {
    private val apiService = ApiClient.createApiService() // Hard to test
    private val prefs = UserPreferences(context) // Where does context come from?
}
```

### ✅ Lazy Initialization

Use lazy initialization for expensive dependencies:

```kotlin
val apiService: ApiService by lazy {
    ApiClient.createApiService() // Only created when first accessed
}
```

### ✅ Clear Documentation

Document dependencies for each class:

```kotlin
/**
 * Repository for authentication operations
 *
 * @param prefs UserPreferences for storing auth tokens
 * @param apiService ApiService for making network requests
 */
class AuthRepository(
    private val prefs: UserPreferences,
    private val apiService: ApiService
) { ... }
```

### ✅ Avoid Service Locator Pattern

Don't pass the entire DependencyContainer to classes:

```kotlin
// Bad - Service Locator anti-pattern
class MyRepository(private val container: DependencyContainer) {
    fun doSomething() {
        val service = container.apiService // Hidden dependency
    }
}

// Good - Constructor Injection
class MyRepository(private val apiService: ApiService) {
    fun doSomething() {
        apiService.call() // Clear dependency
    }
}
```

## Comparison with Other DI Approaches

| Feature               | Manual DI        | Koin             | Dagger/Hilt      |
| --------------------- | ---------------- | ---------------- | ---------------- |
| Setup Complexity      | ⭐ Simple        | ⭐⭐ Moderate    | ⭐⭐⭐ Complex   |
| Runtime Performance   | ⭐⭐⭐ Excellent | ⭐⭐ Good        | ⭐⭐⭐ Excellent |
| Compile-time Safety   | ⭐⭐⭐ Full      | ⭐⭐ Partial     | ⭐⭐⭐ Full      |
| Learning Curve        | ⭐ Easy          | ⭐⭐ Moderate    | ⭐⭐⭐ Steep     |
| External Dependencies | ⭐⭐⭐ None      | ⭐⭐ One library | ⭐ Multiple libs |
| AndroidX Requirement  | ⭐⭐⭐ No        | ⭐ Yes (v3+)     | ⭐ Yes           |
| Build Time            | ⭐⭐⭐ Fast      | ⭐⭐ Fast        | ⭐ Slower (kapt) |

## Testing Example

### Unit Test for Repository

```kotlin
class AuthRepositoryTest {

    private lateinit var mockApiService: ApiService
    private lateinit var mockPreferences: UserPreferences
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        mockApiService = mock(ApiService::class.java)
        mockPreferences = mock(UserPreferences::class.java)

        // Create repository with mocked dependencies
        repository = AuthRepository(mockPreferences, mockApiService)
    }

    @Test
    fun `login should save token on success`() = runBlocking {
        // Given
        val loginResponse = LoginResponse(accessToken = "token123", success = true)
        `when`(mockApiService.login(any())).thenReturn(
            Response.success(loginResponse)
        )

        // When
        val result = repository.login("user", "pass")

        // Then
        assertTrue(result.isSuccess)
        verify(mockPreferences).saveAuthToken("token123")
    }
}
```

### Unit Test for ViewModel

```kotlin
class LoginViewModelTest {

    private lateinit var mockRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        mockRepository = mock(AuthRepository::class.java)
        viewModel = LoginViewModel(
            application = ApplicationProvider.getApplicationContext(),
            authRepository = mockRepository
        )
    }

    @Test
    fun `performLogin should update state on success`() {
        // Test implementation
    }
}
```

## Summary

This manual DI approach provides:

- **Clean architecture** with clear separation of concerns
- **Testable code** through constructor injection
- **No external dependencies** for maximum compatibility
- **Type-safe** dependency resolution
- **Simple to understand** and maintain
- **Follows SOLID principles** consistently

It's perfect for projects that:

- Use pre-AndroidX support libraries
- Want zero external dependencies
- Need maximum control over dependency lifecycle
- Prioritize simplicity and transparency
- Require excellent performance
