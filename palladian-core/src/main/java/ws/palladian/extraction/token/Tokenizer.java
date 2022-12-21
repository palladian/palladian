package ws.palladian.extraction.token;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.apache.commons.lang3.Validate;
import ws.palladian.core.Token;
import ws.palladian.extraction.sentence.PalladianSentenceDetector;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DiskTrie;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <p>
 * The Tokenizer tokenizes strings or creates chunks of that string.
 * </p>
 *
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class Tokenizer {

    /**
     * The RegExp used for tokenization (terms).
     */
    public static final String TOKEN_SPLIT_REGEX = "(?:[A-Z][a-z]?\\.)+|[\\p{L}\\w+]+(?:[-\\.,][\\p{L}\\w]+)*|\\.[\\p{L}\\w]+|</?[\\p{L}\\w]+>|\\$\\d+\\.\\d+|[^\\w\\s<]+";

    /**
     * The RegExp used for sentence splitting.
     */
    public static final String SENTENCE_SPLIT_REGEX_EN = "(?<!(\\.|\\()|([A-Z]\\.[A-Z]){1,10}|St|Mr|mr|Vers|Dr|dr|Prof|Nr|Rev|Mrs|mrs|Jr|jr|vs| eg|e\\.g|ca|max|Min|etc| cu| sq| ft)((\\.|\\?|\\!)(’|”|\")+(?=\\s+[A-Z])|\\.|\\?+|\\!+)(?!(\\.|[0-9]|\"|”|'|\\)|[!?]|(com|de|fr|uk|au|ca|cn|org|net)/?\\s|\\()|[A-Za-z]{1,15}\\.|[A-Za-z]{1,15}\\(\\))";
    public static final String SENTENCE_SPLIT_REGEX_DE = "(?<!(\\.|\\()|([A-Z]\\.[A-Z]){1,10}|St|[mM]r|[dD]r|Ca|Mio|Mind|u\\.A|Inkl|Vers|Prof|[mM]s|zusätzl|äquiv|komp|quiet|elektr\\.|[jJ]r|vs|ca|engl|evtl|max|mind.|etc|Nr|Rev| sog| ident|bzw|i\\.d\\.R|v\\.a|u\\.v\\.m|o\\.k|zzgl|Min|Keyb|Elec|bspw|bsp|m\\.E|bezügl|bzgl|inkl|exkl|ggf|z\\.\\s?[bB]| max| min|\\s[a-z]|u\\.s\\.w|u\\.\\s?a|d\\.h)((\\.|\\?|\\!)(”|\")\\s[A-Z]|\\.|\\?+|\\!+)(?!(\\.|[0-9]|\"|”|'|\\)| B\\.|[!?]|(com|de|fr|uk|au|ca|cn|org|net)/?\\s|\\()|[A-Za-z]{1,15}\\.|[A-Za-z]{1,15}\\(\\))";

    private Tokenizer() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Tokenize a given string.
     * </p>
     *
     * @param inputString The string to be tokenized.
     * @return A list of tokens.
     */
    public static List<String> tokenize(String inputString) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateTokens(inputString);
        return CollectionHelper.newArrayList(CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER));
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a character level. The size of the set can be calculated as: Size =
     * stringLength - n + 1.
     * </p>
     *
     * @param string The string that the n-grams should be calculated for.
     * @param n      The number of characters for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateCharNGrams(String string, int n) {
        Iterator<Token> nGramIterator = new CharacterNGramTokenizer(n, n).iterateTokens(string);
        return CollectionHelper.newHashSet(CollectionHelper.convert(nGramIterator, Token.VALUE_CONVERTER));
    }

    public static Set<String> calculateAllCharEdgeNGrams(String string, int n1, int n2) {
        return calculateAllCharEdgeNGrams(string, n1, n2, false);
    }

    public static Set<String> calculateAllCharEdgeNGrams(String string, int n1, int n2, boolean mustHitLeftEdge) {
        Set<String> nGrams = new HashSet<>();
        String[] parts = string.split(" ");
        for (String part : parts) {
            for (int n = n1; n <= n2; n++) {
                nGrams.addAll(calculateCharEdgeNGrams(part, n, mustHitLeftEdge));
            }
        }

        return nGrams;
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a character level. The size of the set can be calculated as: Size =
     * stringLength - n + 1.
     * </p>
     * <p>
     * Skip 1-4-grams in the middle of the word. E.g. "pROTector" should not be fround with "rot" and "Sleeve" should
     * not be found for "ee"
     * </p>
     *
     * @param string The string that the n-grams should be calculated for.
     * @param n      The number of characters for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateCharEdgeNGrams(String string, int n, boolean mustHitLeftEdge) {
        Set<String> nGrams = new HashSet<>();

        int length = string.length();
        if (length < n) {
            return nGrams;
        }

        for (int i = 0; i <= length - n; i++) {
            // only allow edge ngrams
            if ((i > 0 && i != length - n) || (n == 1 && i > 0 && i != length - n)) {
                continue;
            }

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(string.charAt(j));
            }
            nGrams.add(nGram.toString());

            if (i == 0 && mustHitLeftEdge) {
                break;
            }
        }

        return nGrams;
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1.
     * </p>
     *
     * @param string The string that the n-grams should be calculated for.
     * @param n      The number of words for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateWordNGrams(String string, int n) {
        return calculateAllWordNGrams(string, n, n);
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1.
     * </p>
     *
     * <p>
     * Since the quantity of the encountered n-grams is important for some algorithms, a list is used.
     * </p>
     *
     * @param string The string that the n-grams should be calculated for.
     * @param n      The number of words for a gram.
     * @return A list of n-grams.
     */
    public static List<String> calculateWordNGramsAsList(String string, int n) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateTokens(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n, n);
        return CollectionHelper.newArrayList(CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER));
    }

    /**
     * <p>
     * Calculate all n-grams for a string for different n on a character level. The size of the set can be calculated
     * as: Size = SUM_n(n1,n2) (stringLength - n + 1)
     * </p>
     * <p>
     * XXX this method is about 25% slower than previous implementations (see https://bitbucket.org/palladian/palladian/src/c10127a1b4ba1c98a9f51ba866e509bcae379d68/palladian-core/src/main/java/ws/palladian/extraction/token/Tokenizer.java?at=master)
     *
     * @param string The string the n-grams should be calculated for.
     * @param n1     The smallest n-gram size.
     * @param n2     The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllCharNGrams(String string, int n1, int n2) {
        Iterator<Token> tokenIterator = new CharacterNGramTokenizer(n1, n2).iterateTokens(string);
        return CollectionHelper.newHashSet(CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER));
    }

    /**
     * <p>
     * Calculate all n-grams for a string for different n on a word level. The size of the set can be calculated as:
     * Size = SUM_n(n1,n2) (numberOfWords - n + 1)
     * </p>
     *
     * @param string The string the n-grams should be calculated for.
     * @param n1     The smallest n-gram size.
     * @param n2     The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllWordNGrams(String string, int n1, int n2) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateTokens(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n1, n2);
        return CollectionHelper.newHashSet(CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER));
    }

    /**
     * Compute possible splits, e.g. "starbucks mocha drink"
     * => 3 [starbucks, mocha, drink]
     * => 2 [starbucks mocha, drink]
     * => 2 [starbucks, mocha drink]
     * => 1 [starbucks mocha drink]
     *
     * @param string The string to split.
     * @param n1     Min n for word n-grams.
     * @param n2     Max n for word n-grams.
     * @return A set of possible splits.
     */
    public static Set<List<String>> computeSplits(String string, int n1, int n2, int maxSplits) {
        Validate.notEmpty(string);
        Validate.notNull(string);

        Set<List<String>> splits = new HashSet<>();
        computeSplits(splits, new ArrayList<>(), string, n1, n2, maxSplits);

        return splits;
    }

    private static void computeSplits(Set<List<String>> splits, List<String> currentSplit, String string, int n1, int n2, int maxSplits) {

        if (string.isEmpty()) {
            splits.add(new ArrayList<>(currentSplit));
            currentSplit.remove(currentSplit.size() - 1);
            return;
        }

        if (splits.size() >= maxSplits) {
            return;
        }

        List<String> ngrams = computeStartingWordNGrams(string, n1, n2);

        for (String ngram : ngrams) {
            currentSplit.add(ngram);
            //            computeSplits(splits, currentSplit, string.replaceAll("^" + ngram, "").trim(), n1, n2, maxSplits);
            computeSplits(splits, currentSplit, string.startsWith(ngram) ? string.substring(ngram.length()).trim() : string, n1, n2, maxSplits);
        }
        if (!currentSplit.isEmpty()) {
            currentSplit.remove(currentSplit.size() - 1);
        }
    }

    /**
     * Only consider n-grams that start with the string, e.g. "starbucks mocha" but not "mocha drink" for the string
     * "starbucks mocha drink".
     *
     * @param string The string to split.
     * @param n1     Min n-gram length.
     * @param n2     Max n-gram length.
     * @return A list of possible n-grams.
     */
    public static List<String> computeStartingWordNGrams(String string, int n1, int n2) {
        ArrayList<String> ngrams = new ArrayList<>();

        String[] split = string.split(" ");
        n2 = Math.min(n2, split.length);
        for (int i = 0; i < Math.min(split.length, n2 - n1 + 1); i++) {
            String ngram = "";
            for (int j = 0; j < n2 - i; j++) {
                ngram += split[j] + " ";
            }
            ngram = ngram.trim();

            if (!ngram.isEmpty()) {
                ngrams.add(ngram);
            }
        }

        return ngrams;
    }

    public static String getSentence(String string, int position) {
        return getSentence(string, position, Language.ENGLISH);
    }

    /**
     * <p>
     * Get the sentence in which the specified position is present.
     * </p>
     *
     * @param string   The string.
     * @param position The position in the sentence.
     * @return The whole sentence.
     */
    private static String getSentence(String string, int position, Language language) {
        if (position < 0) {
            return string;
        }

        List<String> sentences = getSentences(string, language);
        String pickedSentence = "";
        for (String sentence : sentences) {
            int start = string.indexOf(sentence);
            if (start <= position) {
                pickedSentence = sentence;
            } else {
                break;
            }
        }
        return pickedSentence;
    }

    public static List<String> getSentences(String inputText, boolean onlyRealSentences) {
        return getSentences(inputText, onlyRealSentences, Language.ENGLISH);
    }

    /**
     * <p>
     * Get a list of sentences of an input text. Also see <a
     * href="http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html">http://alias-i.com/lingpipe/demos
     * /tutorial/sentences/read-me.html</a> for the LingPipe example.
     * </p>
     *
     * @param inputText         An input text.
     * @param onlyRealSentences If true, only sentences that end with a sentence delimiter are considered (headlines in
     *                          texts will likely be discarded)
     * @param language          The language to use for sentence splitting.
     * @return A list with sentences.
     */
    public static List<String> getSentences(String inputText, boolean onlyRealSentences, Language language) {
        return getSentences(inputText, onlyRealSentences, new PalladianSentenceDetector(language));
    }

    public static List<String> getSentences(String inputText, boolean onlyRealSentences, PalladianSentenceDetector palladianSentenceDetector) {
        List<Token> annotations = CollectionHelper.newArrayList(palladianSentenceDetector.iterateTokens(inputText));
        List<String> sentences = CollectionHelper.convertList(annotations, Token.VALUE_CONVERTER);
        // TODO Since requirements might differ slightly from application to application, this filtering should be
        // carried out by each calling application itself.
        if (onlyRealSentences) {
            List<String> realSentences = new ArrayList<>();
            for (String sentence : sentences) {
                String[] parts = sentence.split("\n");
                sentence = parts[parts.length - 1];
                if (sentence.endsWith(".") || sentence.endsWith("?") || sentence.endsWith("!") || sentence.endsWith(".”") || sentence.endsWith(".\"")) {

                    String cleanSentence = StringHelper.trim(sentence, "“”\"");
                    int wordCount = StringHelper.countWhitespaces(cleanSentence) + 1;

                    // TODO Why is this 8?
                    // TODO There are valid english sentences with only one word like "Go!" or "Stop!"
                    if (cleanSentence.length() > 8 && wordCount > 2) {
                        realSentences.add(sentence.trim());
                    }
                }
            }
            sentences = realSentences;
        }
        return sentences;
    }

    /**
     * <p>
     * Splits a text into sentences.
     * </p>
     *
     * @param inputText The text to split.
     * @return The senteces as they appear in the text.
     */
    public static List<String> getSentences(String inputText) {
        return getSentences(inputText, Language.ENGLISH);
    }

    public static List<String> getSentences(String inputText, Language language) {
        return getSentences(inputText, false, language);
    }

    /**
     * <p>
     * Given a string, find the beginning of the sentence, e.g. "...now. Although, many of them" =>
     * "Although, many of them". consider !,?,. and : as end of sentence.
     * </p>
     * TODO control character after delimiter makes it end of sentence.
     *
     * @param inputString the input string
     * @return The phrase from the beginning of the sentence.
     */
    public static String getPhraseFromBeginningOfSentence(String inputString) {
        String string = inputString;
        string = StringHelper.removeDoubleWhitespaces(string);

        // find the beginning of the current sentence by finding the period at the end
        int startIndex = string.lastIndexOf(".");

        startIndex = Math.max(startIndex, string.lastIndexOf("\n"));

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        boolean pointIsSentenceDelimiter = false;
        while (startIndex > -1) {
            if (startIndex >= string.length() - 1) {
                break;
            }

            if (startIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(startIndex - 1)) && Character.isUpperCase(string.charAt(startIndex + 1));
            }
            if (!pointIsSentenceDelimiter && startIndex < string.length() - 2) {
                pointIsSentenceDelimiter = (Character.isUpperCase(string.charAt(startIndex + 2)) || string.charAt(startIndex + 2) == '-' || string.charAt(startIndex + 2) == '=')
                        && string.charAt(startIndex + 1) == ' ';
            }

            // break after period
            if (!pointIsSentenceDelimiter && (string.charAt(startIndex + 1) == '\n' || string.charAt(startIndex) == '\n')) {
                pointIsSentenceDelimiter = true;
            }

            if (pointIsSentenceDelimiter) {
                break;
            }

            if (startIndex < string.length() - 1) {
                startIndex = string.substring(0, startIndex).lastIndexOf(".");
            } else {
                startIndex = -1;
            }
        }

        if (string.lastIndexOf("!") > -1 && string.lastIndexOf("!") > startIndex) {
            startIndex = string.lastIndexOf("!");
        }

        if (string.lastIndexOf("?") > -1 && string.lastIndexOf("?") > startIndex) {
            startIndex = string.lastIndexOf("?");
        }

        if (string.lastIndexOf(":") > -1 && string.lastIndexOf(":") > startIndex) {
            startIndex = string.lastIndexOf(":");
        }

        if (startIndex == -1) {
            startIndex = -1;
        }

        // cut period
        string = string.substring(startIndex + 1);

        // cut first space
        if (string.startsWith(" ")) {
            string = string.substring(1);
        }

        return string;
    }

    /**
     * <p>
     * Given a string, find the end of the sentence, e.g. "Although, many of them (30.2%) are good. As long as" =>
     * "Although, many of them (30.2%) are good.". Consider !,?, and . as end of sentence.
     * </p>
     *
     * @param string The string.
     * @return The phrase to the end of the sentence.
     */
    public static String getPhraseToEndOfSentence(String string) {
        // find the end of the current sentence
        int endIndex = string.indexOf(".");

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        // after point no number because 2 hr. 32 min. would be broken
        boolean pointIsSentenceDelimiter = false;
        while (endIndex > -1) {

            // before period
            if (endIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex - 1));
            }
            // one digit after period
            if (endIndex < string.length() - 1) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 1)) && Character.isUpperCase(string.charAt(endIndex + 1)) || StringHelper.isBracket(
                        string.charAt(endIndex + 1)) || (endIndex > 0 && string.charAt(endIndex - 1) == '"');
            }
            // two digits after period
            if (!pointIsSentenceDelimiter && endIndex < string.length() - 2) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 2)) && (Character.isUpperCase(string.charAt(endIndex + 2)) || StringHelper.isBracket(
                        string.charAt(endIndex + 2))) && string.charAt(endIndex + 1) == ' ';
            }
            // break after period
            if (!pointIsSentenceDelimiter && (string.length() == (endIndex + 1) || string.charAt(endIndex + 1) == '\n')) {
                pointIsSentenceDelimiter = true;
            }

            if (pointIsSentenceDelimiter) {
                break;
            }

            if (endIndex < string.length() - 1) {
                endIndex = string.indexOf(".", endIndex + 1);
            } else {
                endIndex = -1;
            }
        }

        if (string.contains("!") && (string.indexOf("!") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("!");
        }

        if (string.contains("?") && (string.indexOf("?") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("?");
        }

        // XXX commented this out because of aspect ratio "2.35 : 1" wasn't captured
        // if (string.indexOf(":") > -1 && (string.indexOf(":") < endIndex || endIndex == -1)) {
        // int indexColon = string.indexOf(":");
        // if (string.length() > indexColon + 1 && !StringHelper.isNumber(string.charAt(indexColon + 1))) {
        // endIndex = indexColon;
        // }
        //
        // }
        if (endIndex == -1) {
            endIndex = string.length();
        } else {
            ++endIndex; // take last character as well
        }

        return string.substring(0, endIndex);
    }

    private static String getSerializationPath(String key) {
        String shaKey = StringHelper.sha1(key);
        String subFolder = shaKey.substring(0, 3);
        return "data/trie3/" + subFolder + "/node-" + shaKey + ".gz";
    }

    public static void main(String[] args) throws IOException {
        StopWatch stopWatch1 = new StopWatch();
        //        for (int i = 0; i < 10000; i++) {
        //            String serializationPath = getSerializationPath("asdfasdf"+i);
        //            Path path = Paths.get(serializationPath);
        //            Files.exists(path);
        ////            FileHelper.fileExists(serializationPath);
        //        }
        //        System.out.println(stopWatch1.getElapsedTimeString());
        //        System.exit(0);

        ////////////////// load structure //////////////////
        //        DB db = DBMaker.tempFileDB().closeOnJvmShutdown().make();
        //        DB db = DBMaker.fileDB("tmp2.db").closeOnJvmShutdown().fileMmapEnableIfSupported() // Only enable mmap on supported platforms
        //                .fileMmapPreclearDisable()   // Make mmap file faster
        //                // Unmap (release resources) file when its closed.
        //                // That can cause JVM crash if file is accessed after it was unmapped
        //                // (there is possible race condition).
        //                .cleanerHackEnable().make();
        //        BTreeMap<String, IntOpenHashSet> map = db.treeMap("map")
        //                .keySerializer(Serializer.STRING)
        //                .valueSerializer(new SerializerJava())
        //                .createOrOpen();
        File dataFolder = new File("data/trie3/");
        DiskTrie<IntOpenHashSet> map = new DiskTrie<>();
        System.out.println("structure created " + stopWatch1.getElapsedTimeStringAndIncrement());

        ////////////////// fill structure //////////////////
        String words = FileHelper.readFileToString("data/en-src.txt");
        String[] strings = words.toLowerCase().split("\\s|\\n");
        List<String> strings1 = Arrays.asList(strings).subList(0, 1000);
        //List<String> strings1 = Arrays.asList(strings).subList(0, 250000);
        System.out.println("data loaded " + stopWatch1.getElapsedTimeStringAndIncrement());
        ProgressMonitor pm = new ProgressMonitor(strings1.size(), 1.0, "Filling Structure");
        for (String word : strings1) {
            Set<String> ngrams = Tokenizer.calculateAllCharEdgeNGrams(word, 2, 10, true);
            for (String ngram : ngrams) {
                IntOpenHashSet integers = map.get(ngram);
                if (integers == null) {
                    integers = new IntOpenHashSet();
                }
                for (int i = 0; i < 50000 * Math.random(); i++) {
                    integers.add((int) (10000000 * Math.random()));
                }
                map.put(ngram, integers);
            }
            pm.incrementAndPrintProgress();
        }
        map.writeValuesToDisk(dataFolder);

        System.out.println("structure filled " + stopWatch1.getElapsedTimeStringAndIncrement());
        System.out.println(ProcessHelper.getHeapUtilization());
        //        map = null;
        words = "";
        words = null;
        strings = null;
        strings1 = new ArrayList<>();
        strings1 = null;
        System.gc();
        System.out.println(ProcessHelper.getHeapUtilization());
        //        btree.flush(true, true);
        //        btree.close();
        System.gc();
        System.out.println(ProcessHelper.getHeapUtilization());
        //        btree = new BTreeIndex(new File("data/btree5.idx"));
        //        btree.init(false);
        //        System.gc();
        //        System.out.println(ProcessHelper.getHeapUtilization());
        //        IntOpenHashSet value = new IntOpenHashSet();
        //        value.add(1);
        //        map.put("my string", value);
        //        map.put("my string", new int[]{1, 2, 3});

        System.out.println("starting interactive mode:");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            System.out.println("query:");
            String query = in.readLine();
            StopWatch sw = new StopWatch();
            if (query.startsWith("+")) {
                String[] s = query.split(" ");
                s[0] = s[0].substring(1);
                String[] numbers = s[1].split(",");
                //                int[] newNumbers = new int[numbers.length];
                //                for (int i = 0; i < numbers.length; i++) {
                //                    newNumbers[i] = Integer.valueOf(numbers[i]);
                //                }
                //                map.put(s[0], newNumbers);
                IntOpenHashSet newNumbers = new IntOpenHashSet();
                for (int i = 0; i < numbers.length; i++) {
                    newNumbers.add(Integer.valueOf(numbers[i]));
                }
                map.put(s[0], newNumbers);
            } else if (query.equals("flush")) {
                //                btree.flush();
                //            } else if (query.equals("close")) {
                ////                db.close();
                //                btree.flush();
                //                btree.close();
            } else {
                //                int[] ints = map.get(query);
                IntOpenHashSet ints = Optional.ofNullable(map).orElse(new DiskTrie<>()).get(query);
                if (ints == null) {
                    System.out.println("-");
                } else {
                    System.out.print("trie (" + ints.size() + "): ");
                    int c = 0;
                    for (int anInt : ints) {
                        System.out.print(anInt + ",");
                        if (c++ > 10) {
                            break;
                        }
                    }
                    System.out.print("\n");
                }
                //                Value value = btree.getValue(new Value(query));
                //                if (value == null) {
                //                    System.out.println("-");
                //                } else {
                //                    String s = new String(value.getData());
                //                    System.out.print("btree (" + s.split(",").length + "): " + s.substring(0, Math.min(100, s.length())) + "\n");
                //                }

                // Open a read-only Txn. It only sees data that existed at Txn creation time.
                //                final ByteBuffer key = ByteBuffer.allocateDirect(env.getMaxKeySize());
                //                key.put(query.getBytes(StandardCharsets.UTF_8)).flip();
                //                // Our read Txn can fetch key1 without problem, as it existed at Txn creation.
                //
                //                // To fetch any data from LMDB we need a Txn. A Txn is very important in
                //                // LmdbJava because it offers ACID characteristics and internally holds a
                //                // read-only key buffer and read-only value buffer. These read-only buffers
                //                // are always the same two Java objects, but point to different LMDB-managed
                //                // memory as we use Dbi (and Cursor) methods. These read-only buffers remain
                //                // valid only until the Txn is released or the next Dbi or Cursor call. If
                //                // you need data afterwards, you should copy the bytes to your own buffer.
                //                try (Txn<ByteBuffer> txn = env.txnRead()) {
                //                    final ByteBuffer found = db.get(txn, key);
                //                    // The fetchedVal is read-only and points to LMDB memory
                //                    final ByteBuffer fetchedVal = txn.val();
                //                    String list = StandardCharsets.UTF_8.decode(fetchedVal).toString();
                //                    String[] split = list.split(",");
                //                    System.out.println("lmdb (" + split.length + "): " + list);
                //                }
            }
            System.out.println(sw.getElapsedTimeString());
        }
    }
}
