package com.example.madeinitaly

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madeinitaly.cloud.CloudVisionService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.IOException

class TextRecognitionViewModel : ViewModel() {
    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> = _imageUri

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    private val _detectedLabels = MutableLiveData<List<Pair<String, Float>>>()
    val detectedLabels: LiveData<List<Pair<String, Float>>> = _detectedLabels

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val cloudVisionService = CloudVisionService()

    fun setImage(uri: Uri) {
        _imageUri.value = uri
    }

    fun recognizeText(context: Context, uri: Uri) {
        _isProcessing.value = true

        try {
            val inputImage = InputImage.fromFilePath(context, uri)

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
        } catch (e: IOException) {
            _isProcessing.value = false
            _errorMessage.value = "Error processing image: ${e.message}"
            Utils.logError("Error processing image", e)
        } catch (e: Exception) {
            _isProcessing.value = false
            _errorMessage.value = "Unexpected error: ${e.message}"
            Utils.logError("Unexpected error during text recognition", e)
        }
    }

    fun analyzeImageWithCloudVision(context: Context, uri: Uri) {
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                // Process text detection
                cloudVisionService.detectText(context, uri).fold(
                    onSuccess = { text ->
                        _recognizedText.value = if (text.isNotEmpty()) text else "No text found in image"
                    },
                    onFailure = { e ->
                        _errorMessage.value = "Cloud text detection failed: ${e.message}"
                    }
                )

                // Process label detection
                cloudVisionService.detectLabels(context, uri).fold(
                    onSuccess = { labels ->
                        _detectedLabels.value = labels
                    },
                    onFailure = { e ->
                        _errorMessage.value = "Cloud label detection failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Cloud Vision analysis failed: ${e.message}"
                Utils.logError("Cloud Vision analysis failed", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    companion object {
        private const val TAG = "TextRecognitionViewModel"
    }
}