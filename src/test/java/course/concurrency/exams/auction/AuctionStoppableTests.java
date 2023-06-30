package course.concurrency.exams.auction;

import org.junit.jupiter.api.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class AuctionStoppableTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics STAT = new ExecutionStatistics();

    private static final int ITERATIONS = 1_000_000;
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int BID_COUNT = ITERATIONS * POOL_SIZE;

    private ExecutorService executor;
    private BlockingQueue<Long> priceQueue;
    private long latestPrice;
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
        latestPrice = BID_COUNT / 3;
    }

    @AfterEach
    public void tearDown() {
        notifier.shutdown();
    }

    @RepeatedTest(TEST_COUNT)
    public void testPessimistic() throws InterruptedException {
        AuctionStoppable pessimistic = new AuctionStoppablePessimistic(notifier);
        testCorrectLatestBid(pessimistic, "pessimistic");
    }

    @RepeatedTest(TEST_COUNT)
    public void testOptimistic() throws InterruptedException {
        AuctionStoppable optimistic = new AuctionStoppableOptimistic(notifier);
        testCorrectLatestBid(optimistic, "optimistic");
    }

    public void testCorrectLatestBid(AuctionStoppable auction, String tag) throws InterruptedException {
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
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(latestPrice, auction.getLatestBid().getPrice());
        STAT.addData(tag, end - start);
    }

    @Test
    public void testStoppedAuctionPessimistic() throws InterruptedException {
        AuctionStoppable auction = new AuctionStoppablePessimistic(notifier);
        testStoppedAuction(auction);
    }

    @Test
    public void testStoppedAuctionOptimistic() throws InterruptedException {
        AuctionStoppable auction = new AuctionStoppableOptimistic(notifier);
        testStoppedAuction(auction);
    }

    public void testStoppedAuction(AuctionStoppable auction) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int priceToStop = ITERATIONS;
        AtomicReference<Bid> latestBidWhenStopped = new AtomicReference<>();

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
                    if (bid.getPrice() == priceToStop) {
                        Bid latest = auction.stopAuction();
                        latestBidWhenStopped.set(latest);
                    }
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(latestBidWhenStopped.get().getPrice(), auction.getLatestBid().getPrice());
    }
}
