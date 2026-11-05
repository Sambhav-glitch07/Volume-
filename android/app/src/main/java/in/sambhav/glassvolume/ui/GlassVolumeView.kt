package `in`.sambhav.glassvolume.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import `in`.sambhav.glassvolume.R

/**
 * 1:1 Exact Recreation of Apple-Style Frosted Translucent Glass Volume HUD.
 * Matches reference photograph:
 *  - Ultra-clear smoky translucent frosted glass base (visible background through glass).
 *  - Frosted translucent dispersion across the unfilled right section.
 *  - Iconic top specular crystal lens reflection bubble with blue-white refraction arc.
 *  - Curved liquid meniscus fill boundary with razor-sharp cyan/white caustic highlight.
 *  - Razor-sharp dual-layer specular glass perimeter borders.
 *  - Pure white speaker icon, crisp volume number, and circular AirPlay glass button.
 */
class GlassVolumeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentProgress: Float = 0.52f
    private var targetProgress: Float = 0.52f
    private var displayedVolume: Int = 52
    private var isMuted: Boolean = false

    private var progressAnimator: ValueAnimator? = null

    // Layout metrics in pixels
    private var pillWidth: Float = 0f
    private var pillHeight: Float = 0f
    private var circleSize: Float = 0f
    private var gap: Float = 0f

    private val mainPillRect = RectF()
    private val circleRect = RectF()
    private val pillClipPath = Path()
    private val meniscusPath = Path()
    private val flareArcPath = Path()

    // Paints
    private val baseGlassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frostSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val meniscusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val crystalFlareFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crystalFlareStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val innerBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Circle output button paints
    private val circleGlassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleFrostSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleInnerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Text & icon
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val speakerDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_speaker)
    private val outputDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_audio_output)

    private val dp: Float = resources.displayMetrics.density

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateLayout(w.toFloat(), h.toFloat())
    }

    private fun recalculateLayout(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return

        gap = 18f * dp
        val totalAvailableWidth = w - paddingLeft.toFloat() - paddingRight.toFloat()
        val totalAvailableHeight = h - paddingTop.toFloat() - paddingBottom.toFloat()

        val compHeight = totalAvailableHeight.coerceAtMost(128f * dp)
        pillHeight = compHeight
        circleSize = compHeight

        pillWidth = (totalAvailableWidth - circleSize - gap).coerceAtLeast(300f * dp)

        val startX = paddingLeft.toFloat()
        val startY = (h - compHeight) / 2f

        mainPillRect.set(startX, startY, startX + pillWidth, startY + pillHeight)

        val circleLeft = startX + pillWidth + gap
        circleRect.set(circleLeft, startY, circleLeft + circleSize, startY + circleSize)

        val pillCornerRadius = pillHeight / 2f
        pillClipPath.reset()
        pillClipPath.addRoundRect(mainPillRect, pillCornerRadius, pillCornerRadius, Path.Direction.CW)

        // 1. Base Dark Translucent Smoky Glass (transparent to show background through it)
        baseGlassPaint.shader = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.left, mainPillRect.bottom,
            intArrayOf(
                Color.argb(105, 38, 44, 54),  // top translucent smoky tint
                Color.argb(135, 18, 22, 28),  // center dark body
                Color.argb(155, 10, 12, 16)   // bottom depth
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 2. Unfilled section frosted dispersion sheen (light frosty gradient across the glass)
        frostSheenPaint.shader = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.right, mainPillRect.bottom,
            intArrayOf(
                Color.argb(45, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Color.argb(75, 255, 255, 255),
                Color.argb(25, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.4f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )

        // 3. Razor-Sharp Outer Specular White Glass Rim (brightest at top light source)
        outerBorderPaint.strokeWidth = 1.35f * dp
        outerBorderPaint.shader = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.left, mainPillRect.bottom,
            intArrayOf(
                Color.argb(225, 255, 255, 255), // brilliant pure white reflection at top
                Color.argb(110, 255, 255, 255), // mid transition
                Color.argb(65, 255, 255, 255)   // soft bottom ground reflection
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        // 4. Subtle Inner Fresnel Reflection Bevel
        innerBevelPaint.strokeWidth = 1f * dp
        innerBevelPaint.shader = LinearGradient(
            mainPillRect.left, mainPillRect.top,
            mainPillRect.left, mainPillRect.bottom,
            intArrayOf(
                Color.argb(80, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.25f, 1f),
            Shader.TileMode.CLAMP
        )

        // 5. Circle output button paints
        circleGlassPaint.shader = LinearGradient(
            circleRect.left, circleRect.top,
            circleRect.left, circleRect.bottom,
            intArrayOf(
                Color.argb(110, 42, 48, 58),
                Color.argb(140, 20, 24, 30),
                Color.argb(160, 12, 14, 18)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        circleFrostSheenPaint.shader = LinearGradient(
            circleRect.left, circleRect.top,
            circleRect.right, circleRect.bottom,
            intArrayOf(
                Color.argb(50, 255, 255, 255),
                Color.argb(15, 255, 255, 255),
                Color.argb(40, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        circleInnerDiscPaint.color = Color.argb(65, 0, 0, 0)
        circleInnerBorderPaint.strokeWidth = 1f * dp
        circleInnerBorderPaint.color = Color.argb(45, 255, 255, 255)

        circleBorderPaint.strokeWidth = 1.35f * dp
        circleBorderPaint.shader = outerBorderPaint.shader

        textPaint.textSize = (pillHeight * 0.28f).coerceIn(28f * dp, 38f * dp)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pillWidth <= 0f || pillHeight <= 0f) return

        // 1. Draw Main Capsule Glass Slider
        drawMainPill(canvas)

        // 2. Draw Separate Circular AirPlay Glass Button
        drawCircleButton(canvas)
    }

    private fun drawMainPill(canvas: Canvas) {
        val pillRadius = pillHeight / 2f

        // A. Base Translucent Smoky Glass Body
        canvas.drawRoundRect(mainPillRect, pillRadius, pillRadius, baseGlassPaint)

        // B. Frosted Glass Ambient Sheen
        canvas.drawRoundRect(mainPillRect, pillRadius, pillRadius, frostSheenPaint)

        // C. Active Volume Fill with Curved Meniscus & Crystal Specular Flare
        if (currentProgress > 0.005f) {
            canvas.save()
            canvas.clipPath(pillClipPath)

            val fillRight = mainPillRect.left + (mainPillRect.width() * currentProgress)

            // Fill body gradient: darker translucent glass refraction
            fillBodyPaint.shader = LinearGradient(
                mainPillRect.left, mainPillRect.top,
                mainPillRect.left, mainPillRect.bottom,
                intArrayOf(
                    Color.argb(120, 50, 60, 75),
                    Color.argb(85, 30, 36, 48),
                    Color.argb(60, 15, 18, 25)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

            // Draw base rectangular block of fill
            canvas.drawRect(
                mainPillRect.left,
                mainPillRect.top,
                fillRight,
                mainPillRect.bottom,
                fillBodyPaint
            )

            // Draw Curved Meniscus Arc
            val meniscusBulge = (pillHeight * 0.38f).coerceAtMost(fillRight - mainPillRect.left)
            meniscusPath.reset()
            meniscusPath.moveTo(fillRight, mainPillRect.top)
            meniscusPath.cubicTo(
                fillRight + meniscusBulge * 0.62f, mainPillRect.top + pillHeight * 0.25f,
                fillRight + meniscusBulge * 0.62f, mainPillRect.bottom - pillHeight * 0.25f,
                fillRight, mainPillRect.bottom
            )
            meniscusPath.lineTo(mainPillRect.left, mainPillRect.bottom)
            meniscusPath.lineTo(mainPillRect.left, mainPillRect.top)
            meniscusPath.close()

            canvas.drawPath(meniscusPath, fillBodyPaint)

            // Meniscus Leading Edge Caustic Highlight (Curved Razor-Sharp Specular Stroke)
            meniscusStrokePaint.strokeWidth = 2f * dp
            meniscusStrokePaint.shader = LinearGradient(
                fillRight, mainPillRect.top,
                fillRight, mainPillRect.bottom,
                intArrayOf(
                    Color.argb(255, 255, 255, 255), // bright white top specular
                    Color.argb(240, 140, 205, 255), // luminous cyan reflection
                    Color.argb(180, 75, 160, 255),  // crystal blue dispersion
                    Color.argb(40, 255, 255, 255)   // fade into bottom
                ),
                floatArrayOf(0f, 0.25f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )

            val meniscusStrokePath = Path()
            meniscusStrokePath.moveTo(fillRight, mainPillRect.top)
            meniscusStrokePath.cubicTo(
                fillRight + meniscusBulge * 0.62f, mainPillRect.top + pillHeight * 0.25f,
                fillRight + meniscusBulge * 0.62f, mainPillRect.bottom - pillHeight * 0.25f,
                fillRight, mainPillRect.bottom
            )
            canvas.drawPath(meniscusStrokePath, meniscusStrokePaint)

            // D. THE ICONIC TOP SPECULAR CRYSTAL FLARE / REFRACTION BUBBLE
            // (As clearly seen in reference image right at the top crest!)
            val flareRadiusX = 26f * dp
            val flareRadiusY = 16f * dp
            val flareCenterX = fillRight - (4f * dp)
            val flareCenterY = mainPillRect.top + (4f * dp)

            crystalFlareFillPaint.shader = RadialGradient(
                flareCenterX, flareCenterY,
                flareRadiusX,
                intArrayOf(
                    Color.argb(235, 255, 255, 255), // brilliant pure white core
                    Color.argb(195, 215, 240, 255), // soft milky ice flare
                    Color.argb(120, 130, 200, 255), // cyan refraction rim
                    Color.argb(0, 130, 200, 255)    // transparent falloff
                ),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )

            val flareRect = RectF(
                flareCenterX - flareRadiusX,
                flareCenterY - flareRadiusY,
                flareCenterX + flareRadiusX,
                flareCenterY + flareRadiusY
            )
            canvas.drawOval(flareRect, crystalFlareFillPaint)

            // Glowing cyan-blue rim arc along the flare perimeter
            crystalFlareStrokePaint.strokeWidth = 1.6f * dp
            crystalFlareStrokePaint.shader = LinearGradient(
                flareRect.left, flareRect.top,
                flareRect.right, flareRect.bottom,
                intArrayOf(
                    Color.argb(255, 255, 255, 255),
                    Color.argb(230, 90, 185, 255),
                    Color.argb(160, 60, 150, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )

            flareArcPath.reset()
            flareArcPath.arcTo(flareRect, 0f, 180f)
            canvas.drawPath(flareArcPath, crystalFlareStrokePaint)

            canvas.restore()
        }

        // E. Top Horizontal Specular Lens Reflection Arc (Upper edge of capsule)
        val upperGlassReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                mainPillRect.left, mainPillRect.top,
                mainPillRect.right, mainPillRect.top,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(65, 255, 255, 255),
                    Color.argb(125, 255, 255, 255),
                    Color.argb(175, 230, 245, 255),
                    Color.argb(110, 255, 255, 255),
                    Color.argb(45, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.25f, 0.45f, 0.55f, 0.65f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * dp
        }
        canvas.drawRoundRect(
            mainPillRect.left + 1.2f * dp,
            mainPillRect.top + 1.2f * dp,
            mainPillRect.right - 1.2f * dp,
            mainPillRect.bottom - 1.2f * dp,
            pillRadius - 1.2f * dp,
            pillRadius - 1.2f * dp,
            upperGlassReflectionPaint
        )

        // F. Inner Fresnel Bevel
        canvas.drawRoundRect(
            mainPillRect.left + 1f * dp,
            mainPillRect.top + 1f * dp,
            mainPillRect.right - 1f * dp,
            mainPillRect.bottom - 1f * dp,
            pillRadius - 1f * dp,
            pillRadius - 1f * dp,
            innerBevelPaint
        )

        // G. Razor-Sharp Outer Specular Glass Rim
        canvas.drawRoundRect(mainPillRect, pillRadius, pillRadius, outerBorderPaint)

        // H. Speaker Icon & Volume Text (Left section)
        val iconSize = (pillHeight * 0.34f).toInt()
        val contentPaddingLeft = mainPillRect.left + (pillHeight * 0.42f)
        val centerY = mainPillRect.centerY()

        speakerDrawable?.let { icon: Drawable ->
            val iconTop = (centerY - iconSize / 2f).toInt()
            val iconLeft = contentPaddingLeft.toInt()
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            icon.setTint(Color.WHITE)
            icon.draw(canvas)
        }

        val textString = if (isMuted) "MUTE" else displayedVolume.toString()
        val textX = contentPaddingLeft + iconSize.toFloat() + (14f * dp)
        val textBaseline = centerY - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(textString, textX, textBaseline, textPaint)
    }

    private fun drawCircleButton(canvas: Canvas) {
        val cx = circleRect.centerX()
        val cy = circleRect.centerY()
        val radius = circleSize / 2f

        // A. Base Translucent Smoky Glass Disc
        canvas.drawCircle(cx, cy, radius, circleGlassPaint)

        // B. Frosted Glass Sheen
        canvas.drawCircle(cx, cy, radius, circleFrostSheenPaint)

        // C. Concentric Inset Darker Glass Disc
        val innerRadius = radius * 0.70f
        canvas.drawCircle(cx, cy, innerRadius, circleInnerDiscPaint)
        canvas.drawCircle(cx, cy, innerRadius, circleInnerBorderPaint)

        // D. Top Specular Highlight Arc on Circle
        val circleUpperReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - innerRadius, cy - radius,
                cx + innerRadius, cy - radius,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(140, 255, 255, 255),
                    Color.argb(195, 255, 255, 255),
                    Color.argb(140, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * dp
        }
        val circleInnerRect = RectF(
            circleRect.left + 1.2f * dp,
            circleRect.top + 1.2f * dp,
            circleRect.right - 1.2f * dp,
            circleRect.bottom - 1.2f * dp
        )
        canvas.drawArc(circleInnerRect, 200f, 140f, false, circleUpperReflectionPaint)

        // E. Razor-Sharp Outer Specular Glass Rim
        canvas.drawCircle(cx, cy, radius, circleBorderPaint)

        // F. Centered AirPlay / Audio Output Icon
        val iconSize = (circleSize * 0.42f).toInt()
        outputDrawable?.let { icon: Drawable ->
            val left = (cx - iconSize / 2f).toInt()
            val top = (cy - iconSize / 2f).toInt()
            icon.setBounds(left, top, left + iconSize, top + iconSize)
            icon.setTint(Color.WHITE)
            icon.draw(canvas)
        }
    }

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
        val animator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = 180L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { anim: ValueAnimator ->
                currentProgress = (anim.animatedValue as? Float) ?: targetProgress
                invalidate()
            }
        }
        progressAnimator = animator
        animator.start()
    }
}
