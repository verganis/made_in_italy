package com.example.madeinitaly

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionViewModel : ViewModel() {
    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> = _imageUri

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun setImage(uri: Uri) {
        _imageUri.value = uri
    }

    fun recognizeText(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            // Image preprocessing: scale and slightly rotate
            val processedBitmap = preprocessBitmap(bitmap)

            val inputImage = InputImage.fromBitmap(processedBitmap, 0)

            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    _isProcessing.value = false
                    if (text.text.isNotEmpty()) {
                        _recognizedText.value = text.text
                    } else {
                        _recognizedText.value = "No text found in image"
                    }
                }
                .addOnFailureListener { e ->
                    _isProcessing.value = false
                    _errorMessage.value = "Text recognition failed: ${e.message}"
                    Utils.logError("Text recognition failed", e)
                }
        } catch (e: Exception) {
            _isProcessing.value = false
            _errorMessage.value = "Error processing image: ${e.message}"
            Utils.logError("Error processing image", e)
        }
    }

    private fun preprocessBitmap(originalBitmap: Bitmap): Bitmap {
        // Scale the image
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            originalBitmap.width * 2,
            originalBitmap.height * 2,
            true
        )

        // Slight rotation matrix
        val matrix = Matrix().apply {
            postRotate(1f) // Slight 1-degree rotation
        }

        // Apply rotation and return
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }
}