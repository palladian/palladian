package ws.palladian.retrieval.search.images;

public enum ImageFormat {
    JPG, JPEG, PNG, SVG, GIF, BMP;

    public static ImageFormat getByName(String imageFormatName) {
        if (imageFormatName == null) {
            return null;
        }
        imageFormatName = imageFormatName.toUpperCase();
        for (ImageFormat value : values()) {
            if (value.name().equals(imageFormatName)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
