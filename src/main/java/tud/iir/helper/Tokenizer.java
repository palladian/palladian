package tud.iir.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Tokenizer tokenizes strings or creates chunks of that string.
 * 
 * @author David Urbansky
 * 
 */
public class Tokenizer {

    /**
     * Tokenize a given string.
     * 
     * @param inputString The string to be tokenized.
     * @return A list of tokens.
     */
    public static List<String> tokenize(String inputString) {

        List<String> tokens = new ArrayList<String>();

        Pattern pattern = Pattern.compile("(\\w+)(-(\\w+))*|</?(\\w+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)", Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE);
        // Pattern pattern = Pattern.compile("(\\w+)(-(\\w+))*|</?(\\w+)>|([^\\w\\s<]+)", Pattern.DOTALL
        // | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(inputString);
        while (matcher.find()) {
            tokens.add(matcher.group(0));
        }

        return tokens;
    }

    /**
     * Calculate n-grams for a given string on a character level. The size of the set can be calculated as: Size =
     * stringLength - n + 1
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of characters for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateCharNGrams(String string, int n) {
        Set<String> nGrams = new HashSet<String>();

        if (string.length() < n) {
            return nGrams;
        }

        for (int i = 0; i <= string.length() - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(string.charAt(j));
            }
            nGrams.add(nGram.toString());

        }

        return nGrams;
    }

    /**
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of words for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateWordNGrams(String string, int n) {
        Set<String> nGrams = new HashSet<String>();

        String[] words = string.split("\\s");

        if (words.length < n) {
            return nGrams;
        }

        for (int i = 0; i <= words.length - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(words[j]).append(" ");
            }
            nGrams.add(nGram.toString().trim());

        }

        return nGrams;
    }

    /**
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1.
     * 
     * Since the quantity of the encountered n-grams is important for some algorithms, a list is used.
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of words for a gram.
     * @return A list of n-grams.
     */
    public static List<String> calculateWordNGramsAsList(String string, int n) {
        List<String> nGrams = new ArrayList<String>();

        String[] words = string.split("\\s");

        if (words.length < n) {
            return nGrams;
        }

        for (int i = 0; i <= words.length - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(words[j]).append(" ");
            }
            nGrams.add(nGram.toString().trim());

        }

