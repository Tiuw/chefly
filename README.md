# Chefly - AI-Powered Recipe App

Chefly adalah aplikasi Android modern untuk resep dengan deteksi bahan real-time menggunakan model YOLO26 TFLite. Arahkan kamera Anda ke bahan-bahan makanan dan dapatkan rekomendasi resep!

## ✨ Fitur Utama

### 🎥 Deteksi Bahan Real-time
- Menggunakan model YOLO26 TFLite untuk deteksi objek cepat on-device
- Deteksi bahan makanan umum melalui kamera
- Menampilkan bounding boxes dan confidence scores secara real-time
- Support untuk foto dari galeri atau kamera langsung

### 📖 Browsing Resep
- Lihat berbagai koleksi resep
- Cari resep berdasarkan nama atau bahan
- Lihat informasi lengkap resep termasuk bahan, langkah memasak, metode memasak, dan kategori

### ❤️ Resep Favorit
- Simpan resep favorit Anda
- Akses cepat ke semua resep yang disimpan dalam satu tempat
- Persistent storage menggunakan Room Database

### 🔍 Pencarian Resep Cerdas
- Dapatkan rekomendasi resep berdasarkan bahan yang terdeteksi
- Filtering berbasis bahan otomatis
- Temukan resep yang cocok dengan apa yang ada di dapur Anda
- Similarity scoring menggunakan cosine similarity

### ➕ Tambah Bahan Manual
- Tambahkan bahan makanan secara manual jika deteksi otomatis tidak akurat
- Pilih dari daftar bahan yang tersedia
- Cari resep berdasarkan bahan yang dipilih

### 🎨 UI Modern
- Dibangun dengan Jetpack Compose
- Material Design 3 dengan tema custom (Terracotta, Warm Ivory, etc.)
- Responsive navigation untuk berbagai ukuran layar
- Animated transitions dan Lottie animations
- Beautiful card-based layout

### 🚀 Onboarding & Splash Screen
- Welcome screen yang menarik untuk pengguna baru
- Splash screen dengan animation
- Persistent onboarding state menggunakan DataStore

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **ML Framework**: TensorFlow Lite
- **Model**: YOLO26 (640x640 resolution)
- **Camera**: CameraX
- **Navigation**: Navigation Compose
- **Database**: Room Database
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **State Management**: Flow & StateFlow
- **Animation**: Compose Animation + Lottie
- **Persistence**: DataStore Preferences
- **Architecture**: MVVM with ViewModel

## 📁 Project Structure

```
app/
├── data/
│   ├── Recipe.kt                    # Domain Model untuk Recipe
│   ├── local/
│   │   ├── AppDatabase.kt           # Room Database definition
│   │   ├── RecipeDao.kt             # Data Access Object
│   │   └── entity/                  # Database entities
│   ├── model/
│   │   ├── DetectedIngredient.kt    # Model untuk bahan terdeteksi
│   │   └── OnboardingPage.kt        # Model untuk halaman onboarding
│   └── repository/
│       ├── RecipeRepository.kt      # Repository untuk Recipe dengan pagination
│       └── IngredientRepository.kt  # Repository untuk Ingredient
├── ml/
│   ├── YOLO26Detector.kt            # YOLO26 TFLite wrapper dengan optimasi
│   └── DetectionTypes.kt            # Type definitions untuk deteksi
├── ui/
│   ├── navigation/
│   │   └── Screen.kt                # Navigation destinations
│   ├── screens/
│   │   ├── HomeScreen.kt            # Home/Beranda - Tampilkan resep suggested
│   │   ├── CameraScreen.kt          # Pindai - Real-time detection
│   │   ├── RecipeScreen.kt          # Resep - Browse & cari resep
│   │   ├── RecipeDetailScreen.kt    # Detail resep dengan similarity score
│   │   ├── SavedScreen.kt           # Tersimpan - Resep yang disimpan
│   │   ├── AddIngredientScreen.kt   # TambahBahan - Tambah bahan manual
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt
│   │   │   ├── OnboardingContent.kt
│   │   │   └── components/          # Komponen onboarding
│   │   └── splash/
│   │       └── SplashScreen.kt      # Splash screen saat startup
│   ├── viewmodel/
│   │   ├── MainViewModel.kt         # State management untuk onboarding
│   │   ├── HomeViewModel.kt         # ViewModel untuk HomeScreen
│   │   ├── CameraViewModel.kt       # ViewModel untuk camera & detection
│   │   ├── RecipeViewModel.kt       # ViewModel untuk recipe browsing
│   │   ├── RecipeDetailViewModel.kt # ViewModel untuk recipe detail
│   │   ├── SavedScreenViewModel.kt  # ViewModel untuk saved recipes
│   │   ├── AddIngredientViewModel.kt# ViewModel untuk add ingredient
│   │   └── SharedViewModel.kt       # Shared state antar screens
│   └── theme/
│       └── ...                      # Material Theme 3 & custom colors
├── di/
│   ├── DatabaseModule.kt            # Hilt module untuk database
│   └── CoilImageLoaderFactory.kt    # Hilt module untuk image loading
├── util/
│   └── ...                          # Utility functions
├── CheflyApplication.kt             # Hilt Android App
└── MainActivity.kt                  # Main entry point
```

