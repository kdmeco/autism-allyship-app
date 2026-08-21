package org.autismallyship.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

// Pinch-to-zoom ImageView without an extra dependency. Pan only applies once zoomed in.
class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var mode = NONE
    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var saveScale = 1f
    private var viewWidth = 0
    private var viewHeight = 0
    private var origWidth = 0f
    private var origHeight = 0f

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (drawable != null && saveScale == 1f) {
            fitToScreen()
        }
    }

    private fun fitToScreen() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        if (imageWidth <= 0f || imageHeight <= 0f || viewWidth == 0 || viewHeight == 0) return

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = min(scaleX, scaleY)
        matrix.reset()
        matrix.setScale(scale, scale)
        val redundantY = viewHeight - scale * imageHeight
        val redundantX = viewWidth - scale * imageWidth
        matrix.postTranslate(redundantX / 2f, redundantY / 2f)
        origWidth = viewWidth - redundantX
        origHeight = viewHeight - redundantY
        imageMatrix = matrix
        saveScale = 1f
        minScale = 1f
    }

    fun resetZoom() {
        saveScale = 1f
        fitToScreen()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val current = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                last.set(current)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && saveScale > minScale) {
                    val dx = current.x - last.x
                    val dy = current.y - last.y
                    matrix.postTranslate(dx, dy)
                    fixTranslation()
                    last.set(current.x, current.y)
                    imageMatrix = matrix
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }
        return true
    }

    private fun fixTranslation() {
        val values = FloatArray(9)
        matrix.getValues(values)
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val fixX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixX != 0f || fixY != 0f) {
            matrix.postTranslate(fixX, fixY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        return when {
            trans < minTrans -> -trans + minTrans
            trans > maxTrans -> -trans + maxTrans
            else -> 0f
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val previouslyScaled = saveScale
            saveScale *= scaleFactor
            saveScale = max(minScale, min(saveScale, maxScale))
            scaleFactor = saveScale / previouslyScaled
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            fixTranslation()
            imageMatrix = matrix
            return true
        }
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
