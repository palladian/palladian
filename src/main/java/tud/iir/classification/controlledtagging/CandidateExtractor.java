package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;

public class CandidateExtractor {

    private static final int GRAM_SIZE = 1;

    private SnowballStemmer stemmer = new englishStemmer();
    private Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);

    private Corpus corpus = new Corpus();
    private CandidateClassifier classifier = new CandidateClassifier();

    private final static int MIN_GRAM_OCCURENCE = 2;

    public CandidateExtractor() {
        // classifier.useTrainedClassifier();
    }

    public void addToCorpus(String text) {

        List<Token> tokens = tokenize2(text, GRAM_SIZE);
        corpus.addTokens(tokens);

    }

    public void addToCorpus(String text, Set<String> tags) {

        List<Token> tokens = tokenize2(text, GRAM_SIZE);
        corpus.addTokens(tokens);

        // XXX
        tags = stem(tags);

        corpus.addTags(tags);

    }

    public void saveCorpus() {
        corpus.calcRelCorrelations();
        FileHelper.serialize(corpus, "corpus.ser");
    }

    public void loadCorpus() {
        corpus = FileHelper.deserialize("corpus.ser");
        classifier.useTrainedClassifier();
    }

    public void extractFromFile(String filePath) {

        final Counter c = new Counter();
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split("#");
                c.increment();

                if (split.length > 2) {
                    DocumentModel documentModel = makeCandidates(split[0], 3);
                    System.out.println(c + " " + documentModel.getCandidates().size());
                }

            }
        });

    }

    public float[] evaluate(String text, Set<String> tags) {

        Set<String> stemmedTags = stem(tags);

        DocumentModel candidates = makeCandidates(text, 1);
        List<Candidate> candidatesList = new ArrayList<Candidate>(candidates.getCandidates());

        // experimental ::: filter out stopwords
        ListIterator<Candidate> li = candidatesList.listIterator();
        while (li.hasNext()) {
            Candidate current = li.next();
            if (stopwords.contains(current.getValue())) {
                li.remove();
            } else if (!current.getValue().matches("[a-zA-Z]{3,}")) {
                li.remove();
            }
        }

        for (Candidate candidate : candidatesList) {
            classifier.classify(candidate);
            // System.out.println(candidate.getValue() + " " + candidate.getRegressionValue());
        }

        Collections.sort(candidatesList, new CandidateComparator());
        System.out.println("beforeReRanking: " + candidatesList);

        /*
         * for (Candidate c : candidatesList) {
         * System.out.println(c.getValue() + " " + c.getRegressionValue());
         * }
         */

        // / XXX experimental --- do re-raking

        Candidate[] candidateArray = candidatesList.toArray(new Candidate[0]);
        int numReRanking = candidateArray.length * (candidateArray.length - 1) / 2;

        final float correlationWeight = 30000;

        for (int i = 0; i < candidateArray.length; i++) {
            Candidate outerCand = candidateArray[i];
            for (int j = i; j < candidateArray.length; j++) {
                Candidate innerCand = candidateArray[j];
                WordCorrelation correlation = corpus.getCorrelation(outerCand.getStemmedValue(),
                        innerCand.getStemmedValue());
                if (correlation != null) {
                    float reRanking = (float) ((correlationWeight / numReRanking) * correlation
                            .getRelativeCorrelation());
                    innerCand.setRegressionValue(innerCand.getRegressionValue() + reRanking);
                    outerCand.setRegressionValue(outerCand.getRegressionValue() + reRanking);

                }

            }
        }

        Collections.sort(candidatesList, new CandidateComparator());
        System.out.println("afterReRanking: " + candidatesList);

        // / end experimental

        if (candidatesList.size() > 10) {
            candidatesList.subList(10, candidatesList.size()).clear();
        }

        for (Candidate c : candidatesList) {
            System.out.println(c.getValue() + " " + c.getRegressionValue());
        }

        int correctlyAssigned = 0;
        for (Candidate candidate : candidatesList) {
            for (String realTag : stemmedTags) {

                if (realTag.equalsIgnoreCase(candidate.getStemmedValue())) {
                    correctlyAssigned++;
                }

            }
        }

        int totalAssigned = candidatesList.size();
        int realCount = stemmedTags.size();

        float precision = (float) correctlyAssigned / totalAssigned;
        if (Float.isNaN(precision)) {
            precision = 0;
        }
        float recall = (float) correctlyAssigned / realCount;

        System.out.println("real: " + stemmedTags);
        System.out.println("assigned: " + candidatesList);
        System.out.println("correctlyAssigned:" + correctlyAssigned);
        System.out.println("totalAssigned:" + totalAssigned);
        System.out.println("realCount: " + realCount);
        System.out.println("pr: " + precision);
        System.out.println("rc: " + recall);
        System.out.println("------------------");

        float[] result = new float[2];
        result[0] = precision;
        result[1] = recall;
        return result;

    }

    public DocumentModel makeCandidates(String text, int maxNGramSize) {

        DocumentModel model = new DocumentModel(corpus);
        List<Token> tokens = tokenize2(text, GRAM_SIZE);

        for (Token token : tokens) {
            model.addToken(token);
        }

        model.createCandidates();

        return model;

    }

    public List<Token> tokenize2(String text, int maxNGramSize) {

        List<Token> tokens = new ArrayList<Token>();

        List<Token> gramTokens = new ArrayList<Token>();

        int textPosition = 0;
        int sentencePosition = 0;
        int sentenceNumber = 0;

        List<String> sentences = Tokenizer.getSentences(text);

        // if we have not sentence, just take the whole text.
        if (sentences.size() == 0) {
            sentences.add(text);
        }

        for (String sentence : sentences) {

            List<String> sentenceTokens = Tokenizer.tokenize(sentence);
            List<Token> thisSentenceTokens = new ArrayList<Token>();

            for (String string : sentenceTokens) {

                Token token = new Token();
                token.setUnstemmedValue(string);
                token.setStemmedValue(stem(string));
                token.setTextPosition(textPosition);
                token.setSentencePosition(sentencePosition);
                token.setSentenceNumber(sentenceNumber);
//                token.setWordCount(1);
                tokens.add(token);
                thisSentenceTokens.add(token);

                sentencePosition++;
                textPosition++;
            }

//            for (int i = 2; i <= maxNGramSize; i++) {
//                // tokens.addAll(nGrams);
//
//                // XXX List<Token> nGrams = makeNGrams(thisSentenceTokens, i);
//                // XXX gramTokens.addAll(nGrams);
//                
//                List<TokenGroup> nGrams = makeNGrams2(thisSentenceTokens, i);
//                gramTokens.addAll(nGrams);
//
//
//            }

            sentencePosition = 0;
            sentenceNumber++;

        }

        return tokens;

    }

