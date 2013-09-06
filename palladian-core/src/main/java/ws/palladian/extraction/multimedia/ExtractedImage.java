package ws.palladian.extraction.multimedia;

import java.awt.image.BufferedImage;

import ws.palladian.retrieval.search.images.WebImageResult;

/**
 * <p>
 * An extracted image.
 * </p>
 * 
 * @author David Urbansky
 */
class ExtractedImage extends WebImageResult {

    private int rankCount = 1;
    private int duplicateCount = 0;
    private final BufferedImage imageContent;

    public ExtractedImage(WebImageResult image, BufferedImage imageContent) {
        super(image);
        this.imageContent = imageContent;
    }

    public BufferedImage getImageContent() {
        return imageContent;
    }

    public int getRankCount() {
        return rankCount;
    }

    public void addRanking(int ranking) {
        this.rankCount += ranking;
    }

    public void addDuplicate() {
        this.duplicateCount++;
    }

    public double getRanking() {
        return duplicateCount + 1. / getRankCount();
    }

    @Override
    public String toString() {
        return getUrl() + " | " + getRanking();
    }
}