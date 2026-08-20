package org.autismallyship.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// Every mark this view ever shows exists because a finger put it there. Nothing here runs on a
// timer and nothing moves once drawn, so unlike guided breathing this needs no separate sensory
// mode behaviour at all: RULES-APP.md's own words are "a tool that is still when nobody is
// touching it needs no special handling in sensory mode."
class TracingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Shape { CIRCLE, SQUARE, STAR, SPIRAL, WAVE }

    var onNewTouch: (() -> Unit)? = null
    var onComplete: (() -> Unit)? = null

    var shape: Shape = Shape.CIRCLE
        set(value) {
            field = value
            clear()
        }

    val isComplete: Boolean
        get() = touched.isNotEmpty() && touched.count { it } >= (touched.size * COMPLETION_FRACTION)

    private var outline = Path()
    private var samples: List<PointF> = emptyList()
    private var touched: BooleanArray = BooleanArray(0)
    private var touchedColor: IntArray = IntArray(0)
    private var totalDragPx = 0f
    private var lastTouch: PointF? = null
    private var completionFired = false

    private val density = context.resources.displayMetrics.density
    private val touchRadiusPx = 28f * density
    private val sampleSpacingPx = 4f * density
    private val dotRadiusPx = 5f * density

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = ContextCompat.getColor(context, R.color.line_strong)
        alpha = 90
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val palette = intArrayOf(
        ContextCompat.getColor(context, R.color.sensory_breathing),
        ContextCompat.getColor(context, R.color.sensory_pop_it),
        ContextCompat.getColor(context, R.color.sensory_sounds),
        ContextCompat.getColor(context, R.color.sensory_tracing)
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    fun clear() {
        totalDragPx = 0f
        lastTouch = null
        completionFired = false
        rebuild()
    }

    private fun rebuild() {
        if (width == 0 || height == 0) return
        outline = buildPath(shape, width.toFloat(), height.toFloat())
        samples = sampleOutline(outline)
        touched = BooleanArray(samples.size)
        touchedColor = IntArray(samples.size)
        invalidate()
    }

    private fun buildPath(shape: Shape, w: Float, h: Float): Path {
        val cx = w / 2
        val cy = h / 2
        val r = min(w, h) * 0.35f
        val path = Path()
        when (shape) {
            Shape.CIRCLE -> path.addCircle(cx, cy, r, Path.Direction.CW)
            Shape.SQUARE -> path.addRoundRect(
                cx - r, cy - r, cx + r, cy + r,
                r * 0.15f, r * 0.15f,
                Path.Direction.CW
            )
            Shape.STAR -> {
                val outerR = r
                val innerR = r * 0.45f
                for (i in 0..10) {
                    val angleDeg = -90 + i * 36
                    val radius = if (i % 2 == 0) outerR else innerR
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val x = cx + radius * cos(angleRad).toFloat()
                    val y = cy + radius * sin(angleRad).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            Shape.SPIRAL -> {
                val turns = 2.5
                val steps = 240
                for (i in 0..steps) {
                    val theta = turns * 2 * Math.PI * i / steps
                    val radius = (r * i / steps).toFloat()
                    val x = cx + (radius * cos(theta - Math.PI / 2)).toFloat()
                    val y = cy + (radius * sin(theta - Math.PI / 2)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            Shape.WAVE -> {
                val left = cx - r
                val right = cx + r
                val steps = 200
                val cycles = 2.0
                for (i in 0..steps) {
                    val x = left + (right - left) * i / steps
                    val y = cy + r * 0.5f * sin(2 * Math.PI * cycles * i / steps).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
        }
        return path
    }

    private fun sampleOutline(path: Path): List<PointF> {
        val measure = PathMeasure(path, false)
        val points = mutableListOf<PointF>()
        val pos = FloatArray(2)
        var distance = 0f
        do {
            measure.getPosTan(distance, pos, null)
            points.add(PointF(pos[0], pos[1]))
            distance += sampleSpacingPx
        } while (distance < measure.length)
        return points
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val point = PointF(event.x, event.y)
                lastTouch?.let { totalDragPx += hypot((point.x - it.x).toDouble(), (point.y - it.y).toDouble()).toFloat() }
                lastTouch = point

                var newlyTouched = false
                val currentColor = colourAt(totalDragPx)
                for (i in samples.indices) {
                    if (touched[i]) continue
                    val sample = samples[i]
                    if (hypot((point.x - sample.x).toDouble(), (point.y - sample.y).toDouble()) <= touchRadiusPx) {
                        touched[i] = true
                        touchedColor[i] = currentColor
                        newlyTouched = true
                    }
                }

                if (newlyTouched) {
                    onNewTouch?.invoke()
                    invalidate()
                    if (!completionFired && isComplete) {
                        completionFired = true
                        onComplete?.invoke()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouch = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // Cycles through the sensory palette once every three shape-lengths of dragging, so retracing
    // or lingering keeps drifting through it rather than stopping dead at the end.
    private fun colourAt(dragPx: Float): Int {
        val cycleLength = (samples.size * sampleSpacingPx * 3).coerceAtLeast(1f)
        val progress = (dragPx % cycleLength) / cycleLength
        val segments = palette.size
        val scaled = progress * segments
        val index = scaled.toInt().coerceIn(0, segments - 1)
        val next = (index + 1) % segments
        val fraction = scaled - index
        return ColorUtils.blendARGB(palette[index], palette[next], fraction)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(outline, outlinePaint)
        for (i in samples.indices) {
            if (!touched[i]) continue
            dotPaint.color = touchedColor[i]
            canvas.drawCircle(samples[i].x, samples[i].y, dotRadiusPx, dotPaint)
        }
    }

    companion object {
        private const val COMPLETION_FRACTION = 0.9f
    }
}
