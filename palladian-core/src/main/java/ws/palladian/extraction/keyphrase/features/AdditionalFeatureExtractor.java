package ws.palladian.extraction.keyphrase.features;

import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;

public class AdditionalFeatureExtractor implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    
    private final StopTokenRemover stopwords = new StopTokenRemover(Language.ENGLISH);
    
    final FeatureDescriptor<NominalFeature> STARTS_UPPERCASE = FeatureDescriptorBuilder.build("startsUppercase", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> COMPLETE_UPPERCASE = FeatureDescriptorBuilder.build("completelyUppercase", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> CONTAINS_NUMBERS = FeatureDescriptorBuilder.build("containsNumbers", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> IS_NUMBER = FeatureDescriptorBuilder.build("isNumber", NominalFeature.class);
    
    // context features
    final FeatureDescriptor<NominalFeature> PREVIOUS_STOPWORD = FeatureDescriptorBuilder.build("previousStop", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> NEXT_STOPWORD = FeatureDescriptorBuilder.build("nextStop", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> PREVIOUS_STARTS_UPPERCASE = FeatureDescriptorBuilder.build("previousStartsUppercase", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> NEXT_STARTS_UPPERCASE = FeatureDescriptorBuilder.build("nextStartsUppercase", NominalFeature.class);
    
    // 
    final FeatureDescriptor<NominalFeature> CASE_SIGNATURE = FeatureDescriptorBuilder.build("caseSignature", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> PREV_CASE_SIGNATURE = FeatureDescriptorBuilder.build("prevCaseSignature", NominalFeature.class);
    final FeatureDescriptor<NominalFeature> NEXT_CASE_SIGNATURE = FeatureDescriptorBuilder.build("nextCaseSignature", NominalFeature.class);
    
    

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        for (int i = 0; i < annotations.size(); i++) {
            Annotation annotation = annotations.get(i);
            String value = annotation.getValue();
            
            Boolean startsUppercase = Character.isUpperCase(value.charAt(0));
            Boolean completeUppercase = isCompletelyUppercase(value);
            Boolean containsNumbers = containsNumber(value);
            Boolean isNumber = isNumber(value);
            FeatureVector annotationFv = annotation.getFeatureVector();
            annotationFv.add(new NominalFeature(STARTS_UPPERCASE, startsUppercase.toString()));
            annotationFv.add(new NominalFeature(COMPLETE_UPPERCASE, completeUppercase.toString()));
            annotationFv.add(new NominalFeature(CONTAINS_NUMBERS, containsNumbers.toString()));
            annotationFv.add(new NominalFeature(IS_NUMBER, isNumber.toString()));
            
            // previous token
            Boolean previousStopword = false;
            Boolean previousStartsUppercase = false;
            String previousCaseSignature = "";
            if (i > 0) {
                String previousToken = annotations.get(i - 1).getValue();
                previousStopword = isStopword(previousToken);
                previousStartsUppercase = Character.isUpperCase(previousToken.charAt(0));
                previousCaseSignature = StringHelper.getCaseSignature(previousToken);
            }
            Boolean nextStopword = false;
            Boolean nextStartsUppercase = false;
            String nextCaseSignature = "";
            if (i < annotations.size() - 1) {
                String nextToken = annotations.get(i + 1).getValue();
                nextStopword = isStopword(nextToken);
                nextStartsUppercase = Character.isUpperCase(nextToken.charAt(0));
                nextCaseSignature = StringHelper.getCaseSignature(nextToken);
            }
            annotationFv.add(new NominalFeature(PREVIOUS_STOPWORD, previousStopword.toString()));
            annotationFv.add(new NominalFeature(NEXT_STOPWORD, nextStopword.toString()));
            annotationFv.add(new NominalFeature(PREVIOUS_STARTS_UPPERCASE, previousStartsUppercase.toString()));
            annotationFv.add(new NominalFeature(NEXT_STARTS_UPPERCASE, nextStartsUppercase.toString()));
            
            String caseSignature = StringHelper.getCaseSignature(value);
            annotationFv.add(new NominalFeature(CASE_SIGNATURE, caseSignature));
            annotationFv.add(new NominalFeature(PREV_CASE_SIGNATURE, previousCaseSignature));
            annotationFv.add(new NominalFeature(NEXT_CASE_SIGNATURE, nextCaseSignature));
        }
//        for (Annotation annotation : annotations) {
//        }
    }

    private boolean isStopword(String value) {
        return stopwords.isStopword(value.toLowerCase());
    }

    public static boolean isCompletelyUppercase(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isUpperCase(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    public static boolean containsNumber(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    public static boolean isNumber(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isDigit(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
