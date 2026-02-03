# Testing Guide - Chefly Object Detection Fix

## Build Instructions

### 1. Clean Build
```powershell
cd "D:\Ngoding\Skripsi Food"
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### 2. Install on Device
```powershell
# Find the APK
.\gradlew.bat installDebug

# OR manually with adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Checklist

### ✅ App Launches
- [ ] App opens without crashing
- [ ] Home screen displays correctly
- [ ] Bottom navigation works

### ✅ Camera Screen
- [ ] Navigate to Camera tab
- [ ] Camera permission prompt appears
- [ ] Grant camera permission
- [ ] Camera preview shows (no black screen)
- [ ] **No SIGSEGV crash** when camera starts

### ✅ Object Detection
- [ ] Point camera at food items:
  - Banana (class 46)
  - Apple (class 47)
  - Sandwich (class 48)
  - Orange (class 49)
  - Broccoli (class 50)
  - Carrot (class 51)
  - Hot dog (class 52)
  - Pizza (class 53)
  - Donut (class 54)
  - Cake (class 55)

- [ ] Detection boxes appear around items
- [ ] Labels show correct ingredient names
- [ ] Confidence scores display (> 0.5)
- [ ] Multiple items can be detected simultaneously

### ✅ Recipe Search
- [ ] Tap "Find Recipes" button after detection
- [ ] App navigates to Home screen
- [ ] Recipes are filtered by detected ingredients
- [ ] Results show matching recipes

### ✅ Stability
- [ ] App runs for 5+ minutes without crash
- [ ] Can switch between tabs smoothly
- [ ] Memory usage is stable (check in Android Studio Profiler)
- [ ] No ANR (Application Not Responding) dialogs

## Monitoring Logcat

### Filter for Errors
```bash
adb logcat | findstr /i "error crash exception fatal"
```

### Filter for TensorFlow
```bash
adb logcat | findstr /i "tensorflow tflite"
```

### Filter for App Logs
```bash
adb logcat | findstr "com.skripsi.chefly"
```

### Check for Specific Issues
```bash
# Check for SIGSEGV
adb logcat | findstr "SIGSEGV"

# Check for NN_RET_CHECK
adb logcat | findstr "NN_RET_CHECK"

# Check for memory issues
adb logcat | findstr "OutOfMemoryError"
```

## Expected Behavior

### ✅ SUCCESS Indicators
```
✓ Camera preview smooth (30 fps)
✓ Detection runs every frame
✓ UI responsive
✓ No crash logs in Logcat
✓ CPU usage 40-60% during detection
✓ Memory usage ~150-200MB
```

### ❌ FAILURE Indicators
```
✗ App crashes when opening camera
✗ Black screen on camera tab
✗ SIGSEGV in logcat
✗ NN_RET_CHECK errors
✗ OutOfMemoryError
✗ ANR dialogs
```

## Performance Benchmarks

### Expected Inference Times
- **Good Device** (Snapdragon 870+): 50-100ms per frame
- **Mid-Range Device** (Snapdragon 730+): 100-150ms per frame
- **Budget Device** (Snapdragon 660+): 150-250ms per frame

### Check Inference Time
Add logging to ObjectDetector.kt:
```kotlin
fun detectObjects(bitmap: Bitmap): List<DetectedIngredient> {
    if (interpreter == null) return emptyList()
    
    val startTime = System.currentTimeMillis()
    try {
        // ... existing code ...
        
        val endTime = System.currentTimeMillis()
        android.util.Log.d("ObjectDetector", "Inference time: ${endTime - startTime}ms")
        
        return postProcess(outputArray[0], bitmap.width, bitmap.height)
    } catch (e: Exception) {
        // ...
    }
}
```

## Troubleshooting

### Issue: Camera shows black screen
**Solution:**
1. Check camera permission in Settings
2. Restart app
3. Test on different device

### Issue: No detections appearing
**Solution:**
1. Lower confidence threshold to 0.3:
   ```kotlin
   val confidenceThreshold = 0.3f
   ```
2. Test with clear, well-lit images
3. Ensure food items are in COCO dataset

### Issue: App still crashes
**Solution:**
1. Check Logcat for specific error
2. Try reducing input size:
   ```kotlin
   private val inputSize = 320 // Instead of 640
   ```
3. Verify model file integrity:
   ```bash
   adb shell "ls -lh /data/data/com.skripsi.chefly/files/"
   ```

### Issue: Slow performance
**Solution:**
1. Reduce thread count:
   ```kotlin
   setNumThreads(2)
   ```
2. Skip frames in CameraScreen.kt
3. Consider using int8 quantized model

## Device Compatibility

### Tested Devices
- [ ] Emulator (API 30+)
- [ ] Physical device 1: __________
- [ ] Physical device 2: __________
- [ ] Physical device 3: __________

### Minimum Requirements
- Android 9.0+ (API 28)
- 2GB RAM
- ARM64 or x86_64 CPU
- Camera support

## Success Criteria

For this fix to be considered successful:

1. ✅ **No SIGSEGV crashes** - App runs stably during object detection
2. ✅ **No NN_RET_CHECK errors** - NNAPI disabled successfully
3. ✅ **Camera works** - Preview shows and detection runs
4. ✅ **Detections appear** - Food items are detected with bounding boxes
5. ✅ **Performance acceptable** - Inference < 250ms on mid-range devices

## Report Template

After testing, fill this out:

```
Device: _______________
Android Version: _______________
RAM: _______________

✅ App launches: YES / NO
✅ Camera works: YES / NO
✅ Detection works: YES / NO
✅ No crashes: YES / NO

Inference time: _______ms
CPU usage: _______%
Memory usage: _______MB

Issues found:
1. _______________
2. _______________
3. _______________

Additional notes:
_______________________________________________
_______________________________________________
```

## Next Steps After Testing

If all tests pass:
- [ ] Test with custom ingredients
- [ ] Add more recipe data
- [ ] Optimize UI/UX
- [ ] Prepare for release

If issues found:
- [ ] Document exact error
- [ ] Capture Logcat output
- [ ] Note device specifications
- [ ] Report for further debugging

