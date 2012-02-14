package ws.palladian.preprocessing.featureextraction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class OpenNlpPosTagger implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    public static final String PROVIDED_FEATURE = "ws.palladian.features.pos";
    private final POSTagger posTagger;

    public OpenNlpPosTagger(String filePath) {
        POSModel model = loadModel(filePath);
        this.posTagger = new POSTaggerME(model);
    }

    private POSModel loadModel(String filePath) {
        InputStream modelIn = null;
        POSModel model = null;
        try {
            modelIn = new FileInputStream(filePath);
            model = new POSModel(modelIn);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                } catch (IOException e) {
                }
            }
        }
        return model;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException(
                    "Document content is not tokenized. Please use a tokenizer before using an OpenNLP POS Tagger.");
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        String[] tokenValues = new String[tokenList.size()];
        for (int i = 0; i < tokenList.size(); i++) {
            tokenValues[i] = tokenList.get(i).getValue();
        }
        String[] posTags = posTagger.tag(tokenValues);

        for (int i = 0; i < tokenList.size(); i++) {
            NominalFeature posFeature = new NominalFeature(PROVIDED_FEATURE, posTags[i]);
            tokenList.get(i).getFeatureVector().add(posFeature);
        }

    }

}
