package ws.palladian.preprocessing.featureextraction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class Tokenizer implements PipelineProcessor {
    
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";

    private static final Pattern TOKENIZE_REGEXP = Pattern
            .compile(
                    "([A-Z]\\.)+|([\\p{L}\\w]+)([-\\.,]([\\p{L}\\w]+))*|\\.([\\p{L}\\w]+)|</?([\\p{L}\\w]+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process(PipelineDocument document) {
        String text = document.getOriginalContent();
        Matcher matcher = TOKENIZE_REGEXP.matcher(text);
        TokenFeature tokenFeature = new TokenFeature(PROVIDED_FEATURE, document);
        while (matcher.find()) {
            Token token = new Token(document,matcher.start(),matcher.end());
            tokenFeature.addToken(token);
        }
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(tokenFeature);
    }

}
