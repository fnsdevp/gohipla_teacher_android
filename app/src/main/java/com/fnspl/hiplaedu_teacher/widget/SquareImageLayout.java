package com.fnspl.hiplaedu_teacher.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/** A RelativeLayout that will always be square -- same width and height,
 * where the height is based off the width. */
public class SquareImageLayout extends AppCompatImageView {

    public SquareImageLayout(Context context) {
        super(context);
    }

    public SquareImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set a square layout.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }


}
