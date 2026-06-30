    package com.example.smartgarbage
    import androidx.fragment.app.Fragment
    import androidx.fragment.app.FragmentActivity
    import androidx.viewpager2.adapter.FragmentStateAdapter

    class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 7

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NoteFragment()
                1 -> AlertsFragment()
                2 -> NearbyBinsFragment()
                3 -> PointsFragment()
                4 -> HistoryFragment()
                5 -> ExtraFragment()
                6 -> SettingFragment()
                else -> NoteFragment()
            }
        }
    }