package com.javierperals.pertest;

/** Mode-independent result data. Thresholds use the exact ratio, never the rounded grade. */
public final class TestResult {
    private final double percentage;

    public TestResult(int correct, int total) {
        if (total < 0 || correct < 0 || correct > total)
            throw new IllegalArgumentException("Invalid result counts");
        percentage = total == 0 ? 0.0 : (correct / (double) total) * 100.0;
    }

    public double percentage() { return percentage; }
    public double grade() { return percentage / 10.0; }

    public String animationAsset() {
        if (percentage < 50.0) return "result_sad.webp";
        if (percentage < 80.0) return "result_good.webp";
        return "result_celebration.webp";
    }
}
