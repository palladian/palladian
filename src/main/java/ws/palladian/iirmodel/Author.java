/**
 * Created on: 14.02.2011 09:52:58
 */
package ws.palladian.iirmodel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Represents the author of an item stream. Authors publish items to item streams. They usually are unique for an item
 * source but two author with the same username are not necessarily unique cross-item stream.
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 2.0
 */
@Entity
public class Author {
    /**
     * The absolute count of items this author has published. This value might or might not be the same as
     * {@code items.size()}. This depends on whether the count of items was extracted from a user profile page and if
     * the
     * extractor for items missed some items during extraction. Missing items may happen if the stream was started
     * before extraction began and if old items are lost after some time. Web feed for example show such a behaviour.
     */
    private Integer countOfItems;

    /**
     * This is the amount of streams this author created by publishing the first item.
     */
    private Integer countOfStreamsStarted;
    /**
     * The rating of this author. A value often received from other authors for high quality items.
     */
    private Integer authorRating;
    /**
     * The system wide unqiue identifier of this author. This is created by the database and is not always the same as
     * its username. Usernames are ambiguous since the same username might occur for different users in different stream
     * sources.
     */
    @Id
    // @GeneratedValue
    private String identifier;
    /**
     * The items created by this author.
     */
    @OneToMany(mappedBy = "author")
    private Collection<Item> items;
    /**
     * The date and time the author was created.
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date registeredSince;
    /**
     * The authors username identifying the author within a stream source.
     */
    private String username;

    /**
     * The source of streams this {@code Author} creates {@link Item}s for, such as <a
     * href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web Forum or
     * other kind of source the authors username is unique in.
     */
    private String streamSource;

    /**
     * Creates a new author with no initial values. Call this new objects setters before using the author and set all
     * properties to apropriate values.
     */
    protected Author() {
        super();
        this.items = new HashSet<Item>();
    }

    /**
     * Creates a new completely initialised {@code Author}.
     * 
     * @param username The authors username identifying the author within a stream source.
     * @param countOfItems The absolute count of items this author has published. This value might or might not be the
     *            same as {@code items.size()}. This depends on whether the count of items was extracted from a user
     *            profile page and
     *            if the
     *            extractor for items missed some items during extraction. Missing items may happen if the stream was
     *            started
     *            before extraction began and if old items are lost after some time. Web feed for example show such a
     *            behaviour.
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     * @param authorRating The rating of this author. A value often recieved from other authors for high quality items.
     * @param registeredSince The date and time the author was created.
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or
     *            other kind of source the authors username is unique in.
     */
    public Author(String identifier, String username, Integer countOfItems, Integer countOfStreamsStarted,
            Integer authorRating, Date registeredSince, String streamSource) {
        this();
        this.identifier = identifier;
        this.countOfItems = countOfItems;
        this.countOfStreamsStarted = countOfStreamsStarted;
        this.authorRating = authorRating;
        this.registeredSince = registeredSince;
        this.username = username;
        this.streamSource = streamSource;
    }

    /**
     * Provides wether this author is the same as another one or not.
     * 
     * @param author
     * @see java.lang.Object#equals(java.lang.Object)
     * @return {@code true} if both authors have the same username and are members of the same stream source.
     * @see #setUsername(String)
     * @see #setStreamSource(String)
     */
    @Override
    public boolean equals(Object author) {
        if (this == author) {
            return true;
        }
        if (author == null) {
            return false;
        }
        if (getClass() != author.getClass()) {
            return false;
        }
        Author other = (Author)author;
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        } else if (!identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    /**
     * Provides the authors rating.
     * 
     * @return The rating of this author. A value often received from other authors for high quality items.
     */
    public final Integer getAuthorRating() {
        return this.authorRating;
    }

    /**
     * The amount of items published by this author.
     * 
     * @return The absolute count of items this author has published. This value might or might not be the same as
     *         {@code items.size()}. This depends on whether the count of items was extracted from a user profile page
     *         and if
     *         the
     *         extractor for items missed some items during extraction. Missing items may happen if the stream was
     *         started
     *         before extraction began and if old items are lost after some time. Web feed for example show such a
     *         behaviour.
     */
    public final Integer getCountOfItems() {
        return this.countOfItems;
    }

    /**
     * Provides the amount of message item streams created by this author.
     * 
     * @return The amount of streams this author created by publishing the first item.
     * @see ItemStream
     */
    public final Integer getCountOfStreamsStarted() {
        return this.countOfStreamsStarted;
    }

    /**
     * The system wide unique identifier for this author. Generated and used by the persistence layer.
     * 
     * @return The system wide unqiue identifier of this author. This is created by the database and is not always the
     *         same as
     *         its username. Usernames are ambiguous since the same username might occur for different users in
     *         different stream
     *         sources.
     */
    public final String getIdentifier() {
        return this.identifier;
    }

    /**
     * A collection of all the items published by this author.
     * 
     * @return The items created by this author.
     */
    public final Collection<Item> getItems() {
        return this.items;
    }

    /**
     * Provides the data this author was created within its stream source.
     * 
     * @return The date and time the author was created.
     */
    public final Date getRegisteredSince() {
        return this.registeredSince;
    }

    /**
     * An identifier for a stream source, which is a system producing item streams. For example for web forum threads
     * this is the forum, for web feeds this is the web page the feed belongs to and for tweets it is twitter.
     * 
     * @return The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *         href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web Forum
     *         or other kind of source the authors username is unique in.
     */
    public String getStreamSource() {
        return streamSource;
    }

    /**
     * The authors username, which usually serves as identifier within its stream source.
     * 
     * @return The authors username identifying the author within a stream source.
     * @see #getStreamSource()
     */
    public final String getUsername() {
        return this.username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    /**
     * Sets or resets and overwrites the authors rating score.
     * 
     * @param authorRating The new rating of this author. A value often received from other authors for high quality
     *            items.
     */
    public final void setAuthorRating(Integer authorRating) {
        this.authorRating = authorRating;
    }

    /**
     * Sets or resets and overwrites the authors absolute count of created items.
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
     * Sets or resets and overwrites the authors absolute count of threads started.
     * 
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     */
    public final void setCountOfStreamsStarted(Integer countOfStreamsStarted) {
        this.countOfStreamsStarted = countOfStreamsStarted;
    }

    /**
     * Sets or resets and overwrites the authors system wide unique identifier. This method should be called by the
     * persistence layer only to serialize this author to a database.
     * 
     * @param identifier The system wide unqiue identifier of this author. This is created by the database and is not
     *            always the same as its username. Usernames are ambiguous since the same username might occur for
     *            different users in different stream sources.
     */
    public final void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets or resets and overwrites the authors collection of published items.
     * 
     * @param items The items created by this author.
     */
    public final void setItems(Collection<Item> items) {
        this.items = items;
    }

    /**
     * Sets or resets and overwrites the date this author registered as member of the current stream source.
     * 
     * @param registeredSince The date and time the author was created.
     */
    public final void setRegisteredSince(Date registeredSince) {
        this.registeredSince = registeredSince;
    }

    /**
     * Sets or resets and overwrites a system wide unique identifier for the stream source this author creates items
     * for.
     * 
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the authors username is unique in.
     */
    public void setStreamSource(String streamSource) {
        this.streamSource = streamSource;
    }

    /**
     * Sets or resets and overwrites the authors username.
     * 
     * @param username The authors username identifying the author within a stream source.
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
}
