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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import android.os.Looper;
import android.app.AlertDialog;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
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
            for (String button : new String[]{"Tests por temas", "Simulacro PER", "Salir"})
                assertNotNull(findText(content, button));
            assertTrue(content.findViewWithTag("open_settings").performClick());
            Switch toggle = content.findViewWithTag("night_mode");
            assertFalse(toggle.isChecked());
            toggle.performClick();
            assertTrue(settings.getBoolean("night_mode", false));
            assertEquals(Color.rgb(30, 34, 40), ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            assertTrue(((Switch) content.findViewWithTag("night_mode")).isChecked());
            controller.get().onBackPressed();
            assertNotNull(content.findViewWithTag("open_settings"));
            assertEquals(7, stats.getInt("tests", 0));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            assertEquals(Color.rgb(30, 34, 40), ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            assertTrue(content.findViewWithTag("open_settings").performClick());
            Switch toggle = content.findViewWithTag("night_mode");
            assertTrue(toggle.isChecked());
            toggle.performClick();
            assertFalse(settings.getBoolean("night_mode", true));
            assertEquals(Color.WHITE, ((ColorDrawable) content.findViewWithTag("safe_area").getBackground()).getColor());
            assertEquals(7, stats.getInt("tests", 0));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            assertTrue(content.findViewWithTag("open_settings").performClick());
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

    @Test public void allTopicsAndMockKeepTagsUniqueEvenWithSaturatedHistory() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            List<?> bank = ReflectionHelpers.getField(activity, "bank");
            Map<String, Set<String>> tagsByTopic = new LinkedHashMap<>();
            List<String> allIds = new ArrayList<>();
            for (Object q : bank) {
                tagsByTopic.computeIfAbsent(field(q, "topic"), k -> new HashSet<>()).add(field(q, "tag"));
                allIds.add(field(q, "id"));
            }
            assertEquals(1000, bank.size());
            assertEquals(24, tagsByTopic.get("Elementos de amarre y fondeo").size());
            SharedPreferences prefs = activity.getSharedPreferences("per_stats", 0);
            for (boolean saturated : new boolean[]{false, true}) {
                prefs.edit().putString("recent", saturated ? String.join(",", allIds) : "").commit();
                for (Map.Entry<String, Set<String>> entry : tagsByTopic.entrySet()) {
                    Map<String, Integer> request = new LinkedHashMap<>();
                    request.put(entry.getKey(), entry.getValue().size());
                    for (int repeat = 0; repeat < 10; repeat++) {
                        List<?> selected = pick(activity, request);
                        assertEquals(entry.getValue().size(), selected.size());
                        assertUnique(selected);
                    }
                }
                Map<String, Integer> distribution = ReflectionHelpers.getStaticField(MainActivity.class, "MOCK_DISTRIBUTION");
                for (int repeat = 0; repeat < 50; repeat++) {
                    List<?> selected = pick(activity, distribution);
                    assertEquals(45, selected.size());
                    assertUnique(selected);
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    for (Object q : selected) counts.merge(field(q, "topic"), 1, Integer::sum);
                    assertEquals(distribution, counts);
                }
            }
        }
    }

    @Test public void selectionPrefersUnseenTagsAndUnseenVariants() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Map<String, Integer> request = new LinkedHashMap<>();
            request.put("Elementos de amarre y fondeo", 5);
            List<?> first = pick(activity, request);
            List<String> recent = new ArrayList<>();
            Set<String> usedTags = new HashSet<>();
            for (Object q : first) { recent.add(field(q, "id")); usedTags.add(field(q, "tag")); }
            activity.getSharedPreferences("per_stats", 0).edit().putString("recent", String.join(",", recent)).commit();
            for (Object q : pick(activity, request)) assertFalse(usedTags.contains(field(q, "tag")));
            request.put("Elementos de amarre y fondeo", 24);
            for (Object q : pick(activity, request)) assertFalse(recent.contains(field(q, "id")));
        }
    }

    @Test public void excessiveCountStaysInSetupAndRegenerationKeepsTopicAndCount() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            clickText(content, "Tests por temas");
            List<Spinner> spinners = new ArrayList<>();
            collectSpinners(content, spinners);
            assertEquals(2, spinners.size());
            spinners.get(0).setSelection(1);
            spinners.get(1).setSelection(5);
            clickText(content, "Generar test");
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(Shadows.shadowOf(dialog).getMessage().toString().contains("24"));
            assertNull(content.findViewWithTag("progress"));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            spinners.get(1).setSelection(0);
            clickText(content, "Generar test");
            assertExam(content, 1);
            content.findViewWithTag("answer_0").performClick();
            clickText(content, "Confirmar");
            clickText(content, "Terminar");
            clickText(content, "Generar nuevo test");
            assertExam(content, 1);
            Object exam = ReflectionHelpers.getField(controller.get(), "exam");
            assertEquals("Elementos de amarre y fondeo", field(exam, "topic"));
            assertEquals(Integer.valueOf(1), ReflectionHelpers.getField(exam, "requestedCount"));
        }
    }

    @Test public void homePanelsAlignAndCloseWithBackScrimAndCloseButton() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            ViewGroup content = controller.get().findViewById(android.R.id.content);
            content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, 1080, 1920);
            View left = content.findViewWithTag("open_stats"), right = content.findViewWithTag("open_settings");
            assertEquals(left.getTop(), right.getTop());
            assertEquals(left.getHeight(), right.getHeight());
            assertEquals(left.getWidth(), right.getWidth());
            assertTrue(right.getLeft() > left.getLeft());
            assertNull(findText(content, "Ajustes"));
            for (String opener : new String[]{"open_settings", "open_stats"}) {
                content.findViewWithTag(opener).performClick();
                idleAnimations();
                assertEquals(View.VISIBLE, content.findViewWithTag("drawer_scrim").getVisibility());
                controller.get().onBackPressed();
                idleAnimations();
                assertEquals(View.GONE, content.findViewWithTag("drawer_scrim").getVisibility());
                assertFalse(controller.get().isFinishing());
                content.findViewWithTag(opener).performClick();
                idleAnimations();
                content.findViewWithTag("drawer_scrim").performClick();
                idleAnimations();
                assertEquals(View.GONE, content.findViewWithTag("drawer_scrim").getVisibility());
            }
            right.performClick();
            idleAnimations();
            assertEquals(0f, content.findViewWithTag("settings_drawer").getTranslationX(), 0.1f);
            content.findViewWithTag("close_settings").performClick();
            idleAnimations();
            assertEquals(View.GONE, content.findViewWithTag("settings_drawer").getVisibility());
        }
    }

    @Test public void sharedResultsWorkForTopicMockAndTimeoutWithoutChangingStats() {
        for (boolean mock : new boolean[]{false, true}) {
            for (int percentage : new int[]{0, 50, 80, 100}) {
                try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                    MainActivity activity = controller.get();
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    if (mock) clickText(content, "Simulacro PER");
                    else {
                        clickText(content, "Tests por temas");
                        clickText(content, "Generar test");
                    }
                    Object exam = ReflectionHelpers.getField(activity, "exam");
                    List<?> questions = ReflectionHelpers.getField(exam, "questions");
                    int correct = questions.size() * percentage / 100;
                    int[] selected = ReflectionHelpers.getField(exam, "selected");
                    boolean[] confirmed = ReflectionHelpers.getField(exam, "confirmed");
                    for (int i = 0; i < correct; i++) {
                        selected[i] = ReflectionHelpers.getField(questions.get(i), "correct");
                        confirmed[i] = true;
                    }
                    SharedPreferences stats = activity.getSharedPreferences("per_stats", 0);
                    int previousCorrect = stats.getInt("correct", 0);
                    ReflectionHelpers.callInstanceMethod(activity, "finishExam", ClassParameter.from(boolean.class, mock));
                    assertNotNull(content.findViewWithTag("result_animation"));
                    assertEquals(String.format(java.util.Locale.US, "%.1f / 10", 10.0 * correct / questions.size()),
                            taggedText(content, "result_grade"));
                    assertEquals(previousCorrect + correct, stats.getInt("correct", 0));
                    assertNotNull(findText(content, "Generar nuevo test"));
                    clickText(content, "Inicio");
                    assertNotNull(findText(content, "Tests por temas"));
                }
            }
        }
    }

    private static void idleAnimations() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
    }

    private static String field(Object object, String name) {
        return ReflectionHelpers.getField(object, name);
    }

    private static List<?> pick(MainActivity activity, Map<String, Integer> request) {
        return ReflectionHelpers.callInstanceMethod(activity, "pickQuestions", ClassParameter.from(Map.class, request));
    }

    private static void assertUnique(List<?> selected) {
        Set<String> tags = new HashSet<>();
        for (Object q : selected) assertTrue("Repeated tag: " + field(q, "tag"), tags.add(field(q, "tag")));
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
