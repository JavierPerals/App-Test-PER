package com.javierperals.pertest;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Switch;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowInsets;
import android.content.SharedPreferences;
import org.robolectric.RuntimeEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class MainActivityTest {
    @Test public void settingsSaveBothModesAcrossActivityRestartsWithoutChangingStats() {
        SharedPreferences settings = RuntimeEnvironment.getApplication().getSharedPreferences("per_settings", 0);
        SharedPreferences stats = RuntimeEnvironment.getApplication().getSharedPreferences("per_stats", 0);
        settings.edit().clear().commit();
        stats.edit().putInt("tests", 7).commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            for (String button : new String[]{"Tests por temas", "Simulacro PER", "Ajustes", "Salir"})
                assertNotNull(findText(content, button));
            clickText(content, "Ajustes");
            Switch toggle = content.findViewWithTag("night_mode");
            assertFalse(toggle.isChecked());
            toggle.performClick();
            assertTrue(settings.getBoolean("night_mode", false));
            assertEquals(Color.rgb(30, 34, 40), ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            assertTrue(((Switch) content.findViewWithTag("night_mode")).isChecked());
            controller.get().onBackPressed();
            assertNotNull(findText(content, "Ajustes"));
            assertEquals(7, stats.getInt("tests", 0));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            assertEquals(Color.rgb(30, 34, 40), ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            clickText(content, "Ajustes");
            Switch toggle = content.findViewWithTag("night_mode");
            assertTrue(toggle.isChecked());
            toggle.performClick();
            assertFalse(settings.getBoolean("night_mode", true));
            assertEquals(Color.WHITE, ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            assertEquals(7, stats.getInt("tests", 0));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Ajustes");
            assertFalse(((Switch) content.findViewWithTag("night_mode")).isChecked());
        }
    }

    @Test public void nightModePreservesTopicMockAndResultsFlows() {
        SharedPreferences settings = RuntimeEnvironment.getApplication().getSharedPreferences("per_settings", 0);
        settings.edit().putBoolean("night_mode", true).commit();
        try {
            generateTopicExamDisplaysFirstQuestionAndNavigation();
            singleQuestionExamFinishesAndGeneratesAnotherExam();
            mockExamDisplaysFortyFiveQuestionsAndCountdown();
        } finally {
            settings.edit().clear().commit();
        }
    }

    @Test public void examHeaderKeepsSystemBarSpaceWithoutAccumulatingInsets() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Simulacro PER");
            View safeArea = content.findViewWithTag("safe_area");
            WindowInsets insets = android.os.Build.VERSION.SDK_INT >= 30
                    ? new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),
                            android.graphics.Insets.of(0, 32, 0, 24)).build()
                    : org.robolectric.util.ReflectionHelpers.callConstructor(WindowInsets.class,
                            org.robolectric.util.ReflectionHelpers.ClassParameter.from(Rect.class, new Rect(0, 32, 0, 24)));
            safeArea.dispatchApplyWindowInsets(insets);
            safeArea.dispatchApplyWindowInsets(insets);
            assertEquals(32, safeArea.getPaddingTop());
            assertEquals(24, safeArea.getPaddingBottom());
            float density = content.getResources().getDisplayMetrics().density;
            for (int widthDp : new int[]{360, 412}) {
                int width = Math.round(widthDp * density);
                int height = Math.round(720 * density);
                content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                content.layout(0, 0, width, height);
                View header = content.findViewWithTag("exam_header");
                View close = content.findViewWithTag("exit_exam");
                assertEquals(Math.round(24 * density), header.getPaddingTop());
                assertTrue(close.getHeight() >= Math.round(48 * density));
                assertTrue(close.getWidth() >= Math.round(48 * density));
                assertTrue(content.findViewWithTag("next").getHeight() > 0);
                TextView timer = content.findViewWithTag("timer");
                assertTrue(timer.getWidth() >= timer.getPaint().measureText("90 : 00"));
            }
        }
    }

    @Test public void generateTopicExamDisplaysFirstQuestionAndNavigation() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Tests por temas");
            clickText(content, "Generar test");
            assertExam(content, 10);
            assertFalse(content.findViewWithTag("previous").isEnabled());
            assertFalse(content.findViewWithTag("confirm").isEnabled());
            assertTrue(content.findViewWithTag("answer_0").performClick());
            assertTrue(content.findViewWithTag("confirm").isEnabled());
            clickText(content, "Confirmar");
            assertFalse(content.findViewWithTag("confirm").isEnabled());
            clickText(content, "Siguiente");
            assertEquals("Pregunta 2 / 10", taggedText(content, "progress"));
            assertTrue(content.findViewWithTag("previous").isEnabled());
            clickText(content, "Anterior");
            assertEquals("Pregunta 1 / 10", taggedText(content, "progress"));
            assertFalse(content.findViewWithTag("confirm").isEnabled());
        }
    }

    @Test public void singleQuestionExamFinishesAndGeneratesAnotherExam() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Tests por temas");
            List<Spinner> spinners = new ArrayList<>();
            collectSpinners(content, spinners);
            spinners.get(1).setSelection(0);
            clickText(content, "Generar test");
            assertExam(content, 1);
            assertEquals("Terminar", taggedText(content, "next"));
            assertTrue(content.findViewWithTag("answer_1").performClick());
            clickText(content, "Confirmar");
            clickText(content, "Terminar");
            assertNotNull(findText(content, "Generar nuevo test"));
            clickText(content, "Generar nuevo test");
            assertExam(content, 1);
        }
    }

    @Test public void mockExamDisplaysFortyFiveQuestionsAndCountdown() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Simulacro PER");
            assertExam(content, 45);
            assertEquals("90 : 00", taggedText(content, "timer"));
            clickText(content, "Siguiente");
            assertEquals("Pregunta 2 / 45", taggedText(content, "progress"));
        }
    }

    private static void assertExam(ViewGroup content, int count) {
        assertEquals("Pregunta 1 / " + count, taggedText(content, "progress"));
        for (int i = 0; i < 4; i++) assertNotNull(content.findViewWithTag("answer_" + i));
        for (String tag : new String[]{"previous", "confirm", "next"}) {
            View button = content.findViewWithTag(tag);
            assertTrue("Missing navigation button: " + tag, button instanceof Button);
            assertNotNull(button.getParent());
        }
        // Exercise Android measurement/layout as well as screen construction.
        content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, 1080, 1920);
        assertTrue(content.findViewWithTag("next").getHeight() > 0);
    }

    private static String taggedText(View root, String tag) {
        TextView view = root.findViewWithTag(tag);
        assertNotNull("Missing view: " + tag, view);
        return view.getText().toString();
    }

    private static void collectSpinners(View view, List<Spinner> out) {
        if (view instanceof Spinner) out.add((Spinner) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectSpinners(group.getChildAt(i), out);
        }
    }

    private static void clickText(View view, String text) {
        View target = findText(view, text);
        assertNotNull("Missing button: " + text, target);
        assertTrue(target.performClick());
    }

    private static View findText(View view, String text) {
        if (view instanceof TextView && view.isClickable() && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findText(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
