package ws.palladian.retrieval.feeds.persistence;

import java.util.Date;

/**
 * <p>
 * Helper that is used to load cached items from database.
 * </p>
 * 
 * @author Sandro Reichert
 * 
 */
public class CachedItem {

    /** Internal database identifier. */
    private final int id;

    private final String hash;

    private final Date correctedPublishDate;

    public CachedItem(int id, String hash, Date correctedPublishDate) {
        this.id = id;
        this.hash = hash;
        this.correctedPublishDate = correctedPublishDate;
    }

    /**
     * @return the id
     */
    public final int getId() {
        return id;
    }

    /**
     * @return the hash
     */
    public final String getHash() {
        return hash;
    }

    /**
     * @return the correctedPublishDate
     */
    public final Date getCorrectedPublishDate() {
        return correctedPublishDate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CachedItem [id=");
        builder.append(id);
        builder.append(", hash=");
        builder.append(hash);
        builder.append(", correctedPublishDate=");
        builder.append(correctedPublishDate);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + correctedPublishDate.hashCode();
        result = prime * result + hash.hashCode();
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CachedItem other = (CachedItem)obj;
        if (!correctedPublishDate.equals(other.correctedPublishDate)) {
            return false;
        }
        if (!hash.equals(other.hash)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        return true;
    }

}
