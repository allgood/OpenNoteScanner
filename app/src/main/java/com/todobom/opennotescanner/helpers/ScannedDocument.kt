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
        if (processed != null) {
            processed!!.release()
        }

        original.release()

        if (quadrilateral != null) {
            quadrilateral!!.contour.release()
        }
    }
}