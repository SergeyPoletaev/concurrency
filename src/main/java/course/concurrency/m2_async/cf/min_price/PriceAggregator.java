package course.concurrency.m2_async.cf.min_price;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class PriceAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(PriceAggregator.class);

    private PriceRetriever priceRetriever = new PriceRetriever();
    private Collection<Long> shopIds = Set.of(10L, 45L, 66L, 345L, 234L, 333L, 67L, 123L, 768L);
    private final ExecutorService executor =
            new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        return shopIds.stream()
                .map(shopId -> CompletableFuture
                        .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                        .orTimeout(2900, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            if (ex.getClass() != TimeoutException.class) {
                                LOG.error(ex, () -> "Что-то пошло не так ...");
                            }
                            return null;
                        }))
                .collect(Collectors.toSet())
                .stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(NaN);
    }
}
