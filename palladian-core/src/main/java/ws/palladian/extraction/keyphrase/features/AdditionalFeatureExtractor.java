package ws.palladian.extraction.keyphrase.features;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;

public class AdditionalFeatureExtractor implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    
//    private final StopTokenRemover stopwords;
    
    final FeatureDescriptor<NominalFeature> STARTS_UPPERCASE = FeatureDescriptorBuilder.build("startsUppercase", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> COMPLETE_UPPERCASE = FeatureDescriptorBuilder.build("completelyUppercase", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> CONTAINS_NUMBERS = FeatureDescriptorBuilder.build("containsNumbers", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> IS_NUMBER = FeatureDescriptorBuilder.build("isNumber", NominalFeature.class);
    
    // context features
//    final FeatureDescriptor<NominalFeature> PREVIOUS_STOPWORD = FeatureDescriptorBuilder.build("previousStop", NominalFeature.class);
//    final FeatureDescriptor<NominalFeature> NEXT_STOPWORD = FeatureDescriptorBuilder.build("nextStop", NominalFeature.class);
//    final FeatureDescriptor<NominalFeature> PREVIOUS_STARTS_UPPERCASE = FeatureDescriptorBuilder.build("previousStartsUppercase", NominalFeature.class);
//    final FeatureDescriptor<NominalFeature> NEXT_STARTS_UPPERCASE = FeatureDescriptorBuilder.build("nextStartsUppercase", NominalFeature.class);
    
    // 
    final FeatureDescriptor<NominalFeature> CASE_SIGNATURE = FeatureDescriptorBuilder.build("caseSignature", NominalFeature.class);
//    final FeatureDescriptor<NominalFeature> PREV_CASE_SIGNATURE = FeatureDescriptorBuilder.build("prevCaseSignature", NominalFeature.class);
//    final FeatureDescriptor<NominalFeature> NEXT_CASE_SIGNATURE = FeatureDescriptorBuilder.build("nextCaseSignature", NominalFeature.class);
    
//    public AdditionalFeatureExtractor(Language language) {
//        this.stopwords =  new StopTokenRemover(Language.ENGLISH);
//    }    
    
    public AdditionalFeatureExtractor() {
    }
    

    @Override
    public void process(PipelineDocument document) {
        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        for (int i = 0; i < annotations.size(); i++) {
            Annotation annotation = annotations.get(i);
            FeatureVector featureVector = annotation.getFeatureVector();
            //String value = annotation.getValue();
            String value = featureVector.get(StemmerAnnotator.UNSTEM).getValue();
            
            Boolean startsUppercase = Character.isUpperCase(value.charAt(0));
            Boolean completeUppercase = StringUtils.isAllUpperCase(value);
            Boolean containsNumbers = containsNumber(value);
            Boolean isNumber = StringUtils.isNumeric(value);
            featureVector.add(new NominalFeature(STARTS_UPPERCASE, startsUppercase.toString()));
            featureVector.add(new NominalFeature(COMPLETE_UPPERCASE, completeUppercase.toString()));
            featureVector.add(new NominalFeature(CONTAINS_NUMBERS, containsNumbers.toString()));
            featureVector.add(new NominalFeature(IS_NUMBER, isNumber.toString()));
            
            // previous token
//            Boolean previousStopword = false;
//            Boolean previousStartsUppercase = false;
//            String previousCaseSignature = "";
//            if (i > 0) {
//                String previousToken = annotations.get(i - 1).getValue();
//                previousStopword = isStopword(previousToken);
//                previousStartsUppercase = Character.isUpperCase(previousToken.charAt(0));
//                previousCaseSignature = StringHelper.getCaseSignature(previousToken);
//            }
//            Boolean nextStopword = false;
//            Boolean nextStartsUppercase = false;
//            String nextCaseSignature = "";
//            if (i < annotations.size() - 1) {
//                String nextToken = annotations.get(i + 1).getValue();
//                nextStopword = isStopword(nextToken);
//                nextStartsUppercase = Character.isUpperCase(nextToken.charAt(0));
//                nextCaseSignature = StringHelper.getCaseSignature(nextToken);
//            }
//            featureVector.add(new NominalFeature(PREVIOUS_STOPWORD, previousStopword.toString()));
//            featureVector.add(new NominalFeature(NEXT_STOPWORD, nextStopword.toString()));
//            featureVector.add(new NominalFeature(PREVIOUS_STARTS_UPPERCASE, previousStartsUppercase.toString()));
//            featureVector.add(new NominalFeature(NEXT_STARTS_UPPERCASE, nextStartsUppercase.toString()));
            
            String caseSignature = StringHelper.getCaseSignature(value);
            featureVector.add(new NominalFeature(CASE_SIGNATURE, caseSignature));
//            featureVector.add(new NominalFeature(PREV_CASE_SIGNATURE, previousCaseSignature));
//            featureVector.add(new NominalFeature(NEXT_CASE_SIGNATURE, nextCaseSignature));
        }
    }

//    private boolean isStopword(String string) {
//        return stopwords.isStopword(string.toLowerCase());
//    }
    public static boolean containsNumber(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