## 🔄 Alur Kerja Deteksi Bahan

### Object Detection Flow

1. **Preview Kamera**: CameraX menyediakan feed kamera real-time
2. **Capture Image**: Image diambil dari image analysis callback
3. **Preprocessing**: Bitmap di-resize ke 640x640 dan dikonversi ke tensor input
4. **YOLO26 Inference**: TFLite model menjalankan inference on-device
5. **Post-processing**: 
   - Parse output tensor (1 x 25200 x 4+numClasses)
   - Apply confidence threshold (default: 0.4f)
   - Non-Maximum Suppression untuk filter deteksi overlap
   - Scale kembali ke ukuran asli frame
6. **UI Update**: Bahan terdeteksi ditampilkan dengan bounding boxes dan labels

### Recipe Matching Flow

1. User arahkan kamera ke bahan makanan
2. Sistem deteksi dan identifikasi bahan (bisa juga manual via AddIngredientScreen)
3. User tap "Temukan Resep" atau sistem navigate otomatis
4. App filter recipes yang mengandung bahan terdeteksi
5. Calculate cosine similarity antara detected ingredients dan recipe ingredients
6. Results ditampilkan diurutkan berdasarkan similarity score
7. Data query dipasskan ke RecipeDetail sebagai context

## 📱 Navigasi & Screens

### Bottom Navigation (4 Tabs)

1. **Beranda (Home)** - Tampilkan resep yang disarankan, quick action untuk scan
2. **Pindai (Scan)** - Real-time camera detection dan manual add ingredients
3. **Resep (Recipes)** - Browse semua resep, search, dengan similarity filtering
4. **Tersimpan (Saved)** - Tampilkan resep yang sudah disimpan sebagai favorit

### Flow Navigasi

```
Splash Screen
    ↓
Onboarding Screen (jika pertama kali) / Home Screen
    ↓
Home Screen (Beranda)
    ├─→ Camera Screen (Pindai)
    │   ├─→ Add Ingredient Screen (TambahBahan)
    │   └─→ Recipe Screen (Resep) dengan query
    ├─→ Recipe Screen (Resep)
    │   └─→ Recipe Detail Screen
    └─→ Saved Screen (Tersimpan)
        └─→ Recipe Detail Screen
```

## 🚀 Setup Instructions

### Prerequisites

- Android Studio Hedgehog atau lebih baru
- Android SDK 28 (compileSdk 36)
- Kotlin 1.9+
- Physical Android device atau emulator dengan support kamera

### Installation

