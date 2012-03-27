package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ws.palladian.extraction.feature.Annotation;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.util.FastCache;

/**
 * @author Martin Wunderwald
 * @author Philipp Katz
 */
public final class LingPipePosTagger extends BasePosTagger {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LingPipePosTagger.class);

    /** The name of this {@link PosTagger}. */
    private static final String TAGGER_NAME = "LingPipe POS-Tagger";

    /** The model used by the LingPipe POS tagger. */
    private final HiddenMarkovModel model;

    /**
     * <p>
     * Instantiate a new LingPipe POS tagger from the given model.
     * </p>
     * 
     * @param modelFile
     */
    public LingPipePosTagger(File modelFile) {
        this.model = loadModel(modelFile);
    }

    /**
     * @param modelFile
     */
    private HiddenMarkovModel loadModel(File modelFile) {
        String modelFilePath = modelFile.getAbsolutePath();
        HiddenMarkovModel ret = (HiddenMarkovModel) Cache.getInstance().getDataObject(modelFilePath);
        if (ret == null) {
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(modelFile));
                ret = (HiddenMarkovModel) inputStream.readObject();
                Cache.getInstance().putDataObject(modelFilePath, model);
            } catch (IOException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return ret;
    }


    @Override
    public void tag(List<Annotation> annotations) {

        int cacheSize = Integer.valueOf(100);
        FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);

        HmmDecoder posTagger = new HmmDecoder(model, null, cache);

        List<String> tokenList = getTokenList(annotations);
        Tagging<String> tagging = posTagger.tag(tokenList);

        for (int i = 0; i < tagging.size(); i++) {
            assignTag(annotations.get(i), tagging.tag(i));
        }
    }

    

    // /*
    // * (non-Javadoc)
    // * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String)
    // */
    // @Override
    // public LingPipePosTagger tag(String sentence) {
    //
    // int cacheSize = Integer.valueOf(100);
    // FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);
    //
    // // read HMM for pos tagging
    //
    // // construct chunker
    // HmmDecoder posTagger = new HmmDecoder(model, null, cache);
    // TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    //
    // // apply pos tagger
    // String[] tokens = tokenizerFactory.tokenizer(sentence.toCharArray(), 0, sentence.length()).tokenize();
    // List<String> tokenList = Arrays.asList(tokens);
    // Tagging<String> tagging = posTagger.tag(tokenList);
    //
    // TagAnnotations tagAnnotations = new TagAnnotations();
    // for (int i = 0; i < tagging.size(); i++) {
    //
    // TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(tagging.token(i)), tagging.tag(i)
    // .toUpperCase(new Locale("en")), tagging.token(i));
    // tagAnnotations.add(tagAnnotation);
    //
    // }
    // setTagAnnotations(tagAnnotations);
    // return this;
    // }

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

                matrix.increment(correctTag, assignedTag);

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

    public static void main(String[] args) {

        final File modelFile = new File("/Users/pk/Desktop/pos-en-general-brown.HiddenMarkovModel");

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
