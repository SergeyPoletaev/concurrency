package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Notifier {
    private final ExecutorService executor = initPool();

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void shutdownWithTimeout() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void sendOutdatedMessage(Bid bid) {
        executor.submit(this::imitateSending);
    }

    private ExecutorService initPool() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1_000,
                30_000,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(4_000_000));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
