package com.javierperals.pertest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int BLUE = Color.rgb(21, 101, 192);
    private static final int BLUE_DARK = Color.rgb(13, 71, 161);
    private static final int RED = Color.rgb(198, 40, 40);
    private static final int GREEN = Color.rgb(46, 125, 50);
    private static final int TEXT = Color.rgb(28, 36, 48);
    private static final int MUTED = Color.rgb(96, 110, 125);
    private static final int BG = Color.rgb(245, 247, 250);
    private static final int CARD = Color.WHITE;
    private static final int BORDER = Color.rgb(218, 224, 232);
    private static final int SELECTED = Color.rgb(227, 242, 253);
    private static final int CORRECT_BG = Color.rgb(232, 245, 233);
    private static final int WRONG_BG = Color.rgb(255, 235, 238);

    private static final String PREFS = "per_stats";
    private static final String[] TOPICS = new String[]{
            "Nomenclatura náutica",
            "Elementos de amarre y fondeo",
            "Seguridad",
            "Legislación",
            "Balizamiento",
            "RIPA",
            "Maniobra",
            "Emergencias en la mar",
            "Meteorología",
            "Teoría de navegación",
            "Carta de navegación"
    };

    private static final LinkedHashMap<String, Integer> MOCK_DISTRIBUTION = new LinkedHashMap<>();
    static {
        MOCK_DISTRIBUTION.put("Nomenclatura náutica", 4);
        MOCK_DISTRIBUTION.put("Elementos de amarre y fondeo", 2);
        MOCK_DISTRIBUTION.put("Seguridad", 4);
        MOCK_DISTRIBUTION.put("Legislación", 2);
        MOCK_DISTRIBUTION.put("Balizamiento", 5);
        MOCK_DISTRIBUTION.put("RIPA", 10);
        MOCK_DISTRIBUTION.put("Maniobra", 2);
        MOCK_DISTRIBUTION.put("Emergencias en la mar", 3);
        MOCK_DISTRIBUTION.put("Meteorología", 4);
        MOCK_DISTRIBUTION.put("Teoría de navegación", 5);
        MOCK_DISTRIBUTION.put("Carta de navegación", 4);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<Question> bank = new ArrayList<>();
    private SharedPreferences prefs;
    private FrameLayout root;
    private LinearLayout drawer;
    private View scrim;
    private boolean drawerOpen = false;

    private Exam exam;
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.WHITE);
        w.setNavigationBarColor(Color.WHITE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadQuestionBank();
        showHome();
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    private void loadQuestionBank() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    getAssets().open("questions.json"), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray a = o.getJSONArray("answers");
                String[] answers = new String[4];
                for (int j = 0; j < 4; j++) answers[j] = a.getString(j);
                bank.add(new Question(
                        o.getString("id"),
                        o.getString("topic"),
                        o.getString("difficulty"),
                        o.getString("question"),
                        answers,
                        o.getInt("correct"),
                        o.getString("explanation"),
                        o.optString("tag", "")
                ));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error cargando el banco de preguntas", Toast.LENGTH_LONG).show();
        }
    }

    private void showHome() {
        stopTimer();
        exam = null;
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(14), dp(20), dp(24));
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(content, match());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = plainButton("☰", BLUE, Color.TRANSPARENT);
        menu.setTextSize(28);
        menu.setMinWidth(dp(52));
        menu.setOnClickListener(v -> openDrawer());
        top.addView(menu, new LinearLayout.LayoutParams(dp(56), dp(52)));

        TextView title = text("TEST PER", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(52), 1);
        top.addView(title, titleLp);
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(dp(56), dp(52)));
        content.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView subtitle = text("Entrenamiento offline · 1.000 preguntas", 15, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(18), 0, dp(26));
        content.addView(subtitle, matchWrap());

        LinearLayout card = card();
        card.setPadding(dp(18), dp(22), dp(18), dp(22));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(10);
        content.addView(card, cardLp);

        Button byTopic = actionButton("Tests por temas", BLUE);
        byTopic.setOnClickListener(v -> showTopicSetup());
        card.addView(byTopic, buttonLp());

        Button mock = actionButton("Simulacro PER", BLUE);
        LinearLayout.LayoutParams mlp = buttonLp();
        mlp.topMargin = dp(14);
        mock.setLayoutParams(mlp);
        mock.setOnClickListener(v -> startMockExam());
        card.addView(mock);

        Button exit = actionButton("Salir", RED);
        LinearLayout.LayoutParams elp = buttonLp();
        elp.topMargin = dp(14);
        exit.setLayoutParams(elp);
        exit.setOnClickListener(v -> finishAffinity());
        card.addView(exit);

        TextView note = text("Simulacro: 45 preguntas · 90 minutos · distribución oficial PER", 13, MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(26), dp(8), 0);
        content.addView(note, matchWrap());

        setContentView(root);
        createDrawer();
    }

    private void showTopicSetup() {
        stopTimer();
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(20), dp(14), dp(20), dp(24));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = plainButton("‹", BLUE, Color.TRANSPARENT);
        back.setTextSize(36);
        back.setOnClickListener(v -> showHome());
        top.addView(back, new LinearLayout.LayoutParams(dp(56), dp(52)));
        TextView title = text("Generar test", 23, TEXT, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        top.addView(new View(this), new LinearLayout.LayoutParams(dp(56), dp(52)));
        page.addView(top, matchWrap());

        LinearLayout form = card();
        form.setPadding(dp(18), dp(20), dp(18), dp(20));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(18);
        page.addView(form, flp);

        form.addView(fieldLabel("Tema"));
        Spinner topicSpinner = spinner(TOPICS);
        form.addView(topicSpinner, fieldLp());

        form.addView(fieldLabel("Número de preguntas"));
        Spinner countSpinner = spinner(new String[]{"1", "5", "10", "15", "20", "30"});
        countSpinner.setSelection(2);
        form.addView(countSpinner, fieldLp());

        form.addView(fieldLabel("Dificultad"));
        Spinner diffSpinner = spinner(new String[]{"Fácil", "Media", "Difícil", "Mixta"});
        diffSpinner.setSelection(1);
        form.addView(diffSpinner, fieldLp());

        Button generate = actionButton("Generar test", BLUE);
        LinearLayout.LayoutParams glp = buttonLp();
        glp.topMargin = dp(22);
        generate.setLayoutParams(glp);
        generate.setOnClickListener(v -> {
            String t = topicSpinner.getSelectedItem().toString();
            int n = Integer.parseInt(countSpinner.getSelectedItem().toString());
            String d = diffSpinner.getSelectedItem().toString().toLowerCase(Locale.ROOT);
            startTopicExam(t, n, d);
        });
        form.addView(generate);
        setContentView(page);
    }

    private void startTopicExam(String topic, int n, String difficulty) {
        List<Question> selected = pickQuestions(topic, n, difficulty);
        if (selected.isEmpty()) {
            Toast.makeText(this, "No hay preguntas disponibles", Toast.LENGTH_SHORT).show();
            return;
        }
        exam = new Exam(selected, false, topic, difficulty, n);
        showExam();
    }

    private void startMockExam() {
        List<Question> selected = new ArrayList<>();
        for (Map.Entry<String, Integer> e : MOCK_DISTRIBUTION.entrySet()) {
            selected.addAll(pickQuestions(e.getKey(), e.getValue(), "mixta"));
        }
        exam = new Exam(selected, true, "Simulacro PER", "mixta", 45);
        showExam();
    }

    private List<Question> pickQuestions(String topic, int n, String difficulty) {
        List<Question> preferred = new ArrayList<>();
        List<Question> fallback = new ArrayList<>();
        Set<String> recent = getRecentIds();
        for (Question q : bank) {
            if (!q.topic.equals(topic)) continue;
            boolean diffOk = difficulty.equals("mixta") || q.difficulty.equals(difficulty);
            if (diffOk && !recent.contains(q.id)) preferred.add(q);
            if (diffOk) fallback.add(q);
        }
        Collections.shuffle(preferred, random);
        Collections.shuffle(fallback, random);
        LinkedHashMap<String, Question> uniq = new LinkedHashMap<>();
        for (Question q : preferred) uniq.put(q.id, q);
        for (Question q : fallback) uniq.put(q.id, q);
        if (uniq.size() < n && !difficulty.equals("mixta")) {
            List<Question> anyDiff = new ArrayList<>();
            for (Question q : bank) if (q.topic.equals(topic)) anyDiff.add(q);
            Collections.shuffle(anyDiff, random);
            for (Question q : anyDiff) uniq.put(q.id, q);
        }
        List<Question> out = new ArrayList<>(uniq.values());
        if (out.size() > n) out = new ArrayList<>(out.subList(0, n));
        return out;
    }

    private void showExam() {
        if (exam == null) return;
        stopTimer();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(10));
        header.setBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button close = plainButton("×", RED, Color.TRANSPARENT);
        close.setTextSize(30);
        close.setOnClickListener(v -> confirmExitExam());
        row.addView(close, new LinearLayout.LayoutParams(dp(52), dp(46)));
        TextView title = text(exam.mock ? "Simulacro PER" : exam.topic, 18, TEXT, true);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1));
        TextView timer = text("00 : 00", 16, BLUE_DARK, true);
        timer.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        timer.setTag("timer");
        row.addView(timer, new LinearLayout.LayoutParams(dp(88), dp(46)));
        header.addView(row, matchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        TextView progress = text("Pregunta 1 / " + exam.questions.size(), 13, MUTED, true);
        progress.setTag("progress");
        stats.addView(progress, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView score = text("Aciertos 0 · Fallos 0", 13, MUTED, true);
        score.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        score.setTag("score");
        stats.addView(score, new LinearLayout.LayoutParams(0, dp(34), 1));
        header.addView(stats, matchWrap());
        page.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(18), dp(18), dp(20));
        body.setTag("body");
        scroll.addView(body, match());
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(12), dp(10), dp(12), dp(12));
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.WHITE);

        Button previous = actionButton("Anterior", BLUE);
        previous.setTag("previous");
        previous.setOnClickListener(v -> {
            if (exam.index > 0) {
                exam.index--;
                renderQuestion(page);
            }
        });
        nav.addView(previous, navButtonLp());

        Button confirm = actionButton("Confirmar", BLUE);
        confirm.setTag("confirm");
        confirm.setOnClickListener(v -> confirmCurrent(page));
        LinearLayout.LayoutParams clp = navButtonLp();
        clp.leftMargin = dp(8); clp.rightMargin = dp(8);
        nav.addView(confirm, clp);

        Button next = actionButton("Siguiente", BLUE);
        next.setTag("next");
        next.setOnClickListener(v -> {
            if (exam.index == exam.questions.size() - 1) {
                requestFinishExam();
            } else {
                exam.index++;
                renderQuestion(page);
            }
        });
        nav.addView(next, navButtonLp());
        // Attach navigation before renderQuestion looks up its buttons in page.
        page.addView(nav, matchWrap());

        setContentView(page);
        renderQuestion(page);
        startTimer(page);
    }

    private void renderQuestion(LinearLayout page) {
        LinearLayout body = page.findViewWithTag("body");
        body.removeAllViews();
        Question q = exam.questions.get(exam.index);

        TextView progress = page.findViewWithTag("progress");
        progress.setText("Pregunta " + (exam.index + 1) + " / " + exam.questions.size());
        TextView score = page.findViewWithTag("score");
        score.setText("Aciertos " + exam.confirmedCorrect() + " · Fallos " + exam.confirmedWrong());

        TextView topic = text(q.topic + " · " + capitalize(q.difficulty), 12, BLUE_DARK, true);
        topic.setPadding(0, 0, 0, dp(10));
        body.addView(topic, matchWrap());

        TextView question = text(q.question, 20, TEXT, true);
        question.setLineSpacing(0, 1.12f);
        question.setPadding(0, 0, 0, dp(18));
        body.addView(question, matchWrap());

        int selected = exam.selected[exam.index];
        boolean isConfirmed = exam.confirmed[exam.index];
        for (int i = 0; i < 4; i++) {
            final int answerIndex = i;
            String letter = String.valueOf((char)('A' + i));
            TextView answer = text(letter + ")  " + q.answers[i], 16, TEXT, false);
            answer.setGravity(Gravity.CENTER_VERTICAL);
            answer.setPadding(dp(14), dp(14), dp(14), dp(14));
            answer.setTag("answer_" + i);
            int fill = CARD;
            int border = BORDER;
            if (isConfirmed) {
                if (i == q.correct) {
                    fill = CORRECT_BG; border = GREEN; answer.setTextColor(GREEN);
                    answer.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                }
                if (selected == i && i != q.correct) {
                    fill = WRONG_BG; border = RED; answer.setTextColor(RED);
                    answer.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                }
            } else if (selected == i) {
                fill = SELECTED; border = BLUE;
                answer.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            answer.setBackground(roundRect(fill, border, 1, 12));
            if (!isConfirmed) {
                answer.setOnClickListener(v -> {
                    exam.selected[exam.index] = answerIndex;
                    renderQuestion(page);
                });
            }
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.bottomMargin = dp(10);
            body.addView(answer, alp);
        }

        if (isConfirmed) {
            boolean ok = selected == q.correct;
            LinearLayout feedback = new LinearLayout(this);
            feedback.setOrientation(LinearLayout.VERTICAL);
            feedback.setPadding(dp(14), dp(13), dp(14), dp(13));
            feedback.setBackground(roundRect(ok ? CORRECT_BG : WRONG_BG, ok ? GREEN : RED, 1, 12));
            TextView result = text(ok ? "Correcto" : "Incorrecto", 16, ok ? GREEN : RED, true);
            feedback.addView(result, matchWrap());
            if (!ok) {
                TextView correct = text("Correcta: " + (char)('A' + q.correct) + ") " + q.answers[q.correct], 14, TEXT, true);
                correct.setPadding(0, dp(6), 0, 0);
                feedback.addView(correct, matchWrap());
            }
            TextView exp = text(q.explanation, 14, TEXT, false);
            exp.setPadding(0, dp(7), 0, 0);
            exp.setLineSpacing(0, 1.08f);
            feedback.addView(exp, matchWrap());
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flp.topMargin = dp(6);
            body.addView(feedback, flp);
        }

        Button previous = page.findViewWithTag("previous");
        previous.setEnabled(exam.index > 0);
        previous.setAlpha(exam.index > 0 ? 1f : 0.45f);
        Button confirm = page.findViewWithTag("confirm");
        confirm.setEnabled(!isConfirmed && selected >= 0);
        confirm.setAlpha((!isConfirmed && selected >= 0) ? 1f : 0.45f);
        Button next = page.findViewWithTag("next");
        next.setText(exam.index == exam.questions.size() - 1 ? "Terminar" : "Siguiente");
    }

    private void confirmCurrent(LinearLayout page) {
        int i = exam.index;
        if (exam.confirmed[i] || exam.selected[i] < 0) return;
        exam.confirmed[i] = true;
        renderQuestion(page);
    }

    private void requestFinishExam() {
        int unconfirmed = exam.unconfirmedCount();
        if (unconfirmed > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Terminar test")
                    .setMessage("Hay " + unconfirmed + " pregunta" + (unconfirmed == 1 ? "" : "s") + " sin confirmar. Al terminar contarán como fallo.")
                    .setNegativeButton("Volver", null)
                    .setPositiveButton("Terminar", (d, w) -> finishExam(false))
                    .show();
        } else {
            finishExam(false);
        }
    }

    private void finishExam(boolean timeExpired) {
        stopTimer();
        if (exam.finished) return;
        exam.finished = true;
        exam.elapsedSeconds = exam.mock ? Math.min(5400, 5400 - exam.remainingSeconds) : exam.elapsedSeconds;
        saveStats();
        saveRecentIds();
        showResults(timeExpired);
    }

    private void showResults(boolean timeExpired) {
        int correct = exam.finalCorrect();
        int wrong = exam.questions.size() - correct;
        int unanswered = exam.unconfirmedCount();
        double grade = 10.0 * correct / exam.questions.size();
        boolean pass = exam.mock && passesOfficialCriteria();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(page, match());

        TextView title = text(timeExpired ? "Tiempo agotado" : "Resultado final", 26, TEXT, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, matchWrap());

        if (exam.mock) {
            TextView status = text(pass ? "APTO" : "NO APTO", 30, pass ? GREEN : RED, true);
            status.setGravity(Gravity.CENTER);
            status.setPadding(0, dp(12), 0, dp(2));
            page.addView(status, matchWrap());
        }

        TextView score = text(String.format(Locale.US, "%.1f / 10", grade), 36, BLUE_DARK, true);
        score.setGravity(Gravity.CENTER);
        score.setPadding(0, dp(8), 0, dp(16));
        page.addView(score, matchWrap());

        LinearLayout summary = card();
        summary.setPadding(dp(16), dp(16), dp(16), dp(16));
        summary.addView(statLine("Aciertos", String.valueOf(correct), GREEN));
        summary.addView(statLine("Fallos", String.valueOf(wrong), RED));
        summary.addView(statLine("Sin confirmar", String.valueOf(unanswered), MUTED));
        summary.addView(statLine("Tiempo", formatTime(exam.elapsedSeconds), BLUE_DARK));
        page.addView(summary, matchWrap());

        if (exam.mock) {
            TextView criteria = text("Criterio PER: mínimo 32 aciertos; máximo 5 fallos en RIPA, 2 en Balizamiento y 2 en Carta.", 13, MUTED, false);
            criteria.setPadding(dp(4), dp(12), dp(4), 0);
            page.addView(criteria, matchWrap());
        }

        Map<String, Integer> wrongByTopic = exam.wrongByTopic();
        TextView reviewTitle = text("Temas a repasar", 19, TEXT, true);
        reviewTitle.setPadding(0, dp(22), 0, dp(8));
        page.addView(reviewTitle, matchWrap());
        List<Map.Entry<String,Integer>> entries = new ArrayList<>(wrongByTopic.entrySet());
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        if (entries.isEmpty()) {
            page.addView(text("Ninguno prioritario en este test.", 15, GREEN, false), matchWrap());
        } else {
            for (Map.Entry<String,Integer> e : entries) {
                page.addView(text("• " + e.getKey() + ": " + e.getValue() + " fallo" + (e.getValue()==1?"":"s"), 15, TEXT, false), matchWrap());
            }
        }

        Button again = actionButton("Generar nuevo test", BLUE);
        LinearLayout.LayoutParams alp = buttonLp();
        alp.topMargin = dp(26);
        again.setLayoutParams(alp);
        again.setOnClickListener(v -> {
            if (exam.mock) startMockExam();
            else startTopicExam(exam.topic, exam.requestedCount, exam.difficulty);
        });
        page.addView(again);

        Button home = actionButton("Inicio", BLUE_DARK);
        LinearLayout.LayoutParams hlp = buttonLp();
        hlp.topMargin = dp(12);
        home.setLayoutParams(hlp);
        home.setOnClickListener(v -> showHome());
        page.addView(home);

        setContentView(scroll);
    }

    private boolean passesOfficialCriteria() {
        if (!exam.mock) return false;
        if (exam.finalCorrect() < 32) return false;
        Map<String,Integer> w = exam.wrongByTopic();
        if (w.getOrDefault("RIPA", 0) > 5) return false;
        if (w.getOrDefault("Balizamiento", 0) > 2) return false;
        return w.getOrDefault("Carta de navegación", 0) <= 2;
    }

    private void startTimer(LinearLayout page) {
        exam.startedAt = System.currentTimeMillis();
        if (exam.mock && exam.remainingSeconds <= 0) exam.remainingSeconds = 5400;
        timerRunnable = new Runnable() {
            @Override public void run() {
                if (exam == null || exam.finished) return;
                if (exam.mock) {
                    exam.remainingSeconds--;
                    if (exam.remainingSeconds < 0) exam.remainingSeconds = 0;
                } else {
                    exam.elapsedSeconds++;
                }
                TextView timer = page.findViewWithTag("timer");
                int value = exam.mock ? exam.remainingSeconds : exam.elapsedSeconds;
                if (timer != null) timer.setText(formatTime(value));
                if (exam.mock && exam.remainingSeconds <= 0) {
                    finishExam(true);
                    return;
                }
                handler.postDelayed(this, 1000);
            }
        };
        TextView timer = page.findViewWithTag("timer");
        timer.setText(formatTime(exam.mock ? exam.remainingSeconds : exam.elapsedSeconds));
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) handler.removeCallbacks(timerRunnable);
        timerRunnable = null;
    }

    private void confirmExitExam() {
        new AlertDialog.Builder(this)
                .setTitle("Salir del test")
                .setMessage("El progreso de este test no se guardará en estadísticas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salir", (d, w) -> showHome())
                .show();
    }

    private void saveStats() {
        SharedPreferences.Editor ed = prefs.edit();
        int tests = prefs.getInt("tests", 0) + 1;
        int totalQ = prefs.getInt("questions", 0) + exam.questions.size();
        int correct = prefs.getInt("correct", 0) + exam.finalCorrect();
        int wrong = prefs.getInt("wrong", 0) + (exam.questions.size() - exam.finalCorrect());
        int unanswered = prefs.getInt("unanswered", 0) + exam.unconfirmedCount();
        long seconds = prefs.getLong("seconds", 0) + exam.elapsedSeconds;
        float grade = (float)(10.0 * exam.finalCorrect() / exam.questions.size());
        float scoreSum = prefs.getFloat("scoreSum", 0f) + grade;
        float best = Math.max(prefs.getFloat("best", 0f), grade);
        ed.putInt("tests", tests);
        ed.putInt("questions", totalQ);
        ed.putInt("correct", correct);
        ed.putInt("wrong", wrong);
        ed.putInt("unanswered", unanswered);
        ed.putLong("seconds", seconds);
        ed.putFloat("scoreSum", scoreSum);
        ed.putFloat("best", best);
        if (exam.mock) {
            ed.putInt("mocks", prefs.getInt("mocks", 0) + 1);
            if (passesOfficialCriteria()) ed.putInt("mocksPassed", prefs.getInt("mocksPassed", 0) + 1);
        }
        for (int i = 0; i < exam.questions.size(); i++) {
            Question q = exam.questions.get(i);
            String key = keyForTopic(q.topic);
            ed.putInt("topic_" + key + "_q", prefs.getInt("topic_" + key + "_q", 0) + 1);
            boolean ok = exam.confirmed[i] && exam.selected[i] == q.correct;
            if (ok) ed.putInt("topic_" + key + "_ok", prefs.getInt("topic_" + key + "_ok", 0) + 1);
        }
        ed.apply();
    }

    private void saveRecentIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>(getRecentIds());
        for (Question q : exam.questions) {
            ids.remove(q.id);
            ids.add(q.id);
        }
        List<String> list = new ArrayList<>(ids);
        while (list.size() > 250) list.remove(0);
        prefs.edit().putString("recent", String.join(",", list)).apply();
    }

    private Set<String> getRecentIds() {
        String s = prefs.getString("recent", "");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (!s.isEmpty()) {
            String[] parts = s.split(",");
            Collections.addAll(set, parts);
        }
        return set;
    }

    private void createDrawer() {
        if (root == null) return;
        scrim = new View(this);
        scrim.setBackgroundColor(Color.argb(115, 0, 0, 0));
        scrim.setAlpha(0f);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> closeDrawer());
        root.addView(scrim, match());

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Color.WHITE);
        drawer.setPadding(dp(20), dp(26), dp(20), dp(20));
        int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.86f);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT);
        drawer.setTranslationX(-width);
        root.addView(drawer, dlp);
        refreshDrawer();
    }

    private void refreshDrawer() {
        if (drawer == null) return;
        drawer.removeAllViews();
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Estadísticas", 24, TEXT, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button close = plainButton("×", MUTED, Color.TRANSPARENT);
        close.setTextSize(28);
        close.setOnClickListener(v -> closeDrawer());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(52), dp(50)));
        drawer.addView(titleRow, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(0, dp(8), 0, dp(18));
        scroll.addView(stats, match());
        drawer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        int tests = prefs.getInt("tests", 0);
        int total = prefs.getInt("questions", 0);
        int ok = prefs.getInt("correct", 0);
        int bad = prefs.getInt("wrong", 0);
        float pct = total == 0 ? 0f : (100f * ok / total);
        float avg = tests == 0 ? 0f : prefs.getFloat("scoreSum", 0f) / tests;
        float best = prefs.getFloat("best", 0f);
        int mocks = prefs.getInt("mocks", 0);
        int mocksPassed = prefs.getInt("mocksPassed", 0);

        stats.addView(statLine("Tests realizados", String.valueOf(tests), BLUE_DARK));
        stats.addView(statLine("Preguntas", String.valueOf(total), BLUE_DARK));
        stats.addView(statLine("Acierto global", String.format(Locale.US, "%.1f%%", pct), pct >= 70 ? GREEN : TEXT));
        stats.addView(statLine("Nota media", String.format(Locale.US, "%.1f", avg), BLUE_DARK));
        stats.addView(statLine("Mejor nota", String.format(Locale.US, "%.1f", best), GREEN));
        stats.addView(statLine("Tiempo total", formatLongTime(prefs.getLong("seconds", 0)), BLUE_DARK));
        stats.addView(statLine("Simulacros aptos", mocksPassed + " / " + mocks, mocks > 0 && mocksPassed == mocks ? GREEN : TEXT));

        TextView breakdown = text("Por temas", 18, TEXT, true);
        breakdown.setPadding(0, dp(18), 0, dp(8));
        stats.addView(breakdown, matchWrap());
        for (String t : TOPICS) {
            String key = keyForTopic(t);
            int tq = prefs.getInt("topic_" + key + "_q", 0);
            int tok = prefs.getInt("topic_" + key + "_ok", 0);
            float tp = tq == 0 ? 0f : 100f * tok / tq;
            TextView line = text(t + "\n" + (tq == 0 ? "Sin datos" : tok + "/" + tq + " · " + String.format(Locale.US, "%.1f%%", tp)), 14, TEXT, false);
            line.setPadding(dp(10), dp(9), dp(10), dp(9));
            line.setBackground(roundRect(Color.rgb(248,249,251), BORDER, 1, 9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(7);
            stats.addView(line, lp);
        }

        Button reset = actionButton("Reiniciar", RED);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reiniciar estadísticas")
                .setMessage("Se borrarán las estadísticas acumuladas. El banco de preguntas no se modifica.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Reiniciar", (d, w) -> {
                    String recent = prefs.getString("recent", "");
                    prefs.edit().clear().putString("recent", recent).apply();
                    refreshDrawer();
                }).show());
        drawer.addView(reset, buttonLp());
    }

    private void openDrawer() {
        if (drawer == null || drawerOpen) return;
        refreshDrawer();
        drawerOpen = true;
        scrim.setVisibility(View.VISIBLE);
        scrim.animate().alpha(1f).setDuration(180).start();
        drawer.animate().translationX(0).setDuration(220).start();
    }

    private void closeDrawer() {
        if (drawer == null || !drawerOpen) return;
        drawerOpen = false;
        int width = drawer.getWidth() > 0 ? drawer.getWidth() : (int)(getResources().getDisplayMetrics().widthPixels * 0.86f);
        drawer.animate().translationX(-width).setDuration(200).withEndAction(() -> {
            scrim.setVisibility(View.GONE);
            scrim.setAlpha(0f);
        }).start();
    }

    private String keyForTopic(String t) {
        return t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }

    private LinearLayout statLine(String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(7), dp(2), dp(7));
        TextView l = text(label, 14, TEXT, false);
        row.addView(l, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView v = text(value, 15, valueColor, true);
        v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        return row;
    }

    private TextView fieldLabel(String label) {
        TextView v = text(label, 14, TEXT, true);
        v.setPadding(0, dp(12), 0, dp(6));
        return v;
    }

    private Spinner spinner(String[] data) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data);
        s.setAdapter(adapter);
        s.setBackground(roundRect(Color.WHITE, BORDER, 1, 10));
        s.setPadding(dp(10), 0, dp(8), 0);
        return s;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(4);
        return lp;
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setBackground(roundRect(CARD, BORDER, 1, 16));
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button actionButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundRect(color, color, 0, 11));
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private Button plainButton(String label, int textColor, int bg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setBackgroundColor(bg);
        b.setPadding(0,0,0,0);
        return b;
    }

    private GradientDrawable roundRect(int fill, int strokeColor, int strokeDp, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private LinearLayout.LayoutParams buttonLp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
    }

    private LinearLayout.LayoutParams navButtonLp() {
        return new LinearLayout.LayoutParams(0, dp(52), 1);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format(Locale.US, "%02d : %02d", m, s);
    }

    private String formatLongTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) return h + " h " + m + " min";
        return m + " min";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static class Question {
        final String id, topic, difficulty, question, explanation, tag;
        final String[] answers;
        final int correct;
        Question(String id, String topic, String difficulty, String question, String[] answers, int correct, String explanation, String tag) {
            this.id=id; this.topic=topic; this.difficulty=difficulty; this.question=question;
            this.answers=answers; this.correct=correct; this.explanation=explanation; this.tag=tag;
        }
    }

    private static class Exam {
        final List<Question> questions;
        final boolean mock;
        final String topic;
        final String difficulty;
        final int requestedCount;
        final int[] selected;
        final boolean[] confirmed;
        int index=0;
        int elapsedSeconds=0;
        int remainingSeconds=5400;
        long startedAt;
        boolean finished=false;

        Exam(List<Question> questions, boolean mock, String topic, String difficulty, int requestedCount) {
            this.questions=questions; this.mock=mock; this.topic=topic; this.difficulty=difficulty; this.requestedCount=requestedCount;
            selected=new int[questions.size()];
            confirmed=new boolean[questions.size()];
            for(int i=0;i<selected.length;i++) selected[i]=-1;
        }

        int confirmedCorrect() {
            int n=0;
            for(int i=0;i<questions.size();i++) if(confirmed[i] && selected[i]==questions.get(i).correct) n++;
            return n;
        }
        int confirmedWrong() {
            int n=0;
            for(int i=0;i<questions.size();i++) if(confirmed[i] && selected[i]!=questions.get(i).correct) n++;
            return n;
        }
        int finalCorrect() { return confirmedCorrect(); }
        int unconfirmedCount() {
            int n=0; for(boolean c:confirmed) if(!c) n++; return n;
        }
        Map<String,Integer> wrongByTopic() {
            LinkedHashMap<String,Integer> map=new LinkedHashMap<>();
            for(int i=0;i<questions.size();i++) {
                boolean ok=confirmed[i] && selected[i]==questions.get(i).correct;
                if(!ok) map.put(questions.get(i).topic, map.getOrDefault(questions.get(i).topic,0)+1);
            }
            return map;
        }
    }
}
