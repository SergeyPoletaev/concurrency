package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class RestaurantService {

    private final Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};
    private final Map<String, LongAdder> stat = new ConcurrentHashMap<>();

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public static void main(String[] args) throws InterruptedException {
        Map<Object, LongAdder> map = new ConcurrentHashMap<>();

        map.computeIfAbsent("a", k -> new LongAdder()).increment();
        map.computeIfAbsent("a", k -> new LongAdder()).increment();
        System.out.println(map);
    }

    public Set<String> printStat() {
        return stat.keySet().stream()
                .map(rest -> rest + " - " + stat.get(rest).sum())
                .collect(Collectors.toSet());
    }

    public void addToStat(String restaurantName) {
//        stat.merge(restaurantName, 1L, (oldV, newV) -> oldV + 1);
        stat.computeIfAbsent(restaurantName, k -> new LongAdder()).increment();
    }
}
