# Chefly - AI-Powered Recipe App

Chefly is a modern Android recipe application with real-time ingredient detection using YOLOv8 TFLite model. Point your camera at ingredients and get recipe recommendations!

## Features

### 🎥 Real-time Ingredient Detection
- Uses YOLOv8 TFLite model for fast, on-device object detection
- Detects common food ingredients through your camera
- Shows bounding boxes and confidence scores in real-time

### 📖 Recipe Browsing
- Browse through a variety of recipes
- Search recipes by name or ingredients
- View detailed recipe information including ingredients, instructions, prep time, and cooking time

### ❤️ Favorites
- Save your favorite recipes
- Quick access to all your favorite recipes in one place

### 🔍 Smart Recipe Search
- Get recipe recommendations based on detected ingredients
- Automatic ingredient-based filtering
- Find recipes that match what you have in your kitchen

### 📱 Modern UI
- Built with Jetpack Compose
- Material Design 3
- Adaptive navigation for different screen sizes
- Beautiful card-based layout

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **ML Framework**: TensorFlow Lite
- **Model**: YOLOv8n (Nano version for mobile)
- **Camera**: CameraX
- **Navigation**: Navigation Compose
- **Image Loading**: Coil
- **Architecture**: MVVM with ViewModel

## Project Structure

```
app/
├── data/
│   ├── Recipe.kt              # Recipe and DetectedIngredient data classes
│   └── RecipeRepository.kt    # Recipe data source
├── ml/
│   └── ObjectDetector.kt      # YOLOv8 TFLite wrapper
├── ui/
│   ├── RecipeViewModel.kt     # Shared ViewModel
│   ├── navigation/
│   │   └── Screen.kt          # Navigation destinations
│   ├── screens/
│   │   ├── CameraScreen.kt    # Real-time detection screen
│   │   ├── HomeScreen.kt      # Recipe browsing screen
│   │   ├── RecipeDetailScreen.kt  # Recipe details
│   │   └── FavoritesAndProfileScreens.kt
│   └── theme/
│       └── ...                # Material Theme configuration
└── MainActivity.kt            # Main entry point
```

## How It Works

### Object Detection Flow

1. **Camera Preview**: CameraX provides real-time camera feed
2. **Image Processing**: Each frame is captured and preprocessed
3. **YOLOv8 Inference**: TFLite model runs inference on-device
4. **Post-processing**: Non-Maximum Suppression filters overlapping detections
5. **UI Update**: Detected ingredients are displayed with bounding boxes

### Recipe Matching

1. User points camera at ingredients
2. System detects and identifies ingredients
3. User clicks "Find Recipes" button
4. App filters recipes that contain detected ingredients
5. Results are sorted by number of matching ingredients

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 28 or higher
- Physical Android device or emulator with camera support

### Installation

1. Clone the repository
2. Open project in Android Studio
3. Sync Gradle dependencies
4. Ensure `yolov8n.tflite` model is present in `app/src/main/ml/` directory
5. Build and run the app

### YOLOv8 Model

The app uses YOLOv8n TFLite model. Make sure the model file is placed at:
```
app/src/main/ml/yolov8n.tflite
```

The model is configured to detect:
- Common food items (banana, apple, orange, broccoli, carrot, etc.)
- Kitchen items (bottle, cup, bowl, fork, knife, spoon)

## Permissions

The app requires the following permissions:
- **CAMERA**: For real-time ingredient detection
- **INTERNET**: For loading recipe images (optional)

## Usage

1. **Home Tab**: Browse all recipes, search by name or ingredients
2. **Camera Tab**: 
   - Grant camera permission when prompted
   - Point camera at ingredients
   - View detected ingredients in real-time
   - Tap "Find Recipes" to get recommendations
3. **Favorites Tab**: View all your saved favorite recipes
4. **Profile Tab**: View app information

## Customization

### Adding More Recipes

Edit `RecipeRepository.kt` to add more recipes:

```kotlin
Recipe(
    id = 11,
    name = "Your Recipe Name",
    description = "Recipe description",
    imageUrl = "https://image-url.com",
    ingredients = listOf("ingredient1", "ingredient2"),
    instructions = listOf("step1", "step2"),
    prepTime = "10 min",
    cookTime = "20 min",
    servings = 4,
    difficulty = "Easy",
    category = "Category"
)
```

### Modifying Detection Labels

Edit the `labels` list in `ObjectDetector.kt` to match your custom model's classes.

### Adjusting Detection Threshold

Modify `confidenceThreshold` in `ObjectDetector.kt` (default: 0.5):

```kotlin
val confidenceThreshold = 0.5f  // 50% confidence minimum
```

## Performance Optimization

- Uses YOLOv8n (nano) for fast inference
- Multi-threaded CPU inference (4 threads)
- NNAPI disabled (causes compatibility issues with YOLOv8)
- Frame skipping with KEEP_ONLY_LATEST strategy
- Single thread executor for inference

## Troubleshooting

### App crashes on camera detection
- **Solution**: NNAPI has been disabled in ObjectDetector.kt
- This fixes "NN_RET_CHECK failed" and "output shapes vector" errors
- If you enable NNAPI (`setUseNNAPI(true)`), it may crash on some devices

### Camera not working
- Ensure camera permission is granted
- Test on a physical device (emulator cameras may have limitations)

### Model not loading
- Verify `yolov8n.tflite` exists in `app/src/main/ml/`
- Check model file is not corrupted
- Enable `mlModelBinding` in build.gradle
- Check Logcat for TensorFlow Lite errors

### No ingredients detected
- Ensure good lighting conditions
- Hold camera steady and close to ingredients
- Check that ingredients are in the COCO dataset
- Try lowering confidence threshold in ObjectDetector.kt
- Hold camera steady
- Try adjusting confidence threshold

## Future Enhancements

- [ ] User authentication and cloud sync
- [ ] Custom recipe creation
- [ ] Shopping list generation
- [ ] Nutritional information
- [ ] Step-by-step cooking mode
- [ ] Voice commands
- [ ] Social sharing features
- [ ] Recipe ratings and reviews
- [ ] Dietary filters (vegetarian, vegan, gluten-free)
- [ ] Meal planning calendar

## License

This project is for educational purposes as part of a thesis (Skripsi) project.

## Credits

- YOLOv8 by Ultralytics
- TensorFlow Lite
- Android Jetpack libraries
- Material Design 3
- Unsplash for recipe images

## Contact

For questions or suggestions, please contact the development team.

