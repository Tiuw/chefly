# Model Export Guide for YOLOv8

## Problem: Wrong Model or Incorrect Export

If your detection is showing wrong labels with 100% confidence, you likely have one of these issues:

1. Using a pre-trained COCO model instead of your custom model
2. Model exported incorrectly
3. Model not trained properly

## How to Export YOLOv8 Model Correctly

If you trained your model using Ultralytics YOLOv8, here's the correct way to export:

### Python Export Script

```python
from ultralytics import YOLO

# Load your trained model
model = YOLO('path/to/your/best.pt')  # Your trained weights

# Export to TFLite
model.export(
    format='tflite',
    imgsz=640,
    int8=False,  # Set to True only if you want quantized model
    data='path/to/your/data.yaml'  # Important: must reference your training data
)

# This will create a file named 'best_saved_model/best_float32.tflite'
# or 'best_saved_model/best_int8.tflite' if int8=True
```

### Important Notes

1. **Use your custom-trained weights**, not the pre-trained ones:
   - ❌ `yolov8s.pt` (pre-trained COCO)
   - ✅ `runs/detect/train/weights/best.pt` (your custom model)

2. **Specify your data.yaml** to ensure correct class mapping:
   ```yaml
   # data.yaml example
   path: /path/to/dataset
   train: images/train
   val: images/val
   
   # Classes (MUST BE IN CORRECT ORDER)
   names:
     0: Ayam
     1: Bawang Merah
     2: Bawang Putih
     3: Bayam
     4: Cabai Hijau
     5: Cabai Merah
     6: Daging Kambing
     7: Daging Sapi
     8: Daun Bawang
     9: Ikan
     10: Kacang Panjang
     11: Kangkung
     12: Kol
     13: Nasi
     14: Tahu
     15: Telur
     16: Tempe
     17: Terong
     18: Tomat
     19: Udang
     20: Wortel
   ```

3. **Verify class count**:
   - Your model should output 25 values per detection (4 bbox + 21 classes)
   - COCO pre-trained models output 84 values (4 bbox + 80 classes)

## Verify Your Exported Model

### Method 1: Using Netron (Visual Inspection)

1. Install Netron: https://github.com/lutzroeder/netron
2. Open your `yolov8s.tflite` file
3. Check the output tensor:
   - Should be shape `[1, 25, 8400]` or `[1, 8400, 25]`
   - If you see `[1, 84, 8400]`, you exported the COCO model!

### Method 2: Using Python

```python
import tensorflow as tf

# Load the model
interpreter = tf.lite.Interpreter(model_path='yolov8s.tflite')
interpreter.allocate_tensors()

# Get input/output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("="*50)
print("INPUT TENSOR:")
print(f"  Shape: {input_details[0]['shape']}")
print(f"  Type: {input_details[0]['dtype']}")
print(f"  Name: {input_details[0]['name']}")

print("\nOUTPUT TENSOR:")
print(f"  Shape: {output_details[0]['shape']}")
print(f"  Type: {output_details[0]['dtype']}")
print(f"  Name: {output_details[0]['name']}")
print("="*50)

# Expected output for 21-class custom model:
# OUTPUT TENSOR:
#   Shape: [  1  25 8400] or [  1 8400  25]
#   Type: <class 'numpy.float32'>

# If you see [1 84 8400], you have the wrong model!
```

### Method 3: Using TFLite Analyzer

```bash
# Install TFLite analyzer
pip install tensorflow

# Analyze model
python -m tensorflow.lite.tools.flatbuffer_info --input_tflite_file=yolov8s.tflite
```

## Common Export Mistakes

### Mistake 1: Exporting Pre-trained Model
```python
# ❌ WRONG - This exports COCO model (80 classes)
model = YOLO('yolov8s.pt')
model.export(format='tflite')

# ✅ CORRECT - This exports YOUR custom model (21 classes)
model = YOLO('runs/detect/train/weights/best.pt')
model.export(format='tflite', data='data.yaml')
```

### Mistake 2: Wrong Data YAML
If your data.yaml has classes in different order than labels.txt:
- Model outputs class index based on training order
- App shows label based on labels.txt order
- Result: Wrong labels displayed!

**Solution:** Make sure `labels.txt` order EXACTLY matches `data.yaml` order.

### Mistake 3: Not Training Enough
If you trained for too few epochs or with bad data:
- Model might always predict same class
- Confidence might be unrealistic (100% or very low)

**Solution:** Check your training metrics (mAP, precision, recall) before exporting.

## Re-export Steps

If you need to re-export your model:

1. **Locate your training weights:**
   ```
   runs/detect/train/weights/best.pt
   ```

2. **Run export script:**
   ```python
   from ultralytics import YOLO
   
   # Load your best checkpoint
   model = YOLO('runs/detect/train/weights/best.pt')
   
   # Export with correct settings
   model.export(
       format='tflite',
       imgsz=640,
       int8=False,
       data='data.yaml'  # Path to your training data config
   )
   ```

3. **Find exported file:**
   ```
   runs/detect/train/weights/best_saved_model/best_float32.tflite
   ```

4. **Rename and copy to app:**
   ```bash
   # Rename
   cp runs/detect/train/weights/best_saved_model/best_float32.tflite yolov8s.tflite
   
   # Copy to Android app
   cp yolov8s.tflite app/src/main/assets/
   ```

5. **Rebuild your app**

## Testing Your Model

After exporting, test it with a simple script before putting in app:

```python
from ultralytics import YOLO
from PIL import Image

# Test the .pt model first
model = YOLO('runs/detect/train/weights/best.pt')
results = model('test_image.jpg')

# Print results
for r in results:
    boxes = r.boxes
    for box in boxes:
        cls = int(box.cls[0])
        conf = float(box.conf[0])
        print(f"Class: {model.names[cls]}, Confidence: {conf:.2%}")

# If this works correctly, then export to TFLite
# If this shows wrong results, your training is the problem
```

## If Nothing Works

If you've tried everything and still getting wrong results:

1. **Share your training command/config** - How did you train the model?
2. **Share your export command** - How did you export to TFLite?
3. **Share the Logcat output** - What does the debug logging show?
4. **Check training metrics:**
   - What was your final mAP?
   - Did training converge?
   - Did you validate on test set?

The issue is almost certainly:
- Using wrong model file (COCO instead of custom)
- Model not properly trained
- Export configuration mismatch
- Class order mismatch between training and inference

## Quick Checklist

- [ ] Model was trained on 21 Indonesian ingredient classes
- [ ] Used correct training data.yaml with proper class order
- [ ] Exported from best.pt (not yolov8s.pt)
- [ ] Output tensor shape is [1, 25, 8400] or [1, 8400, 25]
- [ ] labels.txt order matches data.yaml order exactly
- [ ] Model file is in app/src/main/assets/yolov8s.tflite
- [ ] Training metrics show good accuracy (mAP > 0.5)

