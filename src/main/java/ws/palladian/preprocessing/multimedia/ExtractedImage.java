package ws.palladian.preprocessing.multimedia;

/**
 * An extracted image.
 * 
 * @author David Urbansky
 */
public class ExtractedImage extends Image {
    private int rankCount = 1;
    private int duplicateCount = 0;

    public int getRankCount() {
        return rankCount;
    }

    public void setRankCount(int rankCount) {
        this.rankCount = rankCount;
    }

    public void addRanking(int ranking) {
        this.rankCount += ranking;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public void addDuplicate() {
        this.duplicateCount++;
    }

    public double getRanking() {
        double ranking = getDuplicateCount() + (1 / (double) getRankCount());
        return ranking;
    }

    @Override
    public String toString() {
        return getURL() + " | " + getRanking();
    }
}