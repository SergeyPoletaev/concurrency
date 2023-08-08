package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.List;

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

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));
        managersMocked.forEach(manager ->
                when(manager.refresh()).thenReturn(true));

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
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

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));
        managersMocked.forEach(manager ->
                when(manager.refresh()).thenReturn(false));

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
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

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));
        managersMocked.forEach(manager -> {
            if (manager.getAdminAddress().startsWith("123")) {
                when(manager.refresh()).thenReturn(true);
            } else {
                when(manager.refresh()).thenReturn(false);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=1,failureCount=3");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
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

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));
        managersMocked.forEach(manager -> {
            if (manager.getAdminAddress().startsWith("123")) {
                when(manager.refresh()).thenThrow(RuntimeException.class);
            } else {
                when(manager.refresh()).thenReturn(true);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("When the current thread is interrupted ")
    public void whenCurrentThreadIsInterruptedThenWriteToTheLog() {
        service.setCacheUpdateTimeout(5000);
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));

        when(managersMocked.get(0).refresh()).thenAnswer(i -> true);
        when(managersMocked.get(1).refresh()).thenAnswer(i -> {
            Thread.sleep(1000);
            return true;
        });
        when(managersMocked.get(2).refresh()).thenAnswer(i -> {
            Thread.sleep(2000);
            return true;
        });
        when(managersMocked.get(3).refresh()).thenAnswer(i -> {
            Thread.sleep(3000);
            return true;
        });

        Thread.currentThread().interrupt();

        mockedService.refresh();

        Assertions.assertFalse(Thread.currentThread().isInterrupted());
        verify(mockedService).log("Mount table entries cache refresh successCount=1,failureCount=3");
        verify(mockedService).log("Mount table cache refresher was interrupted.");
        verify(mockedService, never()).log("Not all router admins updated their cache");
        verify(routerClientsCache, times(3)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));
        managersMocked.forEach(manager -> {
            if (manager.getAdminAddress().startsWith("123")) {
                when(manager.refresh()).thenAnswer((Answer<Boolean>) invocation -> {
                    Thread.sleep(2000);
                    return true;
                });
            } else {
                when(manager.refresh()).thenReturn(true);
            }
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(mockedService, times(1)).log("Not all router admins updated their cache");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
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

        List<Others.MountTableManager> managersMocked = states.stream()
                .map(state -> Mockito.spy(new Others.MountTableManager(state.getAdminAddress())))
                .collect(toList());
        managersMocked.forEach(manager ->
                when(mockedService.getMountTableManager(manager.getAdminAddress())).thenReturn(manager));

        when(managersMocked.get(0).refresh()).thenAnswer(i -> {
            Thread.sleep(2000);
            return true;
        });
        when(managersMocked.get(1).refresh()).thenAnswer(i -> {
            Thread.sleep(3000);
            return true;
        });
        when(managersMocked.get(2).refresh()).thenAnswer(i -> {
            Thread.sleep(4000);
            return true;
        });
        when(managersMocked.get(3).refresh()).thenAnswer(i -> {
            Thread.sleep(500);
            return true;
        });

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=1,failureCount=3");
        verify(mockedService, times(1)).log("Not all router admins updated their cache");
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
        verify(routerClientsCache, times(3)).invalidate(anyString());
    }

}
