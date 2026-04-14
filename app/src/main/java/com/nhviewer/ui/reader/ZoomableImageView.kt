package com.nhviewer.ui.reader

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.absoluteValue
import kotlin.math.abs

/**
 * ImageView with pinch zoom, double tap zoom, and pan.
 * Also exposes single tap / long press / vertical fling callbacks for reader controls.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val transformMatrix = Matrix()
    private var scaleFactor = 1f
    private var isInZoomMode = false

    private val scaleDetector = ScaleGestureDetector(context, PinchListener())
    private val gestureDetector = GestureDetector(context, TapAndScrollListener())
    private val viewConfig = ViewConfiguration.get(context)
    private val minFlingDistance = viewConfig.scaledTouchSlop * 7f
    private val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity * 1.8f
    private val flingDirectionRatio = 1.35f
    private var lastVerticalFlingAtMs = 0L

    private var singleTapListener: ((x: Float, width: Int) -> Unit)? = null
    private var longPressListener: (() -> Unit)? = null
    private var verticalFlingListener: ((velocityY: Float) -> Unit)? = null

    companion object {
        private const val MIN_SCALE = 1f
        private const val MAX_SCALE = 5f
        private const val DOUBLE_TAP_SCALE = 2.5f
        private const val SNAP_TO_MIN_THRESHOLD = 1.05f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleHandled = try {
            scaleDetector.onTouchEvent(event)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IndexOutOfBoundsException) {
            false
        }
        val gestureHandled = try {
            gestureDetector.onTouchEvent(event)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IndexOutOfBoundsException) {
            false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isInZoomMode) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return scaleHandled || gestureHandled || super.onTouchEvent(event)
    }

    fun setGestureCallbacks(
        onSingleTap: ((x: Float, width: Int) -> Unit)?,
        onLongPress: (() -> Unit)?,
        onVerticalFling: ((velocityY: Float) -> Unit)?
    ) {
        singleTapListener = onSingleTap
        longPressListener = onLongPress
        verticalFlingListener = onVerticalFling
    }

    private inner class PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (!isInZoomMode) enterZoomMode()
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val newScale = (scaleFactor * factor).coerceIn(MIN_SCALE, MAX_SCALE)
            val scaleBy = newScale / scaleFactor
            scaleFactor = newScale
            transformMatrix.postScale(scaleBy, scaleBy, detector.focusX, detector.focusY)
            clampTranslation()
            imageMatrix = transformMatrix
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (scaleFactor <= SNAP_TO_MIN_THRESHOLD) resetZoom()
        }
    }

    private inner class TapAndScrollListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (isZoomed()) return false
            singleTapListener?.invoke(e.x, width)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (isZoomed()) return
            longPressListener?.invoke()
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isInZoomMode && scaleFactor > 1.5f) {
                resetZoom()
            } else {
                if (!isInZoomMode) enterZoomMode()
                val targetScale = DOUBLE_TAP_SCALE
                val scaleBy = targetScale / scaleFactor
                scaleFactor = targetScale
                transformMatrix.postScale(scaleBy, scaleBy, e.x, e.y)
                clampTranslation()
                imageMatrix = transformMatrix
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!isInZoomMode || scaleFactor <= MIN_SCALE) return false
            transformMatrix.postTranslate(-distanceX, -distanceY)
            clampTranslation()
            imageMatrix = transformMatrix
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isZoomed()) return false
            val start = e1 ?: return false
            val now = System.currentTimeMillis()
            if (now - lastVerticalFlingAtMs < 220L) return false
            val dx = e2.x - start.x
            val dy = e2.y - start.y
            val absDx = dx.absoluteValue
            val absDy = dy.absoluteValue
            if (absDy > absDx * flingDirectionRatio &&
                absDy > minFlingDistance &&
                velocityY.absoluteValue > minFlingVelocity
            ) {
                lastVerticalFlingAtMs = now
                verticalFlingListener?.invoke(velocityY)
                return true
            }
            return false
        }
    }

    private fun enterZoomMode() {
        val d = drawable ?: return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) return
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0f || vh <= 0f) return

        val scale = minOf(vw / dw, vh / dh)
        val dx = (vw - dw * scale) / 2f
        val dy = (vh - dh * scale) / 2f

        transformMatrix.reset()
        transformMatrix.postScale(scale, scale)
        transformMatrix.postTranslate(dx, dy)

        scaleType = ScaleType.MATRIX
        imageMatrix = transformMatrix
        scaleFactor = 1f
        isInZoomMode = true
    }

    fun resetZoom() {
        scaleType = ScaleType.FIT_CENTER
        transformMatrix.reset()
        imageMatrix = transformMatrix
        scaleFactor = 1f
        isInZoomMode = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    fun isZoomed(): Boolean = isInZoomMode && scaleFactor > 1f

    private fun clampTranslation() {
        val d = drawable ?: return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        val vw = width.toFloat()
        val vh = height.toFloat()

        val values = FloatArray(9)
        transformMatrix.getValues(values)

        val curScale = values[Matrix.MSCALE_X]
        val scaledW = dw * curScale
        val scaledH = dh * curScale

        val newTx = if (scaledW <= vw) {
            (vw - scaledW) / 2f
        } else {
            values[Matrix.MTRANS_X].coerceIn(vw - scaledW, 0f)
        }
        val newTy = if (scaledH <= vh) {
            (vh - scaledH) / 2f
        } else {
            values[Matrix.MTRANS_Y].coerceIn(vh - scaledH, 0f)
        }

        values[Matrix.MTRANS_X] = newTx
        values[Matrix.MTRANS_Y] = newTy
        transformMatrix.setValues(values)
    }
}
