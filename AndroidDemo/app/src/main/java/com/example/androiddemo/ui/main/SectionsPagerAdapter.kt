package com.example.androiddemo.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.androiddemo.R
import com.example.androiddemo.layoutmanager.FLMFragment

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    private val fragments = ArrayList<PlaceholderFragment>()

    init {

        fragments.add(FLMFragment())
        fragments.add(PlaceholderFragment.newInstance(0))
        fragments.add(PlaceholderFragment.newInstance(1))
    }

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragments[position].getTitle()
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return fragments.size
    }
}