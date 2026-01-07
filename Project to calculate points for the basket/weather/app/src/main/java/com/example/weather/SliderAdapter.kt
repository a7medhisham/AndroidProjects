package com.example.weather

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SliderAdapter (fa:FragmentActivity):FragmentStateAdapter(fa) {
    override fun getItemCount(): Int =4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0->Ch1Fragment()
            1->Ch2Fragment()
            2->Ch3Fragment()
            3->Ch4Fragment()
            else->Ch1Fragment()
        }
    }
}