1. Clone repository
2. Open project di Android Studio
3. Sync Gradle dependencies
4. Pastikan model YOLO26 ada di `app/src/main/ml/` directory
5. Build dan run aplikasi

### YOLO26 Model

Aplikasi menggunakan model YOLO26 TFLite dengan resolusi 640x640. Tempat model:
```
app/src/main/ml/model_name.tflite
```

Model dikonfigurasi untuk detect bahan makanan dari COCO dataset:
- Makanan: apple, banana, orange, carrot, broccoli, sandwich, hot dog, pizza, donut, cake, dll
- Kitchen items: bottle, cup, bowl, fork, knife, spoon, plate, dll
- Total classes sesuai dengan COCO dataset

### Build Configuration

```gradle
// build.gradle.kts
android {
    namespace = "com.skripsi.chefly"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 28          // Android 9.0
        targetSdk = 36
    }
    
    buildFeatures {
        compose = true
        mlModelBinding = true  // Required untuk TFLite
    }
    
    androidResources {
        noCompress += "tflite"  // Prevent .tflite file compression
    }
}
```

## 📋 Permissions

Aplikasi memerlukan permission berikut:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 💾 Persistence & Data

### Room Database
- Pre-packaged database dengan koleksi resep
- Automatic initialization dari RecipeRepository
- RecipeDao untuk akses data

### Saved/Favorite Recipes
- Stored di Room Database dengan flag `isFavorite`
- Diakses via SavedScreen dengan SavedScreenViewModel
- Toggle favorite status di RecipeDetailScreen

### Onboarding State
- Stored di DataStore Preferences
- Managed via MainViewModel
- Determines navigation flow di splash screen

## 🎨 Theme & Styling

