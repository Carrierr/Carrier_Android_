package me.tiptap.tiptap.onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.tiptap.tiptap.R


class OnBoardingFragment1 : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(
                R.layout.onboarding_screen1,
                container,
                false
        )
    }
}