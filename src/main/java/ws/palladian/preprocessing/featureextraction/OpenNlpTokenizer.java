package ws.palladian.preprocessing.featureextraction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
        TokenFeature tokenFeature = new TokenFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE, document);
        Span[] spans = tokenizer.tokenizePos(content);
        for (Span span : spans) {
            Token token = new Token(document);
            token.setStartPosition(span.getStart());
            token.setEndPosition(span.getEnd());
            tokenFeature.addToken(token);
        }
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(tokenFeature);
    }
    
//    @Override
//    public void process(PipelineDocument document) {
//        FeatureVector featureVector = document.getFeatureVector();
//        TokenFeature sentenceFeature = (TokenFeature) featureVector.get(OpenNlpSentenceDetector.PROVIDED_FEATURE);
//        TokenFeature tokenFeature = new TokenFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE, document);
//
//        List<Token> sentences = sentenceFeature.getValue();
//        
//        for (Token sentence : sentences) {
//            
//            Span[] spans = tokenizer.tokenizePos(sentence.getValue());
//            
//            int sentenceStart = sentence.getStartPosition();
//            TokenFeature sentenceTokenFeature = new TokenFeature(ws.palladian.preprocessing.featureextraction.Tokenizer.PROVIDED_FEATURE, document);
//            
//            for (Span span : spans) {
//                Token token = new Token(document);
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
