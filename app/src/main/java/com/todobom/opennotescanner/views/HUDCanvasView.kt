package com.todobom.opennotescanner.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.view.View
import java.util.*

/**
 * Draw an array of shapes on a canvas
 *
 * @author <Claudemir Todo Bom> http://todobom.com
</Claudemir> */
class HUDCanvasView : View {
    private val shapes = ArrayList<HUDShape>()

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

    inner class HUDShape(val shape: Shape, private val mPaint: Paint, private val mBorder: Paint?) {
        fun draw(canvas: Canvas?) {
            shape.draw(canvas, mPaint)
            if (mBorder != null) {
                shape.draw(canvas, mBorder)
            }
        }

        init {
            mBorder!!.style = Paint.Style.STROKE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        for (s in shapes) {
            s.shape.resize(contentWidth.toFloat(), contentHeight.toFloat())
            s.draw(canvas)
        }
    }

    fun addShape(shape: Shape, paint: Paint, border: Paint?) {
        val hudShape = HUDShape(shape, paint, border)
        shapes.add(hudShape)
    }

    fun clear() {
        shapes.clear()
    }
}