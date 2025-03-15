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
import com.example.madeinitaly.data.ProductDataModel
import com.example.madeinitaly.databinding.ActivityMainBinding
import com.example.madeinitaly.extraction.DataExtractor

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TextRecognitionViewModel

    // Register for gallery selection result
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setImage(uri)
                binding.buttonAnalyze.isEnabled = true
                binding.authenticityImage.visibility = View.GONE
                binding.authenticityText.visibility = View.GONE
                Glide.with(this).load(uri).into(binding.imageView)
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

        // Set up button click listeners
        binding.buttonUpload.setOnClickListener {
            if (PermissionUtils.checkStoragePermission(this)) {
                openGallery()
            } else {
                requestPermissionLauncher.launch(PermissionUtils.getRequiredPermission())
            }
        }

        binding.buttonAnalyze.setOnClickListener {
            viewModel.imageUri.value?.let { uri ->
                viewModel.analyzeImageWithCloudVision(this, uri)
            }
        }

        // Observe LiveData
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.recognizedText.observe(this) { text ->
            // Process detected info if labels are also available
            viewModel.detectedLabels.value?.let { labels ->
                processExtractedData(text, labels)
            }
        }

        viewModel.detectedLabels.observe(this) { labels ->
            val labelText = labels.joinToString("\n") {
                "${it.first} (${String.format("%.1f", it.second * 100)}%)"
            }

            // Process detected info if text is also available
            viewModel.recognizedText.value?.let { text ->
                if (text.isNotEmpty() && text != "No text found in image") {
                    processExtractedData(text, labels)
                }
            }
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
            binding.buttonAnalyze.isEnabled = !isProcessing && viewModel.imageUri.value != null
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun processExtractedData(text: String, labels: List<Pair<String, Float>>) {
        val productData = DataExtractor.extractProductData(text, labels)

        // Initially hide authenticity indicators
        binding.authenticityImage.visibility = View.GONE
        binding.authenticityText.visibility = View.GONE

        // Check for banned substances first
        if (productData.containsBannedSubstances) {
            // Show the counterfeit indicator if banned substances are found
            binding.authenticityImage.visibility = View.VISIBLE
            binding.authenticityImage.setImageResource(R.drawable.ic_thumb_down)
            binding.authenticityText.visibility = View.VISIBLE
            binding.authenticityText.text = getString(R.string.counterfeit_product)
            binding.authenticityText.setTextColor(getColor(R.color.counterfeit_red))

            // Display which banned substances were found
            val details = StringBuilder()
            details.append("This product contains substances not allowed in Italian/EU products:\n")
            productData.bannedSubstancesFound.forEach { substance ->
                details.append("• $substance\n")
            }
            details.append("\n")

            // Add product details
            addProductDetails(details, productData)

            // Set the result text
            binding.textViewResult.text = details.toString() + "\n\nExtracted Text:\n" + text
        } else {
            // If no banned substances, just display the extracted text
            val details = StringBuilder()

            // Add product details if available
            addProductDetails(details, productData)

            // Set the result text
            binding.textViewResult.text = if (details.isNotEmpty()) {
                details.toString() + "\n\nExtracted Text:\n" + text
            } else {
                text
            }
        }
    }

    private fun addProductDetails(details: StringBuilder, productData: ProductDataModel) {
        if (productData.name.isNotBlank()) details.append("Product: ${productData.name}\n")
        if (productData.manufacturer.isNotBlank()) details.append("Manufacturer: ${productData.manufacturer}\n")
        if (productData.productionLocation.isNotBlank()) details.append("Origin: ${productData.productionLocation}\n")
        if (productData.certifications.isNotEmpty()) details.append("Certifications: ${productData.certifications.joinToString(", ")}\n")
        if (productData.productionDate.isNotBlank()) details.append("Production Date: ${productData.productionDate}\n")
        if (productData.serialNumber.isNotBlank()) details.append("Serial Number: ${productData.serialNumber}\n")
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }
}