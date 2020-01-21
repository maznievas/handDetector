package com.example.photoscanner

import com.google.firebase.ml.vision.common.FirebaseVisionImage

interface ImageDetectedListener {
    fun processImage(image: FirebaseVisionImage)
}