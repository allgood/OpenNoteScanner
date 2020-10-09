package com.todobom.opennotescanner.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.max

/**
 * Based on RectangleView from
 * @author Aidan Follestad (afollestad)
 */
class SquareFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val squareSpec = max(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(squareSpec, squareSpec)
    }
}