package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;
    private volatile Bid latestBid = new Bid(0L, 0L, 0L);
    private volatile boolean isTerminated;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        if (isTerminated) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (bid.getPrice() > latestBid.getPrice()) {
            synchronized (this) {
                if (isTerminated) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                if (bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        isTerminated = true;
        synchronized (this) {
            return latestBid;
        }
    }
}
