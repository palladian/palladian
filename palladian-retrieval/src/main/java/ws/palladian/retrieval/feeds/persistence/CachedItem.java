package ws.palladian.retrieval.feeds.persistence;

import java.util.Date;

/**
 * <p>Helper that is used to load cached items from database.</p>
 * 
 * @author Sandro Reichert
 * 
 */
public class CachedItem {

    /** Internal database identifier. */
    private int id = -1;

    private String hash = null;

    private Date correctedPublishDate = null;

    /**
     * @return the id
     */
    public final int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public final void setId(int id) {
        this.id = id;
    }

    /**
     * @return the hash
     */
    public final String getHash() {
        return hash;
    }

    /**
     * @param hash the hash to set
     */
    public final void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * @return the correctedPublishDate
     */
    public final Date getCorrectedPublishDate() {
        return correctedPublishDate;
    }

    /**
     * @param correctedPublishDate the correctedPublishDate to set
     */
    public final void setCorrectedPublishDate(Date correctedPublishDate) {
        this.correctedPublishDate = correctedPublishDate;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
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

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((correctedPublishDate == null) ? 0 : correctedPublishDate.hashCode());
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
        result = prime * result + id;
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CachedItem other = (CachedItem) obj;
        if (correctedPublishDate == null) {
            if (other.correctedPublishDate != null)
                return false;
        } else if (!correctedPublishDate.equals(other.correctedPublishDate))
            return false;
        if (hash == null) {
            if (other.hash != null)
                return false;
        } else if (!hash.equals(other.hash))
            return false;
        if (id != other.id)
            return false;
        return true;
    }

}
