package me.tiptap.tiptap.onboarding

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.app.AppCompatActivity
import me.tiptap.tiptap.R
import me.tiptap.tiptap.databinding.ActivityOnboardingBinding
import me.tiptap.tiptap.main.MainActivity




class OnBoardingActivity : AppCompatActivity() {
    private lateinit var binding:ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_onboarding)

        val adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment? {
                val next_drawable: Drawable = resources.getDrawable(R.drawable.onboard_next)
                binding.nextOnboarding.setImageDrawable(next_drawable)
                when (position) {
                    0 -> return OnBoardingFragment1()
                    1 -> return OnBoardingFragment2()
                    2 -> {
                        val start_drawable: Drawable = resources.getDrawable(R.drawable.onboard_start)
                        binding.nextOnboarding.setImageDrawable(start_drawable)
                        return OnBoardingFragment3()
                    }
                    else -> return null
                }
            }

            override fun getCount(): Int {
                return 3
            }
        }

        binding.onboardingVp.adapter = adapter

        binding.skipOnboarding.setOnClickListener {
            finishOnboarding()
        }
        binding.nextOnboarding.setOnClickListener {
            if(binding.onboardingVp.currentItem == 2)
                finishOnboarding()
            else
                binding.onboardingVp.setCurrentItem(binding.onboardingVp.currentItem + 1, true)
        }
    }

    private fun finishOnboarding() {
        val preferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)

        preferences.edit().putBoolean("onboarding_complete", true).apply()

        val main = Intent(this, MainActivity::class.java)
        startActivity(main)

        finish()
    }
}