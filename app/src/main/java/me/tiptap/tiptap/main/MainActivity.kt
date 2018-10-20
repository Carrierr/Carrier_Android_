package me.tiptap.tiptap.main

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import me.tiptap.tiptap.R
import me.tiptap.tiptap.adapter.MainViewPagerAdapter
import me.tiptap.tiptap.databinding.ActivityMainBinding
import me.tiptap.tiptap.onboarding.OnBoardingActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val preferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)

        if (!preferences.getBoolean("onboarding_complete", false)) {

            val onboarding = Intent(this, OnBoardingActivity::class.java)
            startActivity(onboarding)

            finish()
            return
        }
        initViewPager()

    }

    private fun initViewPager() {
        binding.vpMain.apply {
            adapter = MainViewPagerAdapter(this@MainActivity.supportFragmentManager, 3)
            currentItem = 1
        }
    }
}
