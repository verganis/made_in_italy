package com.example.madeinitaly

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

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

    companion object {
        private const val TAG = "TextRecognitionViewModel"
    }
}