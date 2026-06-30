package com.example.generatechatvhdlcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ComponentBottomSheet(
    private val component: String,
    private val format: String,
    private val entity: String,
    private val arch: String,
    private val hasEnable: Boolean,
    private val hasReset: Boolean,
    private val onGenerate: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layout = when (component) {
            "MUX" -> R.layout.bottom_sheet_mux
            "DeMUX" -> R.layout.bottom_sheet_demux
            "Decoder" -> R.layout.bottom_sheet_decoder
            "Encoder" -> R.layout.bottom_sheet_encoder
            "Comparator" -> R.layout.bottom_sheet_comparator
            "SRAM"       -> R.layout.bottom_sheet_ram
            else -> R.layout.bottom_sheet_shift  // SIPO, PISO, PIPO
        }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView?>(R.id.tvTitle)?.text = "$component Settings"

        when (component) {
            "PIPO" -> {
                view.findViewById<TextView?>(R.id.tvDirLabel)?.visibility = View.GONE
                view.findViewById<RadioGroup?>(R.id.rgDirection)?.visibility = View.GONE
                view.findViewById<TextView?>(R.id.tvModeLabel)?.visibility = View.GONE
                view.findViewById<Spinner?>(R.id.spinnerMode)?.visibility = View.GONE
            }
            "SIPO" -> {
                view.findViewById<TextView?>(R.id.tvModeLabel)?.visibility = View.GONE
                view.findViewById<Spinner?>(R.id.spinnerMode)?.visibility = View.GONE
            }
            "PISO" -> {
                view.findViewById<TextView?>(R.id.tvModeLabel)?.visibility = View.VISIBLE
                view.findViewById<Spinner?>(R.id.spinnerMode)?.visibility = View.VISIBLE
                setupModeSpinner(view)
            }
            "SRAM" -> {
                view.findViewById<TextView?>(R.id.tvAddrLabel)?.visibility = View.VISIBLE
                view.findViewById<EditText?>(R.id.etAddrBits)?.visibility = View.VISIBLE
            }
        }

        view.findViewById<Button>(R.id.btnGenerate).setOnClickListener {
            val query = buildQuery(view)
            if (query == null) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val code = VHDLGenerator.generate(
                query = query,
                format = format,
                entity = entity,
                arch = arch,
                hasEnable = hasEnable,
                hasReset = hasReset
            )
            onGenerate(code)
            dismiss()
        }
    }

    private fun setupModeSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerMode)
        val modes = arrayOf("Load", "Shift Right", "Shift Left")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun buildQuery(view: View): String? {
        return when (component) {
            "MUX" -> {
                val numInputs = view.findViewById<EditText>(R.id.etNumInputs).text.toString().trim()
                val bitWidth  = view.findViewById<EditText>(R.id.etBitWidth).text.toString().trim()
                if (numInputs.isEmpty()) return null
                val width = if (bitWidth.isEmpty()) "1" else bitWidth
                "MUX inputs=$numInputs width=$width"
            }

            "DeMUX" -> {
                val outputs = view.findViewById<EditText>(R.id.etNumOutputs).text.toString().trim()
                val width   = view.findViewById<EditText>(R.id.etBitWidth).text.toString().trim()
                if (outputs.isEmpty()) return null
                val w = if (width.isEmpty()) "1" else width
                "DEMUX outputs=$outputs width=$w"
            }

            "Decoder" -> {
                val inBits = view.findViewById<EditText>(R.id.etInBits).text.toString().trim()
                if (inBits.isEmpty()) return null
                "$component bits=$inBits"
            }

            "Encoder" -> {
                val inBits = view.findViewById<EditText>(R.id.etInBits).text.toString().trim()
                if (inBits.isEmpty()) return null
                "$component bits=$inBits"
            }

            "Comparator" -> {
                val bits = view.findViewById<EditText>(R.id.etBits).text.toString().trim()
                if (bits.isEmpty()) return null
                "$component bits=$bits"
            }

            "SRAM" -> {
                val addrBits = view.findViewById<EditText>(R.id.etAddrBits).text.toString().trim()
                val dataBits = view.findViewById<EditText>(R.id.etDataBits).text.toString().trim()
                if (addrBits.isEmpty() || dataBits.isEmpty()) return null
                "$component addr=$addrBits data=$dataBits"
            }

            "SIPO" -> {
                val bits = view.findViewById<EditText>(R.id.etBits).text.toString().trim()
                if (bits.isEmpty()) return null
                val directionGroup = view.findViewById<RadioGroup>(R.id.rgDirection)
                val dir = if (directionGroup != null && directionGroup.checkedRadioButtonId == R.id.rbRight) "right" else "left"
                "$component bits=$bits dir=$dir"
            }

            "PISO" -> {
                val bits = view.findViewById<EditText>(R.id.etBits).text.toString().trim()
                if (bits.isEmpty()) return null
                val directionGroup = view.findViewById<RadioGroup>(R.id.rgDirection)
                val dir = if (directionGroup != null && directionGroup.checkedRadioButtonId == R.id.rbRight) "right" else "left"
                val modeSpinner = view.findViewById<Spinner>(R.id.spinnerMode)
                val mode = when (modeSpinner.selectedItemPosition) {
                    0 -> "load"
                    1 -> "shift_right"
                    2 -> "shift_left"
                    else -> "load"
                }
                "$component bits=$bits dir=$dir mode=$mode"
            }

            "PIPO" -> {
                val bits = view.findViewById<EditText>(R.id.etBits).text.toString().trim()
                if (bits.isEmpty()) return null
                "$component bits=$bits"
            }

            else -> null
        }
    }
}