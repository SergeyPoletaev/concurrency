package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReportServiceExecutors {
    private final ExecutorService executor = Executors.newFixedThreadPool(12);

    public Others.Report getReport() {
        Future<Collection<Others.Item>> iFuture = executor.submit(this::getItems);
        Future<Collection<Others.Customer>> customersFuture = executor.submit(this::getActiveCustomers);
        try {
            Collection<Others.Customer> customers = customersFuture.get();
            Collection<Others.Item> items = iFuture.get();
            return combineResults(items, customers);
        } catch (ExecutionException | InterruptedException ex) {
            ex.printStackTrace();
        }
        return new Others.Report();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public ExecutorService getExecutor() {
        return executor;
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
