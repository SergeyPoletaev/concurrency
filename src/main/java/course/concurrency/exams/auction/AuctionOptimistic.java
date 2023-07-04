package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(0L, 0L, 0L));

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid latestBid;
        do {
            latestBid = this.latestBid.get();
            if (bid.getPrice() <= latestBid.getPrice()) {
                return false;
            }
        } while (!this.latestBid.compareAndSet(latestBid, bid));
        notifier.sendOutdatedMessage(latestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
