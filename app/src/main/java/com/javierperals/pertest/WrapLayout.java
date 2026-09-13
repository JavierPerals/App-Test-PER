package com.javierperals.pertest;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Wraps selectable chips without horizontal scrolling, also with enlarged text. */
final class WrapLayout extends ViewGroup {
    private final int gap;

    WrapLayout(Context context) {
        super(context);
        gap = Math.round(8 * getResources().getDisplayMetrics().density);
    }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int x = 0, y = getPaddingTop(), row = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (x > 0 && x + child.getMeasuredWidth() > available) {
                x = 0; y += row + gap; row = 0;
            }
            x += child.getMeasuredWidth() + gap;
            row = Math.max(row, child.getMeasuredHeight());
        }
        setMeasuredDimension(resolveSize(width, widthSpec),
                resolveSize(y + row + getPaddingBottom(), heightSpec));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int available = r - l - getPaddingRight();
        int x = getPaddingLeft(), y = getPaddingTop(), row = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int w = child.getMeasuredWidth(), h = child.getMeasuredHeight();
            if (x > getPaddingLeft() && x + w > available) {
                x = getPaddingLeft(); y += row + gap; row = 0;
            }
            child.layout(x, y, x + w, y + h);
            x += w + gap; row = Math.max(row, h);
        }
    }
}
