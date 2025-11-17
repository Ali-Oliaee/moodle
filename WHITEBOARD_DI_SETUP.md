# Whiteboard Section - Dependency Injection Setup

## Overview

This document describes the **Manual Dependency Injection** setup for the whiteboard section, following the same clean DI pattern established in the auth section.

## Architecture

### Whiteboard Dependencies

The whiteboard section includes the following dependencies managed through DI:

```kotlin
// In DependencyContainer.kt
class DependencyContainer(private val applicationContext: Context) {
    
    // ==================== Whiteboard Section Singletons ====================
    
    /**
     * PageManager singleton
     * Manages multiple pages in the whiteboard application
     * Depends on: userPreferences
     */
    val pageManager: PageManager by lazy {
        PageManager(userPreferences)
    }
    
    /**
     * ExportManager singleton
     * Manages exporting whiteboard content to various formats
     * Depends on: applicationContext
     */
    val exportManager: ExportManager by lazy {
        ExportManager(applicationContext)
    }
    
    /**
     * ImportManager singleton
     * Manages importing content into the whiteboard
     * Depends on: applicationContext
     */
    val importManager: ImportManager by lazy {
        ImportManager(applicationContext)
    }
}
```

## Refactored Components

### 1. WhiteboardViewModel

**Before:**

```kotlin
class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {
    
    // Managers (business logic)
    private val userPreferences = UserPreferences(application)
    val pageManager = PageManager(userPreferences)
    val exportManager = ExportManager(application)
    val importManager = ImportManager(application)
    
    init {
        initializeDefaultValues()
        setupPageManagerListener()
    }
}
```

**After:**

```kotlin
/**
 * ViewModel for WhiteboardActivity
 * Manages UI state and business logic following MVVM architecture
 * 
 * @param application Application context
 * @param userPreferences UserPreferences for storing settings
 * @param pageManager PageManager for page navigation and state
 * @param exportManager ExportManager for exporting whiteboard content
 * @param importManager ImportManager for importing content
 */
class WhiteboardViewModel(
    application: Application,
    private val userPreferences: UserPreferences,
    val pageManager: PageManager,
    val exportManager: ExportManager,
    val importManager: ImportManager
) : AndroidViewModel(application) {
    
    // Managers (business logic) - injected via constructor
    
    init {
        initializeDefaultValues()
        setupPageManagerListener()
    }
}
```

### 2. ViewModelFactory

**Updated to include WhiteboardViewModel:**

```kotlin
class ViewModelFactory(
    private val application: Application,
    private val dependencies: DependencyContainer
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Auth Section ViewModels
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    application = application,
                    authRepository = dependencies.authRepository
                ) as T
            }
            
            // Whiteboard Section ViewModels
            modelClass.isAssignableFrom(WhiteboardViewModel::class.java) -> {
                WhiteboardViewModel(
                    application = application,
                    userPreferences = dependencies.userPreferences,
                    pageManager = dependencies.pageManager,
                    exportManager = dependencies.exportManager,
                    importManager = dependencies.importManager
                ) as T
            }
            
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
```

### 3. WhiteboardActivity

**Before:**

```kotlin
class WhiteboardActivity : AppCompatActivity() {
    private lateinit var viewModel: WhiteboardViewModel
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProviders.of(this).get(WhiteboardViewModel::class.java)
        
        // Initialize preferences
        userPreferences = UserPreferences(this)
        
        // ... rest of initialization
    }
}
```

**After:**

```kotlin
class WhiteboardActivity : AppCompatActivity() {
    private lateinit var viewModel: WhiteboardViewModel
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel with DI
        val dependencies = (application as MoodleApplication).dependencyContainer
        val factory = ViewModelFactory(application, dependencies)
        viewModel = ViewModelProviders.of(this, factory).get(WhiteboardViewModel::class.java)
        
        // Get UserPreferences from dependency container
        userPreferences = dependencies.userPreferences
        
        // ... rest of initialization
    }
}
```

