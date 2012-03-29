/**
 * Created on: 14.02.2011 09:52:58
 */
package ws.palladian.iirmodel;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * <p>
 * Represents the author of an item stream. Authors publish items to item streams. They usually are unique for an item
 * source but two author with the same username are not necessarily unique cross-item stream. E. e. an author can be
 * identified uniquely by his username <b>and</b> stream source (see {@link #hashCode()} and {@link #equals(Object)}).
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 2.0
 * @since 2.0
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"USERNAME", "STREAMSOURCEADDRESS"})}, name = "Author")
public class Author {

    /**
     * <p>
     * The absolute count of items this author has published. This value might or might not be the same as
     * {@code items.size()}. This depends on whether the count of items was extracted from a user profile page and if
     * the extractor for items missed some items during extraction. Missing items may happen if the stream was started
     * before extraction began and if old items are lost after some time. Web feed for example show such a behavior.
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
     * The system wide unique identifier of this author. This is created by the database and is not always the same as
     * its username. Usernames are ambiguous since the same username might occur for different users in different stream
     * sources.
     * </p>
     */
    @Id
    @GeneratedValue(generator = "gentable")
    @TableGenerator(name = "gentable", table = "SEQUENCE", pkColumnName = "SEQ_COUNT", valueColumnName = "SEQ_NAME", pkColumnValue = "SEQ_GEN")
    private Integer identifier;

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
     * href="http://www.twitter.com">Twitter</a>, <a href="http://www.facebook.com">Facebook</a>, any Web Forum or other
     * kind of source the author's username is unique in.
     * </p>
     */
    private String streamSourceAddress;

    /**
     * <p>
     * Creates a new author with no initial values. To be used by the ORM layer.
     * </p>
     */
    protected Author() {
        super();
    }

    /**
     * <p>
     * Creates a new {@link Author} initialized with essential values.
     * </p>
     * 
     * @param username The author's username identifying the author within a stream source.
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://www.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the author's username is unique in.
     */
    public Author(String username, String streamSourceAddress) {
        this();
        this.username = username;
        this.streamSourceAddress = streamSourceAddress;
    }

    /**
     * <p>
     * Creates a new completely initialized {@code Author}.
     * </p>
     * 
     * @param username The author's username identifying the author within a stream source.
     * @param countOfItems The absolute count of items this author has published. This value might or might not be the
     *            same as {@code items.size()}. This depends on whether the count of items was extracted from a user
     *            profile page and if the extractor for items missed some items during extraction. Missing items may
     *            happen if the stream was started before extraction began and if old items are lost after some time.
     *            Web feed for example show such a behavior.
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     * @param authorRating The rating of this author. A value often received from other authors for high quality items.
     * @param registeredSince The date and time the author was created.
     * @param streamSource The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the author's username is unique in.
     */
    public Author(String username, Integer countOfItems, Integer countOfStreamsStarted, Integer authorRating,
            Date registeredSince, String streamSourceAddress) {
        this();
        this.countOfItems = countOfItems;
        this.countOfStreamsStarted = countOfStreamsStarted;
        this.authorRating = authorRating;
        this.registeredSince = registeredSince;
        this.username = username;
        this.streamSourceAddress = streamSourceAddress;
    }

    /**
     * <p>
     * Provides the author's rating.
     * </p>
     * 
     * @return The rating of this author. A value often received from other authors for high quality items.
     */
    public Integer getAuthorRating() {
        return this.authorRating;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's rating score.
     * </p>
     * 
     * @param authorRating The new rating of this author. A value often received from other authors for high quality
     *            items.
     */
    public void setAuthorRating(Integer authorRating) {
        this.authorRating = authorRating;
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
     *         example show such a behavior.
     */
    public Integer getCountOfItems() {
        return this.countOfItems;
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
     *            Web feed for example show such a behavior.
     */
    public void setCountOfItems(Integer countOfItems) {
        this.countOfItems = countOfItems;
    }

    /**
     * <p>
     * Provides the amount of message item streams created by this author.
     * </p>
     * 
     * @return The amount of streams this author created by publishing the first item.
     * @see ItemStream
     */
    public Integer getCountOfStreamsStarted() {
        return this.countOfStreamsStarted;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's absolute count of threads started.
     * </p>
     * 
     * @param countOfStreamsStarted This is the amount of streams this author created by publishing the first item.
     */
    public void setCountOfStreamsStarted(Integer countOfStreamsStarted) {
        this.countOfStreamsStarted = countOfStreamsStarted;
    }

    /**
     * <p>
     * The system wide unique identifier for this author. Generated and used by the persistence layer.
     * </p>
     * 
     * @return The system wide unique identifier of this author. This is created by the database and is not always the
     *         same as its username. Usernames are ambiguous since the same username might occur for different users in
     *         different stream sources.
     */
    public Integer getIdentifier() {
        return this.identifier;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's system wide unique identifier. This method should be called by the
     * persistence layer only to serialize this author to a database.
     * </p>
     * 
     * @param identifier The system wide unique identifier of this author. This is created by the database and is not
     *            always the same as its username. Usernames are ambiguous since the same username might occur for
     *            different users in different stream sources.
     */
    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    /**
     * <p>
     * Provides the data this author was created within its stream source.
     * </p>
     * 
     * @return The date and time the author was created.
     */
    public Date getRegisteredSince() {
        return this.registeredSince;
    }

    /**
     * <p>
     * Sets or resets and overwrites the date this author registered as member of the current stream source.
     * </p>
     * 
     * @param registeredSince The date and time the author was created.
     */
    public void setRegisteredSince(Date registeredSince) {
        this.registeredSince = registeredSince;
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
        return streamSourceAddress;
    }

    /**
     * <p>
     * Sets or resets and overwrites a system wide unique identifier for the stream source this author creates items
     * for.
     * </p>
     * 
     * @param streamSourceAddress The source of streams this {@code Author} creates {@link Item}s for, such as <a
     *            href="http://www.twitter.com">Twitter</a>, <a href="http://ww.facebook.com">Facebook</a>, any Web
     *            Forum or other kind of source the author's username is unique in.
     */
    public void setStreamSource(String streamSourceAddress) {
        this.streamSourceAddress = streamSourceAddress;
    }

    /**
     * <p>
     * The author's username, which usually serves as identifier within its stream source.
     * </p>
     * 
     * @return The author's username identifying the author within a stream source.
     * @see #getStreamSource()
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * <p>
     * Sets or resets and overwrites the author's username.
     * </p>
     * 
     * @param username The author's username identifying the author within a stream source.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Author other = (Author)obj;
        if (streamSourceAddress == null) {
            if (other.streamSourceAddress != null) {
                return false;
            }
        } else if (!streamSourceAddress.equals(other.streamSourceAddress)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((streamSourceAddress == null) ? 0 : streamSourceAddress.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
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
        // builder.append(", items=");
        // builder.append(items);
        builder.append(", registeredSince=");
        builder.append(registeredSince);
        builder.append(", username=");
        builder.append(username);
        // builder.append(", streamSource=");
        // builder.append(streamSource);
        builder.append("]");
        return builder.toString();
    }

}
