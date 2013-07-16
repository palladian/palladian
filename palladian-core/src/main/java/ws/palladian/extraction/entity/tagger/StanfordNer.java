package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotated;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

/**
 * <p>
 * This class wraps the Stanford Named Entity Recognizer which is based on conditional random fields (CRF).<br>
 * The NER has been described in the following paper:
 * </p>
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Person</li>
 * <li>Location</li>
 * <li>Organization</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Jenny Rose Finkel, Trond Grenager, and Christopher Manning<br>
 * "Incorporating Non-local Information into Information Extraction Systems", 2005<br>
 * Proceedings of the 43nd Annual Meeting of the Association for Computational Linguistics (ACL 2005), pp. 363-370<br>
 * <a href="http://nlp.stanford.edu/~manning/papers/gibbscrf3.pdf">Read Paper</a>
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://www-nlp.stanford.edu/software/crf-faq.shtml">http://www-nlp.stanford.edu/software/crf-faq.shtml</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class StanfordNer extends TrainableNamedEntityRecognizer {

    /** Hold the configuration settings here instead of a file. */
    private String configFileContent = "";
    private AbstractSequenceClassifier<CoreLabel> classifier;

    public StanfordNer() {
        buildConfigFile();
    }

    private void buildConfigFile() {
        configFileContent = "";
        configFileContent += "#location of the training file" + "\n";
        configFileContent += "trainFile = ###TRAINING_FILE###" + "\n";
        configFileContent += "#location where you would like to save (serialize to) your" + "\n";
        configFileContent += "#classifier; adding .gz at the end automatically gzips the file," + "\n";
        configFileContent += "#making it faster and smaller" + "\n";
        configFileContent += "serializeTo = ###MODEL_FILE###" + "\n";
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

        String trainingFilePath2 = FileHelper.appendToFileName(trainingFilePath, "_t");
        FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, trainingFilePath2, "_");

        // set the location to the training and the model file in the configs and save the file
        buildConfigFile();
        configFileContent = configFileContent.replaceAll("###TRAINING_FILE###", trainingFilePath2);
        configFileContent = configFileContent.replaceAll("###MODEL_FILE###", modelFilePath);
        String propertiesFilePath = new File(FileHelper.getTempDir(), "stanfordNerConfig.props").getPath();
        FileHelper.writeToFile(propertiesFilePath, configFileContent);

        String[] args = new String[2];
        args[0] = "-props";
        args[1] = propertiesFilePath;

        Properties props = StringUtils.argsToProperties(args);
        CRFClassifier<CoreLabel> crf = new CRFClassifier<CoreLabel>(props);
        String loadPath = crf.flags.loadClassifier;
        String loadTextPath = crf.flags.loadTextClassifier;
        String serializeTo = crf.flags.serializeTo;
        String serializeToText = crf.flags.serializeToText;

        if (loadPath != null) {
            crf.loadClassifierNoExceptions(loadPath, props);
        } else if (loadTextPath != null) {
            System.err.println("Warning: this is now only tested for Chinese Segmenter");
            System.err.println("(Sun Dec 23 00:59:39 2007) (pichuan)");
            try {
                crf.loadTextClassifier(loadTextPath, props);
                // System.err.println("DEBUG: out from crf.loadTextClassifier");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("error loading " + loadTextPath);
            }
        } else if (crf.flags.loadJarClassifier != null) {
            crf.loadJarClassifier(crf.flags.loadJarClassifier, props);
        } else if (crf.flags.trainFile != null || crf.flags.trainFileList != null) {
            crf.train();
        } else {
            crf.loadDefaultClassifier();
        }

        if (serializeTo != null) {
            crf.serializeClassifier(serializeTo);
        }

        if (serializeToText != null) {
            crf.serializeTextClassifier(serializeToText);
        }

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

        LOGGER.info("Model {} successfully loaded in {}", configModelFilePath, stopWatch.getElapsedTimeString());
        return true;
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {

        String inputTextPath = new File(FileHelper.getTempDir(), "inputText.txt").getPath();
        FileHelper.writeToFile(inputTextPath, inputText);

        StringBuilder taggedText = new StringBuilder();
        taggedText.append(classifier.classifyWithInlineXML(inputText));

        String taggedTextFilePath = new File(FileHelper.getTempDir(), "stanfordNERTaggedText.txt").getPath();
        FileHelper.writeToFile(taggedTextFilePath, taggedText);

        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlFile(taggedTextFilePath);

        // FileHelper.writeToFile("data/test/ner/stanfordNEROutput.txt", tagText(inputText, annotations));

        annotations.removeNested();
        annotations.sort();

        // CollectionHelper.print(annotations);

        return new ArrayList<Annotated>(annotations);
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