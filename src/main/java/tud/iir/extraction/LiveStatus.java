package tud.iir.extraction;

public class LiveStatus {

    private double percent = 0;
    private String timeLeft = "";
    private String currentPhase = "";
    private String currentAction = "";
    private String logExcerpt = "";
    private String moreText1 = "";
    private String moreText2 = "";
    private long downloadedBytes = 0;

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public String getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(String timeLeft) {
        this.timeLeft = timeLeft;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
    }

    public String getLogExcerpt() {
        return logExcerpt;
    }

    public void setLogExcerpt(String logExcerpt) {
        this.logExcerpt = logExcerpt;
    }

    public String getMoreText1() {
        return moreText1;
    }

    public void setMoreText1(String moreText1) {
        this.moreText1 = moreText1;
    }

    public String getMoreText2() {
        return moreText2;
    }

    public void setMoreText2(String moreText2) {
        this.moreText2 = moreText2;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }
}
