package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
        verify(mockedService, times(4)).log("Not all router admins updated their cache");
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
        verify(mockedService, times(3)).log("Not all router admins updated their cache");
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
        verify(mockedService).log("Mount table cache refresher was interrupted.");
        verify(mockedService, times(1)).log("Not all router admins updated their cache");
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
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

}
