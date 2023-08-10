package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(4, 4, 0L,
                        TimeUnit.SECONDS, new LinkedBlockingQueue<>(), tf);
        service.setTaskExecutor(executor);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task ->
                when(task.getManager().refresh()).thenReturn(true));

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task ->
                when(task.getManager().refresh()).thenReturn(false));

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task -> {
            if (task.getAdminAddress().startsWith("123")) {
                when(task.getManager().refresh()).thenReturn(true);
            } else {
                when(task.getManager().refresh()).thenReturn(false);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=1,failureCount=3");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(3)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task -> {
            if (task.getAdminAddress().startsWith("123")) {
                when(task.getManager().refresh()).thenThrow(RuntimeException.class);
            } else {
                when(task.getManager().refresh()).thenReturn(true);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task -> {
            if (task.getAdminAddress().startsWith("123")) {
                when(task.getManager().refresh()).thenAnswer(invocation -> {
                    Thread.sleep(2000);
                    return true;
                });
            } else {
                when(task.getManager().refresh()).thenReturn(true);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(mockedService, times(1)).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("Several task exceeds timeout")
    public void severalTaskExceedTimeout() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<MountTableRefresherTask> refresherTasks = states.stream()
                .map(state ->
                        new MountTableRefresherTask(
                                Mockito.spy(new Others.MountTableManager(state.getAdminAddress())), state.getAdminAddress()))
                .collect(toList());
        refresherTasks.forEach(task ->
                when(mockedService.getRefresher(task.getAdminAddress())).thenReturn(task));
        refresherTasks.forEach(task ->
                when(task.getManager().refresh()).thenReturn(false));

        when(refresherTasks.get(0).getManager().refresh()).thenAnswer(i -> {
            Thread.sleep(2000);
            return true;
        });
        when(refresherTasks.get(1).getManager().refresh()).thenAnswer(i -> {
            Thread.sleep(3000);
            return true;
        });
        when(refresherTasks.get(2).getManager().refresh()).thenAnswer(i -> {
            Thread.sleep(4000);
            return true;
        });
        when(refresherTasks.get(3).getManager().refresh()).thenAnswer(i -> {
            Thread.sleep(500);
            return true;
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=1,failureCount=3");
        verify(mockedService, times(1)).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(3)).invalidate(anyString());
    }

}
