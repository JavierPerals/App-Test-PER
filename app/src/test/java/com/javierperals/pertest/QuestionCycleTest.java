package com.javierperals.pertest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestionCycleTest {
    @Test public void exhaustsEveryCycleAndAvoidsImmediateBoundaryRepeats() {
        QuestionCycle<Integer> cycle=new QuestionCycle<>(Arrays.asList(1,2,3,4),new Random(42));
        Integer previous=null;
        for(int round=0;round<100;round++) {
            Set<Integer> seen=new HashSet<>();
            for(int i=0;i<4;i++) {
                Integer next=cycle.next(); assertNotEquals(previous,next); assertTrue(seen.add(next)); previous=next;
            }
            assertEquals(4,seen.size());
        }
    }
    @Test public void singleQuestionCanRepeatIndefinitely() {
        QuestionCycle<Integer> cycle=new QuestionCycle<>(Arrays.asList(7),new Random(1));
        for(int i=0;i<100;i++) assertEquals(Integer.valueOf(7),cycle.next());
    }
}
