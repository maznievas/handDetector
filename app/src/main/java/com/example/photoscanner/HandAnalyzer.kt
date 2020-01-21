package com.example.photoscanner

import android.os.Handler
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata



class HandAnalyzer(var imageDetectedListener: ImageDetectedListener) : ImageAnalysis.Analyzer{

    val TAG = this.javaClass.simpleName

    private fun degreesToFirebaseRotation(degrees: Int): Int {
        when (degrees) {
            0 -> return FirebaseVisionImageMetadata.ROTATION_0
            90 -> return FirebaseVisionImageMetadata.ROTATION_90
            180 -> return FirebaseVisionImageMetadata.ROTATION_180
            270 -> return FirebaseVisionImageMetadata.ROTATION_270
            else -> throw IllegalArgumentException(
                "Rotation must be 0, 90, 180, or 270."
            )
        }
    }
    override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
        val mediaImage = imageProxy?.image
        val imageRotation = degreesToFirebaseRotation(degrees)
        if (mediaImage != null) {
            val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
            // Pass image to an ML Kit Vision API
            // ...
            imageDetectedListener.processImage(image)
            Log.d(TAG, "analyze");
        }
    }
}