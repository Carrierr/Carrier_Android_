package me.tiptap.tiptap.binding

import android.databinding.BindingAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("day")
fun extractDayFromDate(view : TextView, date : Date) {
    view.text = SimpleDateFormat("dd", Locale.KOREAN).format(date)
}