package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

/**
 * <p>
 * This class wraps the Stanford Named Entity Recognizer which is based on conditional random fields (CRF).
 * 
 * <p>
 * The NER has been described in: Jenny Rose Finkel, Trond Grenager, and Christopher Manning;
 * "<a href="http://nlp.stanford.edu/~manning/papers/gibbscrf3.pdf
 * ">Incorporating Non-local Information into Information Extraction Systems</a>"; Proceedings of the 43nd Annual
 * Meeting of the Association for Computational Linguistics (ACL 2005), pp. 363-370.
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Person
 * <li>Location
 * <li>Organization
 * </ul>
 * 
 * @see <a href="http://www-nlp.stanford.edu/software/crf-faq.shtml">Stanford NER CRF FAQ</a>
 * @author David Urbansky
 */
public class StanfordNer extends TrainableNamedEntityRecognizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(StanfordNer.class);

    private AbstractSequenceClassifier<CoreLabel> classifier;

    private static String buildConfigFile(String trainingFile, String modelFile) {
        String configFileContent = "";
        configFileContent += "#location of the training file" + "\n";
        configFileContent += "trainFile = " + trainingFile + "\n";
        configFileContent += "#location where you would like to save (serialize to) your" + "\n";
        configFileContent += "#classifier; adding .gz at the end automatically gzips the file," + "\n";
        configFileContent += "#making it faster and smaller" + "\n";
        configFileContent += "serializeTo = " + modelFile + "\n";
        configFileContent += "#structure of your training file; this tells the classifier" + "\n";
        configFileContent += "#that the word is in column 0 and the correct answer is in" + "\n";
        configFileContent += "#column 1" + "\n";
        configFileContent += "map = word=0,answer=1" + "\n";
        configFileContent += "#these are the features we'd like to train with" + "\n";
        configFileContent += "#some are discussed below, the rest can be" + "\n";
        configFileContent += "#understood by looking at NERFeatureFactory" + "\n";
        configFileContent += "useClassFeature=true" + "\n";
        configFileContent += "useWord=true" + "\n";
        configFileContent += "useNGrams=true" + "\n";
        configFileContent += "#no ngrams will be included that do not contain either the" + "\n";
        configFileContent += "#beginning or end of the word" + "\n";
        configFileContent += "noMidNGrams=true" + "\n";
        configFileContent += "useDisjunctive=true" + "\n";
        configFileContent += "maxNGramLeng=6" + "\n";
        configFileContent += "usePrev=true" + "\n";
        configFileContent += "useNext=true" + "\n";
        configFileContent += "useSequences=true" + "\n";
        configFileContent += "usePrevSequences=true" + "\n";
        configFileContent += "maxLeft=1" + "\n";
        configFileContent += "#the next 4 deal with word shape features" + "\n";
        configFileContent += "useTypeSeqs=true" + "\n";
        configFileContent += "useTypeSeqs2=true" + "\n";
        configFileContent += "useTypeySequences=true" + "\n";
        configFileContent += "wordShape=chris2useLC";
        return configFileContent;
    }

