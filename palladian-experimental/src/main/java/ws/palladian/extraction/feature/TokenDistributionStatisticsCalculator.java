package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.extraction.feature.StemmerAnnotator.Mode;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Implementation for calculating statistically significant phrases, as described in <a href=
 * "http://blog.echen.me/2011/03/22/harry-potter-and-the-keyword-chaos-unsupervised-statistically-significant-phrases/"
 * >Unsupervised Statistically Significant Phrases</a> and in the Paper <a
 * href="http://bioinfo2.ugr.es/Publicaciones/PRE09.pdf">Level statistics of words: Finding keywords in literary texts
 * and symbolic sequences</a>. Implementation inspired by original Ruby code available <a
 * href="https://gist.github.com/971859">here</a>. Documents processed by this {@link PipelineProcessor} need to be
 * treated by a {@link BaseTokenizer} implementation and by {@link TokenMetricsCalculator} first to calculate tokens and
 * their counts. This extractor is aimed on longer texts like long articles, or books. Not on very short text fragments,
 * as term repetitions are relatively sparse in such cases.
 * </p>
 * 
 * <p>
 * Quote from the wegpage: <i>The idea is that uninformative words and phrases (e.g., “the” and “but”) have a more
 * random and uniform distribution than important words and phrases (e.g., names of main characters), which will tend to
 * form clusters. (Funnily, the quantum chaos paper containing the primes graph reverses these characterizations: there,
 * the primes are the ones that are repellent and spread out, while the random numbers are the ones that form
 * clusters.)</i>
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TokenDistributionStatisticsCalculator extends TextDocumentPipelineProcessor {

    /** Identifier for the feature provided by this class. */
    public static final String LEVEL_STATISTICS = "ws.palladian.features.tokens.level";

    /** Minimum number of occurrences for a token to be considered, if below, a value of zero will be assigned. */
    private static final int MIN_OCCURRENCE_COUNT = 10;

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> tokens = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
//        if (tokenAnnotations == null) {
//            throw new DocumentUnprocessableException(
//                    "Token annotations are missing, document needs to be processed by a Tokenizer in advance.");
//        }
//
//        List<Annotation<String>> tokens = tokenAnnotations.getValue();

        MultiMap<String, Long> tokenPositions = new MultiHashMap<String, Long>();
        final int numTokens = tokens.size();

        // collect all positions for specific tokens in text
        for (PositionAnnotation token : tokens) {
            tokenPositions.put(token.getValue(), Long.valueOf(token.getStartPosition()));
        }

        // calculate level statistics for all token values
        for (PositionAnnotation token : tokens) {

            NumericFeature countFeature = token.getFeatureVector().getFeature(NumericFeature.class, TokenMetricsCalculator.COUNT);
            if (countFeature == null) {
                throw new DocumentUnprocessableException("Necessary token count feature (\""
                        + TokenMetricsCalculator.COUNT + "\") is missing. Please use "
                        + TokenMetricsCalculator.class.getName() + " first.");
            }

            int count = countFeature.getValue().intValue();

            if (count < MIN_OCCURRENCE_COUNT) {
                token.getFeatureVector().add(new NumericFeature(LEVEL_STATISTICS, 0.));
                continue;
            }

            long[] tokenPosArray = ArrayUtils.toPrimitive(new ArrayList<Long>(tokenPositions.get(token.getValue())).toArray(new Long[0]));
            long[] distances = MathHelper.getDistances(tokenPosArray);
            double stdDev = MathHelper.getStandardDeviation(distances);
            double avg = MathHelper.getAverage(distances);
            double sigma = stdDev / avg;

            double p = (double)count / numTokens;

            double sigmaNorm = sigma / Math.sqrt(1 - p);
            double sigmaNormBracketed = (2. * count - 1) / (2. * count + 2);
            double sigmaNormSd = 1. / (Math.sqrt(count) * (1 + 2.8 * Math.pow(count, -0.865)));

            double c = (sigmaNorm - sigmaNormBracketed) / sigmaNormSd;
            token.getFeatureVector().add(new NumericFeature(LEVEL_STATISTICS, c));

        }

    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY));
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.add(new LengthTokenRemover(3));
        pipeline.add(new RegExTokenRemover("[A-Za-z0-9-]+", false));
        pipeline.add(new NGramCreator(2, 2));
        pipeline.add(new TokenMetricsCalculator());
        pipeline.add(new TokenDistributionStatisticsCalculator());
        pipeline.add(new DuplicateTokenRemover());

        TextDocument doc = new TextDocument(
                FileHelper.readFileToString("/Users/pk/Desktop/pg1661.txt"));
        pipeline.process(doc);

        List<PositionAnnotation> tokenAnnotations = BaseTokenizer.getTokenAnnotations(doc);
        for (PositionAnnotation annotation : tokenAnnotations) {
            NumericFeature levelStats = annotation.getFeatureVector().getFeature(NumericFeature.class, LEVEL_STATISTICS);
            if (levelStats.getValue() > 6) {
                System.out.println(annotation.getValue() + " / " + levelStats);
            }
        }

    }

}
