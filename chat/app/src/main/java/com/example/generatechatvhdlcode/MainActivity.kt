package com.example.generatechatvhdlcode

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.generatechatvhdlcode.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    private var hasEnable = false
    private var hasReset = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showAnimatedToast("Welcome user 🎉", R.raw.cute_bot_say_user_hello, 7000L)

        setupRecyclerView()
        setupChips()
        setupInput()
        setupFormatSpinner()
        setupEnableResetButtons()

        addBotMessage("Hi! Choose a component from the chips above, or type your request.\n\n" +
                "Options:\n" +
                "• Format: function or procedure\n" +
                "• Enable: Adds enable pin to entity\n" +
                "• Reset: Adds reset pin to entity\n\n" +
                "Supported components:\n" +
                "MUX, DeMUX, Decoder, Encoder, Comparator,\n" +
                "SIPO, PISO, PIPO, SRAM")
    }

    private fun setupFormatSpinner() {
        val formats = arrayOf("function", "procedure", "standard")
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            formats
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.parseColor("#A78BFA"))
                view.textSize = 12f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.parseColor("#A78BFA"))
                view.setBackgroundColor(Color.parseColor("#141926"))
                view.setPadding(24, 16, 24, 16)
                view.textSize = 13f
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.formatSpinner.adapter = adapter
    }

    private fun addUserMessage(text: String) {
        messages.add(Message(text, Role.USER))
        adapter.notifyItemInserted(messages.lastIndex)
        binding.recyclerView.scrollToPosition(messages.lastIndex)
    }

    private fun addBotMessage(text: String) {
        messages.add(Message(text, Role.BOT))
        adapter.notifyItemInserted(messages.lastIndex)
        binding.recyclerView.scrollToPosition(messages.lastIndex)
    }

    private fun setupEnableResetButtons() {
        binding.btnEnable.setOnClickListener {
            hasEnable = !hasEnable
            (it as MaterialButton).isChecked = hasEnable
            addBotMessage(if (hasEnable) "Enable pin added to entity" else " Enable pin removed")
        }

        binding.btnReset.setOnClickListener {
            hasReset = !hasReset
            (it as MaterialButton).isChecked = hasReset
            addBotMessage(if (hasReset) "Reset pin added to entity" else " Reset pin removed")
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
    }

    private fun showSheet(component: String) {
        val format = binding.formatSpinner.selectedItem.toString()
        val entity = binding.entityInput.text.toString().ifBlank { "Entity_1" }
        val arch = binding.archInput.text.toString().ifBlank { "Arch_1" }

        addUserMessage("Generate $component (Format: $format, Enable: $hasEnable, Reset: $hasReset)")

        ComponentBottomSheet(
            component = component,
            format = format,
            entity = entity,
            arch = arch,
            hasEnable = hasEnable,
            hasReset = hasReset,
            onGenerate = { code -> addBotMessage(code) }
        ).show(supportFragmentManager, "sheet_$component")
    }

    private fun setupChips() {
        binding.chipMux.setOnClickListener { showSheet("MUX") }
        binding.chipDemux.setOnClickListener { showSheet("DeMUX") }
        binding.chipDecoder.setOnClickListener { showSheet("Decoder") }
        binding.chipEncoder.setOnClickListener { showSheet("Encoder") }
        binding.chipComparator.setOnClickListener { showSheet("Comparator") }
        binding.chipSipo.setOnClickListener { showSheet("SIPO") }
        binding.chipPiso.setOnClickListener { showSheet("PISO") }
        binding.chipPipo.setOnClickListener { showSheet("PIPO") }
        binding.chipRam.setOnClickListener { showSheet("SRAM") }
    }

    private fun handleUserInput(text: String) {
        addUserMessage(text)
        val format = binding.formatSpinner.selectedItem.toString()
        val entity = binding.entityInput.text.toString().ifBlank { "Entity_1" }
        val arch = binding.archInput.text.toString().ifBlank { "Arch_1" }

        val code = VHDLGenerator.generate(
            query = text,
            format = format,
            entity = entity,
            arch = arch,
            hasEnable = hasEnable,
            hasReset = hasReset
        )
        addBotMessage(code)
    }

    private fun sendMessage() {
        val text = binding.chatInput.text.toString().trim()
        if (text.isEmpty()) return
        binding.chatInput.text?.clear()
        handleUserInput(text)
    }

    private fun setupInput() {
        binding.sendFab.setOnClickListener { sendMessage() }
        binding.chatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun showAnimatedToast(message: String, lottieFile: Int, durationMs: Long = 3000L) {
        binding.toastLayout.visibility = View.VISIBLE
        binding.lottieToast.setAnimation(lottieFile)
        binding.lottieToast.playAnimation()
        binding.toastMessage.text = message

        Handler(Looper.getMainLooper()).postDelayed({
            binding.toastLayout.visibility = View.GONE
        }, durationMs)
    }
}