package ws.palladian.extraction.location.scope;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * A Lucene {@link Analyzer} which can be configured using a Palladian {@link FeatureSetting}.
 * 
 * @author Philipp Katz
 * 
 */
public class FeatureSettingAnalyzer extends Analyzer {

    private final FeatureSetting featureSetting;

    private final Version luceneVersion;

    public FeatureSettingAnalyzer(FeatureSetting featureSetting) {
        this(featureSetting, Version.LUCENE_4_7);
    }

    public FeatureSettingAnalyzer(FeatureSetting featureSetting, Version luceneVersion) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        this.featureSetting = featureSetting;
        this.luceneVersion = luceneVersion;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        int minNGramLength = featureSetting.getMinNGramLength();
        int maxNGramLength = featureSetting.getMaxNGramLength();

        Tokenizer tokenizer;
        if (featureSetting.getTextFeatureType() == TextFeatureType.CHAR_NGRAMS) {
            tokenizer = new NGramTokenizer(minNGramLength, maxNGramLength);
        } else if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            tokenizer = new StandardTokenizer();
        } else {
            throw new UnsupportedOperationException("Unsupported text feature type: "
                    + featureSetting.getTextFeatureType());
        }

        TokenStream stream = new LowerCaseFilter(tokenizer);
        if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS && maxNGramLength > 1) {
            @SuppressWarnings("resource")
            ShingleFilter shingleFilter = new ShingleFilter(stream, Math.max(2, minNGramLength), maxNGramLength);
            if (minNGramLength > 1) {
                shingleFilter.setOutputUnigrams(false);
            }
            stream = shingleFilter;
        }
        stream = new LimitTokenCountFilter(stream, featureSetting.getMaxTerms());

        if (featureSetting.isWordUnigrams()) {
            stream = new LengthFilter(stream, featureSetting.getMinimumTermLength(),
                    featureSetting.getMaximumTermLength());
        }
        return new TokenStreamComponents(tokenizer, stream);
    }

    public List<String> analyze(String string) {
        List<String> result = new ArrayList<String>();
        TokenStream stream = null;
        try {
            stream = tokenStream(null, new StringReader(string));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            // not thrown b/c we're using a string reader...
            throw new RuntimeException(e);
        } finally {
            FileHelper.close(stream);
        }
        return result;
    }

    @Override
    public String toString() {
        return "FeatureSettingAnalyzer " + featureSetting;
    }

    public static void main(String[] args) {
        // FeatureSetting featureSetting = FeatureSettingBuilder.chars(1, 5).maxTerms(10).create();
        // FeatureSetting featureSetting = FeatureSettingBuilder.words(2).maxTerms(10).create();
        // FeatureSetting featureSetting = FeatureSettingBuilder.words(3).maxTerms(10).create();
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).termLength(4, 10).maxTerms(10).create();
        FeatureSettingAnalyzer analyzer = new FeatureSettingAnalyzer(featureSetting, Version.LUCENE_4_7);
        List<String> tokens = analyzer.analyze("The quick brown fox jumps over the lazy dog.");
        System.out.println(analyzer);
        CollectionHelper.print(tokens);
        analyzer.close();
    }

}
