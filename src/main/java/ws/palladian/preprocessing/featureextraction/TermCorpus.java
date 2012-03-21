package ws.palladian.preprocessing.featureextraction;

import java.util.Set;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public abstract class TermCorpus {

    protected static final String SEPARATOR = "#";

    private int numDocs;

    public TermCorpus() {
        this.numDocs = 0;
    }

    public abstract void addTermsFromDocument(Set<String> terms);

    public double getDf(String term) {
        int termCount = getTermCount(term);
        // add 1; prevent division by zero
        double documentFrequency = Math.log10((double) getNumDocs() / (termCount + 1));
        return documentFrequency;
    }

    public int getNumDocs() {
        return numDocs;
    }

    protected void setNumDocs(int numDocs) {
        this.numDocs = numDocs;
    }

    protected void incrementNumDocs() {
        numDocs++;
    }

    protected abstract void setDf(String term, int df);

    public abstract void save(String fileName);

    public void load(String fileName) {
        FileHelper.performActionOnEveryLine(fileName, new LineAction() {
            @Override
            public void performAction(String text, int number) {
                if (number % 100000 == 0) {
                    System.out.println(number);
                }
                if (number > 1) {
                    String[] split = text.split(SEPARATOR);
                    if (split.length > 2) {
                        System.err.println(text);
                        return;
                    }
                    setDf(split[0], Integer.parseInt(split[1]));
                } else if (text.startsWith("numDocs" + SEPARATOR + SEPARATOR + SEPARATOR)) {
                    String[] split = text.split(SEPARATOR + SEPARATOR + SEPARATOR);
                    setNumDocs(Integer.parseInt(split[1]));
                }
            }
        });
    }

    protected abstract int getTermCount(String term);

}