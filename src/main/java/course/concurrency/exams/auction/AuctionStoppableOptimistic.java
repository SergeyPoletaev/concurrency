package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid =
            new AtomicMarkableReference<>(new Bid(0L, 0L, 0L), false);

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        do {
            if (latestBid.isMarked()) {
                return false;
            }
            if (bid.getPrice() <= latestBid.getReference().getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(latestBid.getReference(), bid, false, false));
        notifier.sendOutdatedMessage(latestBid.getReference());
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        boolean rsl;
        do {
            rsl = latestBid.attemptMark(latestBid.getReference(), true);
        } while (!rsl);
        return latestBid.getReference();
    }

    public boolean isTerminated() {
        return latestBid.isMarked();
    }
}
