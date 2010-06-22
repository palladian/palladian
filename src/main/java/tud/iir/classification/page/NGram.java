package tud.iir.classification.page;

import java.io.Serializable;

public class NGram implements Serializable {

    private static final long serialVersionUID = 8745021882046835435L;

    // the string of the ngram
    private String string = "";

    // the length of the ngram
    private int n = 0;

    // number of times the ngram occurred
    private int frequency = 0;

    // the inverse document frequency of the ngram
    private double idf = -1.0;

    // the index of the ngram, this makes it possible to refer to this ngram in the feature list
    private int index = 0;

    public NGram(String string) {
        super();
        this.string = string;
        this.frequency = 1;
        this.n = string.length();
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void increaseFrequency() {
        this.frequency++;
    }

    public double getIdf(int documentCount) {
        return (double) getFrequency() / (double) documentCount;
    }

    public double getIdf() {
        return this.idf;
    }

    public void calculateIdf(int documentCount) {
        this.idf = getIdf(documentCount);
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return getIndex() + " " + getString() + " " + getN() + " " + getFrequency();
    }
}
