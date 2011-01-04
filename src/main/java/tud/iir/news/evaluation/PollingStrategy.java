package tud.iir.news.evaluation;

/**
 * all available polling strategies
 * 
 * @author Sandro Reichert
 */
public enum PollingStrategy {
    MOVING_AVERAGE {
        public String toString() {
            return "Moving Average";
        }
    },
    POST_RATE {
        public String toString() {
            return "Post Rate";
        }
    },
    FIX_LEARNED {
        public String toString() {
            return "Fix Learned";
        }
    },
    FIX_1h {
        public String toString() {
            return "Fix 1h";
        }
    },
    FIX_1d {
        public String toString() {
            return "Fix 1d";
        }
    }
}