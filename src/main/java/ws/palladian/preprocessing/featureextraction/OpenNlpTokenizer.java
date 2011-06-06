package ws.palladian.preprocessing.featureextraction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class OpenNlpTokenizer implements PipelineProcessor {
    
    private static final long serialVersionUID = 1L;
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";
    private final Tokenizer tokenizer;
    
    public OpenNlpTokenizer() {
        this(SimpleTokenizer.INSTANCE);
    }

    public OpenNlpTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }
    
    public OpenNlpTokenizer(String filePath) {
        TokenizerModel model = loadModel(filePath);
        this.tokenizer = new TokenizerME(model);
    }
    
    private TokenizerModel loadModel(String filePath) {
        InputStream modelIn = null;
        TokenizerModel model = null;
        try {
            modelIn = new FileInputStream(filePath);
            model = new TokenizerModel(modelIn);
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
        String content = document.getOriginalContent();
        AnnotationFeature annotationFeature = new AnnotationFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE);
        Span[] spans = tokenizer.tokenizePos(content);
        for (Span span : spans) {
            Annotation annotation = new PositionAnnotation(document, span.getStart(), span.getEnd());
            annotationFeature.addToken(annotation);
        }
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(annotationFeature);
    }
    
//    @Override
//    public void process(PipelineDocument document) {
//        FeatureVector featureVector = document.getFeatureVector();
//        AnnotationFeature sentenceFeature = (AnnotationFeature) featureVector.get(OpenNlpSentenceDetector.PROVIDED_FEATURE);
//        AnnotationFeature tokenFeature = new AnnotationFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE, document);
//
//        List<Annotation> sentences = sentenceFeature.getValue();
//        
//        for (Annotation sentence : sentences) {
//            
//            Span[] spans = tokenizer.tokenizePos(sentence.getValue());
//            
//            int sentenceStart = sentence.getStartPosition();
//            AnnotationFeature sentenceTokenFeature = new AnnotationFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE, document);
//            
//            for (Span span : spans) {
//                Annotation token = new Annotation(document);
//                token.setStartPosition(sentenceStart + span.getStart());
//                token.setEndPosition(sentenceStart + span.getEnd());
//                tokenFeature.addToken(token);
//            }
//            
//            sentence.getFeatureVector().add(sentenceTokenFeature);
//            
//        }
//        
//        featureVector.add(tokenFeature);
//    }


}
