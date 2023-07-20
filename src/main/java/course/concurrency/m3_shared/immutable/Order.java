package course.concurrency.m3_shared.immutable;

import java.util.Collections;
import java.util.List;

public final class Order {

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = items;
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public boolean checkStatus() {
        return items != null
                && !items.isEmpty()
                && paymentInfo != null
                && isPacked
                && status == Status.PAID_AND_PACKED;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        NEW, IN_PROGRESS, DELIVERED, PAID, PACKED, PAID_AND_PACKED
    }
}
