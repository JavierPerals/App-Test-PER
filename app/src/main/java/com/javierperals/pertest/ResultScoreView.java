package com.javierperals.pertest;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Shared score/animation header; future result screens only supply TestResult. */
public final class ResultScoreView extends LinearLayout {
    private final TextView score;
    private final ResultAnimationView animation;

    public ResultScoreView(Context context, TestResult result, int scoreColor) {
        super(context);
        setTag("result_score");
        setGravity(Gravity.CENTER);
        setPadding(0, dp(8), 0, dp(16));
        score = new TextView(context);
        score.setTag("result_grade");
        score.setText(String.format(Locale.US, "%.1f / 10", result.grade()));
        score.setTextSize(36);
        score.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        score.setTextColor(scoreColor);
        score.setGravity(Gravity.CENTER);
        addView(score);
        animation = new ResultAnimationView(context, result.animationAsset());
        animation.setTag("result_animation");
        // Decorative accompaniment: the numeric grade remains the accessible result.
        animation.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        addView(animation);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int available = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE : Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        int naturalScoreWidth = (int) Math.ceil(score.getPaint().measureText(score.getText().toString()))
                + score.getCompoundPaddingLeft() + score.getCompoundPaddingRight();
        int imageSize = Math.min(dp(88), available);
        boolean horizontal = Math.max(dp(280), naturalScoreWidth + dp(16) + imageSize) <= available;
        setOrientation(horizontal ? HORIZONTAL : VERTICAL);
        score.setLayoutParams(new LayoutParams(horizontal ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        LayoutParams imageParams = new LayoutParams(imageSize, imageSize);
        if (horizontal) imageParams.leftMargin = dp(16);
        else imageParams.topMargin = dp(8);
        animation.setLayoutParams(imageParams);
        super.onMeasure(widthSpec, heightSpec);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
