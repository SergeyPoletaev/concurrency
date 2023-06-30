package course.concurrency.exams.auction;

import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class AuctionTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics STAT = new ExecutionStatistics();

    private static final int ITERATIONS = 1_000_000;
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int BID_COUNT = ITERATIONS * POOL_SIZE;

    private ExecutorService executor;
    private BlockingQueue<Long> priceQueue;
    private long expectedPrice;
    private Notifier notifier;

    @AfterAll
    public static void printStat() {
        STAT.printStatistics();
    }

    @BeforeEach
    public void setup() {
        notifier = new Notifier();

        executor = Executors.newFixedThreadPool(POOL_SIZE);
        priceQueue = new ArrayBlockingQueue<>(BID_COUNT);
        for (long i = 0; i < BID_COUNT / 3; i++) {
            priceQueue.offer(i - 1);
            priceQueue.offer(i);
            priceQueue.offer(i + 1);
        }
        expectedPrice = BID_COUNT / 3;
    }

    @AfterEach
    public void tearDown() {
        notifier.shutdown();
    }

    @RepeatedTest(TEST_COUNT)
    public void testOptimistic() throws InterruptedException {
        Auction auction = new AuctionOptimistic(notifier);
        testCorrectLatestBid(auction, "optimistic");
    }

    @RepeatedTest(TEST_COUNT)
    public void testPessimistic() throws InterruptedException {
        Auction auction = new AuctionPessimistic(notifier);
        testCorrectLatestBid(auction, "pessimistic");
    }

    public void testCorrectLatestBid(Auction auction, String tag) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < POOL_SIZE; i++) {

            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {

                }
                for (int it = 0; it < ITERATIONS; it++) {
                    long value = priceQueue.poll();
                    Bid bid = new Bid(value, value, value);
                    auction.propose(bid);
                    if (it % 200 == 0) {
                        auction.getLatestBid();
                    }
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(expectedPrice, auction.getLatestBid().getPrice());
        STAT.addData(tag, end - start);
    }
}