## Dependency Graph

```
MoodleApplication
    └── DependencyContainer
        ├── UserPreferences (singleton)
        ├── PageManager (singleton)
        │   └── depends on: UserPreferences
        ├── ExportManager (singleton)
        │   └── depends on: applicationContext
        ├── ImportManager (singleton)
        │   └── depends on: applicationContext
        └── WhiteboardViewModel (created by factory)
            ├── depends on: UserPreferences
            ├── depends on: PageManager
            ├── depends on: ExportManager
            └── depends on: ImportManager
```

## Component Responsibilities

### PageManager

**Responsibility:** Manages multiple pages in the whiteboard application

**Dependencies:**
- `UserPreferences` - For persisting page state and shared settings

**Provides:**
- Page creation and deletion
- Page navigation
- Undo/redo per page
- Shared settings (color, stroke width, background)

### ExportManager

**Responsibility:** Manages exporting whiteboard content to various formats

**Dependencies:**
- `Context` - For file I/O and resource access

**Provides:**
- Export to PNG (single page or all pages)
- Export to custom .moodle format
- File management and saving

### ImportManager

**Responsibility:** Manages importing content into the whiteboard

**Dependencies:**
- `Context` - For file I/O and resource access

**Provides:**
- Import images from device
- Import .moodle format files
- Deserialization and data restoration

### WhiteboardViewModel

**Responsibility:** Manages UI state and business logic for the whiteboard

**Dependencies:**
- `Application` - For AndroidViewModel
- `UserPreferences` - For settings persistence
- `PageManager` - For page management
- `ExportManager` - For export operations
- `ImportManager` - For import operations

**Provides:**
- UI state management (LiveData)
- Tool selection and configuration
- Page management coordination
- Import/export coordination
- Event handling

## Benefits Achieved

### ✅ Single Responsibility Principle

Each class has one clear responsibility:

- **DependencyContainer** - Manages dependency creation and lifecycle
- **ViewModelFactory** - Creates ViewModels with dependencies
- **PageManager** - Manages page state and navigation
- **ExportManager** - Handles export operations
- **ImportManager** - Handles import operations
- **WhiteboardViewModel** - Manages UI state and coordinates business logic
- **WhiteboardActivity** - Handles UI and user interactions

### ✅ Dependency Inversion Principle

- `WhiteboardViewModel` doesn't create its dependencies - receives them via constructor
- `PageManager` receives `UserPreferences` instead of creating it
- All dependencies flow inward through constructor injection

### ✅ Don't Repeat Yourself (DRY)

- Single instance of `PageManager`, `ExportManager`, `ImportManager` across the app
- `UserPreferences` singleton shared between auth and whiteboard sections
- No duplicate dependency creation code

### ✅ Testability

Dependencies can be easily mocked for unit testing:

```kotlin
// Create test dependencies
val mockUserPreferences = mock<UserPreferences>()
val mockPageManager = mock<PageManager>()
val mockExportManager = mock<ExportManager>()
val mockImportManager = mock<ImportManager>()

// Create ViewModel with mocked dependencies
val viewModel = WhiteboardViewModel(
    application = testApplication,
    userPreferences = mockUserPreferences,
    pageManager = mockPageManager,
    exportManager = mockExportManager,
    importManager = mockImportManager
)

// Test ViewModel behavior with controlled dependencies
```

### ✅ No Tight Coupling

- `WhiteboardViewModel` doesn't create managers - receives them via constructor
- `PageManager` doesn't create `UserPreferences` - receives it via constructor
- Easy to swap implementations without modifying consumers

### ✅ Shared Dependencies

- `UserPreferences` is shared between auth and whiteboard sections
- Single source of truth for user settings
- Consistent data access across the application

## UI-Specific Controllers (Not in DI)

The following controllers are **not** managed by DI because they are tightly coupled to Android UI components and lifecycle:

