/**
 * Created on: 14.02.2011 09:52:58
 */
package ws.palladian.iirmodel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * <p>
 * Represents the author of an item stream. Authors publish items to item streams. They usually are unique for an item
 * source but two author with the same username are not necessarily unique cross-item stream. E. g. an author can be
 * identified uniquely by his username <b>and</b> stream source.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 2.0
 * @since 2.0
 */
@Entity
public class Author {

    /**
     * <p>
     * The absolute count of items this author has published. This value might or might not be the same as
     * {@code items.size()}. This depends on whether the count of items was extracted from a user profile page and if
     * the extractor for items missed some items during extraction. Missing items may happen if the stream was started
     * before extraction began and if old items are lost after some time. Web feed for example show such a behaviour.
     * </p>
     */
    private Integer countOfItems;

    /**
     * <p>
     * This is the amount of streams this author created by publishing the first item.
     * </p>
     */
    private Integer countOfStreamsStarted;

    /**
     * <p>
     * The rating of this author. A value often received from other authors for high quality items.
     * </p>
     */
    private Integer authorRating;

    /**
     * <p>
     * The system wide unqiue identifier of this author. This is created by the database and is not always the same as
     * its username. Usernames are ambiguous since the same username might occur for different users in different stream
     * sources.
     * </p>
     */
    @Id
    @GeneratedValue
    private Integer identifier;

    /**
     * <p>
     * The items created by this author.
     * </p>
     */
    @OneToMany(mappedBy = "author")
    private Collection<Item> items;

    /**
     * <p>
     * The date and time the author was created.
     * </p>
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date registeredSince;

    /**
     * <p>
     * The author's username identifying the author within a stream source.
     * </p>
     */
    private String username;

    /**
     * <p>
     * The source of streams this {@code Author} creates {@link Item}s for, such as <a
     * href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web Forum or other
     * kind of source the author's username is unique in.
     * </p>
     */
    private String streamSource;

    /**
     * <p>
     * Creates a new author with no initial values. Call this new objects setters before using the author and set all
     * properties to apropriate values.
     * </p>
     */
    protected Author() {
        super();
        this.items = new HashSet<Item>();
    }

    /**
     * <p>
     * Creates a new completely initialised {@code Author}.
     * </p>
     * 
     * @param username The author's username identifying the author within a stream source.
     * @param countOfItems The absolute count of items this author has published. This value might or might not be the
     *            same as {@code items.size()}. This depends on whether the count of items was extracted from a user
     *            profile page and if the extractor for items missed some items during extraction. Missing items may
     *            happen if the stream was started before extraction began and if old items are lost after some time.
     *            Web feed for example show such a behaviour.
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     * @param authorRating The rating of this author. A value often recieved from other authors for high quality items.
     * @param registeredSince The date and time the author was created.
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the author's username is unique in.
     */
    public Author(String username, Integer countOfItems, Integer countOfStreamsStarted, Integer authorRating,
            Date registeredSince, String streamSource) {
        this();
        this.countOfItems = countOfItems;
        this.countOfStreamsStarted = countOfStreamsStarted;
        this.authorRating = authorRating;
        this.registeredSince = registeredSince;
        this.username = username;
        this.streamSource = streamSource;
    }

    /* (non-Javadoc)
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
        Author other = (Author) obj;
        if (streamSource == null) {
            if (other.streamSource != null)
                return false;
        } else if (!streamSource.equals(other.streamSource))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    /**
     * <p>
     * Provides the author's rating.
     * </p>
     * 
     * @return The rating of this author. A value often received from other authors for high quality items.
     */
    public final Integer getAuthorRating() {
        return this.authorRating;
    }

    /**
     * <p>
     * The amount of items published by this author.
     * </p>
     * 
     * @return The absolute count of items this author has published. This value might or might not be the same as
     *         {@code items.size()}. This depends on whether the count of items was extracted from a user profile page
     *         and if the extractor for items missed some items during extraction. Missing items may happen if the
     *         stream was started before extraction began and if old items are lost after some time. Web feed for
     *         example show such a behaviour.
     */
    public final Integer getCountOfItems() {
        return this.countOfItems;
    }

    /**
     * <p>
     * Provides the amount of message item streams created by this author.
     * </p>
     * 
     * @return The amount of streams this author created by publishing the first item.
     * @see ItemStream
     */
    public final Integer getCountOfStreamsStarted() {
        return this.countOfStreamsStarted;
    }

