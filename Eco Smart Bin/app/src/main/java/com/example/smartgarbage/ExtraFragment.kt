package com.example.smartgarbage

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class ExtraFragment : Fragment(R.layout.fragment_extra) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.sponsorsRecyclerView)
        val supportImage: View = view.findViewById(R.id.imgSupport)

        val sponsors = arrayListOf(
            SponsorModel(getString(R.string.phonekan), R.drawable.img, R.raw.sound1),
            SponsorModel(getString(R.string.hmskegy), R.drawable.img1, R.raw.sound)
        )

        recyclerView.adapter = AdapterSponsor(requireContext(), sponsors)

        supportImage.setOnClickListener {
            startActivity(Intent(requireContext(), SupportActivity::class.java))
        }
    }
}