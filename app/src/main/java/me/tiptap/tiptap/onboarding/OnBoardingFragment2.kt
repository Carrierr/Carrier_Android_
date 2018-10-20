package me.tiptap.tiptap.onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.tiptap.tiptap.R
import me.tiptap.tiptap.databinding.OnboardingScreen2Binding
import javax.annotation.Nullable


class OnBoardingFragment2 : Fragment() {

    private lateinit var binding:OnboardingScreen2Binding
    @Nullable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
                R.layout.onboarding_screen2,
                container,
                false
        )
    }
}