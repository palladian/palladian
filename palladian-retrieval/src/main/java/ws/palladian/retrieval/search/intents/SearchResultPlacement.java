package ws.palladian.retrieval.search.intents;

import java.util.HashMap;
import java.util.Map;

public class SearchResultPlacement {
    /**
     * The positional index of the placement.
     */
    private int position = 0;

    /**
     * An identifier of the object (e.g. a product id).
     */
    private String id;

    /**
     * Optional content of the placement, e.g. an HTML banner.
     */
    private String content;

    /**
     * A type for the placement to identify how to handle it (e.g. pull product information by id or read content as HTML.
     */
    private String type;

    private Map<String, Object> metaData = new HashMap<>();

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "SearchResultPlacement{" +
                "position=" + position +
                ", id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", type='" + type + '\'' +
                ", metaData=" + metaData +
                '}';
    }
}
