package course.concurrency.exams.refactoring;

import java.util.List;
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

    private ExecutorService taskExecutor;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
        initTaskExecutor();
    }

    public void serviceStop() {
        taskExecutor.shutdown();
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initTaskExecutor() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
        taskExecutor = new ThreadPoolExecutor(100, 200, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(200), tf);
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
        List<MountTableRefresherTask> refreshTasks = routerStore.getCachedRecords().stream()
                .filter(rs -> rs.getAdminAddress() != null && rs.getAdminAddress().length() != 0) // this router has not enabled router admin.
                .map(rs -> getRefresher(rs.getAdminAddress()))
                .collect(Collectors.toList());
        if (!refreshTasks.isEmpty()) {
            invokeRefresh(refreshTasks);
        }
    }

    protected MountTableRefresherTask getRefresher(String adminAddress) {
        if (isLocalAdmin(adminAddress)) {
            /*
             * Local router's cache update does not require RPC call, so no need for
             * RouterClient
             */
            return getLocalRefresher(adminAddress);
        } else {
            return new MountTableRefresherTask(new Others.MountTableManager(adminAddress), adminAddress);
        }
    }

    protected MountTableRefresherTask getLocalRefresher(String adminAddress) {
        return new MountTableRefresherTask(new Others.MountTableManager("local"), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresherTask> refreshTasks) {
        /*
         * Wait for all the thread to complete, await method returns false if
         * refresh is not finished within specified time
         */
        CompletableFuture
                .allOf(refreshTasks.stream()
                        .map(task -> CompletableFuture.runAsync(task, taskExecutor)
                                .exceptionally(ex -> {
                                    ex.printStackTrace();
                                    return null;
                                })).toArray(CompletableFuture[]::new))
                .orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log("Not all router admins updated their cache");
                    return null;
                })
                .join();
        logResult(refreshTasks);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<MountTableRefresherTask> refresherTasks) {
        int successCount = 0;
        int failureCount = 0;
        for (MountTableRefresherTask mountTableRefreshTask : refresherTasks) {
            if (mountTableRefreshTask.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefreshTask.getAdminAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
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

    public void setTaskExecutor(ExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }
}
