package com.example.projectstories

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.clearSpans
import androidx.lifecycle.lifecycleScope
import com.example.projectstories.databinding.ActivityHomeBinding
import kotlinx.coroutines.*
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var tts: TextToSpeech
    private var story: Stories? = null
    private val repository = StoryRepository()

    private var isPlaying = false
    private var currentText = ""
    private var sentences: List<String> = listOf()
    private var currentSentenceIndex = 0
    private var sentenceWords: List<String> = listOf()

    private var highlightJob: Job? = null
    private var sliderJob: Job? = null
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading(true)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.setSpeechRate(0.9f)
                tts.setPitch(1.1f)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread {
                            binding.btnPlay.setImageResource(R.drawable.ic_pause)
                            binding.btnPauseResume.isEnabled = true
                            binding.btnRestart.isEnabled = true
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            if (utteranceId != null && utteranceId.startsWith("SENTENCE_")) {
                                val index = utteranceId.removePrefix("SENTENCE_").toIntOrNull() ?: 0
                                clearSentenceHighlight()

                                if (index < sentences.size - 1) {
                                    currentSentenceIndex = index + 1
                                    speakCurrentSentence()
                                } else {
                                    onStoryFinished()
                                }
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            Toast.makeText(this@HomeActivity,
                                getString(R.string.speech_error_occurred), Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        }

        val storyId = intent.getStringExtra("story_id") ?: "story_1"
        loadStory(storyId)

        binding.btnPlay.setOnClickListener {
            if (isPlaying) {
                stopSpeech()
            } else {
                val lang = LanguageManager.getLanguage(this)
                startSpeech(lang)
            }
        }

        binding.btnPauseResume.setOnClickListener {
            if (tts.isSpeaking) {
                pauseSpeech()
            } else {
                resumeSpeech()
            }
        }

        binding.btnRestart.setOnClickListener {
            restartSpeech()
        }
    }

    private fun loadStory(storyId: String) {
        lifecycleScope.launch {
            story = repository.getStoryById(storyId)
            if (story == null) showError(getString(R.string.story_not_found))
            else {
                setupUI(story!!)
                showLoading(false)
            }
        }
    }

    private fun setupUI(story: Stories) {
        val lang = LanguageManager.getLanguage(this)
        val title = story.title[lang] ?: ""
        val text = story.text[lang] ?: ""

        binding.tvStoryTitle.text = title
        binding.tvStoryText.text = text
        currentText = text

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedSize = prefs.getInt("size", 22)
        binding.tvStoryText.textSize = savedSize.toFloat()
        binding.tvStoryTitle.textSize = savedSize.toFloat() * 1.3f

        sentences = text.split("(?<=[.!?])\\s+".toRegex()).filter { it.isNotBlank() }
        currentSentenceIndex = 0

        binding.viewPagerImages.adapter = StoryImagesAdapter(story.id)
        binding.viewPagerImages.visibility = View.VISIBLE
        startAutoSlide()
        binding.btnPlay.setImageResource(R.drawable.ic_play)
        binding.btnPauseResume.isEnabled = false
        binding.btnRestart.isEnabled = false
    }

    private fun startSpeech(language: String) {
        if (currentSentenceIndex >= sentences.size) {
            currentSentenceIndex = 0
        }

        isPlaying = true

        val locale = when (language) {
            "ar" -> Locale("ar")
            "fr" -> Locale.FRANCE
            "de" -> Locale.GERMANY
            else -> Locale.US
        }

        if (tts.setLanguage(locale) < 0) {
            Toast.makeText(this, getString(R.string.language_not_supported), Toast.LENGTH_LONG).show()
            return
        }

        tts.stop()
        speakCurrentSentence()
    }

    private fun speakCurrentSentence() {
        if (currentSentenceIndex >= sentences.size) {
            stopSpeech()
            return
        }

        val sentence = sentences[currentSentenceIndex]
        highlightSentence()
        tts.speak(sentence, TextToSpeech.QUEUE_ADD, null, "SENTENCE_${currentSentenceIndex}")
    }

    private fun highlightSentence() {
        val sentence = sentences[currentSentenceIndex]
        val fullText = binding.tvStoryText.text.toString()
        val sentenceStart = fullText.indexOf(sentence)

        if (sentenceStart < 0) return

        val sentenceEnd = sentenceStart + sentence.length

        runOnUiThread {
            val spannable = SpannableString(fullText)
            spannable.clearSpans()
            spannable.setSpan(
                BackgroundColorSpan(ContextCompat.getColor(this, R.color.highlight_color)),
                sentenceStart,
                sentenceEnd,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvStoryText.text = spannable

            binding.scrollView.post {
                try {
                    val lineTop = binding.tvStoryText.layout.getLineForOffset(sentenceStart)
                    val y = binding.tvStoryText.layout.getLineTop(lineTop)
                    binding.scrollView.smoothScrollTo(0, y - 100)
                } catch (e: Exception) {}
            }
        }
    }

    private fun clearSentenceHighlight() {
        runOnUiThread {
            binding.tvStoryText.text = currentText
        }
    }

    private fun pauseSpeech() {
        if (tts.isSpeaking) {
            tts.stop()
            highlightJob?.cancel()
            binding.btnPauseResume.setImageResource(R.drawable.ic_play)
        }
    }

    private fun resumeSpeech() {
        if (isPlaying && !tts.isSpeaking) {
            speakCurrentSentence()
            binding.btnPauseResume.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun restartSpeech() {
        tts.stop()
        highlightJob?.cancel()
        currentSentenceIndex = 0
        binding.tvStoryText.text = currentText
        val lang = LanguageManager.getLanguage(this)
        startSpeech(lang)
    }

    private fun onStoryFinished() {
        runOnUiThread {
            clearSentenceHighlight()
            binding.viewPagerImages.setCurrentItem(0, true)
            currentSentenceIndex = 0
            isPlaying = false
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPauseResume.isEnabled = false
            binding.btnRestart.isEnabled = false
            startAutoSlide()
        }
    }

    private fun stopSpeech() {
        isPlaying = false
        tts.stop()
        highlightJob?.cancel()

        binding.tvStoryText.text = currentText
        binding.btnPlay.setImageResource(R.drawable.ic_play)
        binding.btnPauseResume.isEnabled = false
        binding.btnRestart.isEnabled = false
        startAutoSlide()
    }

    private fun startAutoSlide() {
        sliderJob?.cancel()
        val count = binding.viewPagerImages.adapter?.itemCount ?: return

        sliderJob = lifecycleScope.launch {
            while (true) {
                delay(4000)
                currentPage = (currentPage + 1) % count
                withContext(Dispatchers.Main) {
                    binding.viewPagerImages.setCurrentItem(currentPage, true)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedSize = prefs.getInt("size", 22)
        binding.tvStoryText.textSize = savedSize.toFloat()
        binding.tvStoryTitle.textSize = savedSize.toFloat() * 1.3f

        startAutoSlide()
    }

    override fun onPause() {
        super.onPause()
        if (tts.isSpeaking) {
            tts.stop()
        }
        highlightJob?.cancel()
        sliderJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        highlightJob?.cancel()
        sliderJob?.cancel()
        tts.stop()
        tts.shutdown()
    }
}