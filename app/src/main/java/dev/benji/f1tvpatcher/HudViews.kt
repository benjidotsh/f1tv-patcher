package dev.benji.f1tvpatcher

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.DrawableRes

object HudPalette {
    const val bg = 0xFF050507.toInt()
    const val border = 0xFF1A1A20.toInt()
    const val text = 0xFFF4F4F4.toInt()
    const val textDim = 0xFF888888.toInt()
    const val textSubtle = 0xFF4A4A50.toInt()
    const val red = 0xFFE10600.toInt()
    const val cyan = 0xFF00D9FF.toInt()
    const val amber = 0xFFFFAB00.toInt()
    const val keyResting = 0x14FFFFFF
    const val keyFocused = 0x26FFFFFF
    const val keyBorder = 0xFF2A2A30.toInt()
    const val keyText = 0xFFC8C8C8.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

private fun focusStateListDrawable(makeBg: (Boolean) -> GradientDrawable): StateListDrawable =
    StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), makeBg(true))
        addState(intArrayOf(android.R.attr.state_pressed), makeBg(true))
        addState(intArrayOf(), makeBg(false))
    }

object HudTypeface {
    val mono: Typeface = Typeface.MONOSPACE
    val display: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
}

class HudBackgroundDrawable : Drawable() {
    private val bgPaint = Paint().apply { color = HudPalette.bg }
    private val gridPaint = Paint().apply {
        color = 0x14FFFFFF
        strokeWidth = 1f
    }
    private val scanPaint = Paint().apply { color = 0x09FFFFFF }
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridStep = 64f

    override fun onBoundsChange(b: Rect) {
        super.onBoundsChange(b)
        val r = maxOf(b.width(), b.height()) * 0.75f
        vignettePaint.shader = RadialGradient(
            b.exactCenterX(), b.exactCenterY(), r,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, 0xCC000000.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawRect(b, bgPaint)
        var x = b.left.toFloat()
        while (x <= b.right) {
            canvas.drawLine(x, b.top.toFloat(), x, b.bottom.toFloat(), gridPaint)
            x += gridStep
        }
        var y = b.top.toFloat()
        while (y <= b.bottom) {
            canvas.drawLine(b.left.toFloat(), y, b.right.toFloat(), y, gridPaint)
            y += gridStep
        }
        var ys = b.top.toFloat()
        while (ys < b.bottom) {
            canvas.drawRect(b.left.toFloat(), ys, b.right.toFloat(), ys + 1f, scanPaint)
            ys += 4f
        }
        canvas.drawRect(b, vignettePaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.OPAQUE"))
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

class HudStatusDot @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animator = ValueAnimator.ofFloat(0.35f, 1f).apply {
        duration = 1500L
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidate() }
    }

    var dotColor: Int = HudPalette.red
        set(value) { field = value; invalidate() }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val alpha = (animator.animatedValue as? Float) ?: 1f
        paint.color = dotColor
        paint.alpha = (alpha * 255).toInt()
        paint.setShadowLayer(8f, 0f, 0f, dotColor)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, minOf(cx, cy) * 0.55f, paint)
    }
}

fun hudKeyButton(
    context: Context,
    label: String,
    primary: Boolean,
    compact: Boolean = false,
): Button {
    val bg = focusStateListDrawable { focused ->
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(
                when {
                    primary -> HudPalette.red
                    focused -> HudPalette.keyFocused
                    else -> HudPalette.keyResting
                }
            )
            setStroke(
                context.dp(if (focused || primary) 2 else 1),
                when {
                    primary -> if (focused) Color.WHITE else HudPalette.red
                    focused -> Color.WHITE
                    else -> HudPalette.keyBorder
                }
            )
        }
    }

    return Button(context).apply {
        text = if (compact) "[$label]" else "[ $label ]"
        background = bg
        typeface = HudTypeface.mono
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        isAllCaps = true
        letterSpacing = 0.12f
        setTextColor(if (primary) Color.WHITE else HudPalette.keyText)
        stateListAnimator = null
        val horizontalPadding = context.dp(if (compact) 12 else 20)
        setPadding(horizontalPadding, context.dp(13), horizontalPadding, context.dp(13))
        if (primary) elevation = context.dp(4).toFloat()
        minWidth = 0
        minHeight = 0
    }
}

fun hudIconButton(context: Context, @DrawableRes drawableRes: Int): ImageButton {
    val bg = focusStateListDrawable { focused ->
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (focused) HudPalette.keyFocused else HudPalette.keyResting)
            setStroke(
                context.dp(if (focused) 2 else 1),
                if (focused) Color.WHITE else HudPalette.keyBorder,
            )
        }
    }

    return ImageButton(context).apply {
        background = bg
        setImageResource(drawableRes)
        imageTintList = ColorStateList.valueOf(HudPalette.keyText)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        stateListAnimator = null
        setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
        minimumWidth = 0
        minimumHeight = 0
    }
}
