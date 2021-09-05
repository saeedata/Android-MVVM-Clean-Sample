package com.saeed.rssreader.utils.extension

import java.util.*
import java.util.concurrent.TimeUnit


fun Long.toReadableTime(): String {
    val hour = TimeUnit.MILLISECONDS.toHours(this)
    val minute = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val second = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return when {
        hour > 0 -> {
            String.format("%02d:%02d:%02d", hour, minute, second)
        }
        minute > 0 -> {
            String.format("%02d:%02d", minute, second)
        }
        else -> {
            String.format("00:%02d", second)
        }
    }
}

fun Long.toCalendar(): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = this@toCalendar
    }
}