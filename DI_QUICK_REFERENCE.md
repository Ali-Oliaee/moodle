# Dependency Injection - Quick Reference Guide

## Complete DI Structure

### DependencyContainer

Located at: `app/src/main/java/com/example/moodle/di/DependencyContainer.kt`

```kotlin
class DependencyContainer(private val applicationContext: Context) {
    
    // ==================== Shared Dependencies ====================
    val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }
    
    // ==================== Auth Section ====================
    val apiService: ApiService by lazy {
        ApiClient.createApiService()
    }
    
    val authRepository: AuthRepository by lazy {
        AuthRepository(
            prefs = userPreferences,
            apiService = apiService
        )
    }
    
    // ==================== Whiteboard Section ====================
    val pageManager: PageManager by lazy {
        PageManager(userPreferences)
    }
    
    val exportManager: ExportManager by lazy {
        ExportManager(applicationContext)
    }
    
    val importManager: ImportManager by lazy {
        ImportManager(applicationContext)
    }
}
```

### ViewModelFactory

Located at: `app/src/main/java/com/example/moodle/di/ViewModelFactory.kt`

```kotlin
class ViewModelFactory(
    private val application: Application,
    private val dependencies: DependencyContainer
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Auth Section
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(application, dependencies.authRepository) as T
            }
            
            // Whiteboard Section
            modelClass.isAssignableFrom(WhiteboardViewModel::class.java) -> {
                WhiteboardViewModel(
                    application,
                    dependencies.userPreferences,
                    dependencies.pageManager,
                    dependencies.exportManager,
                    dependencies.importManager
                ) as T
            }
            
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
```

## How to Use DI in Activities

### Pattern 1: With ViewModel

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var viewModel: MyViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get dependencies from MoodleApplication
        val dependencies = (application as MoodleApplication).dependencyContainer
        
        // Create ViewModel using factory
        val factory = ViewModelFactory(application, dependencies)
        viewModel = ViewModelProviders.of(this, factory).get(MyViewModel::class.java)
        
        // ... rest of initialization
    }
}
```

### Pattern 2: Direct Access to Dependencies

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get dependencies from MoodleApplication
        val dependencies = (application as MoodleApplication).dependencyContainer
        
        // Access dependencies directly
        userPreferences = dependencies.userPreferences
        
        // ... rest of initialization
    }
}
```

## Dependency Graph

```
MoodleApplication
│
└── DependencyContainer
    │
    ├── userPreferences (singleton)
    │   └── Used by: AuthRepository, PageManager
    │
    ├── apiService (singleton)
    │   └── Used by: AuthRepository
    │
    ├── authRepository (singleton)
    │   └── Used by: LoginViewModel
    │
    ├── pageManager (singleton)
    │   └── Used by: WhiteboardViewModel
    │
    ├── exportManager (singleton)
    │   └── Used by: WhiteboardViewModel
    │
    └── importManager (singleton)
        └── Used by: WhiteboardViewModel
```

## ViewModels and Their Dependencies

| ViewModel | Dependencies |
|-----------|-------------|
| `LoginViewModel` | `authRepository` |
| `WhiteboardViewModel` | `userPreferences`, `pageManager`, `exportManager`, `importManager` |

## Repositories and Their Dependencies

| Repository/Manager | Dependencies |
|-------------------|-------------|
| `AuthRepository` | `userPreferences`, `apiService` |
| `PageManager` | `userPreferences` |
| `ExportManager` | `applicationContext` |
| `ImportManager` | `applicationContext` |

## Adding a New ViewModel to DI

### Step 1: Define Dependencies in Constructor

```kotlin
class ProfileViewModel(
    application: Application,
    private val userPreferences: UserPreferences,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(application) {
    // ViewModel implementation
}
```

### Step 2: Add Repository to DependencyContainer (if needed)

```kotlin
class DependencyContainer(private val applicationContext: Context) {
    // ... existing dependencies
    
    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(userPreferences, apiService)
    }
}
```

### Step 3: Add to ViewModelFactory

```kotlin
modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
    ProfileViewModel(
        application,
        dependencies.userPreferences,
        dependencies.profileRepository
    ) as T
}
```

### Step 4: Use in Activity

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

## Common Patterns

### Singleton Pattern

Use for stateful components that should be shared across the app:

```kotlin
val pageManager: PageManager by lazy {
    PageManager(userPreferences)
}
```

### Factory Pattern

Use for components that need new instances:

```kotlin
fun createImageProcessor(): ImageProcessor {
    return ImageProcessor(applicationContext)
}
```

### Sharing Dependencies Across Sections

`UserPreferences` is shared between auth and whiteboard:

```kotlin
// In DependencyContainer
val userPreferences: UserPreferences by lazy {
    UserPreferences(applicationContext)
}

// Used by AuthRepository
val authRepository: AuthRepository by lazy {
    AuthRepository(prefs = userPreferences, apiService = apiService)
}

// Used by PageManager
val pageManager: PageManager by lazy {
    PageManager(userPreferences)
}
```

## Testing with DI

### Mocking Dependencies

```kotlin
class MyViewModelTest {
    private lateinit var mockRepository: MyRepository
    private lateinit var viewModel: MyViewModel
    
    @Before
    fun setup() {
        mockRepository = mock(MyRepository::class.java)
        viewModel = MyViewModel(
            application = testApplication,
            repository = mockRepository
        )
    }
    
    @Test
    fun `test with mocked dependency`() {
        // Test implementation
    }
}
```

### Creating Test Container

```kotlin
class TestDependencyContainer(context: Context) : DependencyContainer(context) {
    // Override dependencies with test implementations
    override val apiService: ApiService by lazy {
        MockApiService()
    }
}
```

## Files Modified

### Core DI Files
- ✅ `di/DependencyContainer.kt` - Added whiteboard dependencies
- ✅ `di/ViewModelFactory.kt` - Added WhiteboardViewModel

### Auth Section
- ✅ `ui/login/LoginViewModel.kt` - Uses constructor injection
- ✅ `ui/login/LoginActivity.kt` - Uses ViewModelFactory

### Whiteboard Section
- ✅ `ui/whiteboard/WhiteboardViewModel.kt` - Refactored to use constructor injection
- ✅ `ui/whiteboard/WhiteboardActivity.kt` - Uses ViewModelFactory

### Application
- ✅ `MoodleApplication.kt` - Initializes DependencyContainer

## Benefits

1. **Single Source of Truth** - All dependencies in one place
2. **Testability** - Easy to mock dependencies
3. **Type Safety** - Compile-time dependency resolution
4. **No External Libraries** - Pure Kotlin/Java solution
5. **Shared Dependencies** - Reuse singletons across sections
6. **Clear Dependencies** - Constructor injection makes dependencies explicit

## Documentation

- **[DI_SETUP.md](DI_SETUP.md)** - Complete guide for auth section setup
- **[WHITEBOARD_DI_SETUP.md](WHITEBOARD_DI_SETUP.md)** - Complete guide for whiteboard section setup
- **[DI_QUICK_REFERENCE.md](DI_QUICK_REFERENCE.md)** - This quick reference guide

## Next Steps

To add DI to a new section:

1. Identify dependencies needed
2. Add dependencies to `DependencyContainer`
3. Create ViewModel with constructor injection
4. Add ViewModel to `ViewModelFactory`
5. Update Activity to use `ViewModelFactory`
6. Document the changes

---

**Last Updated:** November 2025  
**Pattern:** Manual Dependency Injection  
**Status:** ✅ Production Ready

