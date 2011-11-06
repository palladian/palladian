package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class StemmerAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    public static final String PROVIDED_FEATURE = "ws.palladian.features.unstemed";
    private SnowballStemmer stemmer;
    
    public StemmerAnnotator() {
        this(new englishStemmer());
    }
    
    public StemmerAnnotator(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        for (Annotation annotation : annotations) {
            String unstemmed = annotation.getValue();
            String stem = stem(unstemmed);
            annotation.setValue(stem);
            NominalFeature stemFeature = new NominalFeature(PROVIDED_FEATURE, unstemmed);
            annotation.getFeatureVector().add(stemFeature);
        }
    }

    private String stem(String string) {
//        stemmer.setCurrent(annotation.getValue());
//        stemmer.stem();
//        String stem = stemmer.getCurrent();
//        return stem;
        String[] split = string.split("\\s|\\-");
        for (int i = 0; i < split.length; i++) {
            stemmer.setCurrent(split[i].toLowerCase());
            stemmer.stem();
            split[i] = stemmer.getCurrent();
        }
        return StringUtils.join(split, " ");
        
    }
    
    public static void main(String[] args) {
        SnowballStemmer st = new englishStemmer();
        st.setCurrent("criterion");
        st.stem();
        System.out.println(st.getCurrent());
        
    }

}
