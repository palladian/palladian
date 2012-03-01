package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.util.ArrayList;


public class PollDataSeries extends ArrayList<PollData> {

    private static final long serialVersionUID = -6113047227556633165L;

    /**
     * Calculate the total download size of all polls.
     * 
     * @return The cumulated download size of all polls.
     */
    public double getTotalDownloadSize() {
        double downloadSize = 0;

        for (PollData pd : this) {
            downloadSize += pd.getDownloadSize();
        }

        return downloadSize;
    }

    /**
     * Get the total number of polls.
     * 
     * @return The total number of polls.
     */
    public int getNumberOfPolls() {
        return size();
    }

}
