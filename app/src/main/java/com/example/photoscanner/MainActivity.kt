package com.example.photoscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions

class MainActivity : AppCompatActivity(), ImageDetectedListener {

    val TAG = this.javaClass.simpleName
    lateinit var remoteModel: FirebaseAutoMLRemoteModel
    lateinit var labeler: FirebaseVisionImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeModel()

        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
            .addOnSuccessListener {
                Log.d(TAG, "Model successfully downloaded");
                val optionsBuilder =
                    FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel)

                // Evaluate your model in the Firebase console to determine an appropriate threshold.
                val options = optionsBuilder.setConfidenceThreshold(0.75f).build()
                labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options)

                tryToStartCamera()
            }
    }

    private fun hasCameraPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, CameraFragment(this))
                    .commit()
            }
        }
    }

    fun initializeModel() {
        remoteModel = FirebaseAutoMLRemoteModel.Builder(REMOTE_HANDS_DETECTION_MODEL)
            .build()
        val conditions = FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
            .addOnCompleteListener {
                // Success.
            }
    }

    fun tryToStartCamera() {
        if (hasCameraPermissions()) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, CameraFragment(this))
                .commit()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun processImage(image: FirebaseVisionImage) {
        labeler.processImage(image)
            .addOnSuccessListener { labels ->
                // Task completed successfully
                // ...
                Log.d("test", "image processed")
                for (label in labels) {
                    when (label.text) {
                        "other" -> Log.d(
                            TAG, "label.confidence " + label.confidence +
                                    " label.text = " + label.text
                        )
                        "static_hand" -> Log.e(
                            TAG, "label.confidence " + label.confidence +
                                    " label.text = " + label.text
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
                Log.e(TAG, "error happened: ", e);
            }
    }
}