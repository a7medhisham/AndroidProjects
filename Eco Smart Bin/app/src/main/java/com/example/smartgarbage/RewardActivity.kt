package com.example.smartgarbage

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.smartgarbage.databinding.ActivityRewardBinding
import com.google.firebase.auth.FirebaseAuth
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RewardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardBinding
    private lateinit var auth: FirebaseAuth
    private var currentPoints = 0
    private var selectedPayment = ""
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicPlaying = false

    companion object {
        private const val POINTS_TO_EGP_RATE = 0.25
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        loadUserData()
        loadUserPointsFromPrefs()

        if (currentPoints <= 0) {
            Toast.makeText(this, getString(R.string.no_points_to_redeem), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupPaymentOptions()
        startMusic()

        binding.btnBack.setOnClickListener {
            stopMusic()
            finish()
        }

        binding.btnStopMusic.setOnClickListener {
            stopMusic()
        }

        binding.btnPlayMusic.setOnClickListener {
            startMusic()
        }
    }

    private fun startMusic() {
        if (!isMusicPlaying) {
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.sound1)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                isMusicPlaying = true
                binding.btnPlayMusic.setImageResource(R.drawable.ic_pause)
                binding.btnPlayMusic.visibility = View.VISIBLE
                binding.btnStopMusic.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isMusicPlaying = false
            binding.btnPlayMusic.setImageResource(R.drawable.ic_play)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        val email = user?.email ?: ""
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val name = sharedPref.getString("user_name", getString(R.string.unknown_user)) ?: getString(R.string.unknown_user)

        binding.tvUserName.text = name
        binding.tvUserEmail.text = email

        val profilePrefsName = "UserPrefs_${email.hashCode()}"
        val profilePref = getSharedPreferences(profilePrefsName, Context.MODE_PRIVATE)
        val savedUri = profilePref.getString("profileImageUri", null)

        if (savedUri != null) {
            Glide.with(this)
                .load(Uri.parse(savedUri))
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(binding.ivUserImage)
        } else {
            binding.ivUserImage.setImageResource(R.drawable.ic_personal)
        }
    }

    private fun loadUserPointsFromPrefs() {
        val email = auth.currentUser?.email ?: ""
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        currentPoints = sharedPref.getInt("user_points", 0)
        updatePointsUI()
    }

    private fun updatePointsUI() {
        binding.tvTotalPoints.text = currentPoints.toString()
        binding.tvPointsHint.text = getString(R.string.you_have_points, currentPoints)
    }

    private fun setupPaymentOptions() {
        binding.cardWePay.setOnClickListener {
            selectedPayment = "we_pay"
            highlightSelected(binding.cardWePay, binding.tvWePay)
            showWithdrawDialog(getString(R.string.we_pay))
        }

        binding.cardVodafone.setOnClickListener {
            selectedPayment = "vodafone"
            highlightSelected(binding.cardVodafone, binding.tvVodafone)
            showWithdrawDialog(getString(R.string.vodafone_cash))
        }

        binding.cardOrange.setOnClickListener {
            selectedPayment = "orange"
            highlightSelected(binding.cardOrange, binding.tvOrange)
            showWithdrawDialog(getString(R.string.orange_pay))
        }

        binding.cardInsta.setOnClickListener {
            selectedPayment = "insta"
            highlightSelected(binding.cardInsta, binding.tvInsta)
            showInstaPayDialog()
        }
    }

    private fun highlightSelected(card: CardView, textView: TextView) {
        val cards = listOf(binding.cardWePay, binding.cardVodafone, binding.cardOrange, binding.cardInsta)
        val texts = listOf(binding.tvWePay, binding.tvVodafone, binding.tvOrange, binding.tvInsta)

        cards.forEach { it.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white)) }
        texts.forEach { it.setTextColor(ContextCompat.getColor(this, R.color.black)) }

        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
        textView.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun showWithdrawDialog(paymentMethod: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_withdraw)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etPhone = dialog.findViewById<EditText>(R.id.etPhoneNumber)
        val etPoints = dialog.findViewById<EditText>(R.id.etPoints)
        val tvPointsError = dialog.findViewById<TextView>(R.id.tvPointsError)
        val tvPhoneError = dialog.findViewById<TextView>(R.id.tvPhoneError)
        val tvAmountHint = dialog.findViewById<TextView>(R.id.tvAmountHint)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        tvTitle.text = getString(R.string.withdraw_via, paymentMethod)

        val phonePrefix = when (selectedPayment) {
            "we_pay" -> "015"
            "vodafone" -> "010"
            "orange" -> "012"
            else -> ""
        }

        etPhone.hint = "$paymentMethod ($phonePrefix) ${getString(R.string.number)}"

        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {
                val phone = s.toString()
                val pattern = Regex("^$phonePrefix\\d{8}$")
                val isValid = when {
                    phone.isEmpty() -> {
                        tvPhoneError.visibility = View.GONE
                        false
                    }
                    phone.length != 11 -> {
                        tvPhoneError.visibility = View.VISIBLE
                        tvPhoneError.text = getString(R.string.phone_number_11_digits)
                        false
                    }
                    !phone.matches(pattern) -> {
                        tvPhoneError.visibility = View.VISIBLE
                        tvPhoneError.text = getString(R.string.invalid_phone_number_prefix, phonePrefix)
                        false
                    }
                    else -> {
                        tvPhoneError.visibility = View.GONE
                        true
                    }
                }
                val pointsValid = etPoints.text.toString().toIntOrNull()?.let { it <= currentPoints && it > 0 } == true
                btnConfirm.isEnabled = isValid && pointsValid
                btnConfirm.alpha = if (btnConfirm.isEnabled) 1f else 0.5f
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPoints.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {
                val points = s.toString().toIntOrNull() ?: 0
                val amount = points * POINTS_TO_EGP_RATE
                tvAmountHint.text = getString(R.string.you_will_receive, amount)

                when {
                    points > currentPoints -> {
                        tvPointsError.visibility = View.VISIBLE
                        tvPointsError.text = getString(R.string.not_enough_points, currentPoints)
                        btnConfirm.isEnabled = false
                        btnConfirm.alpha = 0.5f
                    }
                    points <= 0 -> {
                        tvPointsError.visibility = View.VISIBLE
                        tvPointsError.text = getString(R.string.enter_valid_points)
                        btnConfirm.isEnabled = false
                        btnConfirm.alpha = 0.5f
                    }
                    else -> {
                        tvPointsError.visibility = View.GONE
                        val phoneValid = etPhone.text.toString().matches(Regex("^$phonePrefix\\d{8}$"))
                        btnConfirm.isEnabled = phoneValid
                        btnConfirm.alpha = if (phoneValid) 1f else 0.5f
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            val phone = etPhone.text.toString()
            val points = etPoints.text.toString().toIntOrNull() ?: 0
            val pattern = Regex("^$phonePrefix\\d{8}$")

            if (phone.matches(pattern) && points <= currentPoints && points > 0) {
                showConfirmationDialog(paymentMethod, phone, points)
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.enter_valid_phone_prefix, phonePrefix), Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showInstaPayDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_instapay)

        val etAccount = dialog.findViewById<EditText>(R.id.etAccountNumber)
        val etPoints = dialog.findViewById<EditText>(R.id.etPoints)
        val tvPointsError = dialog.findViewById<TextView>(R.id.tvPointsError)
        val tvAccountError = dialog.findViewById<TextView>(R.id.tvAccountError)
        val tvAmountHint = dialog.findViewById<TextView>(R.id.tvAmountHint)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        etAccount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {
                val account = s.toString()
                val isValid = when {
                    account.isEmpty() -> {
                        tvAccountError.visibility = View.GONE
                        false
                    }
                    account.length < 8 || account.length > 12 -> {
                        tvAccountError.visibility = View.VISIBLE
                        tvAccountError.text = getString(R.string.account_number_8_12)
                        false
                    }
                    !account.matches(Regex("^\\d{8,12}$")) -> {
                        tvAccountError.visibility = View.VISIBLE
                        tvAccountError.text = getString(R.string.invalid_account_number)
                        false
                    }
                    else -> {
                        tvAccountError.visibility = View.GONE
                        true
                    }
                }
                val pointsValid = etPoints.text.toString().toIntOrNull()?.let { it <= currentPoints && it > 0 } == true
                btnConfirm.isEnabled = isValid && pointsValid
                btnConfirm.alpha = if (btnConfirm.isEnabled) 1f else 0.5f
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPoints.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {
                val points = s.toString().toIntOrNull() ?: 0
                val amount = points * POINTS_TO_EGP_RATE
                tvAmountHint.text = getString(R.string.you_will_receive, amount)

                when {
                    points > currentPoints -> {
                        tvPointsError.visibility = View.VISIBLE
                        tvPointsError.text = getString(R.string.not_enough_points, currentPoints)
                        btnConfirm.isEnabled = false
                        btnConfirm.alpha = 0.5f
                    }
                    points <= 0 -> {
                        tvPointsError.visibility = View.VISIBLE
                        tvPointsError.text = getString(R.string.enter_valid_points)
                        btnConfirm.isEnabled = false
                        btnConfirm.alpha = 0.5f
                    }
                    else -> {
                        tvPointsError.visibility = View.GONE
                        val accountValid = etAccount.text.toString().matches(Regex("^\\d{8,12}$"))
                        btnConfirm.isEnabled = accountValid
                        btnConfirm.alpha = if (accountValid) 1f else 0.5f
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            val account = etAccount.text.toString()
            val points = etPoints.text.toString().toIntOrNull() ?: 0

            if (account.matches(Regex("^\\d{8,12}$")) && points <= currentPoints && points > 0) {
                showConfirmationDialog(getString(R.string.insta_pay), account, points)
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.enter_valid_account), Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showConfirmationDialog(method: String, account: String, points: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_confirm)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvConfirmMessage)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        val amount = points * POINTS_TO_EGP_RATE
        tvMessage.text = getString(R.string.confirm_withdraw_message, points, amount, method, account)

        btnConfirm.setOnClickListener {
            playSuccessSound()
            updatePointsAfterWithdraw(points)
            dialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 500)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun playSuccessSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.koiroylers)
            mp.setOnCompletionListener {
                it.release()
            }
            mp.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePointsAfterWithdraw(points: Int) {
        val newPoints = currentPoints - points
        currentPoints = newPoints
        updatePointsUI()

        val email = auth.currentUser?.email ?: ""
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sharedPref.edit().putInt("user_points", newPoints).apply()

        sendPointsUpdateBroadcast(newPoints)

        val userId = auth.currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            RetrofitInstance.api.addPoints(userId, mapOf("points" to -points))
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            android.util.Log.d("POINTS", "API updated successfully")
                        } else {
                            android.util.Log.e("POINTS", "API update failed: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        android.util.Log.e("POINTS", "API error: ${t.message}")
                    }
                })
        }
    }

    private fun sendPointsUpdateBroadcast(newPoints: Int) {
        val intent = Intent("POINTS_UPDATED")
        intent.putExtra("new_points", newPoints)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}