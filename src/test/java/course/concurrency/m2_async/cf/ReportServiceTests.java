package course.concurrency.m2_async.cf;

import course.concurrency.m2_async.cf.report.ReportServiceExecutors;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class ReportServiceTests {
    private final ReportServiceExecutors reportService = new ReportServiceExecutors();
//    private ReportServiceCF reportService = new ReportServiceCF();

    @Test
    public void testMultipleTasks() throws InterruptedException {
        int poolSize = Runtime.getRuntime().availableProcessors() * 3;
        int iterations = 5;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {

                }
                for (int it = 0; it < iterations; it++) {
                    reportService.getReport();
                }
            });
        }
        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        System.out.println(((ThreadPoolExecutor) reportService.getExecutor()).getPoolSize());
        long end = System.currentTimeMillis();
        System.out.println("Execution time: " + (end - start));
    }
}
