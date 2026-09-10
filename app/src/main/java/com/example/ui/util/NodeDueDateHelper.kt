package com.example.ui.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Status of a node's due date / reminder deadline.
 */
enum class DueDateStatus {
    NONE,
    OVERDUE,            // Past deadline (Urgent Red)
    DUE_TODAY,          // Due today (Warm Orange / Amber)
    APPROACHING,        // Due in <= 48 hours (Golden Yellow / Warm glow)
    FUTURE              // Due in > 2 days (Normal / Calm Cyan)
}

data class DueDateInfo(
    val status: DueDateStatus,
    val formattedDate: String,
    val relativeLabel: String,
    val ringColor: Color,
    val badgeColor: Color,
    val daysRemaining: Long
)

object NodeDueDateHelper {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDate(timestampMillis: Long): String {
        return dateFormatter.format(Date(timestampMillis))
    }

    fun formatDateTime(timestampMillis: Long): String {
        return "${dateFormatter.format(Date(timestampMillis))} - ${timeFormatter.format(Date(timestampMillis))}"
    }

    fun formatFullDate(timestampMillis: Long?): String {
        if (timestampMillis == null || timestampMillis <= 0L) return ""
        return "${dateFormatter.format(Date(timestampMillis))}  ${timeFormatter.format(Date(timestampMillis))}"
    }

    fun calculateStatus(dueDateMillis: Long?): DueDateStatus {
        return getDueDateInfo(dueDateMillis).status
    }

    fun getStatusGlowColor(status: DueDateStatus): Color {
        return when (status) {
            DueDateStatus.NONE -> Color.Transparent
            DueDateStatus.OVERDUE -> Color(0xFFEF4444)      // Urgent Crimson Red
            DueDateStatus.DUE_TODAY -> Color(0xFFF97316)    // Warm Amber/Orange
            DueDateStatus.APPROACHING -> Color(0xFFF59E0B)  // Warm Golden Yellow
            DueDateStatus.FUTURE -> Color(0xFF06B6D4)       // Calm Cyan / Blue
        }
    }

    fun formatRelativeDueDate(dueDateMillis: Long?): String {
        return getDueDateInfo(dueDateMillis).relativeLabel
    }

    fun getDueDateInfo(dueDateMillis: Long?): DueDateInfo {
        if (dueDateMillis == null || dueDateMillis <= 0L) {
            return DueDateInfo(
                status = DueDateStatus.NONE,
                formattedDate = "",
                relativeLabel = "",
                ringColor = Color.Transparent,
                badgeColor = Color.Transparent,
                daysRemaining = 0L
            )
        }

        val now = System.currentTimeMillis()
        val diffMillis = dueDateMillis - now

        val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
        val calendarDue = Calendar.getInstance().apply { timeInMillis = dueDateMillis }

        val isSameDay = calendarNow.get(Calendar.YEAR) == calendarDue.get(Calendar.YEAR) &&
                calendarNow.get(Calendar.DAY_OF_YEAR) == calendarDue.get(Calendar.DAY_OF_YEAR)

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val daysRemaining = if (diffMillis >= 0) {
            (diffMillis / oneDayMillis) + if (diffMillis % oneDayMillis > 0) 1 else 0
        } else {
            -((-diffMillis / oneDayMillis) + 1)
        }

        val formattedDate = dateFormatter.format(Date(dueDateMillis))

        return when {
            diffMillis < 0 && !isSameDay -> {
                DueDateInfo(
                    status = DueDateStatus.OVERDUE,
                    formattedDate = formattedDate,
                    relativeLabel = "متأخر (${-daysRemaining} يوم)",
                    ringColor = Color(0xFFEF4444),     // Urgent Crimson Red
                    badgeColor = Color(0xFFDC2626),
                    daysRemaining = daysRemaining
                )
            }
            isSameDay -> {
                DueDateInfo(
                    status = DueDateStatus.DUE_TODAY,
                    formattedDate = formattedDate,
                    relativeLabel = "مستحق اليوم ⚠️",
                    ringColor = Color(0xFFF97316),     // Warm Amber/Orange
                    badgeColor = Color(0xFFEA580C),
                    daysRemaining = 0L
                )
            }
            diffMillis <= 2 * oneDayMillis -> {
                DueDateInfo(
                    status = DueDateStatus.APPROACHING,
                    formattedDate = formattedDate,
                    relativeLabel = "يقترب موعده (غداً أو بعد غد)",
                    ringColor = Color(0xFFF59E0B),     // Warm Golden Yellow
                    badgeColor = Color(0xFFD97706),
                    daysRemaining = daysRemaining
                )
            }
            else -> {
                DueDateInfo(
                    status = DueDateStatus.FUTURE,
                    formattedDate = formattedDate,
                    relativeLabel = "متبقي $daysRemaining أيام",
                    ringColor = Color(0xFF06B6D4),     // Calm Cyan
                    badgeColor = Color(0xFF0891B2),
                    daysRemaining = daysRemaining
                )
            }
        }
    }
}
