package course.concurrency.m3_shared.threadlocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TL_example2 {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            executor.submit(new Task());
        }
    }

    public static class Task implements Runnable {
        private static final ThreadLocal<Integer> VALUE = ThreadLocal.withInitial(() -> 0);

        @Override
        public void run() {
            Integer currentValue = VALUE.get();
            VALUE.set(currentValue + 1);
            System.out.print(VALUE.get());
        }
    }
}
