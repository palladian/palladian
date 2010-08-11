package tud.iir.extraction.entity.ner.tagger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Dictionary;
import tud.iir.classification.page.ClassificationDocument;
import tud.iir.classification.page.DictionaryClassifier;
import tud.iir.classification.page.Preprocessor;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.tagging.EntityList;
import tud.iir.tagging.KnowledgeBaseCommunicatorInterface;
import tud.iir.tagging.StringTagger;

public class TUDNER extends NamedEntityRecognizer implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(TUDNER.class);

    private static final long serialVersionUID = -8793232373094322955L;

    /** pattern candidates in the form of: prefix (TODO: ENTITY suffix) */
    private Map<String, CategoryEntries> patternCandidates = null;

    /** patterns in the form of: prefix (TODO ENTITY suffix) */
    private HashMap<String, CategoryEntries> patterns = null;

    /** a dictionary that holds a term vector of words that appear frequently close to the entities */
    private Dictionary dictionary = null;

    /** the connector to the knowledge base */
    private transient KnowledgeBaseCommunicatorInterface kbCommunicator = null;

    public TUDNER() {
        patternCandidates = new TreeMap<String, CategoryEntries>();
        patterns = new HashMap<String, CategoryEntries>();
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        // get all training terms
        Annotations annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        // learn patterns
        createPatternCandidates(trainingFilePath, annotations);

        // update the dictionary
        updateDictionary(annotations, modelFilePath);

        finishTraining(modelFilePath);

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText, String modelPath) {

        load(modelPath);

        Annotations annotations = new Annotations();

        Annotations entityCandidates = StringTagger.getTaggedEntities(inputText);

        // Annotations kbRecognizedAnnotations = verifyEntitiesWithKB(entityCandidates);
        // annotations.addAll(kbRecognizedAnnotations);
        //
        // Annotations patternRecognizedAnnotations = verifyEntitiesWithPattern(entityCandidates, inputText);
        // annotations.addAll(patternRecognizedAnnotations);

        Annotations dictionaryRecognizedAnnotations = verifyEntitiesWithDictionary(entityCandidates, inputText);
        annotations.addAll(dictionaryRecognizedAnnotations);

        System.out.println(tagText(inputText, dictionaryRecognizedAnnotations));

        return annotations;
    }

    public EntityList getTrainingEntities(double percentage) {
        if (kbCommunicator == null) {
            LOGGER.debug("could not get training entities because no KnowledgeBaseCommunicator has been defined");
            return new EntityList();
        }
        return kbCommunicator.getTrainingEntities(percentage);
    }

    private void finishTraining(String modelFilePath) {
        // now that we have seen all training texts and have the pattern candidates we can calculate the patterns
        calculatePatterns();

        LOGGER.info("serializing NERCer");
        FileHelper.serialize(this, modelFilePath);

        LOGGER.info("dictionary size: " + dictionary.size());
    }

    public void load(String modelPath) {
        TUDNER n = (TUDNER) FileHelper.deserialize(modelPath);
        this.dictionary = n.dictionary;
        if (this.dictionary.isUseIndex()) {
            dictionary.useIndex();
        }
        this.kbCommunicator = n.kbCommunicator;
        this.patterns = n.patterns;
    }

    private void createPatternCandidates(String trainingFilePath, Annotations annotations) {

        String xmlTraingFilePath = trainingFilePath.substring(0, trainingFilePath.length() - 4) + "_transformed."
                + FileHelper.getFileType(trainingFilePath);
        FileFormatParser.columnToXML(trainingFilePath, xmlTraingFilePath, "\t");
        String inputText = FileFormatParser.getText(xmlTraingFilePath, TaggingFormat.XML);

        // get all pre- and suffixes
        for (Annotation annotation : annotations) {

            // String sentence = StringHelper.getSentence(text, m.start());

            String annotationText = inputText.substring(annotation.getOffset(), annotation.getEndIndex());
            System.out.println("annotation text: " + annotationText);

            // use prefixes only
            String[] windowWords = getWindowWords(inputText, annotation.getOffset(), annotation.getEndIndex(), true,
                    false);

            StringBuilder patternCandidate = new StringBuilder();
            for (String word : windowWords) {
                patternCandidate.append(StringHelper.trim(word)).append(" ");
            }

            CategoryEntries categoryEntries = patternCandidates.get(patternCandidate.toString());
            if (categoryEntries == null) {
                categoryEntries = new CategoryEntries();
                categoryEntries.addAll(annotation.getTags());
            }

            patternCandidates.put(patternCandidate.toString(), categoryEntries);

        }
    }

    private void calculatePatterns() {

        // patternCandidates.put("1334abcdefg", null);
        // patternCandidates.put("0334abcdefg", null);
        // patternCandidates.put("262323abcde235", null);
        // patternCandidates.put("232323abcde235", null);
        // patternCandidates.put("abvasdfertaay", null);
        // patternCandidates.put("abcasdfertaay", null);

        LOGGER.info("calculate patterns");
        int minPatternLength = 7;

        // keep information about frequency of possible patterns
        LinkedHashMap<String, Integer> possiblePatterns = new LinkedHashMap<String, Integer>();

        // in each iteration the common strings of the last level are compared
        Set<String> currentLevelPatternCandidatesSet = patternCandidates.keySet();
        LinkedHashSet<String> currentLevelPatternCandidates = new LinkedHashSet<String>();
        for (String pc : currentLevelPatternCandidatesSet) {
            if (pc.length() >= minPatternLength) {
                currentLevelPatternCandidates.add(pc);
            }
        }

        LOGGER.info(currentLevelPatternCandidates.size() + " pattern candidates");

        LinkedHashSet<String> nextLevelPatternCandidates = null;

        for (int level = 0; level < 3; level++) {
            nextLevelPatternCandidates = new LinkedHashSet<String>();

            LOGGER.info("level " + level + ", " + currentLevelPatternCandidates.size() + " pattern candidates");

            int it1Position = 0;
            Iterator<String> iterator1 = currentLevelPatternCandidates.iterator();
            while (iterator1.hasNext()) {

                String patternCandidate1 = iterator1.next();
                patternCandidate1 = StringHelper.reverseString(patternCandidate1);
                it1Position++;

                Iterator<String> iterator2 = currentLevelPatternCandidates.iterator();
                int it2Position = 0;

                while (iterator2.hasNext()) {

                    // jump to the position after iterator1
                    if (it2Position < it1Position) {
                        iterator2.next();
                        it2Position++;
                        continue;
                    }

                    String patternCandidate2 = iterator2.next();
                    patternCandidate2 = StringHelper.reverseString(patternCandidate2);

                    // logger.info("get longest common string:");
                    // logger.info(patternCandidate1);
                    // logger.info(patternCandidate2);
                    String possiblePattern = StringHelper.getLongestCommonString(patternCandidate1, patternCandidate2,
                            false, false);
                    possiblePattern = StringHelper.reverseString(possiblePattern);

                    if (possiblePattern.length() < minPatternLength) {
                        continue;
                    }

                    Integer c = possiblePatterns.get(possiblePattern);
                    if (c == null) {
                        possiblePatterns.put(possiblePattern, 1);
                    } else {
                        possiblePatterns.put(possiblePattern, c + 1);
                    }
                    nextLevelPatternCandidates.add(possiblePattern);

                }
            }
            currentLevelPatternCandidates = nextLevelPatternCandidates;
        }

        possiblePatterns = CollectionHelper.sortByValue(possiblePatterns.entrySet());
        // CollectionHelper.print(possiblePatterns);

        // use only patterns longer or equal 7 characters
        LOGGER.info("filtering patterns");
        for (Entry<String, Integer> pattern : possiblePatterns.entrySet()) {
            if (pattern.getKey().length() >= minPatternLength) {

                // Categories categories = null;
                CategoryEntries categoryEntries = new CategoryEntries();

                // find out which categories the original pattern candidates belonged to
                for (Entry<String, CategoryEntries> originalPatternCandidate : patternCandidates.entrySet()) {
                    if (originalPatternCandidate.getKey().toLowerCase().indexOf(pattern.getKey().toLowerCase()) > -1) {
                        categoryEntries = originalPatternCandidate.getValue();
                        break;
                    }
                }

                if (categoryEntries == null) {
                    categoryEntries = new CategoryEntries();
                }

                for (CategoryEntry c : categoryEntries) {
                    c.addAbsoluteRelevance(pattern.getValue().doubleValue());
                }
                patterns.put(pattern.getKey(), categoryEntries);
            }
        }

        LOGGER.info("calculated " + patterns.size() + " patterns:");
        for (Entry<String, CategoryEntries> pattern : patterns.entrySet()) {
            LOGGER.info(" " + pattern);
        }

    }

    private void updateDictionary(Annotations annotations, String modelFilePath) {

        Categories categories = new Categories();

        // learn dictionary from annotations
        DictionaryClassifier dictionaryClassifier = new DictionaryClassifier();
        dictionaryClassifier.getDictionary().setDatabaseType(Dictionary.DB_H2);

        Preprocessor preprocessor = new Preprocessor(dictionaryClassifier);

        for (Annotation annotation : annotations) {

            Categories documentCategories = new Categories();

            Category knownCategory = categories
                    .getCategoryByName(annotation.getMostLikelyTag().getCategory().getName());
            if (knownCategory == null) {
                knownCategory = new Category(annotation.getMostLikelyTag().getCategory().getName());
                categories.add(knownCategory);
            }

            documentCategories.add(knownCategory);

            ClassificationDocument trainingDocument = preprocessor.preProcessDocument(annotation.getEntity().getName());
            trainingDocument.setDocumentType(ClassificationDocument.TRAINING);
            trainingDocument.setRealCategories(documentCategories);
            dictionaryClassifier.getTrainingDocuments().add(trainingDocument);

            dictionaryClassifier.addToDictionary(trainingDocument, ClassificationTypeSetting.SINGLE);

        }

        String modelName = FileHelper.getFileName(modelFilePath);

        dictionary = dictionaryClassifier.getDictionary();
        dictionary.setName(modelName);
        // dictionary.setIndexPath("data/temp/");
        // dictionary.index(true);

        System.out.println("abc");
        // words farther away from the entity get lower score, score = degradeFactor^distance, 1.0 = no degration
        // double degradeFactor = 1.0;

        // for (RecognizedEntity entity : entities) {
        //
        // Pattern pat = Pattern.compile(entity.getName());
        // Matcher m = pat.matcher(text);
        //
        // while (m.find()) {
        // // String sentence = StringHelper.getSentence(text, m.start());
        //
        // String[] windowWords = getWindowWords(text, m.start(), m.end());
        //
        // for (String word : windowWords) {
        // word = StringHelper.trim(word);
        // if (word.length() < 3) {
        // continue;
        // }
        // Term t = new Term(word);
        // for (CategoryEntry categoryEntry : entity.getCategoryEntries()) {
        // dictionary.updateWord(t, categoryEntry.getCategory().getName(), 1.0);
        // }
        // }
        // }
        //
        // }
    }

    private String[] getWindowWords(String text, int startIndex, int endIndex, boolean capturePrefix,
            boolean captureSuffix) {

        // get all words around entities within window (number of characters)
        int windowSize = 30;

        String prefix = "";
        if (capturePrefix) {
            prefix = text.substring(Math.max(0, startIndex - windowSize), startIndex);
        }

        String suffix = "";
        if (captureSuffix) {
            suffix = text.substring(endIndex, Math.min(endIndex + windowSize, text.length()));
        }

        String context = prefix + suffix;
        String[] words = context.split("\\s");

        return words;
    }

    private String[] getWindowWords(String text, int startIndex, int endIndex) {
        return getWindowWords(text, startIndex, endIndex, true, true);
    }

    private Annotations verifyEntitiesWithKB(HashSet<String> entityCandidates) {
        Annotations annotations = new Annotations();

        for (String entityCandidate : entityCandidates) {
            entityCandidate = StringHelper.trim(entityCandidate);
            CategoryEntries categoryEntries = kbCommunicator.categoryEntriesInKB(entityCandidate);
            if (categoryEntries != null) {
                Annotation annotation = new Annotation(0, entityCandidate, categoryEntries);
                annotations.add(annotation);
            }
        }

        return annotations;
    }

    private Annotations verifyEntitiesWithPattern(HashSet<String> entityCandidates, String text) {
        Annotations annotations = new Annotations();

        for (String entityCandidate : entityCandidates) {

            for (Entry<String, CategoryEntries> pattern : patterns.entrySet()) {
                Pattern pat = Pattern.compile(pattern.getKey() + entityCandidate);
                Matcher m = pat.matcher(text);

                if (m.find()) {
                    CategoryEntries categoryEntries = new CategoryEntries();
                    for (CategoryEntry categoryEntry : pattern.getValue()) {
                        categoryEntries.add(categoryEntry);
                    }
                    Annotation annotation = new Annotation(m.start(), entityCandidate, categoryEntries);
                    annotations.add(annotation);
                }
            }
        }

        return annotations;
    }

    private Annotations verifyEntitiesWithDictionary(Annotations entityCandidates, String text) {
        Annotations annotations = new Annotations();

        // TextClassifier dictionaryClassifier = ClassifierManager.load(modelPath);
        DictionaryClassifier dictionaryClassifier = new DictionaryClassifier();
        dictionaryClassifier.setDictionary(dictionary);

        Preprocessor preprocessor = new Preprocessor(dictionaryClassifier);
        for (Annotation entityCandidate : entityCandidates) {

            ClassificationDocument document = preprocessor.preProcessDocument(entityCandidate.getEntity().getName());
            dictionaryClassifier.classify(document, false);

            CategoryEntries categoryEntries = document.getAssignedCategoryEntries();
            Annotation annotation = new Annotation(entityCandidate.getOffset(), entityCandidate.getEntity().getName(),
                    categoryEntries);
            annotations.add(annotation);
        }

        return annotations;
    }

    public KnowledgeBaseCommunicatorInterface getKbCommunicator() {
        return kbCommunicator;
    }

    public void setKbCommunicator(KnowledgeBaseCommunicatorInterface kbCommunicator) {
        this.kbCommunicator = kbCommunicator;
    }

    private void demo(String optionValue) {
        System.out.println(tag(
                "Homer Simpson likes to travel through his hometown Springfield. His friends are Moe and Barney.",
                "data/models/tudnerdemo.model"));

    }

    /**
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        // NERCer nercer1 = new NERCer();
        // nercer1.calculatePatterns();
        // if (true) return;

        // // training can be done with NERCLearner also
        TUDNER tagger = new TUDNER();

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("mode").withDescription("whether to tag or train a model")
                    .create());

            OptionGroup modeOptionGroup = new OptionGroup();
            modeOptionGroup.addOption(OptionBuilder.withArgName("tg").withLongOpt("tag").withDescription("tag a text")
                    .create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("tr").withLongOpt("train")
                    .withDescription("train a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("ev").withLongOpt("evaluate")
                    .withDescription("evaluate a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("dm").withLongOpt("demo")
                    .withDescription("demo mode of the tagger").create());
            modeOptionGroup.setRequired(true);
            options.addOptionGroup(modeOptionGroup);

            options.addOption(OptionBuilder.withLongOpt("trainingFile")
                    .withDescription("the path and name of the training file for the tagger (only if mode = train)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder
                    .withLongOpt("testFile")
                    .withDescription(
                            "the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("configFile")
                    .withDescription("the path and name of the config file for the tagger").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("inputText")
                    .withDescription("the text that should be tagged (only if mode = tag)").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("tag")) {

                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"), cmd.getOptionValue("configFile"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.evaluate(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"),
                            TaggingFormat.XML);

                } else if (cmd.hasOption("demo")) {

                    tagger.demo(cmd.getOptionValue("inputText"));

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // // HOW TO USE ////
        // HashSet<String> trainingTexts = new HashSet<String>();
        // trainingTexts.add("Australia is a country and a continent at the same time. New Zealand is also a country but not a continent");
        // trainingTexts
        // .add("Many countries, such as Germany and Great Britain, have a strong economy. Other countries such as Iceland and Norway are in the north and have a smaller population");
        // trainingTexts.add("In south Europe, a nice country named Italy is formed like a boot.");
        // trainingTexts.add("In the western part of Europe, the is a country named Spain which is warm.");
        // trainingTexts.add("Bruce Willis is an actor, Jim Carrey is an actor too, but Trinidad is a country name and and actor name as well.");
        // trainingTexts.add("In west Europe, a warm country named Spain has good seafood.");
        // trainingTexts.add("Another way of thinking of it is to drive to another coutry and have some fun.");
        //
        // // set the kb communicator that knows the entities
        // nercer.setKbCommunicator(new TestKnowledgeBaseCommunicator());

        // train
        //tagger.train("data/datasets/ner/sample/trainingColumn.tsv", "data/models/tudner.model");
       
        // tag
        // tagger.tag(
        // "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.",
        // "data/models/tudner.model");

        // evaluate
        // tagger.evaluate("data/datasets/ner/sample/testingColumn.tsv", "data/models/tudner.model",
        // TaggingFormat.COLUMN);

        // FileFormatParser.xmlToColumn("data/temp/taggedText.txt", "data/temp/allColumnTaggedText.tsv", "\t");
        // nercer.train("data/temp/allColumnTaggedText.tsv", "data/temp/nercer.model");

        // System.out
        // .println(nercer
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.",
        // "data/temp/nercer.model"));
        //
        // System.out
        // .println(nercer
        // .tag("John J. Smith and the John Reilly location mention Samsung Galaxy in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.",
        // "data/temp/nercer.model"));

        
        // tagger.evaluate("data/temp/evaluate/taggedTextTesting.xml", "data/temp/nercer.model", TaggingFormat.XML);

        // use the trained model to recognize entities in a text
        // EntityList recognizedEntities = null;
        // recognizedEntities = nercer
        // .recognizeEntities("In the north of Europe, there is a country called Sweden which is not far from Norway, there is also a country named Scotland in the north of Europe. But also Denzel Washington is an actor.");
        //
        // CollectionHelper.print(recognizedEntities);
    }


}