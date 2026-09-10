package com.javierperals.pertest;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestResultTest {
    @Test public void exactBoundariesAndDifferentLengths() {
        int[][] counts = {{0,100}, {49,100}, {50,100}, {79,100}, {80,100}, {100,100},
                {0,1}, {1,1}, {1,2}, {2,3}, {4,5}, {22,45}, {23,45}, {35,45}, {36,45},
                {499,1000}, {799,1000}, {0,0}};
        String[] expected = {"sad", "sad", "good", "good", "celebration", "celebration",
                "sad", "celebration", "good", "good", "celebration", "sad", "good", "good", "celebration",
                "sad", "good", "sad"};
        for (int i = 0; i < counts.length; i++) {
            TestResult result = new TestResult(counts[i][0], counts[i][1]);
            double ratio = counts[i][1] == 0 ? 0 : counts[i][0] / (double)counts[i][1] * 100;
            assertEquals(ratio, result.percentage(), 0.000001);
            assertEquals(ratio / 10, result.grade(), 0.000001);
            assertEquals("result_" + expected[i] + ".webp", result.animationAsset());
        }
    }

    @Test public void rejectsInvalidCounts() {
        for (int[] counts : new int[][]{{-1,10},{11,10},{0,-1},{1,0}}) {
            assertThrows(IllegalArgumentException.class, () -> new TestResult(counts[0], counts[1]));
        }
    }
}
