package com.todobom.opennotescanner.helpers;

import org.opencv.core.Mat;

/**
 * Created by allgood on 06/03/16.
 */
public class PreviewFrame {

    private Mat frame;
    private boolean autoMode;

    public PreviewFrame( Mat frame , boolean autoMode ) {
        this.frame = frame;
        this.autoMode = autoMode;
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }
}
