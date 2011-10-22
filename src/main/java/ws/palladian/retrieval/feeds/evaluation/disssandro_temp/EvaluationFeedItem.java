package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import ws.palladian.retrieval.feeds.FeedItem;

/**
 * Required for evaluation purpose. Extends a FeedItem by a sequence number to enumerate all items a feed has.
 * 
 * @author Sandro Reichert
 */
public class EvaluationFeedItem extends FeedItem implements Comparable<EvaluationFeedItem> {

    /**
     * The items sequence number. Items are enumerated by the chronological order when creating the dataset.
     */
    private Integer sequenceNumber = null;

    public EvaluationFeedItem() {
        super();
    }

    /**
     * The items sequence number. Items are enumerated by the chronological order when creating the dataset.
     * 
     * @return The items sequence number.
     */
    public final Integer getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * The items sequence number. Items are enumerated by the chronological order when creating the dataset.
     * 
     * @param sequenceNumber The items sequence number.
     */
    public final void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Compares two {@link EvaluationFeedItem}s by their {@link #getCorrectedPublishedDate()}. In case the dates are
     * equal, compare items by {@link #getHash()} lexicographically.
     */
    @Override
    public int compareTo(EvaluationFeedItem other) {
        if (getCorrectedPublishedDate().before(other.getCorrectedPublishedDate())) {
            return -1;
        } else if (getCorrectedPublishedDate().after(other.getCorrectedPublishedDate())) {
            return 1;
        } else {
            return getHash().compareTo(other.getHash());
        }
    }

}
