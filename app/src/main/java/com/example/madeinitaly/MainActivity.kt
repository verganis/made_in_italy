package com.example.madeinitaly

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.madeinitaly.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TextRecognitionViewModel

    // Register for gallery selection result
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setImage(uri)
                viewModel.recognizeText(this, uri)
            }
        }
    }

    // Register for permission result
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Utils.showToast(this, "Storage permission is required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[TextRecognitionViewModel::class.java]

        // Set up the button click listener
        binding.buttonUpload.setOnClickListener {
            if (PermissionUtils.checkStoragePermission(this)) {
                openGallery()
            } else {
                requestPermissionLauncher.launch(PermissionUtils.getRequiredPermission())
            }
        }

        // Observe LiveData
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.imageUri.observe(this) { uri ->
            Glide.with(this)
                .load(uri)
                .into(binding.imageView)
        }

        viewModel.recognizedText.observe(this) { text ->
            binding.textViewResult.text = text
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }
}