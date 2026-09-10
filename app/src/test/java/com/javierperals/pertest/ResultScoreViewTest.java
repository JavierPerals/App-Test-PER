package com.javierperals.pertest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.webkit.WebView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 28, 35})
public class ResultScoreViewTest {
    @Test public void headerAdaptsToWidthAndPreservesImageAspectRatio() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            ResultScoreView header = new ResultScoreView(activity, new TestResult(45,45), Color.BLUE);
            activity.setContentView(header);
            float density = activity.getResources().getDisplayMetrics().density;
            for (int widthDp : new int[]{180, 280, 360, 600}) {
                int width = Math.round(widthDp * density);
                header.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                header.layout(0, 0, width, header.getMeasuredHeight());
                View score = header.findViewWithTag("result_grade");
                ResultAnimationView animation = header.findViewWithTag("result_animation");
                assertTrue(score.getLeft() >= 0 && score.getRight() <= width);
                assertTrue(animation.getLeft() >= 0 && animation.getRight() <= width);
                assertEquals(animation.getWidth(), animation.getHeight());
                if (widthDp == 180) assertEquals(LinearLayout.VERTICAL, header.getOrientation());
                if (widthDp == 600) assertEquals(LinearLayout.HORIZONTAL, header.getOrientation());
                if (header.getOrientation() == LinearLayout.HORIZONTAL)
                    assertTrue(animation.getLeft() > score.getRight());
                else assertTrue(animation.getTop() > score.getBottom());

            }
        }
    }

    @Test public void allAssetsStartStopAndResumeWithoutHoldingOldScreen() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            for (int correct : new int[]{0, 50, 80}) {
                ResultScoreView header = new ResultScoreView(activity, new TestResult(correct,100), Color.BLUE);
                activity.setContentView(header);
                ResultAnimationView animation = header.findViewWithTag("result_animation");
                    WebView web = ReflectionHelpers.getField(animation, "webView");
                    assertNotNull(web);
                    assertFalse(web.getSettings().getJavaScriptEnabled());
                    assertTrue(web.getSettings().getBlockNetworkLoads());
                    String html = Shadows.shadowOf(web).getLastLoadDataWithBaseURL().data;
                    assertTrue(html.contains("object-fit:contain"));
                    assertTrue(html.contains("data:image/webp;base64,"));
                    header.setVisibility(View.GONE);
                    assertFalse((Boolean) ReflectionHelpers.getField(animation, "active"));
                    header.setVisibility(View.VISIBLE);
                    assertTrue((Boolean) ReflectionHelpers.getField(animation, "active"));
                    activity.setContentView(new View(activity));
                    assertNull(ReflectionHelpers.getField(animation, "webView"));
            }
        }
    }

    @Test public void fallbackOverridesFiniteLoopWithoutChangingFrames() {
        byte[] bytes = new byte[26];
        bytes[12]='A'; bytes[13]='N'; bytes[14]='I'; bytes[15]='M'; bytes[16]=6;
        bytes[24]=3;
        ResultAnimationView.makeLoopInfinite(bytes);
        assertEquals(0, bytes[24]);
        assertEquals(0, bytes[25]);
        ResultAnimationView.makeLoopInfinite(new byte[0]);
    }
}
