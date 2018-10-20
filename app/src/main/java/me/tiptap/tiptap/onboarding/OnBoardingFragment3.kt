package me.tiptap.tiptap.onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.tiptap.tiptap.R
import me.tiptap.tiptap.databinding.OnboardingScreen3Binding
import javax.annotation.Nullable


class OnBoardingFragment3 : Fragment() {

    private lateinit var binding:OnboardingScreen3Binding
    @Nullable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
                R.layout.onboarding_screen3,
                container,
                false
        )

    }
}