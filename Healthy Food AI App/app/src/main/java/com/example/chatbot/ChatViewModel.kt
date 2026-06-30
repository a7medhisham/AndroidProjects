package com.example.chatbot

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import java.io.IOException
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ChatDatabase.getDatabase(application).chatDao()
    private val isGuest = FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true

    private val apiKey: String = "UDJvBMOGNsp7QlWZ0nxpQXBJ8trlEyQk"

    private var forceMockData = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val chatHistory = mutableListOf<JSONObject>()

    private val systemPrompt = """
        You are a healthy food nutrition assistant. 
        - You MUST respond in the SAME LANGUAGE that the user uses.
        - If the user writes in Arabic, respond in Arabic.
        - If the user writes in English, respond in English.
        - Always mention calories for every meal.
        - For healthy food: explain benefits, calories, and ingredients.
        - For unhealthy food: say clearly it is unhealthy, explain why, and mention calories.
        - For recipe requests: provide ingredients and preparation steps.
        - If not about food: say you specialize in healthy nutrition only.
        - For images: identify the food, say if healthy or not, mention calories and nutritional specs.
        - Provide COMPLETE and DETAILED responses.
        
        Important: Always match the user's language!
    """.trimIndent()

    val conversations = if (!isGuest) dao.getAllConversations()
    else MutableStateFlow(emptyList<Conversation>()).asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)

    private val _currentConversationId = MutableStateFlow<String?>(null)
    private val _pendingImageUri = MutableStateFlow<Uri?>(null)

    private val _isImageSelected = MutableStateFlow(false)

    init {
        android.util.Log.d("ChatViewModel", "API Key length: ${apiKey.length}")
    }

    fun newConversation() {
        val id = UUID.randomUUID().toString()
        _currentConversationId.value = id
        _messages.value = emptyList()
        chatHistory.clear()
        _isOfflineMode.value = false
        _pendingImageUri.value = null
        _isImageSelected.value = false
        if (!isGuest) {
            viewModelScope.launch {
                dao.insertConversation(Conversation(id = id, title = "New Conversation"))
            }
        }
    }

    fun openConversation(conversationId: String) {
        _currentConversationId.value = conversationId
        chatHistory.clear()
        _isOfflineMode.value = false
        _pendingImageUri.value = null
        _isImageSelected.value = false
        viewModelScope.launch {
            _isLoading.value = true
            dao.getMessages(conversationId).collect { messagesList ->
                _messages.value = messagesList
            }
            _isLoading.value = false
        }
    }

    fun selectImage(uri: Uri) {
        _pendingImageUri.value = uri
        _isImageSelected.value = true
    }

    fun cancelImageSelection() {
        _pendingImageUri.value = null
        _isImageSelected.value = false
    }

    fun sendMessage(text: String) {
        val conversationId = _currentConversationId.value ?: return
        saveMessage(Message(conversationId = conversationId, text = text, isUser = true))

        chatHistory.add(JSONObject().apply {
            put("role", "user")
            put("content", text)
        })

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            try {
                var botReply = ""
                var usedOfflineMode = false

                if (!forceMockData) {
                    try {
                        botReply = callMistralText()
                        _isOfflineMode.value = false
                        android.util.Log.d("ChatViewModel", "Using Mistral API for text")
                    } catch (e: UnknownHostException) {
                        android.util.Log.e("ChatViewModel", "No internet connection", e)
                        botReply = generateMockReply(text)
                        usedOfflineMode = true
                        _isOfflineMode.value = true
                    } catch (e: SocketTimeoutException) {
                        android.util.Log.e("ChatViewModel", "Connection timeout", e)
                        botReply = generateMockReply(text)
                        usedOfflineMode = true
                        _isOfflineMode.value = true
                    } catch (e: SSLHandshakeException) {
                        android.util.Log.e("ChatViewModel", "SSL error", e)
                        botReply = generateMockReply(text)
                        usedOfflineMode = true
                        _isOfflineMode.value = true
                    } catch (e: IOException) {
                        android.util.Log.e("ChatViewModel", "Network error", e)
                        botReply = generateMockReply(text)
                        usedOfflineMode = true
                        _isOfflineMode.value = true
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "API error", e)
                        botReply = generateMockReply(text)
                        usedOfflineMode = true
                        _isOfflineMode.value = true
                    }
                } else {
                    botReply = generateMockReply(text)
                    usedOfflineMode = true
                    _isOfflineMode.value = true
                }

                if (usedOfflineMode) {
                    botReply += "\n\n📱 [Offline Mode - Local Data]"
                }

                saveMessage(Message(conversationId = conversationId, text = botReply, isUser = false))
                chatHistory.add(JSONObject().apply {
                    put("role", "assistant")
                    put("content", botReply)
                })

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Unexpected error", e)
                saveMessage(Message(
                    conversationId = conversationId,
                    text = generateMockReply(text) + "\n\n Unexpected error: ${e.message}",
                    isUser = false
                ))
                _isOfflineMode.value = true
            }
            _isLoading.value = false
        }
    }

    fun sendImageWithCaption(caption: String, contentResolver: android.content.ContentResolver) {
        val conversationId = _currentConversationId.value ?: return
        val imageUri = _pendingImageUri.value ?: return

        val userMessageText = if (caption.isNotBlank()) {
            " [Image] $caption"
        } else {
            "Image"
        }

        saveMessage(Message(
            conversationId = conversationId,
            text = userMessageText,
            imageUrl = imageUri.toString(),
            isUser = true
        ))

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            try {
                android.util.Log.d("ChatViewModel", "Starting image processing...")

                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(
                        android.graphics.ImageDecoder.createSource(contentResolver, imageUri)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }

                val maxSize = 1024
                val (newWidth, newHeight) = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val scale = maxSize.toFloat() / Math.max(bitmap.width, bitmap.height)
                    Pair((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
                } else {
                    Pair(bitmap.width, bitmap.height)
                }

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Image = android.util.Base64.encodeToString(
                    outputStream.toByteArray(), android.util.Base64.NO_WRAP
                )

                android.util.Log.d("ChatViewModel", "Image processed, size: ${base64Image.length} chars")

                val botReply = analyzeImageWithMistral(base64Image, caption)

                _isOfflineMode.value = false
                android.util.Log.d("ChatViewModel", "Image analysis successful")

                saveMessage(Message(conversationId = conversationId, text = botReply, isUser = false))

                _pendingImageUri.value = null
                _isImageSelected.value = false

            } catch (e: UnknownHostException) {
                android.util.Log.e("ChatViewModel", "No internet connection for image", e)
                saveMessage(Message(
                    conversationId = conversationId,
                    text = "No internet connection. Please check your network and try again.",
                    isUser = false
                ))
                _isOfflineMode.value = true
                _pendingImageUri.value = null
                _isImageSelected.value = false

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Image analysis error", e)
                saveMessage(Message(
                    conversationId = conversationId,
                    text = "Failed to analyze image.\n\nError: ${e.message}\n\nPlease try again.",
                    isUser = false
                ))
                _isOfflineMode.value = true
                _pendingImageUri.value = null
                _isImageSelected.value = false
            }
            _isLoading.value = false
        }
    }

    private fun analyzeImageWithMistral(base64Image: String, caption: String = ""): String {
        android.util.Log.d("ChatViewModel", "Attempting to analyze image with Mistral...")

        val userPrompt = if (caption.isNotBlank()) {
            "I'm sending you a food image. Additional context: $caption\n\nPlease analyze this food image and provide:\n1. Food name\n2. Is it healthy or unhealthy? (explain why)\n3. Approximate calories\n4. Protein, fat, carbs\n5. Digestion speed (Fast/Medium/Slow)\n6. One health tip\n\nRespond in the same language I'm using."
        } else {
            "What food is in this image? Please analyze it and provide:\n1. Food name\n2. Is it healthy or unhealthy? (explain why)\n3. Approximate calories\n4. Protein, fat, carbs\n5. Digestion speed (Fast/Medium/Slow)\n6. One health tip\n\nRespond in the same language I'm using."
        }

        try {
            return callPixtralAPI(base64Image, userPrompt)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Pixtral failed: ${e.message}")
            android.util.Log.d("ChatViewModel", "Trying alternative method...")

            return callMistralWithImageDescription(base64Image, userPrompt)
        }
    }

    private fun callPixtralAPI(base64Image: String, userPrompt: String): String {
        val messages = JSONArray()

        val userContent = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", userPrompt)
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                })
            })
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userContent)
        })

        val requestBody = JSONObject().apply {
            put("model", "pixtral-12b-2409")
            put("messages", messages)
            put("max_tokens", 1000)
            put("temperature", 0.3)
        }.toString()

        android.util.Log.d("ChatViewModel", "Sending request to Pixtral API")

        val request = Request.Builder()
            .url("https://api.mistral.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        android.util.Log.d("ChatViewModel", "Pixtral response code: ${response.code}")

        if (!response.isSuccessful) {
            throw Exception("Mistral API error: HTTP ${response.code} - $responseBody")
        }

        val json = JSONObject(responseBody)

        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun callMistralWithImageDescription(base64Image: String, userPrompt: String): String {
        val messages = JSONArray()

        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", "You are a food analysis expert. Analyze the food image described in the base64 data.")
        })

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", "$userPrompt\n\nHere's the image data: $base64Image")
        })

        val requestBody = JSONObject().apply {
            put("model", "mistral-large-latest")
            put("messages", messages)
            put("max_tokens", 1000)
            put("temperature", 0.3)
        }.toString()

        android.util.Log.d("ChatViewModel", "Sending request to Mistral Large with image description")

        val request = Request.Builder()
            .url("https://api.mistral.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Mistral API error: HTTP ${response.code} - $responseBody")
        }

        val json = JSONObject(responseBody)

        val result = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        return "$result\n\n Note: Image was analyzed using text-based model."
    }

    private fun generateMockReply(input: String): String {
        val cleanInput = input.trim().lowercase()

        if (cleanInput.contains("ingredients") || cleanInput.contains("recipe") ||
            cleanInput.contains("مكونات") || cleanInput.contains("وصفة") ||
            cleanInput.contains("طريقة") || cleanInput.contains("how to make")) {
            return getMockRecipe(cleanInput)
        }
        val database = mapOf(
            "كشري" to Triple("Healthy", "Medium", "Calories: 350, Protein: 12g, Fat: 10g, Carbs: 55g"),
            "koshari" to Triple("Healthy", "Medium", "Calories: 350, Protein: 12g, Fat: 10g, Carbs: 55g"),
            "فول" to Triple("Healthy", "Fast", "Calories: 200, Protein: 10g, Fat: 5g, Carbs: 30g"),
            "foul" to Triple("Healthy", "Fast", "Calories: 200, Protein: 10g, Fat: 5g, Carbs: 30g"),
            "fava" to Triple("Healthy", "Fast", "Calories: 200, Protein: 10g, Fat: 5g, Carbs: 30g"),
            "طعمية" to Triple("Healthy", "Fast", "Calories: 150, Protein: 6g, Fat: 7g, Carbs: 15g"),
            "taameya" to Triple("Healthy", "Fast", "Calories: 150, Protein: 6g, Fat: 7g, Carbs: 15g"),
            "falafel" to Triple("Healthy", "Fast", "Calories: 150, Protein: 6g, Fat: 7g, Carbs: 15g"),
            "ملوخية" to Triple("Healthy", "Fast", "Calories: 120, Protein: 5g, Fat: 4g, Carbs: 10g"),
            "molokhia" to Triple("Healthy", "Fast", "Calories: 120, Protein: 5g, Fat: 4g, Carbs: 10g"),
            "كبدة" to Triple("Healthy", "Medium", "Calories: 180, Protein: 26g, Fat: 5g, Carbs: 4g"),
            "kebda" to Triple("Healthy", "Medium", "Calories: 180, Protein: 26g, Fat: 5g, Carbs: 4g"),
            "liver" to Triple("Healthy", "Medium", "Calories: 180, Protein: 26g, Fat: 5g, Carbs: 4g"),
            "فراخ" to Triple("Healthy", "Medium", "Calories: 220, Protein: 30g, Fat: 10g, Carbs: 0g"),
            "chicken" to Triple("Healthy", "Medium", "Calories: 220, Protein: 30g, Fat: 10g, Carbs: 0g"),
            "سمك" to Triple("Healthy", "Fast", "Calories: 180, Protein: 28g, Fat: 6g, Carbs: 0g"),
            "fish" to Triple("Healthy", "Fast", "Calories: 180, Protein: 28g, Fat: 6g, Carbs: 0g"),
            "عدس" to Triple("Healthy", "Fast", "Calories: 230, Protein: 18g, Fat: 1g, Carbs: 40g"),
            "lentils" to Triple("Healthy", "Fast", "Calories: 230, Protein: 18g, Fat: 1g, Carbs: 40g"),
            "بامية" to Triple("Healthy", "Fast", "Calories: 100, Protein: 3g, Fat: 2g, Carbs: 14g"),
            "okra" to Triple("Healthy", "Fast", "Calories: 100, Protein: 3g, Fat: 2g, Carbs: 14g"),
            "ورق عنب" to Triple("Healthy", "Medium", "Calories: 170, Protein: 8g, Fat: 6g, Carbs: 20g"),
            "vine leaves" to Triple("Healthy", "Medium", "Calories: 170, Protein: 8g, Fat: 6g, Carbs: 20g"),
            "حمص" to Triple("Healthy", "Medium", "Calories: 190, Protein: 10g, Fat: 5g, Carbs: 28g"),
            "hummus" to Triple("Healthy", "Medium", "Calories: 190, Protein: 10g, Fat: 5g, Carbs: 28g"),
            "بيتزا" to Triple("Unhealthy", "Slow", "Calories: 400, Protein: 20g, Fat: 25g, Carbs: 35g"),
            "pizza" to Triple("Unhealthy", "Slow", "Calories: 400, Protein: 20g, Fat: 25g, Carbs: 35g"),
            "برجر" to Triple("Unhealthy", "Slow", "Calories: 500, Protein: 25g, Fat: 30g, Carbs: 40g"),
            "burger" to Triple("Unhealthy", "Slow", "Calories: 500, Protein: 25g, Fat: 30g, Carbs: 40g"),
            "شاورما" to Triple("Unhealthy", "Medium", "Calories: 450, Protein: 30g, Fat: 20g, Carbs: 30g"),
            "shawarma" to Triple("Unhealthy", "Medium", "Calories: 450, Protein: 30g, Fat: 20g, Carbs: 30g"),
            "فرايز" to Triple("Unhealthy", "Slow", "Calories: 365, Protein: 4g, Fat: 17g, Carbs: 48g"),
            "fries" to Triple("Unhealthy", "Slow", "Calories: 365, Protein: 4g, Fat: 17g, Carbs: 48g"),
            "هوت دوج" to Triple("Unhealthy", "Slow", "Calories: 350, Protein: 12g, Fat: 20g, Carbs: 30g"),
            "hot dog" to Triple("Unhealthy", "Slow", "Calories: 350, Protein: 12g, Fat: 20g, Carbs: 30g"),
            "دونتس" to Triple("Unhealthy", "Slow", "Calories: 450, Protein: 5g, Fat: 25g, Carbs: 55g"),
            "donut" to Triple("Unhealthy", "Slow", "Calories: 450, Protein: 5g, Fat: 25g, Carbs: 55g"),
            "كيك" to Triple("Unhealthy", "Slow", "Calories: 380, Protein: 4g, Fat: 18g, Carbs: 50g"),
            "cake" to Triple("Unhealthy", "Slow", "Calories: 380, Protein: 4g, Fat: 18g, Carbs: 50g"),
            "شيبس" to Triple("Unhealthy", "Slow", "Calories: 540, Protein: 7g, Fat: 35g, Carbs: 53g"),
            "chips" to Triple("Unhealthy", "Slow", "Calories: 540, Protein: 7g, Fat: 35g, Carbs: 53g"),
            "كولا" to Triple("Unhealthy", "Fast", "Calories: 140, Protein: 0g, Fat: 0g, Carbs: 37g"),
            "cola" to Triple("Unhealthy", "Fast", "Calories: 140, Protein: 0g, Fat: 0g, Carbs: 37g"),
            "pepsi" to Triple("Unhealthy", "Fast", "Calories: 150, Protein: 0g, Fat: 0g, Carbs: 41g"),
            "سجق" to Triple("Unhealthy", "Slow", "Calories: 300, Protein: 12g, Fat: 26g, Carbs: 2g"),
            "sausage" to Triple("Unhealthy", "Slow", "Calories: 300, Protein: 12g, Fat: 26g, Carbs: 2g"),
            "نجرسكو" to Triple("Unhealthy", "Slow", "Calories: 520, Protein: 10g, Fat: 28g, Carbs: 60g"),
            "nutella" to Triple("Unhealthy", "Slow", "Calories: 539, Protein: 6g, Fat: 30g, Carbs: 58g"),
            "تفاحة" to Triple("Healthy", "Fast", "Calories: 95, Protein: 0.5g, Fat: 0.3g, Carbs: 25g"),
            "apple" to Triple("Healthy", "Fast", "Calories: 95, Protein: 0.5g, Fat: 0.3g, Carbs: 25g"),
            "موز" to Triple("Healthy", "Fast", "Calories: 105, Protein: 1.3g, Fat: 0.4g, Carbs: 27g"),
            "banana" to Triple("Healthy", "Fast", "Calories: 105, Protein: 1.3g, Fat: 0.4g, Carbs: 27g"),
            "سلطة" to Triple("Healthy", "Fast", "Calories: 80, Protein: 3g, Fat: 4g, Carbs: 8g"),
            "salad" to Triple("Healthy", "Fast", "Calories: 80, Protein: 3g, Fat: 4g, Carbs: 8g"),
            "جزر" to Triple("Healthy", "Fast", "Calories: 52, Protein: 1.2g, Fat: 0.3g, Carbs: 12g"),
            "carrot" to Triple("Healthy", "Fast", "Calories: 52, Protein: 1.2g, Fat: 0.3g, Carbs: 12g"),
            "برتقال" to Triple("Healthy", "Fast", "Calories: 62, Protein: 1.2g, Fat: 0.2g, Carbs: 15g"),
            "orange" to Triple("Healthy", "Fast", "Calories: 62, Protein: 1.2g, Fat: 0.2g, Carbs: 15g"),
            "عنب" to Triple("Healthy", "Fast", "Calories: 69, Protein: 0.7g, Fat: 0.2g, Carbs: 18g"),
            "grapes" to Triple("Healthy", "Fast", "Calories: 69, Protein: 0.7g, Fat: 0.2g, Carbs: 18g"),
            "مانجو" to Triple("Healthy", "Fast", "Calories: 99, Protein: 1.4g, Fat: 0.6g, Carbs: 25g"),
            "mango" to Triple("Healthy", "Fast", "Calories: 99, Protein: 1.4g, Fat: 0.6g, Carbs: 25g"),
            "خيار" to Triple("Healthy", "Fast", "Calories: 16, Protein: 0.7g, Fat: 0.1g, Carbs: 4g"),
            "cucumber" to Triple("Healthy", "Fast", "Calories: 16, Protein: 0.7g, Fat: 0.1g, Carbs: 4g"),
            "طماطم" to Triple("Healthy", "Fast", "Calories: 18, Protein: 0.9g, Fat: 0.2g, Carbs: 4g"),
            "tomato" to Triple("Healthy", "Fast", "Calories: 18, Protein: 0.9g, Fat: 0.2g, Carbs: 4g"),
            "بيض" to Triple("Healthy", "Medium", "Calories: 155, Protein: 13g, Fat: 11g, Carbs: 1g"),
            "eggs" to Triple("Healthy", "Medium", "Calories: 155, Protein: 13g, Fat: 11g, Carbs: 1g"),
            "تونة" to Triple("Healthy", "Fast", "Calories: 130, Protein: 28g, Fat: 1g, Carbs: 0g"),
            "tuna" to Triple("Healthy", "Fast", "Calories: 130, Protein: 28g, Fat: 1g, Carbs: 0g"),
            "لبن" to Triple("Healthy", "Fast", "Calories: 60, Protein: 3g, Fat: 3g, Carbs: 5g"),
            "yogurt" to Triple("Healthy", "Fast", "Calories: 60, Protein: 3g, Fat: 3g, Carbs: 5g"),
            "جبنة" to Triple("Healthy", "Medium", "Calories: 110, Protein: 7g, Fat: 9g, Carbs: 1g"),
            "cheese" to Triple("Healthy", "Medium", "Calories: 110, Protein: 7g, Fat: 9g, Carbs: 1g"),
            "لحمة" to Triple("Healthy", "Medium", "Calories: 250, Protein: 26g, Fat: 15g, Carbs: 0g"),
            "beef" to Triple("Healthy", "Medium", "Calories: 250, Protein: 26g, Fat: 15g, Carbs: 0g"),
        )

        val match = database.entries.find { cleanInput.contains(it.key) }

        return if (match != null) {
            val info = match.value
            val emoji = if (info.first == "Healthy") "Healthy " else "Unhealthy "
            "Healthy? $emoji\nDigestion Speed: ${info.second}\nSpecs: ${info.third}"
        } else {
            "Sorry, I don't have data about this food yet.\nTry: Koshari, Foul, Pizza, Burger, Chicken, Salad, Eggs, Tuna!"
        }
    }

    private fun getMockRecipe(input: String): String {
        return when {
            input.contains("كشري") || input.contains("koshari") ->
                "Koshari Recipe:\nIngredients: Rice, Lentils, Macaroni, Tomato sauce, Onion, Spices.\nPreparation: Cook each separately, mix with sauce and top with fried onions."
            input.contains("فول") || input.contains("foul") || input.contains("fava") ->
                "Foul Recipe:\nIngredients: Fava beans, Garlic, Lemon, Olive oil.\nPreparation: Cook beans, mash slightly, add garlic, lemon juice, and olive oil."
            input.contains("بيتزا") || input.contains("pizza") ->
                "Pizza Recipe:\nIngredients: Dough, Tomato sauce, Cheese, Toppings.\nPreparation: Spread sauce on dough, add toppings, bake at 220 C for 15 min."
            input.contains("طعمية") || input.contains("taameya") || input.contains("falafel") ->
                "Taameya Recipe:\nIngredients: Fava beans, Herbs, Onion, Spices.\nPreparation: Mash beans, mix with herbs, shape into patties, fry."
            input.contains("برجر") || input.contains("burger") ->
                "Burger Recipe:\nIngredients: Bun, Beef patty, Lettuce, Tomato, Cheese, Sauce.\nPreparation: Grill beef, assemble with bun and toppings."
            input.contains("شاورما") || input.contains("shawarma") ->
                "Shawarma Recipe:\nIngredients: Chicken/Beef, Spices, Pita bread, Vegetables.\nPreparation: Marinate meat, grill, serve in pita with veggies."
            input.contains("ملوخية") || input.contains("molokhia") ->
                "Molokhia Recipe:\nIngredients: Molokhia leaves, Chicken broth, Garlic, Coriander.\nPreparation: Cook molokhia in broth, add fried garlic and coriander."
            input.contains("بيض") || input.contains("eggs") ->
                "Eggs Recipe:\nIngredients: Eggs, Salt, Pepper, Butter.\nPreparation: Scramble or fry in butter, season to taste."
            input.contains("فراخ") || input.contains("chicken") ->
                "Grilled Chicken Recipe:\nIngredients: Chicken, Lemon, Garlic, Spices, Olive oil.\nPreparation: Marinate chicken, grill on medium heat for 20-25 min."
            input.contains("سمك") || input.contains("fish") ->
                "Grilled Fish Recipe:\nIngredients: Fish, Lemon, Garlic, Cumin, Salt.\nPreparation: Season fish, grill for 10-15 min each side."
            else -> "Recipe not available for this food yet."
        }
    }

    private fun callMistralText(): String {
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        val recentHistory = chatHistory.takeLast(10)
        recentHistory.forEach { messages.put(it) }

        val requestBody = JSONObject().apply {
            put("model", "mistral-small-latest")
            put("messages", messages)
            put("max_tokens", 2000)
            put("temperature", 0.7)
        }.toString()

        val request = Request.Builder()
            .url("https://api.mistral.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun saveMessage(message: Message) {
        if (!isGuest) {
            viewModelScope.launch { dao.insertMessage(message) }
        } else {
            _messages.value = _messages.value + message
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            dao.deleteMessages(conversation.id)
            dao.deleteConversation(conversation)
        }
    }
}