### RecordingController

**Why not in DI:**
- Requires `Activity` for permissions and `MediaProjection`
- Manages Android-specific `MediaRecorder` lifecycle
- Tightly coupled to activity lifecycle

**Current approach:**
```kotlin
recordingController = RecordingController(this)
recordingController.initialize(toggleRecordBtn)
```

### PopupController

**Why not in DI:**
- Creates and manages `PopupWindow` instances
- Requires view references for anchoring popups
- Manages UI-specific click listeners

**Current approach:**
```kotlin
popupController = PopupController(this, whiteboardView)
setupPopupCallbacks()
```

### SplitModeController

**Why not in DI:**
- Manages split screen UI layout
- Directly manipulates view visibility and positioning
- Tightly coupled to specific view hierarchy

**Current approach:**
```kotlin
splitModeController = SplitModeController(this, whiteboardView)
splitModeController.initialize(enableSplit, disableSplit, toolbar)
```

## Testing Example

### Unit Test for WhiteboardViewModel

```kotlin
class WhiteboardViewModelTest {

    private lateinit var mockUserPreferences: UserPreferences
    private lateinit var mockPageManager: PageManager
    private lateinit var mockExportManager: ExportManager
    private lateinit var mockImportManager: ImportManager
    private lateinit var viewModel: WhiteboardViewModel

    @Before
    fun setup() {
        mockUserPreferences = mock(UserPreferences::class.java)
        mockPageManager = mock(PageManager::class.java)
        mockExportManager = mock(ExportManager::class.java)
        mockImportManager = mock(ImportManager::class.java)

        // Create ViewModel with mocked dependencies
        viewModel = WhiteboardViewModel(
            application = ApplicationProvider.getApplicationContext(),
            userPreferences = mockUserPreferences,
            pageManager = mockPageManager,
            exportManager = mockExportManager,
            importManager = mockImportManager
        )
    }

    @Test
    fun `selectTool should update UI state with new tool`() {
        // Given
        val newTool = DrawingTool.ERASER

        // When
        viewModel.selectTool(newTool)

        // Then
        assertEquals(newTool, viewModel.uiState.value?.currentTool)
    }

    @Test
    fun `selectColor should update shared color in PageManager`() {
        // Given
        val newColor = Color.RED

        // When
        viewModel.selectColor(newColor)

        // Then
        verify(mockPageManager).updateSharedColor(newColor)
        assertEquals(newColor, viewModel.uiState.value?.currentColor)
    }

    @Test
    fun `nextPageOrCreate should create new page when at last page`() {
        // Given
        `when`(mockPageManager.canGoToNextPage()).thenReturn(false)
        `when`(mockPageManager.canCreateNewPage()).thenReturn(true)

        // When
        viewModel.nextPageOrCreate()

        // Then
        verify(mockPageManager).createNewPage()
    }
}
```

### Unit Test for PageManager

```kotlin
class PageManagerTest {

    private lateinit var mockUserPreferences: UserPreferences
    private lateinit var pageManager: PageManager

    @Before
    fun setup() {
        mockUserPreferences = mock(UserPreferences::class.java)
        
        // Setup default returns
        `when`(mockUserPreferences.getSharedColor()).thenReturn(Color.BLACK)
        `when`(mockUserPreferences.getSharedStrokeWidth()).thenReturn(8f)
        `when`(mockUserPreferences.getCurrentPageId()).thenReturn(1)
        `when`(mockUserPreferences.getNextPageId()).thenReturn(2)
        `when`(mockUserPreferences.getPageIds()).thenReturn(setOf(1))

        pageManager = PageManager(mockUserPreferences)
    }

    @Test
    fun `getCurrentPage should return first page initially`() {
        // When
        val currentPage = pageManager.getCurrentPage()

        // Then
        assertEquals(1, currentPage.pageId)
    }

    @Test
    fun `createNewPage should add page and navigate to it`() {
        // Given
        val initialTotalPages = pageManager.getTotalPages()

        // When
        val result = pageManager.createNewPage()

        // Then
        assertTrue(result)
        assertEquals(initialTotalPages + 1, pageManager.getTotalPages())
    }

    @Test
    fun `updateSharedColor should persist to UserPreferences`() {
        // Given
        val newColor = Color.BLUE

        // When
        pageManager.updateSharedColor(newColor)

        // Then
        verify(mockUserPreferences).saveSharedColor(newColor)
        assertEquals(newColor, pageManager.sharedColor)
    }
}
```

