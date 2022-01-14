package ru.study21.jcsv.xxl.algorithms;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class PairingHeapTest {

    @Test
    public void simpleTest() {
        PairingHeap<Integer> heap = new PairingHeap<>(Comparator.naturalOrder());

        heap.addAll(List.of(5, 4, 3, 2, 1));
        assertEquals(1, heap.pollMin());
        assertEquals(2, heap.pollMin());
        assertEquals(3, heap.pollMin());
        assertEquals(4, heap.pollMin());
        assertEquals(5, heap.pollMin());
        assertTrue(heap.isEmpty());

        heap.addAll(List.of(1, 2, 1, 2, 1));
        assertEquals(1, heap.pollMin());
        assertEquals(1, heap.pollMin());
        assertEquals(1, heap.pollMin());
        assertEquals(2, heap.pollMin());
        assertEquals(2, heap.pollMin());
        assertTrue(heap.isEmpty());

        heap.add(5);
        assertEquals(5, heap.pollMin());
        heap.add(1);
        assertEquals(1, heap.pollMin());
    }

    @Test
    public void stressTest() {
        PairingHeap<Integer> heap = new PairingHeap<>(Comparator.naturalOrder());
        PriorityQueue<Integer> queue = new PriorityQueue<>();

        final int OPS = 1_000_000;
        Random random = new Random();
        for (int op = 0; op < OPS; op++) {
            switch (random.nextInt() % 4) {
                case 0, 1 -> {
                    // add
                    int value = random.nextInt();
                    heap.add(value);
                    queue.add(value);
                }
                case 2 -> {
                    // peek
                    if (queue.isEmpty()) {
                        assertTrue(heap.isEmpty());
                        break;
                    }
                    int got = heap.peekMin();
                    int expected = queue.peek();
                    assertEquals(got, expected);
                }
                case 3 -> {
                    // poll
                    if (queue.isEmpty()) {
                        assertTrue(heap.isEmpty());
                        break;
                    }
                    int got = heap.pollMin();
                    int expected = queue.poll();
                    assertEquals(got, expected);
                }
            }
        }
    }

}
