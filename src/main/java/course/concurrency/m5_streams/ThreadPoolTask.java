package course.concurrency.m5_streams;

import java.util.concurrent.*;

public class ThreadPoolTask {

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        BlockingQueue<Runnable> deque = new LinkedBlockingDeque<>(20) {
            @Override
            public Runnable take() throws InterruptedException {
                return super.takeLast();
            }
        };
        return new ThreadPoolExecutor(10, 100, 60L, TimeUnit.SECONDS, deque);
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(8, 8, 0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.DiscardPolicy());
    }
}
