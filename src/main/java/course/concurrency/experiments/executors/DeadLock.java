package course.concurrency.experiments.executors;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class DeadLock {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Map<Integer, String> map = new ConcurrentHashMap<>();
        map.put(1, "Anya");
        map.put(2, "Sveta");

        Supplier<String> task1 = () -> map.compute(1, (k1, oldV1) -> {
            String newV = oldV1 + "!";
            map.compute(2, (k2, oldV2) -> oldV2 + "&");
            return newV;
        });
        Supplier<String> task2 = () -> map.compute(2, (k2, oldV2) -> {
            String newV = oldV2 + "&";
            map.compute(1, (k1, oldV1) -> oldV1 + "!");
            return newV;
        });

        Supplier<String> task3 = () -> map.computeIfAbsent(3, (v3) -> {
            String newV = "!";
            map.compute(2, (k2, oldV2) -> oldV2 + "&");
            return newV;
        });
        Supplier<String> task4 = () -> map.compute(2, (k2, oldV2) -> {
            String newV = oldV2 + "&";
            map.compute(3, (k3, oldV3) -> oldV3 + "!");
            return newV;
        });

        Supplier<String> task5 = () -> map.merge(1, "!", (oldV, newV) -> {
            String newVal = oldV + newV;
            map.compute(3, (k3, oldV3) -> oldV3 + "!");
            return newVal;
        });
        Supplier<String> task6 = () -> map.compute(3, (k3, oldV3) -> {
            String newV = oldV3 + "&";
            map.compute(1, (k1, oldV1) -> oldV1 + "!");
            return newV;
        });

        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(task5, executor);
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(task6, executor);

        System.out.println("r1 = " + cf1.join());
        System.out.println("r2 = " + cf2.join());

        executor.shutdown();
    }
}
