package ws.palladian.retrieval.search.videos;

import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.resources.WebImage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoPlaylist {
    private String id;
    private String title;
    private String description;
    private int numberOfItems = 0;
    private List<WebImage> thumbnails = new ArrayList<>();
    private Date publishDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public List<WebImage> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<WebImage> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }
}
