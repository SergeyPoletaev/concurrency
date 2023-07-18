package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public long createOrder(List<Item> items) {
        long id = ids.incrementAndGet();
        Order order = new Order(id, items, null, false, Order.Status.NEW);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order newOrder = currentOrders.computeIfPresent(
                orderId,
                (k, v) -> new Order(k, v.getItems(), paymentInfo, v.isPacked(), Order.Status.IN_PROGRESS)
        );
        if (newOrder != null && newOrder.checkStatus()) {
            deliver(newOrder);
        }
    }

    public void setPacked(long orderId) {
        Order newOrder = currentOrders.computeIfPresent(
                orderId,
                (k, v) -> new Order(k, v.getItems(), v.getPaymentInfo(), true, Order.Status.IN_PROGRESS)
        );
        if (newOrder != null && newOrder.checkStatus()) {
            deliver(newOrder);
        }
    }

    private void deliver(Order order) {
        /* ... */
        currentOrders.computeIfPresent(
                order.getId(),
                (k, v) -> new Order(k, v.getItems(), v.getPaymentInfo(), v.isPacked(), Order.Status.DELIVERED)
        );
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
