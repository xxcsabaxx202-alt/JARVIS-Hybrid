package com.jarvis.hybrid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/** Original animated holographic HUD, inspired by cinematic sci-fi interfaces. */
class JarvisCoreView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cyan = Color.rgb(45, 225, 249)
    private var angle = 0f
    private val tick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            angle = (angle + 0.9f) % 360f
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
        val r = min(width, height) * 0.40f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        paint.color = Color.argb(40, 45, 225, 249)
        paint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, r * 1.03f, paint)
        canvas.drawCircle(cx, cy, r * 0.76f, paint)

        paint.color = cyan
        paint.strokeWidth = 5f
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