        return nGrams;
    }

    /**
     * Calculate all n-grams for a string for different n on a character level. The size of the set can be calculated
     * as: Size = SUM_n(n1,n2)
     * (stringLength - n + 1)
     * 
     * @param string The string the n-grams should be calculated for.
     * @param n1 The smallest n-gram size.
     * @param n2 The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllCharNGrams(String string, int n1, int n2) {
        Set<String> nGrams = new HashSet<String>();
        for (int n = n1; n <= n2; n++) {
            nGrams.addAll(calculateCharNGrams(string, n));
        }

        return nGrams;
    }

    /**
     * Calculate all n-grams for a string for different n on a word level. The size of the set can be calculated as:
     * Size = SUM_n(n1,n2)
     * (numberOfWords - n + 1)
     * 
     * @param string The string the n-grams should be calculated for.
     * @param n1 The smallest n-gram size.
     * @param n2 The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllWordNGrams(String string, int n1, int n2) {
        Set<String> nGrams = new HashSet<String>();
        for (int n = n1; n <= n2; n++) {
            nGrams.addAll(calculateWordNGrams(string, n));
        }

        return nGrams;
    }

    /**
     * Get the sentence that the specified position is in.
     * 
     * @param string The string.
     * @param position The position in the sentence.
     * @return The whole sentence.
     */
    public static String getSentence(String string, int position) {
        if (position < 0) {
            return string;
        }

        String beginning = getPhraseFromBeginningOfSentence(string.substring(0, position));
        String end = getPhraseToEndOfSentence(string.substring(position));
        if (beginning.endsWith(" ")) {
            end = end.trim();
        }

        return beginning + end;
    }

    /**
     * Get a list of sentences of an input text.
     * Also see <a
     * href="http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html">http://alias-i.com/lingpipe/demos
     * /tutorial/sentences/read-me.html</a> for the LingPipe example.
     * 
     * @param inputText An input text.
     * @return A list with sentences.
     */
    public static List<String> getSentences(String inputText) {

        List<String> sentences = new ArrayList<String>();
        String[] sentenceArray = inputText.split("(?<!(\\.|\\())(\\.|\\?+|\\!+)(?!(\\.|\\())");

        // for (String sentence : sentenceArray) {
        for (int i = 0; i < sentenceArray.length; i++) {
            String sentence = sentenceArray[i];
            if (sentence.length() == 0) {
                continue;
            }

            // get end of sentence
            String sentenceTermination = ".";

            if (i < sentenceArray.length - 1) {
                sentenceTermination = StringHelper.getSubstringBetween(inputText, sentence, sentenceArray[i + 1]);
            } else {
                int pos = inputText.lastIndexOf(sentence);
                sentenceTermination = inputText.substring(pos + sentence.length());
            }

            sentences.add(sentence.trim() + sentenceTermination);
        }

        return sentences;
    }

    /**
     * Given a string, find the beginning of the sentence, e.g. "...now. Although, many of them" =>
     * "Although, many of them". consider !,?,. and : as end of
     * sentence TODO control character after delimiter makes it end of sentence
     * 
     * @param inputString the input string
     * @return The phrase from the beginning of the sentence.
     */
    public static String getPhraseFromBeginningOfSentence(String inputString) {

        String string = inputString;
        // find the beginning of the current sentence by finding the period at the end
        int startIndex = string.lastIndexOf(".");

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        boolean pointIsSentenceDelimiter = false;
        while (!pointIsSentenceDelimiter && startIndex > -1) {
            if (startIndex >= string.length() - 1) {
                break;
            }

            if (startIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(startIndex - 1))
                        && Character.isUpperCase(string.charAt(startIndex + 1));
            }
            if (!pointIsSentenceDelimiter && startIndex < string.length() - 2) {
                pointIsSentenceDelimiter = Character.isUpperCase(string.charAt(startIndex + 2))
                        && string.charAt(startIndex + 1) == ' ';
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

        string = string.substring(startIndex + 1); // cut point
        if (string.startsWith(" ")) {
            string = string.substring(1); // cut first space
        }

        return string;
    }

    /**
     * Given a string, find the end of the sentence, e.g. "Although, many of them (30.2%) are good. As long as" =>
     * "Although, many of them (30.2%) are good."
     * consider !,?,. and : as end of sentence
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
        while (!pointIsSentenceDelimiter && endIndex > -1) {

            // before point
            if (endIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex - 1));
            }
            // one digit after point
            if (endIndex < string.length() - 1) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 1))
                        && Character.isUpperCase(string.charAt(endIndex + 1))
                        || StringHelper.isBracket(string.charAt(endIndex + 1));
            }
            // two digits after point
            if (!pointIsSentenceDelimiter && endIndex < string.length() - 2) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 2))
                        && (Character.isUpperCase(string.charAt(endIndex + 2)) || StringHelper.isBracket(string
                                .charAt(endIndex + 2)))
                        && string.charAt(endIndex + 1) == ' ';
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

        if (string.indexOf("!") > -1 && (string.indexOf("!") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("!");
        }

        if (string.indexOf("?") > -1 && (string.indexOf("?") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("?");
        }

        if (string.indexOf(":") > -1 && (string.indexOf(":") < endIndex || endIndex == -1)) {
            int indexColon = string.indexOf(":");
            if (string.length() > indexColon + 1 && !StringHelper.isNumber(string.charAt(indexColon + 1))) {
                endIndex = indexColon;
            }

        }
        if (endIndex == -1) {
            endIndex = string.length();
        }

        else {
            ++endIndex; // take last character as well
        }

        return string.substring(0, endIndex);
    }
    
    public static void main(String[] args) {
        
        
        // demo for the tokenizer problem
        String text = FileHelper.readFileToString("data/test/tokenizerProblem.txt");
        
        // tokenize the whole text
        int count = 0;
        List<String> tokens = Tokenizer.tokenize(text);
        for (String token : tokens) {
            if (token.equals("Number")) {
                count++;
            }
        }
        System.out.println("# occurences 1 : " + count);
        
        // split text into sentences, 
        // then tokenize each sentence
        count = 0;
        List<String> sentences = Tokenizer.getSentences(text);
        for (String sentence : sentences) {
            List<String> tokensInSentence = Tokenizer.tokenize(sentence);
            for (String token : tokensInSentence) {
                if (token.equals("Number")) {
                    count++;
                }
            }
        }
        System.out.println("# occurences 2 : " + count);
        
        
    }

}
