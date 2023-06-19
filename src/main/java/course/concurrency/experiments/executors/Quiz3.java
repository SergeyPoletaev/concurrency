package course.concurrency.experiments.executors;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Quiz3 {

    public static long longTask() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ThreadLocalRandom.current().nextInt();
    }

    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> longTask());
            System.out.print(executor.getPoolSize() + " ");
        }
        executor.shutdown();
    }
}
