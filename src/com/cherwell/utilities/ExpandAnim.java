package com.cherwell.utilities;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ExpandAnim extends Animation {
    int targetWidth;
    View view;
    
    public ExpandAnim(View view, int targetWidth) {
        this.view = view;
        this.targetWidth = targetWidth;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.getLayoutParams().width = (int) (targetWidth * interpolatedTime);
        view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth,
            int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}