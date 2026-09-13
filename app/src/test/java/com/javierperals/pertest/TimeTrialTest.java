package com.javierperals.pertest;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowSystemClock;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class TimeTrialTest {
    @Test public void setupModalSelectAllRemoveCancelAndDurationsInBothThemes() {
        for (boolean night : new boolean[]{false, true}) {
            try (ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class)) {
                MainActivity a = c.get();
                a.getSharedPreferences("per_settings", 0).edit().putBoolean("night_mode", night).commit();
                c.setup();
                clickText(a, "Contrarreloj");
                assertTrue(tag(a, "minutes_3").isSelected());
                tag(a, "generate_trial").performClick();
                assertNull(exam(a));
                tag(a, "add_topics").performClick();
                AlertDialog d = topicDialog();
                assertEquals(12, d.getListView().getCount());
                toggle(d, 0);
                for (int i = 0; i < 12; i++) assertTrue(d.getListView().isItemChecked(i));
                toggle(d, 0);
                for (int i = 0; i < 12; i++) assertFalse(d.getListView().isItemChecked(i));
                toggle(d, 6); // RIPA
                toggle(d, 5); // Balizamiento
                d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(2, topics(a).size());
                tag(a, "remove_topic_RIPA").performClick();
                assertEquals(new LinkedHashSet<>(Arrays.asList("Balizamiento")), topics(a));
                tag(a, "add_topics").performClick();
                d = topicDialog();
                assertTrue(d.getListView().isItemChecked(5));
                assertFalse(d.getListView().isItemChecked(6));
                toggle(d, 0);
                d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(1, topics(a).size());
                for (int min : new int[]{1,3,5,10}) {
                    tag(a, "minutes_" + min).performClick();
                    for (int other : new int[]{1,3,5,10})
                        assertEquals(other == min, tag(a, "minutes_" + other).isSelected());
                }
                tag(a, "add_topics").performClick();
                d = topicDialog();
                toggle(d, 0);
                d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(11, topics(a).size());
                View content = a.findViewById(android.R.id.content);
                for (int width : new int[]{320,480,1080}) {
                    content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
                    content.layout(0,0,width,640);
                    ViewGroup chips = (ViewGroup) tag(a, "trial_topics");
                    for (int i=0;i<chips.getChildCount();i++) {
                        View chip = chips.getChildAt(i);
                        assertTrue(chip.getRight() <= chips.getWidth());
                        assertTrue(chip.getBottom() <= chips.getHeight());
                    }
                }
                tag(a, "generate_trial").performClick();
                assertEquals(600, (int) field(exam(a), "remainingSeconds"));
                assertEquals(night, (boolean) field(a, "nightMode"));
                assertEquals(11, ((Set<?>) field(exam(a), "selectedTopics")).size());
            }
        }
    }

    @Test public void everyDurationStartsExactlyAndExpiresOnceWithNoAnswers() {
        for (int minutes : new int[]{1,3,5,10}) {
            try (ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup()) {
                MainActivity a=c.get();
                stats(a).edit().clear().commit();
                start(a, minutes, "RIPA");
                assertEquals(String.format(java.util.Locale.US,"%02d : 00",minutes), ((TextView)tag(a,"timer")).getText());
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(minutes * 60 - 1, TimeUnit.SECONDS);
                assertFalse((boolean)field(exam(a),"finished"));
                assertEquals("00 : 01", ((TextView)tag(a,"timer")).getText());
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
                assertTrue((boolean)field(exam(a),"finished"));
                assertEquals(1,stats(a).getInt("tests",0));
                assertEquals(0,stats(a).getInt("questions",-1));
                assertEquals(0f,stats(a).getFloat("scoreSum",-1),0);
                assertNotNull(findText(a.findViewById(android.R.id.content),"Tiempo agotado"));
                assertNotNull(findText(a.findViewById(android.R.id.content),"Preguntas respondidas"));
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);
                assertEquals(1,stats(a).getInt("tests",0));
                assertNull(field(a,"timerRunnable"));
            }
        }
    }

    @Test public void onlySelectedTopicsCycleAndScoreConfirmedAnswersIncludingRepeats() {
        for (String[] selected : new String[][]{{"RIPA"},{"RIPA","Balizamiento"}}) {
            try (ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
                MainActivity a=c.get();
                stats(a).edit().clear().commit();
                // A tiny pool makes exhaustion observable without answering the whole bank.
                List<Object> bank=field(a,"bank");
                Set<String> kept=new LinkedHashSet<>();
                bank.removeIf(q -> !Arrays.asList(selected).contains((String)field(q,"topic"))
                        || !kept.add(field(q,"topic")));
                start(a,1,selected);
                String previous=null;
                for(int i=0;i<12;i++) {
                    Object current=question(a);
                    assertTrue(Arrays.asList(selected).contains((String)field(current,"topic")));
                    String id=field(current,"id");
                    if(selected.length>1) assertNotEquals(previous,id);
                    previous=id;
                    int correct=field(current,"correct");
                    tag(a,"answer_"+(i%2==0 ? correct : (correct+1)%4)).performClick();
                    tag(a,"confirm").performClick();
                    tag(a,"next").performClick();
                }
                assertEquals(13, ((List<?>)field(exam(a),"questions")).size());
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(60,TimeUnit.SECONDS);
                assertEquals(12,stats(a).getInt("questions",0));
                assertEquals(6,stats(a).getInt("correct",0));
                assertEquals(6,stats(a).getInt("wrong",0));
                assertEquals(0,stats(a).getInt("unanswered",0));
                assertEquals(5f,stats(a).getFloat("scoreSum",0),0);
                assertNotNull(findText(a.findViewById(android.R.id.content),"50.0%"));
                assertEquals(selected.length == 1 ? 12 : 6,stats(a).getInt("topic_ripa_q",0));
            }
        }
    }

    @Test public void lateInputCannotCountAndOldViewsCannotChangeNewSession() {
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); stats(a).edit().clear().commit(); start(a,1,"RIPA");
            tag(a,"answer_0").performClick();
            View confirm=tag(a,"confirm"), answer=tag(a,"answer_1"), next=tag(a,"next");
            // Advance the monotonic clock without dispatching the queued timer callback.
            ShadowSystemClock.advanceBy(Duration.ofSeconds(60));
            confirm.performClick();
            assertEquals(0,stats(a).getInt("questions",-1));
            answer.performClick(); next.performClick(); confirm.performClick();
            assertEquals(1,stats(a).getInt("tests",0));
            clickText(a,"Generar nuevo test");
            Object fresh=exam(a);
            confirm.performClick(); answer.performClick(); next.performClick();
            assertSame(fresh,exam(a));
            assertEquals(-1, ((int[])field(fresh,"selected"))[0]);
            assertEquals("01 : 00",((TextView)tag(a,"timer")).getText());
        }
    }

    @Test public void exitCancelsTimerAndExpiryClosesExitDialog() {
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); stats(a).edit().clear().commit(); start(a,1,"Seguridad");
            a.onBackPressed();
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse((boolean)field(exam(a),"finished"));
            tag(a,"exit_exam").performClick();
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(exam(a)); assertNull(field(a,"timerRunnable"));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(70,TimeUnit.SECONDS);
            assertEquals(0,stats(a).getInt("tests",0));
            start(a,1,"Seguridad");
            a.onBackPressed(); dialog=ShadowAlertDialog.getLatestAlertDialog();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(60,TimeUnit.SECONDS);
            assertFalse(dialog.isShowing());
            assertEquals(1,stats(a).getInt("tests",0));
            clickText(a,"Generar nuevo test");
            assertEquals(0,(int)field(exam(a),"elapsedSeconds"));
            assertEquals(1,((List<?>)field(exam(a),"questions")).size());
            c.pause().stop().destroy();
            assertNull(field(a,"timerRunnable"));
        }
    }

    private static void start(MainActivity a,int minutes,String... topics) {
        ReflectionHelpers.callInstanceMethod(a,"startTimeTrial",
                ClassParameter.from(Set.class,new LinkedHashSet<>(Arrays.asList(topics))),
                ClassParameter.from(int.class,minutes));
    }
    private static AlertDialog topicDialog() {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        View decor = dialog.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 1080, 1920);
        return dialog;
    }
    private static void toggle(AlertDialog dialog,int position) {
        ListView list=dialog.getListView();
        list.performItemClick(list.getAdapter().getView(position,null,list),position,position);
    }
    private static SharedPreferences stats(MainActivity a) {return a.getSharedPreferences("per_stats",0);}
    private static Object exam(MainActivity a) {return field(a,"exam");}
    private static Set<String> topics(MainActivity a) {return field(a,"timeTrialTopics");}
    private static Object question(MainActivity a) {
        List<?> qs=field(exam(a),"questions"); int i=field(exam(a),"index"); return qs.get(i);
    }
    private static <T> T field(Object o,String name) {return ReflectionHelpers.getField(o,name);}
    private static View tag(MainActivity a,String tag) {
        View v=a.findViewById(android.R.id.content).findViewWithTag(tag); assertNotNull(tag,v); return v;
    }
    private static void clickText(MainActivity a,String label) {
        View v=findText(a.findViewById(android.R.id.content),label); assertNotNull(label,v); v.performClick();
    }
    private static View findText(View v,String label) {
        if(v instanceof TextView && label.contentEquals(((TextView)v).getText())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            View found=findText(((ViewGroup)v).getChildAt(i),label); if(found!=null)return found;
        }
        return null;
    }
}
