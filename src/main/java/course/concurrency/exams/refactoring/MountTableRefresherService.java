package course.concurrency.exams.refactoring;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("MountTableRefresh_ClientsCacheCleaner");
            t.setDaemon(true);
            return t;
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<Others.MountTableManager> managers = routerStore.getCachedRecords().stream()
                .filter(rs -> rs.getAdminAddress() != null && rs.getAdminAddress().length() != 0) // this router has not enabled router admin.
                .map(rs -> getMountTableManager(rs.getAdminAddress()))
                .collect(Collectors.toList());
        if (!managers.isEmpty()) {
            invokeRefresh(managers);
        }
    }

    protected Others.MountTableManager getMountTableManager(String adminAddress) {
        return new Others.MountTableManager(adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<Others.MountTableManager> managers) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(managers.size());
        Map<String, Integer> results = managers.stream()
                .peek(manager -> {
                    ThreadFactory tf = r -> {
                        Thread t = new Thread(r, "MountTableRefresh_" + manager.getAdminAddress());
                        t.setDaemon(true);
                        return t;
                    };
                    executor.setThreadFactory(tf);
                })
                .map(manager -> CompletableFuture.supplyAsync(() -> manager.refresh(), executor)
                        .completeOnTimeout(null, cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                        .handle((rsl, ex) -> {
                            boolean res = true;
                            if (ex != null) {
                                log("Mount table cache refresher was interrupted.");
                                res = false;
                            }
                            if (rsl == null || !rsl) {
                                log("Not all router admins updated their cache");
                                res = false;
                            }
                            if (!res) {
                                removeFromCache(manager.getAdminAddress());
                            }
                            return res;
                        }))
                .collect(Collectors.toList())
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(
                        k -> k ? "successCount" : "failureCount",
                        v -> 1,
                        (oldV, newV) -> oldV + 1));
        executor.shutdown();
        logResult(results);
    }

    private void logResult(Map<String, Integer> results) {
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                results.getOrDefault("successCount", 0), results.getOrDefault("failureCount", 0)));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}
