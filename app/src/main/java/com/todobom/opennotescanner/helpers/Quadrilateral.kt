package com.todobom.opennotescanner.helpers

import org.opencv.core.MatOfPoint
import org.opencv.core.Point

class Quadrilateral(val contour: MatOfPoint, val points: Array<Point>)