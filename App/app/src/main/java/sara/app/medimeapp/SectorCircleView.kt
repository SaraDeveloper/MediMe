package sara.app.medimeapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * A circle divided into 15 sectors. Fills the view; use a square aspect ratio so it renders as a circle.
 * Numbers start at 8 and go clockwise: 8, 9, 10, …, 15, 1, 2, …, 7 (so 8 is at 12 o'clock).
 */
class SectorCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val sectorCount = 15
    private val sweepAngle = 360f / sectorCount

    /** Display order: 8 at top, then clockwise 9..15, 1..7. */
    private val sectorNumbers = intArrayOf(8, 9, 10, 11, 12, 13, 14, 15, 1, 2, 3, 4, 5, 6, 7)

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF000000.toInt()
        strokeWidth = 2f
    }

    private val sectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val padding = strokePaint.strokeWidth
        rect.set(padding, padding, size - padding, size - padding)

        for (i in 0 until sectorCount) {
            sectorPaint.color = 0xFFE0FFFF.toInt() // #E0FFFF
            val startAngle = -90f + i * sweepAngle
            canvas.drawArc(rect, startAngle, sweepAngle, true, sectorPaint)
        }

        for (i in 0 until sectorCount) {
            val startAngle = -90f + i * sweepAngle
            canvas.drawArc(rect, startAngle, sweepAngle, true, strokePaint)
        }

        val cx = size / 2f
        val cy = size / 2f
        val circleRadius = minOf(rect.width(), rect.height()) / 2f
        val textRadius = circleRadius * 0.60f
        textPaint.textSize = circleRadius * 0.12f

        for (i in 0 until sectorCount) {
            val centerAngleDeg = -90f + (i + 0.5f) * sweepAngle
            val rad = Math.toRadians(centerAngleDeg.toDouble())
            val tx = cx + (textRadius * cos(rad)).toFloat()
            val ty = cy + (textRadius * sin(rad)).toFloat()
            val text = sectorNumbers[i].toString()
            val bounds = android.graphics.Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            canvas.drawText(text, tx, ty - (bounds.top + bounds.bottom) / 2f, textPaint)
        }
    }
}
