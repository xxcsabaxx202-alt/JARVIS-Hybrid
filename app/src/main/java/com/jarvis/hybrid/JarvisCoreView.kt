package com.jarvis.hybrid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.math.sin
import android.view.View
import kotlin.math.min

/** Original animated holographic HUD, inspired by cinematic sci-fi interfaces. */
class JarvisCoreView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cyan = Color.rgb(45, 225, 249)
    private var angle = 0f
    var mode: String = "idle"
        set(value) {
            field = value
            invalidate()
        }
    private val tick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            angle = (angle + when (mode) {
                "listening" -> 2.2f
                "thinking" -> 3.6f
                "speaking" -> 1.8f
                else -> 0.9f
            }) % 360f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    init {
        contentDescription = "Animált JARVIS holografikus vezérlőmag"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(tick)
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.33f
        canvas.drawColor(Color.rgb(3, 12, 23))
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1f
        paint.color = Color.argb(27, 45, 225, 249)
        val grid = width / 12f
        for (i in 0..12) canvas.drawLine(i * grid, 0f, i * grid, height.toFloat(), paint)
        for (i in 0..14) canvas.drawLine(0f, i * grid, width.toFloat(), i * grid, paint)
        paint.color = Color.argb(100, 45, 225, 249)
        canvas.drawLine(18f, 24f, width * 0.30f, 24f, paint)
        canvas.drawLine(width * 0.70f, 24f, width - 18f, 24f, paint)
        canvas.drawLine(18f, height - 24f, width * 0.30f, height - 24f, paint)
        canvas.drawLine(width * 0.70f, height - 24f, width - 18f, height - 24f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        paint.textSize = 12f * resources.displayMetrics.scaledDensity
        paint.color = Color.rgb(72, 201, 226)
        canvas.drawText("SYS // JARVIS", 20f, 49f, paint)
        canvas.drawText("CORE STATUS", 20f, height - 45f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("HUD / 07", width - 20f, 49f, paint)
        canvas.drawText("SECURE LINK", width - 20f, height - 45f, paint)
        paint.style = Paint.Style.STROKE

        paint.color = Color.argb(40, 45, 225, 249)
        paint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, r * 1.03f, paint)
        canvas.drawCircle(cx, cy, r * 0.76f, paint)
        canvas.drawCircle(cx, cy, r * 1.23f, paint)
        canvas.drawCircle(cx, cy, r * 1.33f, paint)
        paint.strokeWidth = 2f
        paint.color = Color.argb(120, 45, 225, 249)
        for (i in 0 until 18) {
            canvas.drawArc(cx-r*1.23f, cy-r*1.23f, cx+r*1.23f, cy+r*1.23f,
                i*20f + angle*0.15f, 10f, false, paint)
        }
        paint.strokeWidth = 1f
        for (i in 0 until 12) {
            val y = cy - r * 0.66f + i * (r * 1.32f / 11f)
            val pulse = (sin(angle * 0.05f + i * 0.7f) * 0.5f + 0.5f).toFloat()
            val amplitude = if (mode == "speaking") 0.24f else 0.08f
            paint.color = Color.argb((60 + pulse * 130).toInt(), 45, 225, 249)
            canvas.drawLine(cx-r*1.62f, y, cx-r*(1.62f-amplitude*pulse), y, paint)
            canvas.drawLine(cx+r*(1.62f-amplitude*pulse), y, cx+r*1.62f, y, paint)
        }

        paint.color = when (mode) {
            "listening" -> Color.rgb(73, 255, 180)
            "thinking" -> Color.rgb(255, 196, 85)
            else -> cyan
        }
        paint.strokeWidth = if (mode == "speaking") 8f else 5f
        paint.setShadowLayer(12f, 0f, 0f, cyan)
        canvas.drawArc(cx-r, cy-r, cx+r, cy+r, angle, 105f, false, paint)
        canvas.drawArc(cx-r, cy-r, cx+r, cy+r, angle+180f, 75f, false, paint)

        paint.strokeWidth = 2f
        paint.color = Color.rgb(116, 241, 255)
        val inner = r * 0.61f
        canvas.drawArc(cx-inner, cy-inner, cx+inner, cy+inner, -angle, 130f, false, paint)
        canvas.drawArc(cx-inner, cy-inner, cx+inner, cy+inner, -angle+185f, 95f, false, paint)
        paint.clearShadowLayer()

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(45, 45, 225, 249)
        canvas.drawCircle(cx, cy, r * 0.39f, paint)
        paint.color = cyan
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD)
        paint.textSize = r * 0.31f
        canvas.drawText("J", cx, cy + r * 0.11f, paint)
        paint.textSize = r * 0.115f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        val caption = when (mode) {
            "listening" -> "HALLGATLAK"
            "thinking" -> "FELDOLGOZÁS"
            "speaking" -> "BESZÉLEK"
            else -> "KÉSZENLÉT"
        }
        canvas.drawText(caption, cx, cy + r * 0.53f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(160, 45, 225, 249)
        for (i in 0 until 48) {
            canvas.save()
            canvas.rotate(i * 7.5f, cx, cy)
            canvas.drawLine(cx, cy-r*0.83f, cx, cy-r*(if (i%4==0) 0.94f else 0.88f), paint)
            canvas.restore()
        }
    }
}
