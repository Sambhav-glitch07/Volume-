package in.sambhav.glassvolume.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import in.sambhav.glassvolume.R

/**
 * Custom Canvas-rendered Apple-style Frosted Glass Volume HUD.
 * Reproduces the exact visual reference:
 *  - Left: Pill-shaped dark translucent glass volume slider with animated fill,
 *          speaker icon, and real-time volume number.
 *  - Right: Separate circular glass button with audio output (AirPlay) symbol.
 *  - Crystal specular highlight and subtle refraction along the fill boundary.
 */
class GlassVolumeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dynamic state
    private var currentProgress = 0.52f // 0.0f to 1.0f
    private var targetProgress = 0.52f
    private var displayedVolume = 52
    private var isMuted = false

    private var progressAnimator: ValueAnimator? = null

    // Geometry measurements in pixels
    private var pillWidth = 0f
    private var pillHeight = 0f
    private var circleSize = 0f
    private var gap = 0f

    private val mainPillRect = RectF()
    private val circleRect = RectF()
    private val pillClipPath = Path()
    private val fillClipPath = Path()
    private val crestPath = Path()

    // Paints
    private val pillGlassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillCrestHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val circleGlassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val circleInnerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    // Cached drawables for speaker and output icons
    private val speakerDrawable = ContextCompat.getDrawable(context, R.drawable.ic_speaker)
    private val outputDrawable = ContextCompat.getDrawable(context, R.drawable.ic_audio_output)

    // Visual dimensions in DP converted to PX
    private val dp = resources.displayMetrics.density

    init {
        // High quality rendering
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateLayout(w.toFloat(), h.toFloat())
    }

    private fun recalculateLayout(w: Float, h: Float) {
        if (w <= 0 || h <= 0) return

        // Proportions derived from reference image:
        // Pill is approx 76% of total width, Circle is approx 20%, gap is ~4%
        // Height is typically 110dp-128dp on 1080p TV
        gap = 18f * dp
        val totalAvailableWidth = w - paddingLeft - paddingRight
        val totalAvailableHeight = h - paddingTop - paddingBottom

        // Height of the component
        val compHeight = totalAvailableHeight.coerceAtMost(128f * dp)
        pillHeight = compHeight
        circleSize = compHeight

        pillWidth = (totalAvailableWidth - circleSize - gap).coerceAtLeast(300f * dp)

        val startX = paddingLeft.toFloat()
        val startY = (h - compHeight) / 2f

        mainPillRect.set(startX, startY, startX + pillWidth, startY + pillHeight)

        val circleLeft = startX + pillWidth + gap
        circleRect.set(circleLeft, startY, circleLeft + circleSize, startY + circleSize)

        // Precompute Clip Paths
        val pillCornerRadius = pillHeight / 2f
        pillClipPath.reset()
        pillClipPath.addRoundRect(mainPillRect, pillCornerRadius, pillCornerRadius, Path.Direction.CW)

        // Shaders for dark frosted glass
        val glassGradient = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.left, mainPillRect.bottom,
            intArrayOf(
                Color.argb(205, 48, 54, 64),  // top subtle highlight
                Color.argb(225, 26, 30, 36),  // center dark body
                Color.argb(240, 18, 21, 26)   // bottom depth
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        pillGlassPaint.shader = glassGradient

        // Specular border gradient (brighter at top reflection, subtle at bottom)
        val borderGradient = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.left, mainPillRect.bottom,
            intArrayOf(
                Color.argb(130, 255, 255, 255), // crisp top rim
                Color.argb(55, 255, 255, 255),
                Color.argb(25, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        pillBorderPaint.strokeWidth = 1.35f * dp
        pillBorderPaint.shader = borderGradient

        // Circular output button shaders
        circleGlassPaint.shader = LinearGradient(
            circleRect.left, circleRect.top,
            circleRect.left, circleRect.bottom,
            intArrayOf(
                Color.argb(210, 44, 50, 60),
                Color.argb(230, 24, 28, 34),
                Color.argb(245, 16, 19, 23)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        circleBorderPaint.strokeWidth = 1.35f * dp
        circleBorderPaint.shader = borderGradient

        circleInnerRingPaint.strokeWidth = 1f * dp
        circleInnerRingPaint.color = Color.argb(35, 255, 255, 255)

        // Text sizing based on height
        textPaint.textSize = (pillHeight * 0.28f).coerceIn(28f * dp, 38f * dp)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pillWidth <= 0 || pillHeight <= 0) return

        // 1. DRAW MAIN PILL SLIDER
        drawMainPill(canvas)

        // 2. DRAW RIGHT CIRCULAR AUDIO OUTPUT BUTTON
        drawCircleButton(canvas)
    }

    private fun drawMainPill(canvas: Canvas) {
        val pillRadius = pillHeight / 2f

        // A. Base dark translucent glass capsule
        canvas.drawRoundRect(mainPillRect, pillRadius, pillRadius, pillGlassPaint)

        // B. Active Volume Fill with curved specular meniscus crest
        if (currentProgress > 0.005f) {
            canvas.save()
            // Restrict fill inside the capsule contour
            canvas.clipPath(pillClipPath)

            val fillRight = mainPillRect.left + (mainPillRect.width() * currentProgress)

            // Fill body gradient (lighter smoky glass fill)
            val fillShader = LinearGradient(
                mainPillRect.left, mainPillRect.top,
                mainPillRect.left, mainPillRect.bottom,
                intArrayOf(
                    Color.argb(125, 110, 125, 145),
                    Color.argb(95, 75, 88, 105),
                    Color.argb(70, 50, 60, 75)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            fillPaint.shader = fillShader

            // Draw filled rectangular section up to the crest
            canvas.drawRect(
                mainPillRect.left,
                mainPillRect.top,
                fillRight,
                mainPillRect.bottom,
                fillPaint
            )

            // Draw the curved refractive crescent edge (as seen in the reference image)
            // Notice how the fill border arcs smoothly outwards like liquid surface tension / optical lens!
            val bulge = (pillHeight * 0.35f).coerceAtMost(fillRight - mainPillRect.left)
            crestPath.reset()
            crestPath.moveTo(fillRight, mainPillRect.top)
            crestPath.cubicTo(
                fillRight + bulge * 0.65f, mainPillRect.top + pillHeight * 0.25f,
                fillRight + bulge * 0.65f, mainPillRect.bottom - pillHeight * 0.25f,
                fillRight, mainPillRect.bottom
            )
            crestPath.lineTo(mainPillRect.left, mainPillRect.bottom)
            crestPath.lineTo(mainPillRect.left, mainPillRect.top)
            crestPath.close()

            canvas.drawPath(crestPath, fillPaint)

            // Draw the glowing blue-white specular crest reflection along that meniscus arc
            fillCrestHighlightPaint.strokeWidth = 2.2f * dp
            val crestShader = LinearGradient(
                fillRight, mainPillRect.top,
                fillRight, mainPillRect.bottom,
                intArrayOf(
                    Color.argb(220, 255, 255, 255), // top crystal specular
                    Color.argb(235, 175, 215, 255), // soft cyan-blue refraction
                    Color.argb(140, 130, 185, 245),
                    Color.argb(30, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            fillCrestHighlightPaint.shader = crestShader

            // Path just for the stroke highlight
            val strokeCrestPath = Path()
            strokeCrestPath.moveTo(fillRight, mainPillRect.top)
            strokeCrestPath.cubicTo(
                fillRight + bulge * 0.65f, mainPillRect.top + pillHeight * 0.25f,
                fillRight + bulge * 0.65f, mainPillRect.bottom - pillHeight * 0.25f,
                fillRight, mainPillRect.bottom
            )
            canvas.drawPath(strokeCrestPath, fillCrestHighlightPaint)

            canvas.restore()
        }

        // C. Upper glass internal lens flare / horizontal highlight
        val upperReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                mainPillRect.left, mainPillRect.top,
                mainPillRect.right, mainPillRect.top,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(45, 255, 255, 255),
                    Color.argb(70, 220, 240, 255),
                    Color.argb(40, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.35f, 0.55f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 2f * dp
        }
        canvas.drawRoundRect(
            mainPillRect.left + 1.5f * dp,
            mainPillRect.top + 1.5f * dp,
            mainPillRect.right - 1.5f * dp,
            mainPillRect.bottom - 1.5f * dp,
            pillRadius - 1.5f * dp,
            pillRadius - 1.5f * dp,
            upperReflectionPaint
        )

        // D. Outer thin reflective white border
        canvas.drawRoundRect(mainPillRect, pillRadius, pillRadius, pillBorderPaint)

        // E. Speaker Icon & Volume Text (Left section)
        val iconSize = (pillHeight * 0.34f).toInt()
        val contentPaddingLeft = mainPillRect.left + (pillHeight * 0.42f)
        val centerY = mainPillRect.centerY()

        speakerDrawable?.let { icon ->
            val iconTop = (centerY - iconSize / 2f).toInt()
            val iconLeft = contentPaddingLeft.toInt()
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            icon.setTint(Color.WHITE)
            icon.draw(canvas)
        }

        // Draw volume number string
        val textString = if (isMuted) "MUTE" else displayedVolume.toString()
        val textX = contentPaddingLeft + iconSize + (14f * dp)
        val textBaseline = centerY - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(textString, textX, textBaseline, textPaint)
    }

    private fun drawCircleButton(canvas: Canvas) {
        val cx = circleRect.centerX()
        val cy = circleRect.centerY()
        val radius = circleSize / 2f

        // A. Background glass disc
        canvas.drawCircle(cx, cy, radius, circleGlassPaint)

        // B. Subtle inset ring (as seen in Apple AirPlay button design)
        canvas.drawCircle(cx, cy, radius * 0.70f, circleInnerRingPaint)

        // C. Outer thin reflective border
        canvas.drawCircle(cx, cy, radius, circleBorderPaint)

        // D. Centered AirPlay / Audio output icon
        val iconSize = (circleSize * 0.42f).toInt()
        outputDrawable?.let { icon ->
            val left = (cx - iconSize / 2f).toInt()
            val top = (cy - iconSize / 2f).toInt()
            icon.setBounds(left, top, left + iconSize, top + iconSize)
            icon.setTint(Color.WHITE)
            icon.draw(canvas)
        }
    }

    /**
     * Updates the current volume with high-framerate fluid animation.
     * @param volume 0 to 100
     * @param maxVolume usually 15 or 100 depending on stream
     * @param animate true for smooth remote steps
     */
    fun setVolume(volume: Int, maxVolume: Int = 100, muted: Boolean = false, animate: Boolean = true) {
        this.isMuted = muted
        val clampedVol = volume.coerceIn(0, maxVolume)
        this.displayedVolume = clampedVol

        val newTarget = if (maxVolume > 0) clampedVol.toFloat() / maxVolume.toFloat() else 0f
        this.targetProgress = newTarget.coerceIn(0f, 1f)

        if (!animate) {
            progressAnimator?.cancel()
            currentProgress = targetProgress
            invalidate()
            return
        }

        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = 180L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { anim ->
                currentProgress = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