Custom Material Design 3 theme dengan warna:
- **Terracotta** (#E36C47) - Primary accent color
- **Warm Ivory** - Background color
- Custom color scheme untuk card, button, text

## 📊 Data Models

### Recipe
```kotlin
data class Recipe(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val ingredients: String,      // Delimited string (comma, semicolon, etc)
    val steps: String,            // Delimited string
    val totalIngredients: Int?,
    val totalSteps: Int?,
    val loves: Int?,              // For popularity/sorting
    val cookingMethod: String?,
    val isFavorite: Boolean,
    val similarity: Float = 0f    // Cosine similarity score
)
```

### DetectedIngredient
```kotlin
data class DetectedIngredient(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val imageUrl: String? = null
)
```

## 🔧 Customization

### Adding/Editing Recipes

Edit `RecipeRepository.kt` atau langsung update database:

```kotlin
Recipe(
    id = "11",
    name = "Pizza Homemade",
    imageUrl = "https://image-url.com",
    category = "Italian",
    ingredients = "flour,tomato,cheese,salt,water",
    steps = "Mix flour and water\nAdd tomato sauce\nAdd cheese\nBake at 200C",
    totalIngredients = 5,
    totalSteps = 4,
    loves = 150,
    cookingMethod = "Baking"
)
```

### Modifying Detection Model

Edit `YOLO26Detector.kt` untuk customize detection:

```kotlin
// Adjust confidence threshold
companion object {
    private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.4f  // 40% confidence
    private const val MAX_TOTAL_DETECTIONS = 10
    private const val INPUT_SIZE = 640
}

// Ubah jumlah threads
val options = Interpreter.Options().apply {
    setNumThreads(4)  // Adjust untuk performance
    setUseXNNPACK(true)
    setUseNNAPI(false)
}
```

### Custom Theme Colors

Edit theme file untuk customize warna:
- Primary color
- Secondary color
- Background colors
- Text colors

## ⚡ Performance Optimization

- **YOLO26n pada 640x640** untuk balance accuracy vs speed
- **Multi-threaded inference (4 threads)** untuk CPU optimization
- **XNNPACK enabled** untuk ARM CPU acceleration
- **NNAPI disabled** (dapat cause crashes pada beberapa device)
- **Memory optimization**: Global buffer allocation di YOLO26Detector init
- **Image cache** via Coil untuk recipe images
- **DataStore** untuk efficient preference storage
- **Pagination** untuk recipe loading

## 🐛 Troubleshooting

### App crash pada camera detection
- NNAPI sudah disabled di YOLO26Detector.kt
- Fix untuk "NN_RET_CHECK failed" dan output shapes errors
- Jika enable NNAPI (`setUseNNAPI(true)`), mungkin crash di beberapa device

### Camera tidak bekerja
- Pastikan permission camera sudah granted
- Test di physical device (emulator kadang punya limitation)
- Check Logcat untuk camera-related errors

### Model tidak load
- Verify `model.tflite` ada di `app/src/main/ml/`
- Check file tidak corrupted
- Enable `mlModelBinding` di build.gradle
- Disable compression: `noCompress += "tflite"`
- Check Logcat untuk TensorFlow Lite errors

### Tidak ada bahan yang terdeteksi
- Pastikan pencahayaan cukup baik
- Hold camera steady dan dekat ke ingredient
- Check ingredient ada di COCO dataset
- Try lowering confidence threshold di YOLO26Detector.kt
- Test dengan gambar dari galeri terlebih dahulu

### Database initialization error
- Clear app data dan reinstall
- Check database file di `app/src/main/ml/` (jika preloaded)
- Check RecipeRepository initialization di MainActivity

### UI lag atau performance issues
- Reduce thread count di YOLO26Detector (dari 4 ke 2)
- Disable XNNPACK: `setUseXNNPACK(false)`
- Reduce image resolution untuk preview
- Check Logcat untuk memory leaks

## 🔄 API & Response Format

### Recipe API Response Format

Jika menggunakan remote API di masa depan:

```json
{
  "id": "1",
  "name": "Recipe Name",
  "imageUrl": "https://...",
  "category": "Category",
  "ingredients": "ingredient1,ingredient2,ingredient3",
  "steps": "Step 1\nStep 2\nStep 3",
  "totalIngredients": 3,
  "totalSteps": 3,
  "loves": 100,
  "cookingMethod": "Cooking method"
}
```

## 📚 Dependencies Utama

```gradle
// Compose & UI
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended:1.7.5")
implementation("com.airbnb.android:lottie-compose:6.4.0")

// ML & Camera
implementation("org.tensorflow:tensorflow-lite:2.13.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("androidx.camera:camera-core")
implementation("androidx.camera:camera-camera2")
implementation("androidx.camera:camera-lifecycle")
implementation("androidx.camera:camera-view")

// Database & Storage
implementation("androidx.room:room-runtime:2.8.4")
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.51")

// Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")

// Navigation
implementation("androidx.navigation:navigation-compose")
```

## 🚀 Future Enhancements

- [ ] User authentication & cloud sync
- [ ] Custom recipe creation dari user
- [ ] Shopping list generation
- [ ] Nutritional information display
- [ ] Step-by-step cooking mode dengan timer
- [ ] Voice commands
- [ ] Social sharing features
- [ ] Recipe ratings & reviews
- [ ] Dietary filters (vegetarian, vegan, gluten-free, etc)
- [ ] Meal planning calendar
- [ ] Multiple language support
- [ ] History tracking untuk detected ingredients
- [ ] Advanced search filters

## 📄 License

Proyek ini untuk keperluan pendidikan sebagai bagian dari proyek thesis (Skripsi).

## 🙏 Credits

- **YOLO26** - Ultralytics
- **TensorFlow Lite** - Google
- **Jetpack Libraries** - Google
- **Material Design 3** - Google
- **Recipe Images** - Unsplash & various sources

## 📧 Contact

Untuk pertanyaan atau saran, silakan hubungi tim development.

---

**Last Updated**: May 2026
**Version**: 1.0
**Min SDK**: Android 28 (9.0)
**Target SDK**: Android 36

