package ws.palladian.retrieval.facebook;

import java.util.Date;

public class FeedItem {
    private final String id;
    private final Date createdTime;
    private final String message;
    private final String story;

    FeedItem(String id, Date createdTime, String message, String story) {
        this.id = id;
        this.createdTime = createdTime;
        this.message = message;
        this.story = story;
    }

    public String getId() {
        return id;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public String getMessage() {
        return message;
    }

    public String getStory() {
        return story;
    }

    @Override
    public String toString() {
        return "FeedItem [id=" + id + ", createdTime=" + createdTime + ", message=" + message + ", story=" + story + "]";
    }

}
