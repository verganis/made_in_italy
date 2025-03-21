package com.example.madeinitaly

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
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

                // Clear previous results
                binding.authenticityImage.visibility = View.GONE
                binding.authenticityText.visibility = View.GONE
                binding.textViewResult.text = ""

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
        // Still need to extract product data to check for banned substances
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

            // Add alternative shopping suggestion with clickable link
            details.append("FIND THE AUTHENTIC PRODUCT AT BIG C SUPERMARKET (500m)\n\n")

            // Set the result text - just show banned substances info and extracted text
            binding.textViewResult.text = details.toString() + "\nExtracted Text:\n" + text

            // Make the supermarket text clickable
            val spannable = SpannableString(binding.textViewResult.text)
            val startPos = details.toString().indexOf("FIND THE AUTHENTIC")
            val endPos = details.toString().indexOf("(500m)") + "(500m)".length

            if (startPos != -1 && endPos != -1) {
                spannable.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val mapIntent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://maps.app.goo.gl/A9nVKNyMdcCtYrnK9"))
                            startActivity(mapIntent)
                        }
                    },
                    startPos,
                    endPos,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.textViewResult.text = spannable
                binding.textViewResult.movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            // If no banned substances, just display the extracted text without product details
            binding.textViewResult.text = text
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }
}