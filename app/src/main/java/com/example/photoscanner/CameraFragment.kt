package com.example.photoscanner

import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraFragment(var imageDetectedListener: ImageDetectedListener) : Fragment() {

    val TAG = this.javaClass.simpleName

    private lateinit var cameraView: TextureView

    private var cameraRunning = false
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        cameraView = view.findViewById(R.id.textureView)
        cameraView.post {
            startCamera()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (cameraRunning) {
            CameraX.unbindAll()
            cameraRunning = false
        }
    }

    fun startCamera() {
        CameraX.unbindAll()

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
          //  setTargetResolution(Size(cameraView.width, cameraView.height))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = cameraView.parent as ViewGroup
            parent.removeView(cameraView)
            parent.addView(cameraView, 0)

            cameraView.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            val analyzerThread = HandlerThread(
                "LuminosityAnalysis").apply { start() }
            setTargetResolution(Size(1280, 720))
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, HandAnalyzer(imageDetectedListener))
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = cameraView.width / 2f
        val centerY = cameraView.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(cameraView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        cameraView.setTransform(matrix)
    }

}