package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ReportServiceCF {
    private ExecutorService executor = ForkJoinPool.commonPool();

    public Others.Report getReport() {
        CompletableFuture<Collection<Others.Item>> itemsCF =
                CompletableFuture.supplyAsync(() -> getItems(), executor);

        CompletableFuture<Collection<Others.Customer>> customersCF =
                CompletableFuture.supplyAsync(() -> getActiveCustomers(), executor);

        CompletableFuture<Others.Report> reportTask =
                customersCF.thenCombine(itemsCF,
                        (customers, orders) -> combineResults(orders, customers));

        return reportTask.join();
    }

    public void shutdown() {
        executor.shutdown();
    }

    private Others.Report combineResults(Collection<Others.Item> items, Collection<Others.Customer> customers) {
        return new Others.Report();
    }

    private Collection<Others.Customer> getActiveCustomers() {
        LoadGenerator.work();
        LoadGenerator.work();
        return List.of(new Others.Customer(), new Others.Customer());
    }

    private Collection<Others.Item> getItems() {
        LoadGenerator.work();
        return List.of(new Others.Item(), new Others.Item());
    }
}