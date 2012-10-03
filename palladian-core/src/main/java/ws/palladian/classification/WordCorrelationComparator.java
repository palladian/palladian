package ws.palladian.classification;

import java.util.Comparator;

class WordCorrelationComparator implements Comparator<WordCorrelation> {

    @Override
    public int compare(WordCorrelation o1, WordCorrelation o2) {
        return (int)(1000000 * o2.getRelativeCorrelation() - 1000000 * o1.getRelativeCorrelation());
    }

}
