package com.example.madeinitaly.cloud

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.madeinitaly.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudVisionService {
    private val client: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun detectText(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IOException("Cannot read image"))

            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val jsonRequest = createTextDetectionRequest(base64Image)
            val response = executeRequest(jsonRequest)

            Result.success(parseTextDetectionResponse(response))
        } catch (e: Exception) {
            Utils.logError("Text detection failed", e)
            Result.failure(e)
        }
    }

    suspend fun detectLabels(context: Context, imageUri: Uri): Result<List<Pair<String, Float>>> = withContext(Dispatchers.IO) {
        try {
            val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IOException("Cannot read image"))

            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val jsonRequest = createLabelDetectionRequest(base64Image)
            val response = executeRequest(jsonRequest)

            Result.success(parseLabelDetectionResponse(response))
        } catch (e: Exception) {
            Utils.logError("Label detection failed", e)
            Result.failure(e)
        }
    }

    private fun createTextDetectionRequest(base64Image: String): String {
        val jsonRequest = JSONObject()
        val requestArray = JSONArray()
        val request = JSONObject()
        val image = JSONObject()
        val features = JSONArray()
        val feature = JSONObject()

        image.put("content", base64Image)
        feature.put("type", "TEXT_DETECTION")
        feature.put("maxResults", CloudVisionConfig.MAX_RESULTS)
        features.put(feature)

        request.put("image", image)
        request.put("features", features)
        requestArray.put(request)
        jsonRequest.put("requests", requestArray)

        return jsonRequest.toString()
    }

    private fun createLabelDetectionRequest(base64Image: String): String {
        val jsonRequest = JSONObject()
        val requestArray = JSONArray()
        val request = JSONObject()
        val image = JSONObject()
        val features = JSONArray()
        val feature = JSONObject()

        image.put("content", base64Image)
        feature.put("type", "LABEL_DETECTION")
        feature.put("maxResults", CloudVisionConfig.MAX_RESULTS)
        features.put(feature)

        request.put("image", image)
        request.put("features", features)
        requestArray.put(request)
        jsonRequest.put("requests", requestArray)

        return jsonRequest.toString()
    }

    private suspend fun executeRequest(jsonRequest: String): String = withContext(Dispatchers.IO) {
        val url = "${CloudVisionConfig.VISION_API_URL}?key=${CloudVisionConfig.API_KEY}"
        val body = jsonRequest.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Request failed: ${response.code} - ${response.message}")
        }

        response.body?.string() ?: throw IOException("Empty response")
    }

    private fun parseTextDetectionResponse(response: String): String {
        val jsonResponse = JSONObject(response)
        val responseArray = jsonResponse.getJSONArray("responses")
        if (responseArray.length() == 0) return ""

        val firstResponse = responseArray.getJSONObject(0)
        if (!firstResponse.has("textAnnotations")) return ""

        val textAnnotations = firstResponse.getJSONArray("textAnnotations")
        if (textAnnotations.length() == 0) return ""

        return textAnnotations.getJSONObject(0).getString("description")
    }

    private fun parseLabelDetectionResponse(response: String): List<Pair<String, Float>> {
        val results = mutableListOf<Pair<String, Float>>()
        val jsonResponse = JSONObject(response)
        val responseArray = jsonResponse.getJSONArray("responses")
        if (responseArray.length() == 0) return results

        val firstResponse = responseArray.getJSONObject(0)
        if (!firstResponse.has("labelAnnotations")) return results

        val labelAnnotations = firstResponse.getJSONArray("labelAnnotations")
        for (i in 0 until labelAnnotations.length()) {
            val annotation = labelAnnotations.getJSONObject(i)
            val description = annotation.getString("description")
            val score = annotation.getDouble("score").toFloat()
            results.add(Pair(description, score))
        }

        return results
    }
}