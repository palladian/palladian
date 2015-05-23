package ws.palladian.helper.nlp;

public abstract class AbstractStringMetric implements StringMetric {

    @Override
    public double getDistance(String i1, String i2) {
        return 1 - getSimilarity(i1, i2);
    }

}
