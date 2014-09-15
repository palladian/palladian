package ws.palladian.extraction.entity.tagger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderEventStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * This class wraps the OpenNLP Named Entity Recognizer which uses a maximum entropy approach.
 * </p>
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Date</li>
 * <li>Location</li>
 * <li>Money</li>
 * <li>Organization</li>
 * <li>Percentage</li>
 * <li>Person</li>
 * <li>Time</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Changes to the original OpenNLP code:
 * <ul>
 * <li>made nameFinder public in NameFinder.java</li>
 * <li>NameSampleDataStream.java added lines 43 to 46 to allow non white-spaced tagging</li>
 * <li>the model names must have the following format openNLP_TAG.bin.gz where "TAG" is the name of the tag that will be
 * tagged by this model</li>
 * </ul>
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Name_Finder#Named_Entity_Annotation_Guidelines"
 * >http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Name_Finder#Named_Entity_Annotation_Guidelines</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class OpenNlpNer extends TrainableNamedEntityRecognizer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNlpNer.class);

    /** Set this true if you evaluate on the CoNLL 2003 corpus. */
    private boolean conllEvaluation = false;
    private NameFinderME[] finders;
    private String[] tags;

    /**
     * Adds tags to the given text using the name entity models.
     * 
     * @param finders The name finders to be used.
     * @param tags The tag names for the corresponding name finder.
     * @param input The input reader.
     * @return A tagged string.
     * @throws IOException
     */
    private String processText(NameFinderME[] finders, String[] tags, String text) throws IOException {
        if (text.isEmpty()) {
            return "";
        }

        // the names of the tags
        Span[][] nameSpans = new Span[finders.length][];
        String[][] nameOutcomes = new String[finders.length][];
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;

        StringBuilder output = new StringBuilder();

        Span[] spans = tokenizer.tokenizePos(text);
        String[] tokens = Span.spansToStrings(spans, text);

        // let each model (one for each concept) tag the text
        for (int fi = 0, fl = finders.length; fi < fl; fi++) {
            NameFinderME finder = finders[fi];
            finder.clearAdaptiveData(); // necessary to make results deterministic.
            nameSpans[fi] = finder.find(tokens);
            nameOutcomes[fi] = NameFinderEventStream.generateOutcomes(nameSpans[fi], null, tokens.length);
        }

        // if this is true, we have tagged a token already and should not tag it again with another name finder
        boolean tagOpen = false;
        String openTag = "";

        for (int ti = 0, tl = tokens.length; ti < tl; ti++) {
            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                // check for end tags
                if (ti != 0) {
                    if (tagOpen
                            && (nameOutcomes[fi][ti].endsWith(NameFinderME.START) || nameOutcomes[fi][ti]
                                    .endsWith(NameFinderME.OTHER))
                                    && (nameOutcomes[fi][ti - 1].endsWith(NameFinderME.START) || nameOutcomes[fi][ti - 1]
                                            .endsWith(NameFinderME.CONTINUE))) {
                        output.append("</").append(openTag).append(">");
                        tagOpen = false;
                    }
                }
            }
            if (ti > 0 && spans[ti - 1].getEnd() < spans[ti].getStart()) {
                output.append(text.substring(spans[ti - 1].getEnd(), spans[ti].getStart()));
            }
            // check for start tags

            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                if (!tagOpen) {
                    if (nameOutcomes[fi][ti].endsWith(NameFinderME.START)) {
                        openTag = tags[fi];
                        tagOpen = true;
                        output.append("<").append(openTag).append(">");
                    }
                }
            }
            output.append(tokens[ti]);
        }
        // final end tags
        if (tokens.length != 0) {
            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                if (nameOutcomes[fi][tokens.length - 1].endsWith(NameFinderME.START)
                        || nameOutcomes[fi][tokens.length - 1].endsWith(NameFinderME.CONTINUE)) {
                    output.append("</").append(tags[fi]).append(">");
                }
            }
        }
        if (tokens.length != 0) {
            if (spans[tokens.length - 1].getEnd() < text.length()) {
                output.append(text.substring(spans[tokens.length - 1].getEnd()));
            }
        }

        return output.toString();
    }

    /**
     * Load the models for the tagger. The models in the specified folders must start with "openNLP_" and all of them will be loaded.
     * @param configModelFilePath The path to the folder where the models lie.
     */
    @Override
    public boolean loadModel(String configModelFilePath) {

        StopWatch stopWatch = new StopWatch();

        File modelDirectory = new File(configModelFilePath);
        if (!modelDirectory.isDirectory()) {
            throw new IllegalArgumentException("Model file path must be an existing directory.");
        }

        // get all models in the given folder that have the schema of "openNLP_" + conceptName + ".bin"
        File[] modelFiles = FileHelper.getFiles(modelDirectory.getPath(), "openNLP_");
        if (modelFiles.length == 0) {
            throw new IllegalArgumentException("No model files found at path " + modelDirectory.getPath());
        }

        this.finders = new NameFinderME[modelFiles.length];
        this.tags = new String[finders.length];

        for (int finderIndex = 0; finderIndex < modelFiles.length; finderIndex++) {

            String modelName = modelFiles[finderIndex].getPath();
            String tagName = modelName;
            int tagStartIndex = modelName.lastIndexOf("_");
            if (tagStartIndex > -1) {
                tagName = modelName.substring(tagStartIndex + 1, modelName.indexOf(".", tagStartIndex));
            } else {
                LOGGER.warn("Model name does not comply \"openNLP_TAG.bin\" format: {}", modelName);
            }

            try {
                finders[finderIndex] = new NameFinderME(new TokenNameFinderModel(new FileInputStream(
                        new File(modelName))));
            } catch (IOException e) {
                LOGGER.error("{} error in loading model: {}, {}", new Object[] {getName(), modelName, e.getMessage()});
                return false;
            }
            tags[finderIndex] = tagName.toUpperCase();
        }

        LOGGER.info("Models {} successfully loaded in {}", Arrays.toString(modelFiles),
                stopWatch.getElapsedTimeString());

        return true;
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        if (finders == null || tags == null) {
            throw new IllegalStateException("No model available; make sure to load an existing model.");
        }


        String taggedText = "";
        try {
            taggedText = processText(finders, tags, inputText).toString();
        } catch (IOException e) {
            LOGGER.error("could not tag text with {}, {}", getName(), e.getMessage());
        }

        // String taggedTextFilePath = "data/test/ner/openNLPOutput_tmp.txt";
        // FileHelper.writeToFile(taggedTextFilePath, taggedText);
        // List<Annotation> annotations = FileFormatParser.getAnnotationsFromXmlFile(taggedTextFilePath);
        // FileHelper.writeToFile("data/test/ner/openNLPOutput.txt", tagText(inputText, annotations));
        Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromXmlText(taggedText);
        return Collections.<Annotation> unmodifiableList(annotations);
    }

    @Override
    public String getModelFileEnding() {
        return "bin";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean oneModelPerConcept() {
        return true;
    }

    private String[] getUsedTags(String filePath) {
        Set<String> tags = new HashSet<String>();
        String inputString = FileHelper.tryReadFileToString(filePath);
        Pattern pattern = Pattern.compile("</?(.*?)>");
        Matcher matcher = pattern.matcher(inputString);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags.toArray(new String[tags.size()]);
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        // Open NLP creates several model files for each trained tag, so for the supplied model file path, a directory
        // will be created, which contains all those files.
        File modelDirectory = new File(modelFilePath);
        if (modelDirectory.isFile()) {
            throw new IllegalArgumentException("File " + modelFilePath + " already exists.");
        }
        modelDirectory.mkdirs();

        // open nlp needs xml format
        File tempDir = FileHelper.getTempDir();
        String tempTrainingFile = new File(tempDir, "openNLPNERTraining.xml").getPath();
        String tempTrainingFile2 = new File(tempDir, "openNLPNERTraining2.xml").getPath();
        FileFormatParser.columnToXml(trainingFilePath, tempTrainingFile, "\t");

        // let us get all tags that are used
        String[] tags = getUsedTags(tempTrainingFile);
        LOGGER.debug("Found {} tags in the training file, computing the models now", tags.length);

        // create one model for each used tag, that is delete all the other tags from the file and learn
        for (int i = 0; i < tags.length; i++) {

            String tag = tags[i].toUpperCase();
            LOGGER.debug("Start learning for tag {}", tag);

            // XXX this is for the TUD dataset, for some reason opennlp does not find some concepts when they're only in
            // few places, so we delete all lines with no tags for the concepts with few mentions
            if (!isConllEvaluation()/*
             * conceptName.equalsIgnoreCase("mouse") || conceptName.equalsIgnoreCase("car")
             * || conceptName.equalsIgnoreCase("actor")|| conceptName.equalsIgnoreCase("phone")
             */) {

                List<String> array = FileHelper.readFileToArray(tempTrainingFile);

                StringBuilder sb = new StringBuilder();
                for (String string : array) {
                    if (string.indexOf("<" + tag + ">") > -1) {
                        sb.append(string).append("\n");
                    }
                }

                FileHelper.writeToFile(tempTrainingFile2, sb);
            } else {
                FileHelper.copyFile(tempTrainingFile, tempTrainingFile2);
            }

            String content = FileHelper.tryReadFileToString(tempTrainingFile2);

            // we need to use the tag style <START:tagname> blabla <END>
            content = content.replaceAll("<" + tag + ">", "<START:" + tag.toLowerCase() + "> ");
            content = content.replaceAll("</" + tag + ">", " <END> ");

            // we need to remove all other tags for training the current tag
            for (String otherTag : tags) {
                if (otherTag.equalsIgnoreCase(tag)) {
                    continue;
                }
                content = content.replace("<" + otherTag.toUpperCase() + ">", "");
                content = content.replace("</" + otherTag.toUpperCase() + ">", "");
            }

            String tempFileTag = new File(tempDir, "openNLPNERTraining" + tag + ".xml").getPath();
            FileHelper.writeToFile(tempFileTag, content);

            ObjectStream<String> lineStream = null;
            TokenNameFinderModel model;
            try {
                lineStream = new PlainTextByLineStream(new FileInputStream(tempFileTag), "UTF-8");

                ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

                model = NameFinderME.train("en", tag, sampleStream, (AdaptiveFeatureGenerator)null,
                        Collections.<String, Object> emptyMap(), 100, 5);

            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                if (lineStream != null) {
                    try {
                        lineStream.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            BufferedOutputStream modelOut = null;

            try {
                File modelFile = new File(modelDirectory, "openNLP_" + tag + ".bin");
                modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
                model.serialize(modelOut);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                FileHelper.close(modelOut);
            }
        }
        return true;
    }

    public void setConllEvaluation(boolean conllEvaluation) {
        this.conllEvaluation = conllEvaluation;
    }

    public boolean isConllEvaluation() {
        return conllEvaluation;
    }

    @Override
    public String getName() {
        return "OpenNLP NER";
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // OpenNlpNer tagger = new OpenNlpNer();

        // // HOW TO USE (some functions require the models in
        // data/models/opennlp) ////
        // // train
        // tagger.train("data/datasets/ner/sample/trainingPhoneXML.xml",
        // "data/models/opennlp/openNLP_phone.bin.gz");

        // // tag
        // String taggedText = tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. New York City is where he wants to buy an iPhone 4 or a Samsung i7110 phone. The iphone 4 is modern. Seattle is a rainy city.",
        // "data/models/opennlp/openNLP_location.bin,data/models/opennlp/openNLP_person.bin");
        // System.out.println(taggedText);
        // System.exit(1);
        // System.out.println(taggedText);

        // // demo
        // tagger.demo();

        // // evaluate
        // System.out
        // .println(
        // tagger
        // .evaluate(
        // "data/datasets/ner/sample/testingXML.xml",
        // "data/models/opennlp/openNLP_organization.bin.gz,data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz",
        // TaggingFormat.XML));

        // /////////////////////////// train and test /////////////////////////////
        // tagger.setConllEvaluation(true);
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/openNLP.bin");
        // tagger.train("data/temp/seedsTest1.txt", "data/temp/openNLP.bin");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", "data/temp/openNLP.bin",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        // TODO one model per concept
        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test_0/index_split1.txt");
        // tagger.train(trainingDataset, "data/temp/openNLP.bin");
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test_0/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset,
        // "data/temp/openNLP_MOVIE.bin,data/temp/openNLP_POLITICIAN.bin");
        // // EvaluationResult er = tagger.evaluate(testingDataset, "data/models/opennlp/openNLP_person.bin");
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

    }

}
