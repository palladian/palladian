package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

/**
 * all available polling strategies in evaluation for ICWSM2011-paper
 * 
 * @author Sandro Reichert
 */
public enum PollingStrategy {
    /**
     * Polling strategy Moving Average, also known as adaptive. 
     */
    MOVING_AVERAGE {
        /**
         * Returns the name of this strategy formated to use it as header in a csv file.
         * 
         * @return the name of this strategy formated to use it as header in a csv file.
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return "Moving Average";
        }
    },
    /**
     * Polling strategy Post Rate, also known as probabilistic.
     */
    POST_RATE {
        /**
         * Returns the name of this strategy formated to use it as header in a csv file.
         * 
         * @return the name of this strategy formated to use it as header in a csv file.
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return "Post Rate";
        }
    },
    /**
     * Polling strategy Fix learned.
     */
    FIX_LEARNED {
        /**
         * Returns the name of this strategy formated to use it as header in a csv file.
         * 
         * @return the name of this strategy formated to use it as header in a csv file.
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return "Fix Learned";
        }
    },
    /**
     * Polling strategy Fix 1h, also known as fix60.
     */
    FIX_1h {
        /**
         * Returns the name of this strategy formated to use it as header in a csv file.
         * 
         * @return the name of this strategy formated to use it as header in a csv file.
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return "Fix 1h";
        }
    },
    /**
     * Polling strategy Fix 1d, also known as fix1440.
     */
    FIX_1d {
        /**
         * Returns the name of this strategy formated to use it as header in a csv file.
         * 
         * @return the name of this strategy formated to use it as header in a csv file.
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return "Fix 1d";
        }
    }
}