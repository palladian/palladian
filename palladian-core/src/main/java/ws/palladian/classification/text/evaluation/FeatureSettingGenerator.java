package ws.palladian.classification.text.evaluation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.helper.functional.Factory;

/**
 * Factory for different feature setting combinations.
 * 
 * @author pk
 */
public final class FeatureSettingGenerator implements Factory<Set<FeatureSetting>> {

    private int minCharLength;
    private int maxCharLength;
    private int minWordLength;
    private int maxWordLength;
    private boolean withCombinations = true;

    /**
     * Evaluate character features in the given range [min,max].
     * 
     * @param min The minimum character n-gram length.
     * @param max The maximum character n-gram length.
     * @return The instance, builder pattern.
     */
    public FeatureSettingGenerator chars(int min, int max) {
        Validate.isTrue(min > 0, "min must be greater zero");
        Validate.isTrue(max >= min, "max must be greater/equal min");
        this.minCharLength = min;
        this.maxCharLength = max;
        return this;
    }

    /**
     * Evaluate word features in the given range [min,max].
     * 
     * @param min The minimum word n-gram length.
     * @param max The maximum word n-gram length.
     * @return The instance, builder pattern.
     */
    public FeatureSettingGenerator words(int min, int max) {
        Validate.isTrue(min > 0, "min must be greater zero");
        Validate.isTrue(max >= min, "max must be greater/equal min");
        this.minWordLength = min;
        this.maxWordLength = max;
        return this;
    }

    /**
     * Indicate, that no combinations (e.g. [2,3]-grams) should be generated.
     * 
     * @return The instance, builder pattern.
     */
    public FeatureSettingGenerator noCombinations() {
        this.withCombinations = false;
        return this;
    }

    @Override
    public Set<FeatureSetting> create() {
        Set<FeatureSetting> settings = new LinkedHashSet<>();
        if (minCharLength > 0) {
            for (int min = minCharLength; min <= maxCharLength; min++) {
                for (int max = min; max <= maxCharLength; max++) {
                    if (min == max || withCombinations) {
                        settings.add(FeatureSettingBuilder.chars(min, max).create());
                    }
                }
            }
        }
        if (minWordLength > 0) {
            for (int min = minWordLength; min <= maxWordLength; min++) {
                for (int max = min; max <= maxWordLength; max++) {
                    if (min == max || withCombinations) {
                        settings.add(FeatureSettingBuilder.words(min, max).create());
                    }
                }
            }
        }
        return settings;
    }

}