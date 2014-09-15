package ws.palladian.extraction.entity.tagger;

import static ws.palladian.helper.functional.Filters.NONE;
import static ws.palladian.helper.functional.Filters.fileExtension;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * This class wraps the <a href="https://opennlp.apache.org">OpenNLP</a> Named Entity Recognizer which uses a maximum
 * entropy approach.
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Date
 * <li>Location
 * <li>Money
 * <li>Organization
 * <li>Percentage
 * <li>Person
 * <li>Time
 * </ul>
 * 
 * @see <a href="https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html#tools.namefind">Apache OpenNLP
 *      Developer Documentation: Name Finder</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public class OpenNlpNer extends TrainableNamedEntityRecognizer implements ClassifyingTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNlpNer.class);

    /** Set this true if you evaluate on the CoNLL 2003 corpus. */
    private boolean conllEvaluation = false;

    private List<TokenNameFinderModel> nameFinderModels;

    /**
     * Load the models for the tagger. The models in the specified folders must start with "openNLP_" and all of them
     * will be loaded.
     * 
     * @param configModelFilePath The path to the folder where the models lie.
     */
    @Override
    public boolean loadModel(String configModelFilePath) {
        File modelDirectory = new File(configModelFilePath);
        Validate.isTrue(modelDirectory.isDirectory(), "Model file path must be an existing directory.");

        List<File> modelFiles = FileHelper.getFiles(new File(configModelFilePath), fileExtension(".bin"), NONE);
        Validate.isTrue(modelFiles.size() > 0, "Model file path must at least provide one .bin model.");

        this.nameFinderModels = CollectionHelper.newArrayList();
        for (File modelFile : modelFiles) {
            try {
                this.nameFinderModels.add(new TokenNameFinderModel(modelFile));
            } catch (InvalidFormatException e) {
                throw new IllegalStateException("InvalidFormatException when trying to load " + modelFile);
            } catch (IOException e) {
                throw new IllegalStateException("IOException when trying to load " + modelFile);
            }
        }

        LOGGER.info("{} models successfully loaded", modelFiles.size());
        return true;
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String inputText) {
        if (nameFinderModels == null || nameFinderModels.isEmpty()) {
            throw new IllegalStateException("No model available; make sure to load an existing model.");
        }
        // Map to collect all classifications for a given span; this is necessary, because individual entity types are
        // classified separately, and more than one entity classifier might tag an entity occurrence. We use the
        // probability values provided by the name finders to weight the individual type assignments.
        Map<Pair<Integer, Integer>, CategoryEntriesBuilder> collectedAnnotations = LazyMap
                .create(new Factory<CategoryEntriesBuilder>() {
                    @Override
                    public CategoryEntriesBuilder create() {
                        return new CategoryEntriesBuilder();
                    }
                });
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        Span[] tokenSpans = tokenizer.tokenizePos(inputText);
        String[] tokenStrings = Span.spansToStrings(tokenSpans, inputText);
        for (TokenNameFinderModel nameFinderModel : nameFinderModels) {
            NameFinderME nameFinder = new NameFinderME(nameFinderModel);
            Span[] nameSpans = nameFinder.find(tokenStrings);
            double[] probs = nameFinder.probs(nameSpans);
            for (int i = 0; i < nameSpans.length; i++) {
                Span nameSpan = nameSpans[i];
                int startOffset = tokenSpans[nameSpan.getStart()].getStart();
                int endOffset = tokenSpans[nameSpan.getEnd() - 1].getEnd();
                collectedAnnotations.get(Pair.of(startOffset, endOffset)).add(nameSpan.getType(), probs[i]);
            }
        }
        Annotations<ClassifiedAnnotation> annotations = new Annotations<ClassifiedAnnotation>();
        for (Entry<Pair<Integer, Integer>, CategoryEntriesBuilder> entry : collectedAnnotations.entrySet()) {
            int startOffset = entry.getKey().getLeft();
            int endOffset = entry.getKey().getRight();
            String value = inputText.substring(startOffset, endOffset);
            CategoryEntries categoryEntries = entry.getValue().create();
            annotations.add(new ClassifiedAnnotation(startOffset, value, categoryEntries));
        }
        annotations.sort();
        return annotations;
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

        OpenNlpNer tagger = new OpenNlpNer();
        tagger.loadModel("/Users/pk/Desktop/OpenNLP-Models");
        tagger.getAnnotations("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. New York City is where he wants to buy an iPhone 4 or a Samsung i7110 phone. The iphone 4 is modern. Seattle is a rainy city.");

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