    /**
     * <p>
     * The system wide unique identifier for this author. Generated and used by the persistence layer.
     * </p>
     * 
     * @return The system wide unqiue identifier of this author. This is created by the database and is not always the
     *         same as its username. Usernames are ambiguous since the same username might occur for different users in
     *         different stream sources.
     */
    public final Integer getIdentifier() {
        return this.identifier;
    }

    /**
     * <p>
     * A collection of all the items published by this author.
     * </p>
     * 
     * @return The items created by this author.
     */
    public final Collection<Item> getItems() {
        return this.items;
    }

    /**
     * <p>
     * Provides the data this author was created within its stream source.
     * </p>
     * 
     * @return The date and time the author was created.
     */
    public final Date getRegisteredSince() {
        return this.registeredSince;
    }

    /**
     * <p>
     * An identifier for a stream source, which is a system producing item streams. For example for web forum threads
     * this is the forum, for web feeds this is the web page the feed belongs to and for tweets it is twitter.
     * </p>
     * 
     * @return The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *         href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web Forum
     *         or other kind of source the author's username is unique in.
     */
    public String getStreamSource() {
        return streamSource;
    }

    /**
     * <p>
     * The author's username, which usually serves as identifier within its stream source.
     * </p>
     * 
     * @return The author's username identifying the author within a stream source.
     * @see #getStreamSource()
     */
    public final String getUsername() {
        return this.username;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((streamSource == null) ? 0 : streamSource.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's rating score.
     * </p>
     * 
     * @param authorRating The new rating of this author. A value often received from other authors for high quality
     *            items.
     */
    public final void setAuthorRating(Integer authorRating) {
        this.authorRating = authorRating;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's absolute count of created items.
     * </p>
     * 
     * @param countOfItems The absolute count of items this author has published. This value might or might not be the
     *            same as {@code items.size()}. This depends on whether the count of items was extracted from a user
     *            profile page and if the extractor for items missed some items during extraction. Missing items may
     *            happen if the stream was started before extraction began and if old items are lost after some time.
     *            Web feed for example show such a behaviour.
     */
    public final void setCountOfItems(Integer countOfItems) {
        this.countOfItems = countOfItems;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's absolute count of threads started.
     * </p>
     * 
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     */
    public final void setCountOfStreamsStarted(Integer countOfStreamsStarted) {
        this.countOfStreamsStarted = countOfStreamsStarted;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's system wide unique identifier. This method should be called by the
     * persistence layer only to serialize this author to a database.
     * </p>
     * 
     * @param identifier The system wide unqiue identifier of this author. This is created by the database and is not
     *            always the same as its username. Usernames are ambiguous since the same username might occur for
     *            different users in different stream sources.
     */
    public final void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's collection of published items.
     * </p>
     * 
     * @param items The items created by this author.
     */
    public final void setItems(Collection<Item> items) {
        this.items = items;
    }

    /**
     * <p>
     * Sets or resets and overwrites the date this author registered as member of the current stream source.
     * </p>
     * 
     * @param registeredSince The date and time the author was created.
     */
    public final void setRegisteredSince(Date registeredSince) {
        this.registeredSince = registeredSince;
    }

    /**
     * <p>
     * Sets or resets and overwrites a system wide unique identifier for the stream source this author creates items
     * for.
     * </p>
     * 
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the author's username is unique in.
     */
    public void setStreamSource(String streamSource) {
        this.streamSource = streamSource;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's username.
     * </p>
     * 
     * @param username The author's username identifying the author within a stream source.
     */
    public final void setUsername(String username) {
        this.username = username;
    }

    /**
     * @param item
     */
    public void addItem(Item item) {
        items.add(item);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Author [countOfItems=");
        builder.append(countOfItems);
        builder.append(", countOfStreamsStarted=");
        builder.append(countOfStreamsStarted);
        builder.append(", authorRating=");
        builder.append(authorRating);
        builder.append(", identifier=");
        builder.append(identifier);
        builder.append(", items=");
        builder.append(items);
        builder.append(", registeredSince=");
        builder.append(registeredSince);
        builder.append(", username=");
        builder.append(username);
        builder.append(", streamSource=");
        builder.append(streamSource);
        builder.append("]");
        return builder.toString();
    }

}
