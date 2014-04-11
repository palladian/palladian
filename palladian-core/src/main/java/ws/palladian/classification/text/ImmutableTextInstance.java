package ws.palladian.classification.text;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.FeatureVectorBuilder;
import ws.palladian.core.ImmutableTextValue;
import ws.palladian.core.Instance;
import ws.palladian.core.TextValue;

public final class ImmutableTextInstance implements Instance {

    private final String category;
    private final FeatureVector featureVector;

    public ImmutableTextInstance(String text, String category) {
        Validate.notNull(text, "text must not be null");
        Validate.notNull(category, "category must not be null");
        TextValue textValue = new ImmutableTextValue(text);
        this.featureVector = new FeatureVectorBuilder().set(VECTOR_TEXT_IDENTIFIER, textValue).create();
        this.category = category;
    }

    @Override
    public FeatureVector getVector() {
        return featureVector;
    }

    @Override
    public String getCategory() {
        return category;
    }

}