//    private List<Token> makeNGrams(List<Token> tokens, int size) {
//
//        List<Token> nGrams = new ArrayList<Token>();
//        Token[] tokenArray = tokens.toArray(new Token[0]);
//
//        for (int i = 0; i < tokenArray.length - size + 1; i++) {
//
//            StringBuilder nGram = new StringBuilder();
//            StringBuilder stemmedNGram = new StringBuilder();
//            Token firstToken = tokenArray[i];
//
//            for (int j = i; j < i + size; j++) {
//                String tokenValue = tokenArray[j].getValue();
//                nGram.append(tokenValue);
//                stemmedNGram.append(stem(tokenValue));
//            }
//
//            if (nGram.length() > 0) {
//                Token nGramToken = new Token();
//                nGramToken.setValue(nGram.toString());
//                nGramToken.setStemmedValue(stemmedNGram.toString());
//                nGramToken.setTextPosition(firstToken.getTextPosition());
//                nGramToken.setSentencePosition(firstToken.getSentencePosition());
//                nGramToken.setWordCount(size);
//                nGrams.add(nGramToken);
//            }
//        }
//
//        return nGrams;
//    }

//    private List<TokenGroup> makeNGrams2(List<Token> tokens, int size) {
//
//        // List<Token> nGrams = new ArrayList<Token>();
//        List<TokenGroup> nGrams = new ArrayList<TokenGroup>();
//
//        Token[] tokenArray = tokens.toArray(new Token[0]);
//
//        for (int i = 0; i < tokenArray.length - size + 1; i++) {
//
//            TokenGroup group = new TokenGroup();
//
//            for (int j = i; j < i + size; j++) {
//                Token t = tokenArray[j];
//                group.addToken(t);
//            }
//
//            nGrams.add(group);
//
//        }
//
//        return nGrams;
//    }

    public String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {

        final CandidateExtractor extractor = new CandidateExtractor();


        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
        //createCorpus(extractor);

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
        //createTrainData(extractor);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////
        evaluate(extractor);

        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";

        extractor.addToCorpus(d1);
        extractor.addToCorpus(d2);
        extractor.addToCorpus(d3);
        extractor.addToCorpus(d4);
        extractor.addToCorpus(d5);

        // System.out.println(". -> " + extractor.corpus.getInverseDocumentFrequency("."));

        DocumentModel candidates = extractor.makeCandidates(d2, 1);
        System.out.println(candidates);
        System.exit(0);

        String text2 = "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // String text = "apple apple apples apples";
        // String text = "Apple sells phones called iPhones. The iPhone is a smart phone. Smart phones are great!";
        // String text = "iPhones iPhone iPhones";

        DocumentModel makeCandidates = extractor.makeCandidates(text2, 1);
        // System.out.println(makeCandidates);
        System.out.println(makeCandidates.toCSV());
        System.exit(0);

        // List<Token> tokens = extractor.tokenize(text, -1);
        // System.out.println(tokens);
        // DocumentModel model = extractor.tokenize(text, 2);
        // System.out.println(model);
        // List<Token> tokenize2 = extractor.tokenize2(text, 3);
        // CollectionHelper.print(tokenize2);
        // DocumentModel c = extractor.makeCandidates(text, 3);
        // System.out.println(c);
        //
        // System.exit(0);

        // String x = FileHelper.readFileToString("tokenizerProblem.txt");
        // List<String> t = Tokenizer.tokenize(x);
        // System.out.println(t.size());
        //
        // DocumentModel tokenize = extractor.tokenize(x, 3);
        // Collection<Candidate> candidates = tokenize.getCandidates(2);
        // for (Candidate candidate : candidates) {
        // System.out.println(candidate);
        // }
        // //System.out.println(tokenize);
        // System.exit(0);

        StopWatch sw = new StopWatch();
        extractor.extractFromFile("dataset_10000.txt");
        System.out.println(sw.getElapsedTimeString());

        System.exit(1);
        //
        // String text =
        // "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // // List<Token> tokens = extractor.tokenize(text, -1);
        // // System.out.println(tokens);
        // DocumentModel model = extractor.extract(text, 2);
        // System.out.println(model);

    }

    private static void evaluate(final CandidateExtractor extractor) {
        extractor.loadCorpus();
        final DescriptiveStatistics prStats = new DescriptiveStatistics();
        final DescriptiveStatistics rcStats = new DescriptiveStatistics();
        final Counter counter = new Counter();

        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);

                float[] prRc = extractor.evaluate(content, entry.getTags().uniqueSet());
                System.out.println("pr:" + prRc[0] + " rc:" + prRc[1]);
                counter.increment();

                prStats.addValue(prRc[0]);
                rcStats.addValue(prRc[1]);

            }
        };
        reader.read(callback, 1000);

        double meanPr = prStats.getMean();
        double meanRc = rcStats.getMean();
        double meanF1 = 2 * meanPr * meanRc / (meanPr + meanRc);

        System.out.println("avgPr: " + meanPr);
        System.out.println("avgRc: " + meanRc);
        System.out.println("avgF1: " + meanF1);

    }

    private static void createCorpus(final CandidateExtractor extractor) {
        final Counter counter = new Counter();

        FileHelper.performActionOnEveryLine("data/tag_dataset_10000.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i]);
                }

                counter.increment();

                if (split.length > 2) {
                    extractor.addToCorpus(split[0], tags);
                }

                if (counter.getCount() % 10 == 0) {
                    System.out.println(counter);
                }

            }
        });

        extractor.saveCorpus();
    }

    private static void createTrainData(final CandidateExtractor extractor) {
        extractor.loadCorpus();

        final Counter counter = new Counter();
        final StringBuilder data = new StringBuilder();
        counter.reset();

        FileHelper.performActionOnEveryLine("data/tag_dataset_10000.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                if (split.length > 2) {
                    DocumentModel candidates = extractor.makeCandidates(split[0], 2);
                    Set<String> tags = new HashSet<String>();
                    for (int i = 1; i < split.length; i++) {
                        tags.add(split[i].toLowerCase());
                    }

                    // XXX
                    tags = extractor.stem(tags);

                    System.out.println(tags);

                    for (Candidate candidate : candidates.getCandidates()) {
                        if (tags.contains(candidate.getStemmedValue())) {
                            candidate.setPositive(true);
                        } else {
                            candidate.setPositive(false);
                        }
                    }

                    if (counter.getCount() == 0) {
                        data.append("#")
                                .append(StringUtils.join(candidates.getCandidates().iterator().next().getFeatures()
                                        .keySet(), ";")).append("\n");
                    }

                    data.append(candidates.toCSV());

                }

                counter.increment();
                // if (counter.getCount() % 10 == 0) {
                System.out.println(counter);
                // }
                if (counter.getCount() == 1000) {
                    breakLineLoop();
                }

            }
        });

        FileHelper.writeToFile("train_1000_new.csv", data);

    }

}