## Migration Notes

### What Changed

1. **WhiteboardViewModel** now receives all managers via constructor
2. **WhiteboardActivity** uses `ViewModelFactory` to create ViewModel
3. **UserPreferences** is obtained from `DependencyContainer` instead of being created
4. **All managers** are singleton instances managed by DI

### What Stayed the Same

1. **UI Controllers** (Recording, Popup, SplitMode) remain activity-scoped
2. **WhiteboardView** remains a custom view, not managed by DI
3. **Public API** of all classes remains unchanged
4. **Functionality** is identical - only instantiation changed

## Adding New Whiteboard Dependencies

### Step 1: Create the Dependency Class

```kotlin
class DrawingStateManager(private val context: Context) {
    fun saveState() { /* ... */ }
    fun restoreState() { /* ... */ }
}
```

### Step 2: Add to DependencyContainer

```kotlin
class DependencyContainer(private val applicationContext: Context) {
    // ... existing dependencies
    
    val drawingStateManager: DrawingStateManager by lazy {
        DrawingStateManager(applicationContext)
    }
}
```

### Step 3: Update WhiteboardViewModel Constructor

```kotlin
class WhiteboardViewModel(
    application: Application,
    private val userPreferences: UserPreferences,
    val pageManager: PageManager,
    val exportManager: ExportManager,
    val importManager: ImportManager,
    val drawingStateManager: DrawingStateManager  // New dependency
) : AndroidViewModel(application) {
    // Use the new dependency
}
```

### Step 4: Update ViewModelFactory

```kotlin
modelClass.isAssignableFrom(WhiteboardViewModel::class.java) -> {
    WhiteboardViewModel(
        application = application,
        userPreferences = dependencies.userPreferences,
        pageManager = dependencies.pageManager,
        exportManager = dependencies.exportManager,
        importManager = dependencies.importManager,
        drawingStateManager = dependencies.drawingStateManager  // Add new dependency
    ) as T
}
```

## Best Practices

### ✅ Constructor Injection

Always use constructor injection for dependencies:

```kotlin
// Good - Clear dependencies
class PageManager(
    private val userPreferences: UserPreferences
) {
    // Dependencies are clear and testable
}

// Bad - Hidden dependencies
class PageManager {
    private val userPreferences = UserPreferences(context)  // Where does context come from?
}
```

### ✅ Singleton for Stateful Components

Use singletons for components that maintain state:

```kotlin
// PageManager maintains state across the app - singleton
val pageManager: PageManager by lazy {
    PageManager(userPreferences)
}
```

### ✅ Activity-Scoped for UI Controllers

Keep UI-specific controllers activity-scoped:

```kotlin
// RecordingController is tied to activity lifecycle - not in DI
recordingController = RecordingController(this)
```

### ✅ Separate Business Logic from UI Logic

- **ViewModel** contains business logic and state
- **Activity** handles UI events and view updates
- **Controllers** handle Android-specific UI components

## Summary

The whiteboard DI setup provides:

- **Clean architecture** with clear separation of concerns
- **Testable components** through constructor injection
- **Shared dependencies** across sections (UserPreferences)
- **Singleton pattern** for stateful managers
- **Type-safe** dependency resolution
- **Consistent pattern** across the application

This setup follows the same principles as the auth section and can be easily extended to other sections of the app.

