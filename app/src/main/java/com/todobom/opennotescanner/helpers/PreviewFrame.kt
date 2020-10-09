package com.todobom.opennotescanner.helpers

import org.opencv.core.Mat

data class PreviewFrame(val frame: Mat, val isAutoMode: Boolean, val isPreviewOnly: Boolean)