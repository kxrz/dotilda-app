package com.dotilda.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The last crash, on a plain screen made of Android views: nothing from Compose, MMD, Room or
 * DataStore, so it shows even when one of them is what fell over. The report can be saved to a
 * file, or simply photographed; [onContinue] (when Dotilda can start at all) opens her as usual.
 */
fun Activity.showCrashReport(report: String, onSave: () -> Unit, onContinue: (() -> Unit)?) {
    val density = resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()
    fun button(label: Int, filled: Boolean, onClick: () -> Unit) = Button(this).apply {
        setText(label)
        isAllCaps = false
        stateListAnimator = null
        setTextColor(if (filled) Color.WHITE else Color.BLACK)
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (filled) Color.BLACK else Color.WHITE)
            setStroke(dp(2), Color.BLACK)
        }
        setOnClickListener { onClick() }
    }

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)
        setPadding(dp(20), dp(20), dp(20), dp(20))
    }
    root.addView(TextView(this).apply {
        setText(R.string.crash_title)
        textSize = 24f
        setTextColor(Color.BLACK)
    })
    root.addView(TextView(this).apply {
        setText(R.string.crash_text)
        textSize = 15f
        setTextColor(Color.BLACK)
        setPadding(0, dp(8), 0, dp(12))
    })
    // The header (version, device) and the top of the trace, which says what went wrong.
    root.addView(
        TextView(this).apply {
            text = report.lineSequence().filter { it.isNotBlank() }.take(40).joinToString("\n")
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextColor(Color.BLACK)
            setTextIsSelectable(true)
        },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f),
    )
    val buttons = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(12), 0, 0)
    }
    val half = LinearLayout.LayoutParams(0, dp(52), 1f)
    buttons.addView(button(R.string.crash_save, filled = onContinue == null, onClick = onSave), half)
    if (onContinue != null) {
        buttons.addView(android.widget.Space(this), LinearLayout.LayoutParams(dp(10), 1))
        buttons.addView(button(R.string.crash_continue, filled = true, onClick = onContinue), half)
    }
    root.addView(buttons)
    setContentView(root)
}
