package com.shouvick.mapup.feature.location.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun calculateDuration(start: Long, end: Long?): String {
    if (end == null) return "Ongoing"
    val diff = end - start
    val minutes = (diff / 1000) / 60
    val seconds = (diff / 1000) % 60
    return "${minutes}m ${seconds}s"
}