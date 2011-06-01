package ws.palladian.preprocessing.featureextraction;


import ws.palladian.classification.page.Stopwords;
import ws.palladian.classification.page.Stopwords.Predefined;

public class StopTokenRemover extends TokenRemover {
    
    private Stopwords stopwords;

    public StopTokenRemover(Predefined predefined) {
        stopwords = new Stopwords(predefined);
    }

    public StopTokenRemover(String filePath) {
        stopwords = new Stopwords(filePath);
    }
    
    public StopTokenRemover() {
        this(Predefined.EN);
    }

    @Override
    protected boolean remove(Annotation annotation) {
        return stopwords.contains(annotation.getValue());
    }

}
