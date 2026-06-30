package com.example.chatbot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: com.example.chatbot.databinding.ActivityHomeBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var conversationsAdapter: ConversationsAdapter
    private lateinit var imageUri: Uri
    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showImageCaptionDialog()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = imageUri
            showImageCaptionDialog()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.example.chatbot.databinding.ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupClickListeners()
        observeData()

        viewModel.newConversation()
    }

    private fun setupRecyclerViews() {
        messagesAdapter = MessagesAdapter()
        binding.rvMessages.apply {
            adapter = messagesAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity).apply {
                stackFromEnd = true
            }
        }

        conversationsAdapter = ConversationsAdapter(
            onConversationClick = { conversation ->
                viewModel.openConversation(conversation.id)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            },
            onDeleteClick = { conversation ->
                viewModel.deleteConversation(conversation)
            }
        )
        binding.rvConversations.apply {
            adapter = conversationsAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }

    private fun setupClickListeners() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnNewChat.setOnClickListener {
            viewModel.newConversation()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        binding.btnSendImageWithCaption.setOnClickListener {
            val caption = binding.editImageCaption.text.toString().trim()
            selectedImageUri?.let { uri ->
                viewModel.selectImage(uri)
                viewModel.sendImageWithCaption(caption, contentResolver)
                hideImageCaptionDialog()
            }
        }

        binding.btnCancelImageCaption.setOnClickListener {
            hideImageCaptionDialog()
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("photo", ".jpg", cacheDir)
        imageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(imageUri)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                messagesAdapter.submitList(messages)
                if (messages.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvMessages.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvMessages.visibility = View.VISIBLE
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.conversations.collect { conversations ->
                conversationsAdapter.submitList(conversations)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnSend.isEnabled = !isLoading
            }
        }
    }

    private fun showImageCaptionDialog() {
        binding.cardImageCaption.visibility = View.VISIBLE
        binding.etMessage.isEnabled = false
        binding.btnSend.isEnabled = false
        binding.btnGallery.isEnabled = false
        binding.btnCamera.isEnabled = false
        binding.editImageCaption.text?.clear()
        binding.editImageCaption.requestFocus()
    }

    private fun hideImageCaptionDialog() {
        binding.cardImageCaption.visibility = View.GONE
        binding.etMessage.isEnabled = true
        binding.btnSend.isEnabled = true
        binding.btnGallery.isEnabled = true
        binding.btnCamera.isEnabled = true
        selectedImageUri = null
    }
}