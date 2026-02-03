package com.skripsi.chefly.ml

class LiveObjectTracker(private val colorsList: List<Int>) {
    private var nextId = 0
    private val trackedObjects = mutableMapOf<Int, TrackedObjectCamera>()

    fun updateTracking(detections: List<DetectionCamera>): List<TrackedObjectCamera> {
        val currentObjects = mutableListOf<TrackedObjectCamera>()

        detections.forEach { detection ->
            val trackId = nextId++

            // Pick color based on class index (loop if not enough colors provided)
            val color = colorsList.getOrNull(detection.classIndex)
                ?: colorsList[detection.classIndex % colorsList.size]

            val trackedObj = TrackedObjectCamera(
                id = trackId,
                box = detection.box,
                classIndex = detection.classIndex,
                className = detection.className,
                confidence = detection.confidence,
                color = color
            )

            trackedObjects[trackId] = trackedObj
            currentObjects.add(trackedObj)
        }

        return currentObjects
    }
}