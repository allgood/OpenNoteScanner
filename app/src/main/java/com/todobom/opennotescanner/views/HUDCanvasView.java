package com.todobom.opennotescanner.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Draw an array of shapes on a canvas
 *
 * @author <Claudemir Todo Bom> http://todobom.com
 */
public class HUDCanvasView extends View {

    private ArrayList<HUDShape> shapes = new ArrayList<>();

    public HUDCanvasView(Context context) {
        super(context);
    }

    public HUDCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HUDCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public class HUDShape {
        private final Shape mShape;
        private final Paint mPaint;
        private final Paint mBorder;

        public HUDShape( Shape shape , Paint paint ) {
            mShape = shape;
            mPaint = paint;
            mBorder = null;
        }

        public HUDShape( Shape shape , Paint paint , Paint border ) {
            mShape = shape;
            mPaint = paint;
            mBorder = border;
            mBorder.setStyle(Paint.Style.STROKE);
        }

        public void draw ( Canvas canvas ) {
            mShape.draw(canvas,mPaint);

            if (mBorder != null) {
                mShape.draw(canvas,mBorder);
            }
        }

        public Shape getShape() {
            return mShape;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        for ( HUDShape s: shapes ) {
            s.getShape().resize(contentWidth, contentHeight);
            s.draw(canvas);
        }

    }

    public HUDShape addShape(Shape shape , Paint paint ) {
        HUDShape hudShape = new HUDShape(shape, paint);
        shapes.add( hudShape );
        return hudShape;
    }

    public HUDShape addShape(Shape shape , Paint paint , Paint border ) {
        HUDShape hudShape = new HUDShape(shape, paint , border );
        shapes.add(hudShape);
        return hudShape;
    }

    public void removeShape(HUDShape shape) {
        shapes.remove(shape);
    }

    public void removeShape(int index) {
        shapes.remove(index);
    }

    public void clear() {
        shapes.clear();
    }

}
