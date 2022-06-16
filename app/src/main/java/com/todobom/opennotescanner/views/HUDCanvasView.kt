package com.todobom.opennotescanner.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.view.View

/**
 * Draw an array of shapes on a canvas
 *
 * @author <Claudemir Todo Bom> http://todobom.com
</Claudemir> */
class HUDCanvasView : View {
    private var detectedShape: HUDShape? = null
    private var documentBoxShape: HUDShape? = null
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
        if (documentBoxShape != null) {
            documentBoxShape!!.shape.resize(contentWidth.toFloat(), contentHeight.toFloat())
            documentBoxShape!!.draw(canvas)
        }
        if (detectedShape != null) {
            detectedShape!!.shape.resize(contentWidth.toFloat(), contentHeight.toFloat())
            detectedShape!!.draw(canvas)
        }
    }

    fun setDetectedShape(shape: Shape, paint: Paint, border: Paint?) {
        detectedShape = HUDShape(shape, paint, border)
    }

    fun setDocumentBoxShape(shape: Shape?, paint: Paint?, border: Paint?) {
        if (shape == null) {
            documentBoxShape = null;
            return;
        }
        documentBoxShape = HUDShape(shape, paint!!, border)
    }

    fun clear() {
        detectedShape = null
    }
}