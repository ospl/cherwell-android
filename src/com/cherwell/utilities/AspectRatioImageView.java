package com.cherwell.utilities;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AspectRatioImageView extends ImageView {

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    	
    	if (widthMode != MeasureSpec.AT_MOST) {
	        int width = MeasureSpec.getSize(widthMeasureSpec);
	        int height = width * getDrawable().getIntrinsicHeight() / getDrawable().getIntrinsicWidth();
	        setMeasuredDimension(width, height);
    	} else {
    		int height = MeasureSpec.getSize(heightMeasureSpec);
    		int width = height * getDrawable().getIntrinsicWidth() / getDrawable().getIntrinsicHeight();
    		setMeasuredDimension(width, height);
    	}
    }
}
