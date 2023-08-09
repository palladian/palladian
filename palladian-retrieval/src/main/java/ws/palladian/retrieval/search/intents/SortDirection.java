package ws.palladian.retrieval.search.intents;

public enum SortDirection {
    ASC("ASCENDING"), DESC("DESCENDING");

    private final String longVersion;

    SortDirection(String longVersion) {
        this.longVersion = longVersion;
    }

    public String getLongVersion() {
        return longVersion;
    }

    public static SortDirection getByName(String key) {
        for (SortDirection direction : values()) {
            if (direction.getLongVersion().equals(key)) {
                return direction;
            }
        }
        return valueOf(key);
    }
}
