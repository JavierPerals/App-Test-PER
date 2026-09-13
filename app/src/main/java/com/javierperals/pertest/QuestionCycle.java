package com.javierperals.pertest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** A shuffled cycle that exhausts its pool before repeating, including across boundaries. */
final class QuestionCycle<T> {
    private final List<T> pool;
    private final Random random;
    private final List<T> remaining = new ArrayList<>();
    private T previous;

    QuestionCycle(List<T> pool, Random random) {
        if (pool.isEmpty()) throw new IllegalArgumentException("Empty question pool");
        this.pool = new ArrayList<>(pool);
        this.random = random;
    }

    T next() {
        if (remaining.isEmpty()) {
            remaining.addAll(pool);
            Collections.shuffle(remaining, random);
            int last = remaining.size() - 1;
            if (last > 0 && remaining.get(last).equals(previous))
                Collections.swap(remaining, last, 0);
        }
        previous = remaining.remove(remaining.size() - 1);
        return previous;
    }
}
