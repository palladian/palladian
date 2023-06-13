package ws.palladian.extraction.text.vector;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.TextValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.ValueDefinitions;
import ws.palladian.extraction.token.Tokenizer;

import java.util.List;
import java.util.Objects;

public class EmbeddingTextVectorizer extends AbstractDatasetFeatureVectorTransformer implements ITextVectorizer {
    private final String inputFeatureName;
    private final WordVectorDictionary dictionary;

    public EmbeddingTextVectorizer(String inputFeatureName, WordVectorDictionary dictionary) {
        this.inputFeatureName = Objects.requireNonNull(inputFeatureName, "inputFeatureName must not be null");
        this.dictionary = Objects.requireNonNull(dictionary, "dictionary must not be null");
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        FeatureInformationBuilder builder = new FeatureInformationBuilder();
        for (int i = 0; i < dictionary.vectorSize(); i++) {
            builder.set("embedding-" + i, ValueDefinitions.floatValue());
        }
        return builder.create();
    }

    @Override
    public FeatureVector apply(FeatureVector featureVector) {
        String textValue = getTextValue(featureVector);
        if (!dictionary.isCaseSensitive()) {
            textValue = textValue.toLowerCase();
        }
        List<String> words = Tokenizer.tokenize(textValue.toLowerCase());

        float[] documentVector = new float[dictionary.vectorSize()];
        for (String word : words) {
            float[] wordVector = dictionary.getVector(word);
            if (wordVector != null) {
                documentVector = FloatVectorUtil.add(documentVector, wordVector);
            }
        }
        if (words.size() > 0) {
            documentVector = FloatVectorUtil.scalar(documentVector, 1f / words.size());
            // documentVector = FloatVectorUtil.normalize(documentVector);
        }

        InstanceBuilder vectorBuilder = new InstanceBuilder();
        for (int i = 0; i < documentVector.length; i++) {
            vectorBuilder.set("embedding-" + i, documentVector[i]);
        }

        return vectorBuilder.create();
    }

    // XXX copied from TextVectorizer
    private String getTextValue(FeatureVector featureVector) {
        Value value = featureVector.get(inputFeatureName);
        if (value instanceof NominalValue) {
            return ((NominalValue) value).getString();
        } else if (value instanceof TextValue) {
            return ((TextValue) value).getText();
        }
        throw new IllegalArgumentException("Invalid type: " + value.getClass().getName());
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", this.getClass().getSimpleName(), dictionary);
    }

}
