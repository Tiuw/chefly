# Debugging Guide: Wrong Labels with 100% Confidence

## Problem Description
The object detection model is showing:
- **Wrong labels** (detecting ingredients that aren't in the image)
- **100% confidence** (which is unrealistic and indicates a problem)

## Likely Root Causes

### 1. Model Output Format Mismatch
Your YOLOv8s model was custom-trained on 21 Indonesian ingredient classes, but the code might be interpreting the output incorrectly.

**Expected format for custom YOLOv8:**
- Output shape: `[1, 25, 8400]` where 25 = 4 bbox coords + 21 classes
- OR: `[1, 8400, 25]` depending on the export format

**What to check:**
- The actual output shape of your model
- Whether the model outputs normalized coordinates (0-1) or pixel coordinates
- Whether class scores need softmax or sigmoid activation

### 2. Model Not Properly Trained
If your model wasn't trained correctly:
- It might always predict the same class
- Confidence values might be raw logits (not probabilities)
- The model might need post-processing that isn't being applied

### 3. Wrong Model File
You might be using the wrong model file:
- `yolov8s.tflite` might be a COCO-pretrained model (80 classes) instead of your custom model (21 classes)
- This would cause class index misalignment

## Diagnostic Steps

### Step 1: Check Logcat Output
I've added extensive logging to help diagnose the issue. Run your app and check Logcat for these messages:

```
Tag: YOLODetector

Look for:
1. "Model loaded: 640x640, Quantized: [true/false], NNAPI=false, NCHW=[true/false]"
   → Confirms model input format

2. "Parsed output: numBoxes=8400 attrCount=25 attrIsLast=false batch=1"
   → Shows output tensor shape interpretation
   → attrCount should be 25 (4 bbox + 21 classes)
   → If it's 84 (4 + 80), you're using a COCO model, not your custom model!

3. "Box 0: xc=0.XXX yc=0.XXX w=0.XXX h=0.XXX | maxConf=X.XXX classIdx=X | First 10 class scores: X.XXX,X.XXX,..."
   → Shows raw model output for first few detections
   → If maxConf is always 1.0 or very close, there's a problem
   → If all class scores are similar, the model isn't confident

4. "Adding detection: className='Ayam' (idx=0/20), confidence=XX%, box=(x,y,x,y)"
   → Shows what's being detected
   → Check if className matches what's actually in the image

5. "Detection summary: Found X raw detections, Y after NMS"
   → Shows how many detections were found
```

### Step 2: Verify Your Model
Check your `yolov8s.tflite` file:

```bash
# If you have Python and TensorFlow Lite tools:
python -c "
import tensorflow as tf
interpreter = tf.lite.Interpreter(model_path='app/src/main/assets/yolov8s.tflite')
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print('INPUT:')
print(f'  Shape: {input_details[0][\"shape\"]}')
print(f'  Type: {input_details[0][\"dtype\"]}')

print('OUTPUT:')
print(f'  Shape: {output_details[0][\"shape\"]}')
print(f'  Type: {output_details[0][\"dtype\"]}')
"
```

**What you should see:**
- Input: `[1, 3, 640, 640]` or `[1, 640, 640, 3]`
- Output: `[1, 25, 8400]` or `[1, 8400, 25]` for 21 classes

**If you see:**
- Output: `[1, 84, 8400]` → You're using a COCO model (80 classes), NOT your custom model!

### Step 3: Verify Labels Match Model
Your `labels.txt` has 21 classes in this order:
```
0. Ayam
1. Bawang Merah
2. Bawang Putih
3. Bayam
4. Cabai Hijau
5. Cabai Merah
6. Daging Kambing
7. Daging Sapi
8. Daun Bawang
9. Ikan
10. Kacang Panjang
11. Kangkung
12. Kol
13. Nasi
14. Tahu
15. Telur
16. Tempe
17. Terong
18. Tomat
19. Udang
20. Wortel
```

**Make sure:**
- This order EXACTLY matches the order used when training your model
- If the order is different, the wrong labels will be assigned

## Solutions

### Solution 1: Use the Correct Model
If you're using a COCO-pretrained model instead of your custom-trained model:

1. Replace `app/src/main/assets/yolov8s.tflite` with your custom-trained model
2. Make sure the model was exported correctly from your training

### Solution 2: Fix Model Export
If your model output needs activation functions:

The code currently reads raw output values. If your model outputs raw logits instead of probabilities, you might need to apply sigmoid:

```kotlin
// In YOLOv8sDetector.kt, around line 405
for (c in 4 until attrCount) {
    val rawScore = outputArray.getOrNull(c)?.getOrNull(b) ?: 0f
    // Apply sigmoid if model outputs logits
    val classConfidence = 1.0f / (1.0f + kotlin.math.exp(-rawScore))
    if (classConfidence > maxConfidence) {
        maxConfidence = classConfidence
        maxClassIndex = c - 4
    }
}
```

### Solution 3: Adjust Confidence Threshold
If the model is working but too sensitive:

In `CameraScreen.kt`, the confidence threshold is set to `0.25f` (25%). Try different values:

```kotlin
// Line 158 (for uploaded images):
val dets = detector.detectObjects(scaledBmp, 0.50f) // Try 50%

// Line 283 (for camera feed):
val dets = detector.detectObjects(bmp, 0.50f) // Try 50%
```

### Solution 4: Retrain Your Model
If the model genuinely has poor accuracy:

1. Check your training dataset quality
2. Ensure sufficient samples for each class (at least 100+ per class)
3. Use proper data augmentation
4. Train for more epochs
5. Validate your model on a test set before exporting

## Testing Procedure

1. **Upload a clear test image** with known ingredients (e.g., a photo of a tomato)
2. **Check Logcat immediately** (filter by "YOLODetector")
3. **Look for these specific things:**
   - What is `attrCount`? Should be 25 (4+21)
   - What are the raw class scores? They should vary, not all be the same
   - What is the detected className? Does it match reality?
   - What is the confidence? Should be 0.0-1.0, not exactly 1.0

4. **Share the Logcat output** if you need help interpreting it

## Quick Test
To quickly test if it's a model issue vs code issue:

1. Take a photo of a **tomato** (Tomat is index 18)
2. Upload it
3. Check if it detects "Tomat" or something else
4. Check the confidence value
5. Look at Logcat for the raw scores

If it always detects the same class regardless of the image, your model file is wrong or corrupted.

## Expected Behavior

**Good detection:**
```
YOLODetector: Box 0: xc=0.512 yc=0.423 w=0.234 h=0.345 | maxConf=0.856 classIdx=18 | First 10 class scores: 0.023,0.012,0.034,0.011,0.008,0.015,0.021,0.009,0.013,0.008,
YOLODetector: Adding detection: className='Tomat' (idx=18/20), confidence=85%, box=(123,234,456,567)
```

**Bad detection (wrong model):**
```
YOLODetector: Box 0: xc=0.512 yc=0.423 w=0.234 h=0.345 | maxConf=1.000 classIdx=0 | First 10 class scores: 1.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000,
YOLODetector: Adding detection: className='Ayam' (idx=0/20), confidence=100%, box=(123,234,456,567)
```

The second example shows 100% confidence with all other scores at 0, which indicates a model problem.

