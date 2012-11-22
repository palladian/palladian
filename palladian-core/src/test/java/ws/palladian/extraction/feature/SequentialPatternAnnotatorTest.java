/**
 * Created on: 30.01.2012 15:22:30
 */
package ws.palladian.extraction.feature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.extraction.patterns.LabeledSequentialPatternExtractionStrategy;
import ws.palladian.extraction.patterns.NGramPatternExtractionStrategy;
import ws.palladian.extraction.patterns.SequentialPattern;
import ws.palladian.extraction.patterns.SequentialPatternAnnotator;
import ws.palladian.extraction.pos.OpenNlpPosTagger;
import ws.palladian.extraction.sentence.PalladianSentenceDetector;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureUtils;

/**
 * <p>
 * Tests whether the Labeled Sequential Pattern annotator works correct or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
@RunWith(Parameterized.class)
public class SequentialPatternAnnotatorTest {

    private final String inputText;
    private final String[] keywords;
    private final List<SequentialPattern> expectedPatterns;
    private final List<SequentialPattern> expectedNGramPatterns;

    public SequentialPatternAnnotatorTest(String inputText, String[] keywords,
            List<SequentialPattern> expectedPatterns, List<SequentialPattern> expectedNGramPatterns) {
        super();
        this.inputText = inputText;
        this.keywords = keywords;
        this.expectedPatterns = expectedPatterns;
        this.expectedNGramPatterns = expectedNGramPatterns;
    }

    @Parameters
    public static Collection<Object[]> testData() throws IOException {
        List<SequentialPattern> firstExtractedPatterns = new ArrayList<SequentialPattern>();
        List<SequentialPattern> thirdExtractedPatterns = new ArrayList<SequentialPattern>();
        List<SequentialPattern> secondExtractedPatterns = new ArrayList<SequentialPattern>();

        List<String> firstPattern = new ArrayList<String>();
        List<String> secondPattern = new ArrayList<String>();
        List<String> thirdPattern = new ArrayList<String>();
        List<String> fourthPattern = new ArrayList<String>();
        List<String> fifthPattern = new ArrayList<String>();

        firstPattern.addAll(Arrays.asList(new String[] {"This", "VBZ", "DT", "test"}));
        secondPattern.addAll(Arrays.asList(new String[] {"RB", ",", "this", "VBZ"}));
        thirdPattern.addAll(Arrays.asList(new String[] {"PRP", "VBP", ":", "IN"}));
        fourthPattern.addAll(Arrays.asList(new String[] {"PRP", "MD", "VB", "TO"}));
        fifthPattern.addAll(Arrays.asList(new String[] {"JJ", "NN", "VBZ", "DT"}));

        firstExtractedPatterns.add(new SequentialPattern("firstPattern", firstPattern));
        secondExtractedPatterns.add(new SequentialPattern("secondPattern", secondPattern));
        secondExtractedPatterns.add(new SequentialPattern("thirdPattern", thirdPattern));
        secondExtractedPatterns.add(new SequentialPattern("fourthPattern", fourthPattern));
        thirdExtractedPatterns.add(new SequentialPattern("fifthPattern", fifthPattern));

        SequentialPattern firstRunNGram1 = new SequentialPattern("firstRunNGram1", Arrays.asList(new String[] {"this",
                "VBZ", "DT"}));
        SequentialPattern firstRunNGram2 = new SequentialPattern("firstRunNGram2", Arrays.asList(new String[] {"VBZ",
                "DT", "test"}));
        SequentialPattern firstRunNGram3 = new SequentialPattern("firstRunNGram3", Arrays.asList(new String[] {"this",
                "VBZ", "DT", "test"}));

        List<SequentialPattern> firstRunNGrams = new ArrayList<SequentialPattern>(3);
        firstRunNGrams.add(firstRunNGram1);
        firstRunNGrams.add(firstRunNGram2);
        firstRunNGrams.add(firstRunNGram3);

        SequentialPattern secondRunNGram1 = new SequentialPattern("secondRunNGram1", Arrays.asList(new String[] {"RB",
                ",", "this"}));
        SequentialPattern secondRunNGram2 = new SequentialPattern("secondRunNGram2", Arrays.asList(new String[] {",",
                "this", "VBZ"}));
        SequentialPattern secondRunNGram3 = new SequentialPattern("secondRunNGram3", Arrays.asList(new String[] {
                "this", "VBZ", "the"}));
        SequentialPattern secondRunNGram4 = new SequentialPattern("secondRunNGram4", Arrays.asList(new String[] {"VBZ",
                "the", "RBS"}));
        SequentialPattern secondRunNGram5 = new SequentialPattern("secondRunNGram5", Arrays.asList(new String[] {"the",
                "RBS", "JJ"}));
        SequentialPattern secondRunNGram6 = new SequentialPattern("secondRunNGram6", Arrays.asList(new String[] {"RBS",
                "JJ", "NN"}));
        SequentialPattern secondRunNGram7 = new SequentialPattern("secondRunNGram7", Arrays.asList(new String[] {"JJ",
                "NN", "IN"}));
        SequentialPattern secondRunNGram8 = new SequentialPattern("secondRunNGram8", Arrays.asList(new String[] {"NN",
                "IN", "DT"}));
        SequentialPattern secondRunNGram9 = new SequentialPattern("secondRunNGram9", Arrays.asList(new String[] {"IN",
                "DT", "JJ"}));
        SequentialPattern secondRunNGram10 = new SequentialPattern("secondRunNGram10", Arrays.asList(new String[] {
                "DT", "JJ", "NN"}));
        SequentialPattern secondRunNGram11 = new SequentialPattern("secondRunNGram11", Arrays.asList(new String[] {
                "JJ", "NN", "CC"}));
        SequentialPattern secondRunNGram12 = new SequentialPattern("secondRunNGram12", Arrays.asList(new String[] {
                "NN", "CC", "POS"}));
        SequentialPattern secondRunNGram13 = new SequentialPattern("secondRunNGram13", Arrays.asList(new String[] {
                "CC", "POS", "NN"}));
        SequentialPattern secondRunNGram14 = new SequentialPattern("secondRunNGram14", Arrays.asList(new String[] {
                "POS", "NN", "RB"}));
        SequentialPattern secondRunNGram15 = new SequentialPattern("secondRunNGram15", Arrays.asList(new String[] {
                "NN", "RB", "VBN"}));
        SequentialPattern secondRunNGram16 = new SequentialPattern("secondRunNGram16", Arrays.asList(new String[] {
                "RB", "VBN", "."}));
        SequentialPattern secondRunNGram17 = new SequentialPattern("secondRunNGram17", Arrays.asList(new String[] {
                "RB", ",", "this", "VBZ"}));
        SequentialPattern secondRunNGram18 = new SequentialPattern("secondRunNGram18", Arrays.asList(new String[] {",",
                "this", "VBZ", "the"}));
        SequentialPattern secondRunNGram19 = new SequentialPattern("secondRunNGram19", Arrays.asList(new String[] {
                "this", "VBZ", "the", "RBS"}));
        SequentialPattern secondRunNGram20 = new SequentialPattern("secondRunNGram20", Arrays.asList(new String[] {
                "VBZ", "the", "RBS", "JJ"}));
        SequentialPattern secondRunNGram21 = new SequentialPattern("secondRunNGram21", Arrays.asList(new String[] {
                "the", "RBS", "JJ", "NN"}));
        SequentialPattern secondRunNGram22 = new SequentialPattern("secondRunNGram22", Arrays.asList(new String[] {
                "RBS", "JJ", "NN", "IN"}));
        SequentialPattern secondRunNGram23 = new SequentialPattern("secondRunNGram23", Arrays.asList(new String[] {
                "JJ", "NN", "IN", "DT"}));
        SequentialPattern secondRunNGram24 = new SequentialPattern("secondRunNGram24", Arrays.asList(new String[] {
                "NN", "IN", "DT", "JJ"}));
        SequentialPattern secondRunNGram25 = new SequentialPattern("secondRunNGram25", Arrays.asList(new String[] {
                "IN", "DT", "JJ", "NN"}));
        SequentialPattern secondRunNGram26 = new SequentialPattern("secondRunNGram26", Arrays.asList(new String[] {
                "DT", "JJ", "NN", "CC"}));
        SequentialPattern secondRunNGram27 = new SequentialPattern("secondRunNGram27", Arrays.asList(new String[] {
                "JJ", "NN", "CC", "POS"}));
        SequentialPattern secondRunNGram28 = new SequentialPattern("secondRunNGram28", Arrays.asList(new String[] {
                "NN", "CC", "POS", "NN"}));
        SequentialPattern secondRunNGram29 = new SequentialPattern("seconRunNGram29", Arrays.asList(new String[] {"CC",
                "POS", "NN", "RB"}));
        SequentialPattern secondRunNGram30 = new SequentialPattern("secondRunNGram30", Arrays.asList(new String[] {
                "POS", "NN", "RB", "VBN"}));
        SequentialPattern secondRunNGram31 = new SequentialPattern("secondRunNGram31", Arrays.asList(new String[] {
                "NN", "RB", "VBN", "."}));

        List<SequentialPattern> secondRunNGrams = new ArrayList<SequentialPattern>();
        secondRunNGrams.add(secondRunNGram1);
        secondRunNGrams.add(secondRunNGram2);
        secondRunNGrams.add(secondRunNGram3);
        secondRunNGrams.add(secondRunNGram4);
        secondRunNGrams.add(secondRunNGram5);
        secondRunNGrams.add(secondRunNGram6);
        secondRunNGrams.add(secondRunNGram7);
        secondRunNGrams.add(secondRunNGram8);
        secondRunNGrams.add(secondRunNGram9);
        secondRunNGrams.add(secondRunNGram10);
        secondRunNGrams.add(secondRunNGram11);
        secondRunNGrams.add(secondRunNGram12);
        secondRunNGrams.add(secondRunNGram13);
        secondRunNGrams.add(secondRunNGram14);
        secondRunNGrams.add(secondRunNGram15);
        secondRunNGrams.add(secondRunNGram16);
        secondRunNGrams.add(secondRunNGram17);
        secondRunNGrams.add(secondRunNGram18);
        secondRunNGrams.add(secondRunNGram19);
        secondRunNGrams.add(secondRunNGram20);
        secondRunNGrams.add(secondRunNGram21);
        secondRunNGrams.add(secondRunNGram22);
        secondRunNGrams.add(secondRunNGram23);
        secondRunNGrams.add(secondRunNGram24);
        secondRunNGrams.add(secondRunNGram25);
        secondRunNGrams.add(secondRunNGram26);
        secondRunNGrams.add(secondRunNGram27);
        secondRunNGrams.add(secondRunNGram28);
        secondRunNGrams.add(secondRunNGram29);
        secondRunNGrams.add(secondRunNGram30);
        secondRunNGrams.add(secondRunNGram31);

        List<SequentialPattern> thirdRunNGrams = new ArrayList<SequentialPattern>();

        return Arrays
                .asList(new Object[] {"This is a test", new String[] {"This", "test"}, firstExtractedPatterns,
                        firstRunNGrams},
                        new Object[] {
                                "honestly, this is the most bizarre reference t a JS library I've ever seen. I assume - if http access produces a valie JS file - that it just loads slower as the page so that it is missing when the objects are called. You may want to download the JS library and then link it from the application you build.",
                                new String[] {"this", "is", "the", "what,", "how", "who", "a"},
                                secondExtractedPatterns, secondRunNGrams}, new Object[] {
                                "Slow loading is the least of my problems right now... ", new String[] {},
                                thirdExtractedPatterns, thirdRunNGrams});
    }

    // TODO since the LSP algorithm is so slow this test should not run regularly.
    @Test
    @Ignore
    public final void testWithLSPExtractionStrategy() throws Exception {
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.connectToPreviousProcessor(new PalladianSentenceDetector());
        processingPipeline.connectToPreviousProcessor(new RegExTokenizer());
        processingPipeline.connectToPreviousProcessor(new OpenNlpPosTagger(ResourceHelper
                .getResourceFile("/model/en-pos-maxent.bin")));
        processingPipeline.connectToPreviousProcessor(new SequentialPatternAnnotator(keywords, 1, 4,
                new LabeledSequentialPatternExtractionStrategy()));

        TextDocument document = new TextDocument(inputText);

        processingPipeline.process(document);
        List<SequentialPattern> patterns = FeatureUtils.getFeaturesAtPath(document.getFeatureVector(),
                SequentialPattern.class, "ws.palladian.features.sentence/lsp");
        for (SequentialPattern pattern : expectedPatterns) {
            Assert.assertThat(pattern, Matchers.isIn(patterns));
        }
    }

    @Test
    public void testWithNGramExtractionStrategy() throws FileNotFoundException, DocumentUnprocessableException {
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.connectToPreviousProcessor(new PalladianSentenceDetector());
        processingPipeline.connectToPreviousProcessor(new LowerCaser());
        processingPipeline.connectToPreviousProcessor(new RegExTokenizer());
        processingPipeline.connectToPreviousProcessor(new OpenNlpPosTagger(ResourceHelper
                .getResourceFile("/model/en-pos-maxent.bin")));
        processingPipeline.connectToPreviousProcessor(new SequentialPatternAnnotator(keywords, 3, 4,
                new NGramPatternExtractionStrategy()));

        TextDocument document = new TextDocument(inputText);

        processingPipeline.process(document);

        List<SequentialPattern> extractedPatterns = FeatureUtils.getFeaturesAtPath(document.getFeatureVector(),
                SequentialPattern.class, "ws.palladian.features.sentence/lsp");
        Assert.assertThat(extractedPatterns,
                Matchers.hasItems(expectedNGramPatterns.toArray(new SequentialPattern[expectedNGramPatterns.size()])));
    }
}
