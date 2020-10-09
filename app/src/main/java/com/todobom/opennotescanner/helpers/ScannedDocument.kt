package com.todobom.opennotescanner.helpers

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size

class ScannedDocument(val original: Mat) {
    var processed: Mat? = null
    var quadrilateral: Quadrilateral? = null
    var previewPoints: Array<Point>? = null
    var previewSize: Size? = null

    fun release() {
        processed?.release()
        original.release()
        quadrilateral?.contour?.release()
    }
}