//    @SuppressWarnings("unchecked")
//    public void demo(String inputText) throws IOException {
//
//        String serializedClassifier = "data/temp/stanfordner/classifiers/ner-eng-ie.crf-3-all2008.ser.gz";
//
//        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
//
//        String inputTextPath = "data/temp/inputText.txt";
//        FileHelper.writeToFile(inputTextPath, inputText);
//
//        /*
//         * For either a file to annotate or for the hardcoded text example,
//         * this demo file shows two ways to process the output, for teaching
//         * purposes. For the file, it shows both how to run NER on a String
//         * and how to run it on a whole file. For the hard-coded String,
//         * it shows how to run it on a single sentence, and how to do this
//         * and produce an inline XML output format.
//         */
//        if (inputTextPath.length() > 1) {
//            String fileContents = IOUtils.slurpFile(inputTextPath);
//            List<List<CoreLabel>> out = classifier.classify(fileContents);
//            for (List<CoreLabel> sentence : out) {
//                for (CoreLabel word : sentence) {
//                    LOGGER.debug(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
//                }
//            }
//            out = classifier.classifyFile(inputTextPath);
//            for (List<CoreLabel> sentence : out) {
//                for (CoreLabel word : sentence) {
//                    LOGGER.debug(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
//                }
//            }
//
//        } else {
//            String s1 = "Good afternoon Rajat Raina, how are you today?";
//            String s2 = "I go to school at Stanford University, which is located in California.";
//            LOGGER.info(classifier.classifyToString(s1));
//            LOGGER.info(classifier.classifyWithInlineXML(s2));
//            LOGGER.info(classifier.classifyToString(s2, "xml", true));
//        }
//    }

    @Override
    public String getModelFileEnding() {
        return "ser.gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return true;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        File tempDirectory = FileHelper.getTempDir();

        String transformedTrainingPath = new File(tempDirectory, "StanfordNer-" + UUID.randomUUID() + ".txt").getPath();
        FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, transformedTrainingPath, "_");

        // set the location to the training and the model file in the configs and save the file
        String configFileContent = buildConfigFile(transformedTrainingPath, modelFilePath);
        String propertiesFilePath = new File(tempDirectory, "StanfordNer-" + UUID.randomUUID() + ".props").getPath();
        FileHelper.writeToFile(propertiesFilePath, configFileContent);

        String[] args = {"-props", propertiesFilePath};
        Properties props = StringUtils.argsToProperties(args);
        CRFClassifier<CoreLabel> crf = new CRFClassifier<CoreLabel>(props);
        crf.train();
        crf.serializeClassifier(crf.flags.serializeTo);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        try {
            classifier = CRFClassifier.getClassifierNoExceptions(configModelFilePath);
        } catch (Exception e) {
            LOGGER.error("{} error in loading model from {}: {}",
                    new Object[] {getName(), configModelFilePath, e.getMessage()});
            return false;
        }

        LOGGER.debug("Model {} successfully loaded in {}", configModelFilePath, stopWatch.getElapsedTimeString());
        return true;
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {

        String inputTextPath = new File(FileHelper.getTempDir(), "inputText.txt").getPath();
        FileHelper.writeToFile(inputTextPath, inputText);

        StringBuilder taggedText = new StringBuilder();
        taggedText.append(classifier.classifyWithInlineXML(inputText));

        String taggedTextFilePath = new File(FileHelper.getTempDir(), "stanfordNERTaggedText.txt").getPath();
        FileHelper.writeToFile(taggedTextFilePath, taggedText);

        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlFile(taggedTextFilePath);

        annotations.removeNested();
        return new ArrayList<Annotation>(annotations);
    }

    @Override
    public String getName() {
        return "Stanford NER";
    }

    // public void evaluateNER(String modelFilePath, String testFilePath) throws Exception {
    //
    // String[] args = new String[4];
    // args[0] = "-loadClassifier";
    // args[1] = modelFilePath;
    // args[2] = "-testFile";
    // args[3] = testFilePath;
    //
    // Properties props = StringUtils.argsToProperties(args);
    // CRFClassifier crf = new CRFClassifier(props);
    // String testFile = crf.flags.testFile;
    // String loadPath = crf.flags.loadClassifier;
    //
    // if (loadPath != null) {
    // crf.loadClassifierNoExceptions(loadPath, props);
    // } else {
    // crf.loadDefaultClassifier();
    // }
    //
    // if (testFile != null) {
    // if (crf.flags.searchGraphPrefix != null) {
    // crf.classifyAndWriteViterbiSearchGraph(testFile, crf.flags.searchGraphPrefix);
    // } else if (crf.flags.printFirstOrderProbs) {
    // crf.printFirstOrderProbs(testFile);
    // } else if (crf.flags.printProbs) {
    // crf.printProbs(testFile);
    // } else if (crf.flags.useKBest) {
    // int k = crf.flags.kBest;
    // crf.classifyAndWriteAnswersKBest(testFile, k);
    // } else if (crf.flags.printLabelValue) {
    // crf.printLabelInformation(testFile);
    // } else {
    // // crf.classifyAndWriteAnswers(testFile);
    //
    // String testText = FileHelper.readFileToString(testFilePath);
    // String classifiedString = crf.classifyToString(testText, "inlineXML", true);
    // LOGGER.info("cs:" + classifiedString);
    //
    // FileHelper.writeToFile("data/temp/stanfordClassified.xml", classifiedString);
    //
    // FileFormatParser ffp = new FileFormatParser();
    // ffp.xmlToColumn("data/temp/stanfordClassified.xml", "data/temp/stanfordClassifiedColumn.tsv", "\t");
    //
    // /*
    // * List<List<CoreLabel>> out = crf.classify(testFile);
    // * for (List<CoreLabel> sentence : out) {
    // * for (CoreLabel word : sentence) {
    // * System.out.println(word.word());
    // * System.out.println(word.get(AnswerAnnotation.class));
    // * System.out.println(word.value());
    // * System.out.println(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
    // * }
    // * System.out.println();
    // * }
    // */
    // }
    // }
    //
    // // port to Java: http://www.cnts.ua.ac.be/conll2002/ner/bin/conlleval.txt
    // }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        StanfordNer tagger = new StanfordNer();

        // // HOW TO USE ////
        // tagger.loadModel("data/models/stanfordner/data/ner-eng-ie.crf-3-all2008.ser.gz");
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");

        // tagger.tag(
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.",
        // "data/models/stanfordner/data/ner-eng-ie.crf-3-all2008.ser.gz");

        // demo
        // st.demo("John J. Smith and the Nexus One location mention Seattle in the text.");
        // learn
        // st.trainNER("data/temp/stanfordner/example/austen.prop");
        // st.trainNER("data/temp/mobilephone.prop");

        // use
        // st.useLearnedNER("data/temp/stanfordner/example/ner-model.ser.gz","John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");
        // tagger.useLearnedNER(
        // "data/temp/ner-model-mobilePhone.ser.gz",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");
        // st.useLearnedNER("data/temp/stanfordner/classifiers/ner-eng-ie.crf-3-all2008.ser.gz","John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");

        // evaluate
        // st.evaluateNER("data/temp/stanfordner/example/ner-model.ser.gz","data/temp/stanfordner/example/jane-austen-emma-ch2.tsv");
        // st.evaluateNER("data/temp/ner-model-mobilePhone.ser.gz", "data/temp/allUntagged.xml");

        // /////////////////////////// train and test /////////////////////////////
        // tagger.train("data/temp/nerEvaluation/www_eval_2_cleansed/allColumn.txt", "data/temp/stanfordNER.model");
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/stanfordNER.model");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
        // "data/temp/stanfordNER.model", TaggingFormat.COLUMN);

        tagger.train("data/datasets/ner/tud/tud2011_train.txt", "data/temp/stanfordNER2.model");
        tagger.loadModel("data/temp/stanfordNER2.model");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/tud/tud2011_test.txt", TaggingFormat.COLUMN);

        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test/index_split1.txt");
        // tagger.train(trainingDataset, "data/temp/stanfordner." + tagger.getModelFileEnding());
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset, "data/temp/stanfordner." +
        // tagger.getModelFileEnding());
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
    }

}