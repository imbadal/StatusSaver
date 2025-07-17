# ProGuard Configuration for StatusSaver

## Overview
ProGuard has been enabled for the StatusSaver Android application to optimize the release build by:
- Reducing APK size through code shrinking and obfuscation
- Removing unused code and resources
- Optimizing bytecode for better performance

## Configuration Details

### Build Configuration
- **Enabled for**: Release builds only
- **Disabled for**: Debug builds (for easier debugging)
- **Minification**: Enabled (`minifyEnabled true`)
- **Resource shrinking**: Enabled (`shrinkResources true`)

### Key Preserved Classes

#### Data Classes
The following data classes are preserved to prevent serialization/deserialization issues:
- `StatusModel` - Main status data model
- `SavedStatusModel` - Saved status with favorite functionality
- `StatusEntity` - Domain entity for status
- `StatusUiState` - UI state classes (sealed class and implementations)
- `OnboardingUiState` - Onboarding UI state classes

#### Libraries
Comprehensive rules for all used libraries:
- **GSON**: JSON serialization classes and annotations
- **Jetpack Compose**: All Compose UI components and navigation
- **Firebase**: Analytics, Messaging, Crashlytics, and Config
- **Coil**: Image loading and transformation classes
- **ExoPlayer**: Video player components
- **Accompanist**: UI utilities and pagination
- **Shimmer**: Loading effect library
- **AndroidX & Material Design**: Core Android libraries

#### Application-Specific Classes
- `StatusSaverApplication` - Main application class
- `StatusSaverFirebaseMessagingService` - Firebase messaging service
- All constants in `core.constants` package
- ViewModels and UseCases
- Repository implementations
- Utility classes

### Optimization Features

#### Code Optimization
- 5 optimization passes enabled
- Arithmetic and cast simplification disabled for safety
- Field and class merging disabled
- Access modification allowed for better optimization

#### Logging Removal
- Debug, verbose, and info log statements are removed in release builds
- This reduces APK size and improves performance

#### Debug Information
- Source file and line number information preserved for crash reporting
- Original source file names hidden for security

## Build Commands

### Release Build
```bash
./gradlew assembleRelease
```

### Debug Build (ProGuard disabled)
```bash
./gradlew assembleDebug
```

## APK Size Impact
- **Release APK size**: ~11MB (optimized)
- **Debug APK size**: Larger (no optimization)

## Troubleshooting

### Common Issues
1. **Serialization errors**: Ensure all data classes used with GSON are in the keep rules
2. **Missing classes**: Check if any new libraries need ProGuard rules
3. **Crash reporting**: Source file information is preserved for better debugging

### Adding New Rules
When adding new libraries or data classes:
1. Add appropriate `-keep` rules to `proguard-rules.pro`
2. Test with release build
3. Verify functionality works correctly

### Testing ProGuard
Always test the release build thoroughly to ensure:
- All features work correctly
- No crashes occur
- Data serialization/deserialization works
- Firebase services function properly

## Notes
- The configuration is conservative to prevent runtime issues
- All data classes are preserved to avoid serialization problems
- Library-specific rules ensure compatibility
- Debug builds remain unaffected for development convenience 