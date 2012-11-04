package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.LingPipeTokenizer;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.features.Annotation;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.util.FastCache;

/**
 * <p>
 * POS tagger based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>.
 * </p>
 * 
 * @see Look <a href="http://alias-i.com/lingpipe/web/models.html">here</a> for models.
 * @author Martin Wunderwald
 * @author Philipp Katz
 */
public final class LingPipePosTagger extends BasePosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LingPipePosTagger.class);

    /** The name of this {@link PosTagger}. */
    private static final String TAGGER_NAME = "LingPipe POS-Tagger";

    /** The model used by the LingPipe POS tagger. */
    private final HiddenMarkovModel model;

    /** The tokenizer used by the LingPipe POS tagger. */
    private static final LingPipeTokenizer TOKENIZER = new LingPipeTokenizer();

    /**
     * <p>
     * Instantiate a new LingPipe POS tagger from the given model.
     * </p>
     * 
     * @param modelFile
     */
    public LingPipePosTagger(File modelFile) {
        Validate.notNull(modelFile, "modelFile must not be null");
        this.model = loadModel(modelFile);
    }

    /**
     * @param modelFile
     */
    private HiddenMarkovModel loadModel(File modelFile) {
        String modelFilePath = modelFile.getAbsolutePath();
        HiddenMarkovModel ret = (HiddenMarkovModel)Cache.getInstance().getDataObject(modelFilePath);
        if (ret == null) {
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(modelFile));
                ret = (HiddenMarkovModel)inputStream.readObject();
                Cache.getInstance().putDataObject(modelFilePath, model);
            } catch (IOException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } finally {
                FileHelper.close(inputStream);
            }
        }
        return ret;
    }

    @Override
    public void tag(List<Annotation<String>> annotations) {

        int cacheSize = Integer.valueOf(100);
        FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);

        HmmDecoder posTagger = new HmmDecoder(model, null, cache);

        List<String> tokenList = getTokenList(annotations);
        Tagging<String> tagging = posTagger.tag(tokenList);

        for (int i = 0; i < tagging.size(); i++) {
            assignTag(annotations.get(i), tagging.tag(i));
        }
    }

    public void evaluate(String folderPath, String modelFilePath) {

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start evaluating the tagger");

        ConfusionMatrix matrix = new ConfusionMatrix();

        int c = 1;
        int correct = 0;
        int total = 0;

        int cacheSize = Integer.valueOf(100);
        FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);

        // construct chunker
        HmmDecoder posTagger = new HmmDecoder(model, null, cache);

        File[] testFiles = FileHelper.getFiles(folderPath);
        for (File file : testFiles) {

            String content = FileHelper.readFileToString(file);

            String[] wordsAndTagPairs = content.split("\\s");

            for (String wordAndTagPair : wordsAndTagPairs) {

                if (wordAndTagPair.isEmpty()) {
                    continue;
                }

                String[] wordAndTag = wordAndTagPair.split("/");

                if (wordAndTag.length < 2) {
                    continue;
                }

                Tagging<String> tagging = posTagger.tag(Arrays.asList(wordAndTag[0]));

                String assignedTag = tagging.tags().get(0);
                String correctTag = normalizeTag(wordAndTag[1]).toLowerCase();

                matrix.add(correctTag, assignedTag);

                if (assignedTag.equals(correctTag)) {
                    correct++;
                }
                total++;
            }

            ProgressHelper.showProgress(c++, testFiles.length, 1);
        }

        LOGGER.info("all files read in " + stopWatch.getElapsedTimeString());

        LOGGER.info("Accuracy: " + MathHelper.round(100.0 * correct / total, 2) + "%");
        LOGGER.info("\n" + matrix);

        LOGGER.info("finished evaluating the tagger in " + stopWatch.getElapsedTimeString());
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    protected BaseTokenizer getTokenizer() {
        return TOKENIZER;
    }

    public static void main(String[] args) {

        final File modelFile = new File("pos-en-general-brown.HiddenMarkovModel");

        // PipelineDocument document = new PipelineDocument("I'm here to say that we're about to do that.");
        // ProcessingPipeline pipeline = new ProcessingPipeline();
        // pipeline.add(new LingPipeTokenizer());
        // pipeline.add(new LingPipePosTagger(modelFile));
        // pipeline.process(document);
        // AnnotationFeature annotations = (AnnotationFeature)
        // document.getFeatureVector().get(Tokenizer.PROVIDED_FEATURE);
        // System.out.println(annotations.getValue());
        //
        // System.exit(0);

        BasePosTagger tagger = new LingPipePosTagger(modelFile);
        System.out.println(tagger.tag("I'm here to say that we're about to do that.").getTaggedString());
        // System.out.println(tagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
        // tagger.evaluate("data/datasets/pos/testSmall/",
        // "data/models/lingpipe/pos-en-general-brown.HiddenMarkovModel");
    }
}
