package ws.palladian.helper.nlp;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.normalization.StringNormalizer;
import ws.palladian.helper.normalization.UnitNormalizer;

import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <p>
 * The StringHelper provides functionality for typical String manipulation operations.
 * </p>
 *
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 * @author Martin Gregor
 */
public final class StringHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringHelper.class);
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,###,##0.##", new DecimalFormatSymbols(Locale.US));
    private static final Pattern PATTERN_FIRST_WORD = Pattern.compile("^(\\w+)(?:\\s|$)");
    private static final Pattern PATTERN_STRING = Pattern.compile(RegExp.STRING);
    private static final Pattern PATTERN_NUMBER = Pattern.compile(RegExp.NUMBER);
    private static final Pattern PATTERN_NUMBER_STRICT = Pattern.compile(
            "-?((\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?)|(^\\d+$)|(\\d{1,3}(,\\d{3})+(\\.\\d{1,2})?)|(\\d+,\\d{1,20})|(\\d+\\.\\d{1,20}))");
    private static final Pattern PATTERN_EXPONENTIAL_NUMBER = Pattern.compile("^-?\\d+\\.\\d+E\\d+$");
    private static final Pattern PATTERN_STARTS_WITH_NUMBER = Pattern.compile("^" + RegExp.NUMBER);
    private static final Pattern PATTERN_NUMBERING1 = Pattern.compile("^\\s*\\d+(\\.?\\d?)*\\s*");
    private static final Pattern PATTERN_NUMBERING2 = Pattern.compile("^\\s*#\\d+(\\.?\\d?)*\\s*");
    public static final Pattern PATTERN_LIMITED_WHITESPACES = Pattern.compile("[ ]{2,10}");
    private static final Pattern PATTERN_NON_ASCII_SPACE = Pattern.compile(" ");
    private static final Pattern PATTERN_NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern PATTERN_BRACKETS = Pattern.compile("[(\\[{].*?[)\\]}]");
    private static final Pattern PATTERN_MULTIPLE_WHITESPACES = Pattern.compile("[ ]{2,}");
    private static final Pattern PATTERN_MULTIPLE_HYPHENS = Pattern.compile("[-]{2,}");
    private static final Pattern PATTERN_DIGIT = Pattern.compile("[^0-9]");
    private static final Pattern PATTERN_UPPERCASE = Pattern.compile("[^A-Z]");

    private static final Pattern FOUR_BYTE_UTF8 = Pattern.compile("[^ -\uD7FF\uE000-\uFFFF\n\r]");

    public static final char[] TRIMMABLE_CHARACTERS = {',', '.', ':', ';', '!', '|', '?', '¬', ' ', ' ', '#', '\'', '"', '*', '/', '\\', '@', '<', '>', '=', '·', '^', '_', '+',
            '»', 'ￂ', '•', '”', '“', '´', '`', '¯', '~', '®', '™', '○', '-'};

    private StringHelper() {
        // utility class.
    }

    /**
     * In ontologies names can not have certain characters so they have to be changed.
     *
     * @param name      The name.
     * @param maxLength The maximum length of the string. -1 means no maximum length.
     * @return The safe name.
     */
    public static String makeSafeName(String name, int maxLength) {
        String safeName = name.replace(" ", "-");
        safeName = safeName.replace("_", "-");
        safeName = safeName.replace("/", "-");
        safeName = safeName.replace("'", "");
        safeName = safeName.replace("`", "");
        safeName = safeName.replace("´", "");
        safeName = safeName.replace("’", "");
        safeName = safeName.replace("%", "");
        safeName = safeName.replace("@", "");
        safeName = safeName.replace("~", "");
        safeName = safeName.replace("&", "-");
        safeName = safeName.replace("#", "-");
        safeName = safeName.replace("$", "-");
        safeName = safeName.replace("§", "-");
        safeName = safeName.replace("\"", "");
        safeName = safeName.replace(",", "-");
        safeName = safeName.replace("*", "-");
        safeName = safeName.replace(".", "-");
        safeName = safeName.replace(";", "-");
        safeName = safeName.replace(":", "-");
        safeName = safeName.replace("|", "-");
        safeName = safeName.replace("!", "");
        safeName = safeName.replace("?", "");
        safeName = safeName.replace(">", "");
        safeName = safeName.replace("<", "");
        safeName = safeName.replace("^", "");
        safeName = safeName.replace("(", "");
        safeName = safeName.replace(")", "");
        safeName = safeName.replace("[", "");
        safeName = safeName.replace("]", "");
        safeName = safeName.replace("{", "");
        safeName = safeName.replace("}", "");
        safeName = safeName.replace("+", "");
        safeName = safeName.replace("ä", "ae");
        safeName = safeName.replace("Ä", "Ae");
        safeName = safeName.replace("ö", "oe");
        safeName = safeName.replace("Ö", "Oe");
        safeName = safeName.replace("ü", "ue");
        safeName = safeName.replace("Ü", "Ue");
        safeName = safeName.replace("ß", "ss");

        safeName = removeControlCharacters(safeName);
        safeName = removeNonAsciiCharacters(safeName);

        safeName = PATTERN_MULTIPLE_HYPHENS.matcher(safeName).replaceAll("-");

        safeName = safeName.toLowerCase();

        if (maxLength > 0) {
            safeName = safeName.substring(0, Math.min(safeName.length(), maxLength));
        }

        return safeName;
    }

    public static String makeSafeName(String name) {
        return makeSafeName(name, -1);
    }

    public static String shorten(String string, int maxLength) {
        if (string == null) {
            return null;
        }
        return string.substring(0, Math.min(string.length(), maxLength));
    }

    /**
     * Shorten a string to a given length if necessary and add ellipsis.
     *
     * @param string    The string to shorten.
     * @param maxLength The maximum length.
     * @return The original string if it was shorter than the max. length, or a shortened string with appended "...", or
     * <code>null</code> in case the given string was null.
     */
    public static String shortenEllipsis(String string, int maxLength) {
        if (string == null) {
            return null;
        }
        if (string.length() <= maxLength) {
            return string;
        }
        return string.substring(0, maxLength).concat("…");
    }

    /**
     * <p>
     * Get indices of a string within a text. For example, for the text "This is a text" and the search string " ", the
     * indices [4, 7, 9] are returned, giving the positions of the white spaces.
     * </p>
     *
     * @param text   The text to check.
     * @param search The search string for which to get the indices.
     * @return A {@link List} of positions for the specified search string within the text, or an empty List if the
     * search string was not found or empty, or an empty text or <code>null</code> was supplied.
     */
    public static List<Integer> getOccurrenceIndices(String text, String search) {
        if (text == null || search == null || search.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> indices = new ArrayList<>();
        int lastPosition = 0;
        int position;
        while ((position = text.indexOf(search, lastPosition)) > -1) {
            indices.add(position);
            lastPosition = position + 1;
        }
        return indices;
    }

    /**
     * <p>
     * Make camel case.
     * </p>
     *
     * @param name           the name
     * @param uppercaseFirst the uppercase first
     * @return the string
     */
    public static String makeCamelCase(String name, boolean uppercaseFirst) {
        String camelCasedName = "";
        String modName = name.replaceAll("\\s", "_");

        String[] parts = modName.split("_");
        for (String part : parts) {
            camelCasedName += upperCaseFirstLetter(part);
        }

        if (!uppercaseFirst) {
            camelCasedName = lowerCaseFirstLetter(camelCasedName);
        }

        return camelCasedName;

    }

    /**
     * <p>
     * Make first letter of word upper case.
     * </p>
     *
     * @param string The term.
     * @return The term with an upper case first letter.
     */
    public static String upperCaseFirstLetter(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * <p>
     * Make first letters of all words upper case.
     * </p>
     *
     * @param string The term.
     * @return The term with an upper case first letters.
     */
    public static String upperCaseFirstLetters(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        String[] words = string.split("\\s");
        for (int i = 0; i < words.length; i++) {
            words[i] = upperCaseFirstLetter(words[i]);
        }

        return StringUtils.join(words, " ");
    }

    /**
     * Make first letter of word lower case.
     *
     * @param string The term.
     * @return The term with an lower case first letter.
     */
    public static String lowerCaseFirstLetter(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    /**
     * <p>
     * Replace a certain string only within a substring of a text.
     * </p>
     *
     * @param text         The text in which something should be replaced.
     * @param start        The start of the substring in which we want to replace something.
     * @param end          The end of the substring in which we want to replace something.
     * @param searchString The string we want to replace.
     * @param replacement  The replacement.
     * @return The string with the replaced search string.
     */
    public static String replaceWithin(String text, int start, int end, String searchString, String replacement) {

        String retText = text.substring(0, start);

        retText += text.substring(start, end).replace(searchString, replacement);

        retText += text.substring(end);

        return retText;
    }

    /**
     * <p>
     * Transform a name For example: jim carrey => Jim Carrey, university of los angeles => University of Los Angeles
     * </p>
     * <p>
     * <em>Note: This works for English only!</em>
     * </p>
     *
     * @param name The entity name.
     * @return The normalized entity name.
     */
    public static String normalizeCapitalization(String name) {
        String normalizedName = "";

        List<String> noUppercase = Arrays.asList("of", "and", "the");

        String[] parts = name.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0 && noUppercase.contains(part)) {
                normalizedName += part + " ";
            } else {
                normalizedName += upperCaseFirstLetter(part) + " ";
            }
        }

        return normalizedName.trim();
    }

    /**
     * Replace number before a text. 1.1 Text => Text
     *
     * @param numberedText The text that possibly has numbers before it starts.
     * @return The text without the numbers.
     */
    public static String removeNumbering(String numberedText) {
        String modText = PATTERN_NUMBERING1.matcher(numberedText).replaceAll("");
        modText = PATTERN_NUMBERING2.matcher(modText).replaceAll("");
        return modText;
    }

    /**
     * Replace numbers in a text. 1.1 Text => Text, Text 1.2 => Text
     *
     * @param numberedText The text that possibly has numbers before it starts.
     * @return The text without the numbers.
     */
    public static String removeNumbers(String numberedText) {
        return PATTERN_NUMBER.matcher(numberedText).replaceAll("");
    }

    /**
     * <p>
     * Check whether a given string contains a proper noun.
     * </p>
     *
     * @param searchString The search string.
     * @return True if the string contains a proper noun, else false.
     */
    public static boolean containsProperNoun(String searchString) {
        return PATTERN_STRING.matcher(searchString).find();
    }

    public static boolean containsWordRegExp(Collection<String> words, String searchString) {
        for (String word : words) {
            if (containsWordRegExp(word, searchString)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsWord(Collection<String> words, String searchString) {
        for (String word : words) {
            if (containsWord(word, searchString)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsWordCaseSensitive(Collection<String> words, String searchString) {
        for (String word : words) {
            if (containsWordCaseSensitive(word, searchString)) {
                return true;
            }
        }
        return false;
    }

    public static String containsWhichWord(Collection<String> words, String searchString) {
        for (String word : words) {
            if (containsWord(word, searchString)) {
                return word;
            }
        }
        return null;
    }

    public static Set<String> containsWhichWords(Collection<String> words, String searchString) {
        Set<String> containedWords = new HashSet<>();

        for (String word : words) {
            if (containsWord(word, searchString)) {
                containedWords.add(word);
            }
        }

        return containedWords;
    }

    public static Set<String> containsWhichWordsCaseSensitive(Collection<String> words, String searchString) {
        Set<String> containedWords = new HashSet<>();

        for (String word : words) {
            if (containsWordCaseSensitive(word, searchString)) {
                containedWords.add(word);
            }
        }

        return containedWords;
    }

    /**
     * <p>
     * Check whether a string contains a word given as a regular expression. The word can be surrounded by whitespaces
     * or punctuation but can not be within another word.
     * </p>
     *
     * @param word         The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return True, if the word is contained, false if not.
     */
    public static boolean containsWordRegExp(String word, String searchString) {
        String allowedNeighbors = "[\\s,.;-?!()\\[\\]]";
        String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|(" + allowedNeighbors + word + "$)|(^" + word + "$)";

        try {
            Pattern pattern = PatternHelper.compileOrGet(regexp, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(searchString).find();
        } catch (PatternSyntaxException e) {
            LOGGER.error("PatternSyntaxException for {} with regExp {}", new Object[]{searchString, regexp, e});
            return false;
        }
    }

    /**
     * <p>
     * Get the index of the word contained in the search string. Return -1 if is not contained.
     * </p>
     *
     * @param word         The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return The index position or -1 if the word is not contained.
     */
    public static int indexOfWordCaseSensitive(String word, String searchString) {
        Matcher matcher = PatternHelper.compileOrGet("((?<=^)|(?<=[;!?.,: ]))" + word + "(?=([;!?.,: ]|$))").matcher(searchString);
        boolean found = matcher.find();
        if (found) {
            return matcher.start(1);
        }
        return -1;
    }

    /**
     * <p>
     * Get the index of the word contained in the search string. Return -1 if is not contained.
     * </p>
     *
     * @param word         The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return The index position or -1 if the word is not contained.
     */
    public static int lastIndexOfWordCaseSensitive(String word, String searchString) {
        Matcher matcher = PatternHelper.compileOrGet("((?<=^)|(?<=[;!?.,: ]))" + word + "(?=([;!?.,: ]|$))").matcher(searchString);
        int start = -1;
        while (matcher.find()) {
            start = matcher.start(1);
        }
        return start;
    }

    /**
     * <p>
     * Check whether a string contains a word. The word can be surrounded by whitespaces or punctuation but can not be
     * within another word.
     * </p>
     *
     * @param word         The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return True, if the word is contained, false if not.
     */
    public static boolean containsWordCaseSensitive(String word, String searchString) {
        return containsWordCaseSensitiveRecursive(word, searchString, false, -1);
    }

    public static boolean containsWordCaseSensitiveRecursive(String word, String searchString, boolean contained, int startIndex) {
        int index = searchString.indexOf(word, startIndex);
        if (index == -1 || word.isEmpty()) {
            return contained;
        }
        boolean leftBorder;
        if (index == 0) {
            leftBorder = true;
        } else {
            char prevChar = searchString.charAt(index - 1);
            leftBorder = !(Character.isLetter(prevChar) || Character.isDigit(prevChar));

            // 1.5mm -> 5mm is not a word but part of 1.5mm
            if (leftBorder && index > 1) {
                char prevPrevChar = searchString.charAt(index - 2);
                if (Character.isDigit(prevPrevChar) && prevChar == '.') {
                    leftBorder = false;
                }
            }
        }
        boolean rightBorder;
        if (index + word.length() == searchString.length()) {
            rightBorder = true;
        } else {
            char nextChar = searchString.charAt(index + word.length());
            rightBorder = !(Character.isLetter(nextChar) || Character.isDigit(nextChar) || nextChar == '+');
        }

        if (leftBorder && rightBorder) {
            return true;
        }

        return containsWordCaseSensitiveRecursive(word, searchString, false, index + 1);
    }

    /**
     * <p>
     * Check whether a string contains a word. The word can be surrounded by whitespaces or punctuation but can not be
     * within another word.
     * </p>
     * <p>
     * NOTE: <b>This method is case INsensitive</b>. {@link StringHelper#containsWordCaseSensitive(String, String)} is a
     * case sensitive alternative which is considerably faster.
     * </p>
     *
     * @param word         The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return True, if the word is contained, false if not.
     */
    public static boolean containsWord(String word, String searchString) {
        return containsWordCaseSensitive(word.toLowerCase(), searchString.toLowerCase());
    }

    /**
     * <p>
     * Determine, whether the supplied char is a punctuation character (i.e. one of [.,:;?!]).
     * </p>
     *
     * @param character The character to check.
     * @return <code>true</code> if punctuation character, <code>false</code> otherwise.
     */
    public static boolean isPunctuation(char character) {
        return Arrays.asList('.', ',', ':', ';', '?', '!').contains(character);
    }

    public static String getFirstWord(String searchString) {
        Matcher matcher = PATTERN_FIRST_WORD.matcher(searchString);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    public static String removeFirstWord(String searchString) {
        String firstWord = getFirstWord(searchString);
        return searchString.replaceFirst(firstWord + "(\\s|$)", "");
    }

    public static String removeWords(List<String> words, String searchString) {
        words.sort(StringLengthComparator.INSTANCE);
        for (String word : words) {
            searchString = removeWord(word, searchString);
        }
        return searchString;
    }

    public static String removeWord(String word, String searchString) {
        return PATTERN_LIMITED_WHITESPACES.matcher(replaceWord(word, "", searchString)).replaceAll(" ");
    }

    public static String removeStemmedWord(String word, String searchString) {
        return PATTERN_LIMITED_WHITESPACES.matcher(replaceStemmedWord(word, "", searchString)).replaceAll(" ");
    }

    public static String replaceStemmedWord(String word, String replacement, String searchString) {
        if (word == null || word.isEmpty()) {
            return searchString;
        }

        // reconstruct the full word
        List<String> fullWords = getRegexpMatches(word + "[A-Za-z]{0,5}", searchString);
        for (String fullWord : fullWords) {
            searchString = replaceWord(fullWord, replacement, searchString);
        }

        return searchString;
    }

    public static String replaceWord(String word, String replacement, String searchString) {
        if (word == null || word.isEmpty()) {
            return searchString;
        }
        return replaceWordCaseSensitive(word.toLowerCase(), replacement, searchString, searchString.toLowerCase());
    }

    public static String replaceWordCaseSensitive(String word, String replacement, String searchString, String searchStringLc) {
        if (word == null || word.isEmpty()) {
            return searchString;
        }

        int oldIndex = 0;
        int index;
        do {
            index = searchStringLc.indexOf(word, oldIndex);
            if (index == -1) {
                return searchString;
            }
            oldIndex = index + word.length();

            boolean leftBorder;
            if (index == 0) {
                leftBorder = true;
            } else {
                char prevChar = searchStringLc.charAt(index - 1);
                leftBorder = !(Character.isLetter(prevChar) || Character.isDigit(prevChar) || Character.getType(prevChar) == Character.DASH_PUNCTUATION);
            }
            boolean rightBorder;
            if (index + word.length() == searchStringLc.length()) {
                rightBorder = true;
            } else {
                char nextChar = searchStringLc.charAt(index + word.length());
                rightBorder = !(Character.isLetter(nextChar) || Character.isDigit(nextChar) || Character.getType(nextChar) == Character.DASH_PUNCTUATION);
            }

            // if word exists, cut it out and replace with replacement
            if (leftBorder && rightBorder) {
                String before = searchString.substring(0, index);
                String after = searchString.substring(oldIndex);
                searchString = before + replacement + after;
                searchStringLc = searchString.toLowerCase();

                oldIndex = index + replacement.length();
            }

        } while (index > -1);

        return searchString;
    }

    public static StringBuilder removeWordCaseSensitive(String word, StringBuilder searchStringLowerCase) {
        if (word == null || word.isEmpty()) {
            return searchStringLowerCase;
        }

        int oldIndex = 0;
        int index;
        do {
            index = searchStringLowerCase.indexOf(word, oldIndex);
            if (index == -1) {
                return searchStringLowerCase;
            }
            oldIndex = index + word.length();

            boolean leftBorder;
            if (index == 0) {
                leftBorder = true;
            } else {
                char prevChar = searchStringLowerCase.charAt(index - 1);
                leftBorder = !(Character.isLetter(prevChar) || Character.isDigit(prevChar) || Character.getType(prevChar) == Character.DASH_PUNCTUATION);
            }
            if (!leftBorder) {
                continue;
            }
            boolean rightBorder;
            if (index + word.length() == searchStringLowerCase.length()) {
                rightBorder = true;
            } else {
                char nextChar = searchStringLowerCase.charAt(index + word.length());
                rightBorder = !(Character.isLetter(nextChar) || Character.isDigit(nextChar) || Character.getType(nextChar) == Character.DASH_PUNCTUATION);
            }

            // if word exists, cut it out and replace with replacement
            if (rightBorder) {
                searchStringLowerCase.delete(index, oldIndex);
                oldIndex = index;
            }
        } while (index > -1);

        return searchStringLowerCase;
    }

    public static String replaceWordCaseSensitive(String word, String replacement, String searchStringLowerCase) {
        if (word == null || word.isEmpty()) {
            return searchStringLowerCase;
        }

        int oldIndex = 0;
        int index;
        do {
            index = searchStringLowerCase.indexOf(word, oldIndex);
            if (index == -1) {
                return searchStringLowerCase;
            }
            oldIndex = index + word.length();

            boolean leftBorder;
            if (index == 0) {
                leftBorder = true;
            } else {
                char prevChar = searchStringLowerCase.charAt(index - 1);
                leftBorder = !(Character.isLetter(prevChar) || Character.isDigit(prevChar) || Character.getType(prevChar) == Character.DASH_PUNCTUATION);
            }
            if (!leftBorder) {
                continue;
            }
            boolean rightBorder;
            if (index + word.length() == searchStringLowerCase.length()) {
                rightBorder = true;
            } else {
                char nextChar = searchStringLowerCase.charAt(index + word.length());
                rightBorder = !(Character.isLetter(nextChar) || Character.isDigit(nextChar) || Character.getType(nextChar) == Character.DASH_PUNCTUATION);
            }

            // if word exists, cut it out and replace with replacement
            if (rightBorder) {
                String before = searchStringLowerCase.substring(0, index);
                String after = searchStringLowerCase.substring(oldIndex);
                searchStringLowerCase = before + replacement + after;
                oldIndex = index + replacement.length();
            }
        } while (index > -1);

        return searchStringLowerCase;
    }

    /**
     * Check whether a given string contains a numeric value.
     * NOTE: the number must be by itself and not within or at the end of a word, e.g. V200 does NOT contain a number according to this logic
     *
     * @param searchString The search string.
     * @return True if the string contains a numeric value, else false.
     */
    public static boolean containsNumber(String searchString) {
        return PATTERN_NUMBER.matcher(searchString).find();
    }

    /**
     * <p>
     * Replace "non-breaking" aka. protected whitespace (unicode 0x00A0) with normal whitespace.
     * </p>
     *
     * @param string the string
     * @return the string
     */
    public static String replaceProtectedSpace(String string) {
        return string.replaceAll("\u00A0", " ");
    }

    /**
     * <p>
     * Strips all non-ASCII characters from the supplied string. Useful to remove Asian characters, for example.
     * </p>
     *
     * @param string The input string.
     * @return The input string removed from non-ascii characters.
     * see http://forums.sun.com/thread.jspa?threadID=5370865
     */
    public static String removeNonAsciiCharacters(String string) {
        string = PATTERN_NON_ASCII_SPACE.matcher(string).replaceAll(" ");
        return PATTERN_NON_ASCII.matcher(string).replaceAll("");
    }

    /**
     * <p>
     * Remove brackets and everything in between the brackets. "()[]{}" will be removed. For example
     * "This is a text (just a sample)." becomes "This is a text ."
     * </p>
     *
     * @param bracketString The bracket string
     * @return The string without brackets.
     */
    public static String removeBrackets(String bracketString) {
        String string;
        string = PATTERN_BRACKETS.matcher(bracketString).replaceAll("");
        string = removeDoubleWhitespaces(string);
        return string.trim();
    }

    /**
     * Checks whether character is a bracket.
     *
     * @param character The character.
     * @return True if character is a bracket, else false.
     */
    public static boolean isBracket(char character) {
        return Arrays.asList('(', ')', '{', '}', '[', ']').contains(character);
    }

    /**
     * Check if the string is a number.
     *
     * @param ch the ch
     * @return True if string is number, else false.
     */
    public static boolean isNumber(char ch) {
        return isNumber(Character.toString(ch));
    }

    /**
     * <p>
     * Checks if the input string is a number.
     * </p>
     *
     * @param string The string to check.
     * @return true, if is number
     */
    public static boolean isNumber(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }

        // consider negation
        if (string.startsWith("-")) {
            string = string.substring(1);
        }

        boolean isNumber = true;
        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER && ch != '.' && ch != ',') {
                isNumber = false;
            }
        }

        if (string.startsWith(".") || string.endsWith(".")) {
            return false;
        }

        // consider exponential format
        boolean expMatch = false;
        if (!isNumber && PATTERN_EXPONENTIAL_NUMBER.matcher(string).matches()) {
            isNumber = true;
            expMatch = true;
        }

        if (!expMatch && isNumber && !PATTERN_NUMBER_STRICT.matcher(string).matches()) {
            isNumber = false;
        }

        return isNumber;
    }

    public static boolean isNumberOrNumberWord(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }

        if (isNumber(string)) {
            return true;
        }

        string = StringHelper.trim(string).toLowerCase();

        return Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve").contains(string);
    }

    /**
     * <p>
     * Checks if is numeric expression.
     * </p>
     *
     * @param string the string
     * @return <code>true</code>, if is numeric expression, <code>false</code> otherwise.
     */
    public static boolean isNumericExpression(String string) {
        if (string.isEmpty()) {
            return false;
        }

        boolean isNumericExpression = true;

        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER && Character.getType(ch) != Character.DASH_PUNCTUATION && Character.getType(ch)
                    != Character.CONNECTOR_PUNCTUATION && Character.getType(ch) != Character.CURRENCY_SYMBOL && Character.getType(ch) != Character.DIRECTIONALITY_WHITESPACE
                    && ch != '%' && ch != '.' && ch != ',' && ch != ':') {
                isNumericExpression = false;
                break;
            }
        }

        Matcher m = PATTERN_STARTS_WITH_NUMBER.matcher(string);
        try {

            if (m.find()) {
                double number = Double.parseDouble(StringNormalizer.normalizeNumber(m.group()));
                double convertedNumber = UnitNormalizer.getNormalizedNumber(number, string.substring(m.end(), string.length()));
                if (number != convertedNumber) {
                    return true;
                }

            }
        } catch (NumberFormatException e) {
            LOGGER.debug("{}, {}", m.group(), e.getMessage());
            return false;
        }

        return isNumericExpression;
    }

    /**
     * Checks if is time expression.
     *
     * @param string the string
     * @return true, if is time expression
     */
    public static boolean isTimeExpression(String string) {
        return string.matches("(\\d){1,2}:(\\d){1,2}(\\s)?(am|pm)");
    }

    /**
     * Checks if is completely uppercase.
     *
     * @param testString the test string
     * @return true, if is completely uppercase
     */
    public static boolean isCompletelyUppercase(String testString) {
        String string = StringHelper.trim(testString);
        if (string.length() == 0) {
            return false;
        }

        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.UPPERCASE_LETTER && Character.getType(ch) != Character.INITIAL_QUOTE_PUNCTUATION && Character.getType(ch)
                    != Character.FINAL_QUOTE_PUNCTUATION && ch != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts uppercase.
     *
     * @param testString the test string
     * @return true, if successful
     */
    public static boolean startsUppercase(String testString) {
        String string = StringHelper.trim(testString);
        return string.length() != 0 && Character.isUpperCase(string.charAt(0));
    }

    /**
     * <p>
     * Count letters and digits in the supplied string.
     * </p>
     *
     * @param string The string in which to count.
     * @return The number of letters and digits, 0 in case the string was empty or <code>null</code>.
     */
    public static int countLettersDigits(String string) {
        if (string == null) {
            return 0;
        }
        return string.replaceAll("[^a-zA-Z0-9]", "").length();
    }

    /**
     * <p>
     * Count digits (0, 1, 2, … 9) in the supplied string.
     * </p>
     *
     * @param string The string in which to count.
     * @return The number of digits, 0 in case the string was empty or <code>null</code>.
     */
    public static int countDigits(String string) {
        if (string == null) {
            return 0;
        }
        return PATTERN_DIGIT.matcher(string).replaceAll("").length();
    }

    /**
     * <p>
     * Count upper case letters in the supplied string.
     * </p>
     *
     * @param string The string in which to count.
     * @return The number of uppercase letters, 0 in case the string was empty or <code>null</code>.
     */
    public static int countUppercaseLetters(String string) {
        if (string == null || string.isEmpty()) {
            return 0;
        }
        return PATTERN_UPPERCASE.matcher(string).replaceAll("").length();
    }

    /**
     * <p>
     * Align casings of words, e.g. "dog","Dogge" => "Dog".
     * </p>
     * <p>
     * This method is useful for handling stemming and other word transformations.
     * </p>
     *
     * @param toAlign      The word that needs to get the same casing as the targetCasing.
     * @param targetCasing The word which casing should be induced into the toAlign word.
     * @return The toAlign word with the casing of the targetCasing word.
     */
    public static String alignCasing(String toAlign, String targetCasing) {
        if (startsUppercase(targetCasing)) {
            return upperCaseFirstLetter(toAlign);
        } else {
            return lowerCaseFirstLetter(toAlign);
        }
    }

    /**
     * <p>
     * Check, if a character is a vowel.
     * </p>
     *
     * @param character The character to check.
     * @return <code>true</code> if character is a vowel, <code>false</code> otherwise.
     */
    public static boolean isVowel(char character) {
        return Arrays.asList('A', 'E', 'I', 'O', 'U').contains(Character.toUpperCase(character));
    }

    public static String trimLeft(String string) {
        return trimLeft(string, "");
    }

    public static String trimLeft(String string, String keepCharacters) {
        return trim(string, true, false, keepCharacters);
    }

    public static String trimRight(String string) {
        return trimRight(string, "");
    }

    public static String trimRight(String string, String keepCharacters) {
        return trim(string, false, true, keepCharacters);
    }

    /**
     * Remove unwanted characters from beginning and end of string.
     *
     * @param string The string.
     * @return The trimmed string.
     */
    public static String trim(String string) {
        return trim(string, "");
    }

    /**
     * Trim.
     *
     * @param inputString    the input string
     * @param keepCharacters the keep characters
     * @return the string or null if inputString was null.
     */
    public static String trim(String inputString, String keepCharacters) {
        return trim(inputString, true, true, keepCharacters);
    }

    public static String trim(String inputString, boolean trimLeft, boolean trimRight, String keepCharacters) {
        if (inputString == null) {
            return null;
        }

        String string = inputString.trim();
        if (string.length() == 0) {
            return string;
        }

        string = StringEscapeUtils.unescapeHtml(string);

        // whitespace is also unwanted but trim() handles that, " " here is another character (ASCII code 160)

        // delete quotes only if it is unlikely to be a unit (foot and inches)
        // Pattern p = Pattern.compile("((\\d)+'')|('(\\s)?(\\d)+\")");
        // Pattern p = Pattern.compile("(\\A\"([^\"]*)[^\"]$)|((\\d)+'')|('(\\s)?(\\d)+\")");
        // if (p.matcher(string).find()) {
        // unwanted[12] = " ";
        // unwanted[13] = " ";
        // }

        boolean deleteFirst = true;
        boolean deleteLast = true;
        while (((deleteFirst && trimLeft) || (deleteLast && trimRight)) && !string.isEmpty()) {
            deleteFirst = false;
            deleteLast = false;
            char first = string.charAt(0);
            char last = string.charAt(string.length() - 1);
            // System.out.println(Character.getType(last));
            for (char element : TRIMMABLE_CHARACTERS) {
                if (keepCharacters.indexOf(element) > -1) {
                    continue;
                }

                // System.out.println(first.charValue());
                // System.out.println(Character.isSpaceChar(first));
                if (first == element || Character.getType(first) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || Character.isSpaceChar(first)) {
                    deleteFirst = true;
                }
                if (last == element || Character.getType(last) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || Character.isSpaceChar(last)) {
                    deleteLast = true;
                }
                if (deleteFirst && deleteLast) {
                    break;
                }
            }

            if (deleteFirst && trimLeft) {
                string = string.substring(1);
            }

            if (deleteLast && trimRight && string.length() > 0) {
                string = string.substring(0, string.length() - 1);
            }

            string = string.trim();
        }

        return string.trim();
    }

    /**
     * <p>
     * Removes unwanted control characters from the specified string.
     * </p>
     *
     * @param string The string with control characters.
     */
    public static String removeControlCharacters(String string) {
        // replace line breaks encoded in utf-8
        string = string.replace("\u2028", "\n");

        // replace line breaks encoded in html entities
        string = string.replace("&#10", "\n");

        for (int i = 0, l = string.length(); i < l; ++i) {
            // < 33 means all control characters are not wanted as well
            if (string.charAt(i) < 33) {
                string = string.replace(string.charAt(i), ' ');
            }
        }

        return string;
    }

    /**
     * <p>
     * This is a shortcut method for frequent text cleaning needs.
     * </p>
     * <p>
     * This method does the following.
     * </p>
     * <ul>
     * <li>Unescape HTML (&_lt; becomes >)</li>
     * <li>Remove control characters.</li>
     * <li>Remove protected spaces.</li>
     * <li>Remove double white spaces.</li>
     * <li>Remove HTML tags (<b>stop</b> becomes stop).</li>
     * </ul>
     *
     * @param text The text that should be cleansed.
     * @return The cleansed text.
     */
    public static String clean(String text) {
        return clean(text, ".?!“”\"");
    }

    public static String clean(String text, String keepCharacters) {
        text = removeControlCharacters(text);
        text = cleanKeepFormat(text, keepCharacters);
        return text;
    }

    public static String cleanKeepFormat(String text) {
        return cleanKeepFormat(text, ".?!“”\"");
    }

    public static String cleanKeepFormat(String text, String keepCharacters) {
        text = HtmlHelper.stripHtmlTags(text);
        text = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(text);
        text = replaceProtectedSpace(text);
        text = removeDoubleWhitespaces(text);
        // text = removeNonAsciiCharacters(text);

        // trim but keep sentence delimiters
        text = StringHelper.trim(text, keepCharacters);
        text = text.replace("″", "\"");
        text = text.replace("\u00AD", "-");
        if (text.startsWith(")")) {
            text = text.substring(1);
        }

        return text;
    }

    /**
     * Put article in front.
     *
     * @param inputString the input string
     * @return the string
     */
    public static String putArticleInFront(String inputString) {
        String string = inputString.trim();

        if (string.toLowerCase().endsWith(",the")) {
            string = "The " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", the")) {
            string = "The " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",a")) {
            string = "A " + string.substring(0, string.length() - 2);
        } else if (string.toLowerCase().endsWith(", a")) {
            string = "A " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(",an")) {
            string = "An " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", an")) {
            string = "An " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",der")) {
            string = "Der " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", der")) {
            string = "Der " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",die")) {
            string = "Die " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", die")) {
            string = "Die " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",das")) {
            string = "Das " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", das")) {
            string = "Das " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",le")) {
            string = "Le " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", le")) {
            string = "Le " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",la")) {
            string = "La " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", la")) {
            string = "La " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",les")) {
            string = "Les " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", les")) {
            string = "Les " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",las")) {
            string = "Las " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", las")) {
            string = "Las " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",los")) {
            string = "Los " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", los")) {
            string = "Los " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",ta")) {
            string = "Ta " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", ta")) {
            string = "Ta " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",il")) {
            string = "Il " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", il")) {
            string = "Il " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",un")) {
            string = "Un " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", un")) {
            string = "Un " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",uno")) {
            string = "Uno " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", uno")) {
            string = "Uno " + string.substring(0, string.length() - 5);
        }

        return string;
    }

    /**
     * <p>
     * Count number of words, words are separated by a blank " ".
     * </p>
     *
     * @param string The string.
     * @return The number of words in the string.
     */
    public static int countWords(String string) {
        return string.replaceAll("\\s{2,}", "\\s").split(" ").length;
    }

    /**
     * Calculate similarity.
     *
     * @param string1 the string1
     * @param string2 the string2
     * @return the double
     */
    public static double calculateSimilarity(String string1, String string2) {
        return calculateSimilarity(string1, string2, true);
    }

    /**
     * Calculate similarity.
     *
     * @param string1       the string1
     * @param string2       the string2
     * @param caseSensitive the case sensitive
     * @return the double
     */
    public static double calculateSimilarity(String string1, String string2, boolean caseSensitive) {
        double longestCommonStringLength = getLongestCommonString(string1, string2, caseSensitive, true).length();
        if (longestCommonStringLength == 0) {
            return 0.0;
        }

        return longestCommonStringLength / Math.min(string1.length(), string2.length());
    }

    /**
     * Get the longest common character chain two strings have in common.
     *
     * @param string1       The first string.
     * @param string2       The second string.
     * @param caseSensitive True if the check should be case sensitive, false otherwise.
     * @param shiftString   If true, the shorter string will be shifted and checked against the longer string. The longest
     *                      common string of two strings is found
     *                      regardless whether they start with the same characters. If true, ABCD and BBCD have BCD in common, if
     *                      false the longest common string is
     *                      empty.
     * @return The longest common string.
     */
    public static String getLongestCommonString(String string1, String string2, boolean caseSensitive, boolean shiftString) {
        String string1Compare = string1;
        String string2Compare = string2;
        if (!caseSensitive) {
            string1Compare = string1.toLowerCase();
            string2Compare = string2.toLowerCase();
        }

        // string length, string
        TreeMap<Integer, String> commonStrings = new TreeMap<>();

        // string s1 is shortened and shifts over string s2, s1 should be the
        // shorter string
        String s1 = string1Compare;
        String s2 = string2Compare;
        if (s1.length() > s2.length()) {
            s1 = string2Compare;
            s2 = string1Compare;
        }

        // shorten string 1 one character from the beginning in each iteration
        while (s1.length() > 1) {
            // shift the shorter string s1 over s2
            for (int startPosition = 0; startPosition < s2.length(); startPosition++) {

                // check how many characters are in common for both strings
                int index;
                for (index = startPosition; index < Math.min(s1.length() + startPosition, s2.length()); index++) {
                    if (s1.charAt(index - startPosition) != s2.charAt(index)) {
                        break;
                    }
                }
                commonStrings.put(index - startPosition, s1.substring(0, index - startPosition));
                if (!shiftString) {
                    break;
                }

            }

            if (!shiftString) {
                break;
            }

            s1 = s1.substring(1);
        }

        if (commonStrings.isEmpty()) {
            return "";
        }

        return commonStrings.descendingMap().entrySet().iterator().next().getValue();
    }

    /**
     * Reverse a string. ABC => CBA.
     *
     * @param string The string to be reversed.
     * @return The reversed string.
     */
    public static String reverseString(String string) {
        return new StringBuilder(string).reverse().toString();
    }

    /**
     * Transform a given text into a 20 byte sha-1 encoded string.
     *
     * @param text The text to be encoded.
     * @return The 20 byte (40 hexadecimal characters) string.
     */
    public static String sha1(String text) {
        StringBuilder sha1 = new StringBuilder();

        try {
            byte[] theTextToDigestAsBytes = text.getBytes("8859_1"/* encoding */);
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(theTextToDigestAsBytes);
            // md.update( int ) processes only the low order 8-bits. It actually
            // expects an unsigned byte.
            byte[] digest = md.digest();

            // will print SHA
            // Logger.getRootLogger().debug("Algorithm used: " + md.getAlgorithm());

            // should be 20 bytes, 160 bits long
            // Logger.getRootLogger().debug("Digest is " + digest.length + " bytes long.");

            // dump out the hash
            // Logger.getRootLogger().debug("Digest: ");
            for (byte b : digest) {
                // print byte as 2 hex digits with lead 0. Separate pairs of
                // digits with space
                // System.out.printf("%02X ", b & 0xff);
                sha1.append(String.format("%02x", b & 0xff));
            }
            // System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sha1.toString();
    }

    /**
     * Encode base64.
     *
     * @param string the string
     * @return the string
     */
    public static String encodeBase64(String string) {
        return new String(Base64.encodeBase64(string.getBytes()));
    }

    /**
     * Decode base64.
     *
     * @param string the string
     * @return the string
     */
    public static String decodeBase64(String string) {
        return new String(Base64.decodeBase64(string.getBytes()));
    }

    /**
     * <p>
     * Get the all substrings in the supplied string between the given sequences.
     * </p>
     *
     * @param string      The string from which to extract the substring, not <code>null</code>.
     * @param leftBorder  The left border, not empty, if <code>null</code> the start of the string is the left border.
     * @param rightBorder The right border, not empty, if <code>null</code> the end of the string is the right border..
     * @return {@link List} of substrings between the two given strings, or an empty List if not matches were found.
     */
    public static List<String> getSubstringsBetween(String string, String leftBorder, String rightBorder) {
        Validate.notNull(string, "string must not be null");

        List<String> substrings = new ArrayList<>();

        int leftBorderLength = 0;
        if (leftBorder != null) {
            leftBorderLength = leftBorder.length();
        }
        int rightIndex = 0;
        for (int i = 0; ; i++) {
            int leftIndex = 0;
            if (leftBorder != null) {
                leftIndex = string.indexOf(leftBorder, rightIndex);
            }
            if (rightBorder != null) {
                rightIndex = string.indexOf(rightBorder, leftIndex + leftBorderLength);
            } else {
                rightIndex = string.length();
            }
            if (rightIndex > leftIndex && ((leftIndex > -1 && leftBorder != null) || (i == 0 && leftBorder == null))) {
                substrings.add(string.substring(leftIndex + leftBorderLength, rightIndex));
            } else {
                break;
            }
        }

        return substrings;
    }

    /**
     * <p>
     * Get the first substring in the supplied string between the given sequences.
     * </p>
     *
     * @param string      The string from which to extract the substring, not <code>null</code>.
     * @param leftBorder  The left border, not <code>null</code> or empty.
     * @param rightBorder The right border, not <code>null</code> or empty.
     * @return The substring between the two given strings or an empty string if no match was found.
     */
    public static String getSubstringBetween(String string, String leftBorder, String rightBorder) {
        List<String> substrings = getSubstringsBetween(string, leftBorder, rightBorder);
        return !substrings.isEmpty() ? substrings.get(0) : "";
    }

    /**
     * Transforms a CamelCased String into a split String.
     *
     * @param camelCasedString The String to split.
     * @param separator        The separator to insert between the camelCased fragments.
     * @return The separated String.
     */
    public static String camelCaseToWords(String camelCasedString, String separator) {
        StringBuilder result = new StringBuilder();

        if (camelCasedString != null && !camelCasedString.isEmpty()) {

            char[] chars = camelCasedString.toCharArray();

            // append first character
            result.append(chars[0]);

            // append the rest
            for (int i = 1; i < chars.length; i++) {

                char current = chars[i];
                boolean currentIsUpper = Character.getType(current) == Character.UPPERCASE_LETTER;
                boolean previousIsLower = Character.getType(chars[i - 1]) == Character.LOWERCASE_LETTER;

                if (currentIsUpper && previousIsLower) {
                    result.append(separator);
                }
                result.append(current);
            }
        }

        return result.toString();
    }

    /**
     * Transforms a CamelCased String into a space separated String. For example: <code>camelCaseString</code> is
     * converted to <code>camel Case String</code>.
     *
     * @param camelCasedString The String to split.
     * @return The separated String.
     */
    public static String camelCaseToWords(String camelCasedString) {
        return camelCaseToWords(camelCasedString, " ");
    }

    /**
     * <p>
     * Replaces two or more white spaces (includes tabs, line breaks) by one.
     * </p>
     *
     * @param text The text to remove multiple white spaces from.
     * @return The cleansed text.
     */
    public static String removeDoubleWhitespaces(String text) {
        return PATTERN_MULTIPLE_WHITESPACES.matcher(text).replaceAll(" ");
    }

    /**
     * <p>
     * Counts whitespace in a text.
     * </p>
     *
     * @param text The text to count white spaces in.
     * @return The number of white spaces in the text.
     */
    public static int countWhitespaces(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ') {
                count++;
            }
        }
        return count;
    }

    /**
     * Shorten a String; returns the first num words.
     */
    public static String getFirstWords(String string, int num) {
        StringBuilder sb = new StringBuilder();
        if (string != null && num > 0) {
            String[] split = string.split("\\s");
            if (split.length == 0) {
                return "";
            }
            sb.append(split[0]);
            for (int i = 1; i < Math.min(num, split.length); i++) {
                sb.append(" ").append(split[i]);
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * Count number of occurrences of a specific string within a text (hint: to count the number of matches for a
     * regular expression, use {@link #countRegexMatches(String, String)} instead).
     * </p>
     *
     * @param text   The text which to check for patterns.
     * @param search The string which to search in the text.
     * @return The number of occurrences of the specified string in the text, or 0 if string was not found, or the
     * supplied pattern and/or text were empty or <code>null</code>.
     */
    public static int countOccurrences(String text, String search) {
        if (text == null || search == null || text.isEmpty() || search.isEmpty()) {
            return 0;
        }
        // return (text.length() - text.replace(search, "").length()) / search.length();
        int count = 0;
        for (int i = text.indexOf(search); i != -1; i = text.indexOf(search, i + search.length())) {
            count++;
        }
        return count;
    }

    /**
     * <p>
     * Count number of occurrences of a specific regular expression within a text (hint: to count the number of matches
     * of an ordinary string, use {@link #countOccurrences(String, String)} instead).
     * </p>
     *
     * @param text    The text which to check for occurrences.
     * @param pattern The regular expression to search in the text, not <code>null</code>.
     * @return The number of occurrences of the specified pattern in the text, or 0 if pattern was not found, or the
     * supplied text was empty or <code>null</code>.
     */
    public static int countRegexMatches(String text, String pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        return countRegexMatches(text, PatternHelper.compileOrGet(pattern));
    }

    public static int countRegexMatches(String text, Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Matcher matcher = pattern.matcher(text);
        int matches = 0;
        while (matcher.find()) {
            matches++;
        }
        return matches;
    }

    /**
     * This method ensures that the output String has only valid XML unicode characters as specified by the XML 1.0
     * standard. For reference, please see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty String if the input is null or empty.
     * <p>
     * For stream processing purposes see Xml10FilterReader.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     * see http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     */
    // TODO move this method to HtmlHelper
    public static String stripNonValidXMLCharacters(String in) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || "".equals(in)) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if (current == 0x9 || current == 0xA || current == 0xD || current >= 0x20 && current <= 0xD7FF || current >= 0xE000 && current <= 0xFFFD
                    || current >= 0x10000 && current <= 0x10FFFF) {
                out.append(current);
            }
        }
        return out.toString();
    }

    /**
     * <p>
     * Transform numbers between 0 and 12 to words, e.g. 1 becomes "one" and 12 becomes "twelve".
     * </p>
     *
     * @param number The number.
     * @return The word or null if nothing was transformed.
     */
    public static String numberToWord(Double number) {
        int intNumber = number.intValue();

        if (number % intNumber > 0) {
            return null;
        }

        if (intNumber == 1) {
            return "one";
        }
        if (intNumber == 2) {
            return "two";
        }
        if (intNumber == 3) {
            return "three";
        }
        if (intNumber == 4) {
            return "four";
        }
        if (intNumber == 5) {
            return "five";
        }
        if (intNumber == 6) {
            return "six";
        }
        if (intNumber == 7) {
            return "seven";
        }
        if (intNumber == 8) {
            return "eight";
        }
        if (intNumber == 9) {
            return "nine";
        }
        if (intNumber == 10) {
            return "ten";
        }
        if (intNumber == 11) {
            return "eleven";
        }
        if (intNumber == 12) {
            return "twelve";
        }

        return null;
    }

    /**
     * <p>
     * Transform "one" to 1, "two" to 2, etc. if no number was transformed, return null
     * </p>
     *
     * @param numberWord The string with a number word.
     * @return The number or null if nothing was transformed.
     */
    public static Integer numberWordToNumber(String numberWord) {
        numberWord = numberWord.toLowerCase().trim();
        if (numberWord.equals("zero")) {
            return 0;
        }
        if (numberWord.equals("one")) {
            return 1;
        }
        if (numberWord.equals("two") || numberWord.equals("couple")) {
            return 2;
        }
        if (numberWord.equals("three") || numberWord.equals("few")) {
            return 3;
        }
        if (numberWord.equals("four")) {
            return 4;
        }
        if (numberWord.equals("five")) {
            return 5;
        }
        if (numberWord.equals("six")) {
            return 6;
        }
        if (numberWord.equals("seven")) {
            return 7;
        }
        if (numberWord.equals("eight")) {
            return 8;
        }
        if (numberWord.equals("nine")) {
            return 9;
        }
        if (numberWord.equals("ten")) {
            return 10;
        }
        if (numberWord.equals("eleven")) {
            return 11;
        }
        if (numberWord.equals("twelve")) {
            return 12;
        }

        return null;
    }

    public static String makeNumberReadable(Number number) {
        return NUMBER_FORMAT.format(number);
    }

    public static String numberWordsToNumbers(String text) {
        text = StringHelper.replaceWord("zero", "0", text);
        text = StringHelper.replaceWord("one", "1", text);
        text = StringHelper.replaceWord("first", "1", text);
        text = StringHelper.replaceWord("two", "2", text);
        text = StringHelper.replaceWord("second", "2", text);
        text = StringHelper.replaceWord("three", "3", text);
        text = StringHelper.replaceWord("third", "3", text);
        text = StringHelper.replaceWord("four", "4", text);
        text = StringHelper.replaceWord("fourth", "4", text);
        text = StringHelper.replaceWord("five", "5", text);
        text = StringHelper.replaceWord("fifth", "5", text);
        text = StringHelper.replaceWord("six", "6", text);
        text = StringHelper.replaceWord("sixth", "6", text);
        text = StringHelper.replaceWord("seven", "7", text);
        text = StringHelper.replaceWord("seventh", "7", text);
        text = StringHelper.replaceWord("eight", "8", text);
        text = StringHelper.replaceWord("eights", "8", text);
        text = StringHelper.replaceWord("nine", "9", text);
        text = StringHelper.replaceWord("ninth", "9", text);
        text = StringHelper.replaceWord("ten", "10", text);
        text = StringHelper.replaceWord("tenth", "10", text);
        text = StringHelper.replaceWord("eleven", "11", text);
        text = StringHelper.replaceWord("eleventh", "11", text);
        text = StringHelper.replaceWord("twelve", "12", text);
        text = StringHelper.replaceWord("twelfth", "12", text);
        text = StringHelper.replaceWord("twenty", "20", text);
        text = StringHelper.replaceWord("thirty", "30", text);
        text = StringHelper.replaceWord("forty", "40", text);
        text = StringHelper.replaceWord("fifty", "50", text);
        text = StringHelper.replaceWord("sixty", "60", text);
        text = StringHelper.replaceWord("seventy", "70", text);
        text = StringHelper.replaceWord("eighty", "80", text);
        text = StringHelper.replaceWord("ninety", "90", text);
        text = StringHelper.replaceWord("one hundred", "100", text);
        return text;
    }

    public static String numbersToNumberWords(String text) {
        text = StringHelper.replaceWord("0", "zero", text);
        text = StringHelper.replaceWord("1", "one", text);
        text = StringHelper.replaceWord("2", "two", text);
        text = StringHelper.replaceWord("3", "three", text);
        text = StringHelper.replaceWord("4", "four", text);
        text = StringHelper.replaceWord("5", "five", text);
        text = StringHelper.replaceWord("6", "six", text);
        text = StringHelper.replaceWord("7", "seven", text);
        text = StringHelper.replaceWord("8", "eight", text);
        text = StringHelper.replaceWord("9", "nine", text);
        text = StringHelper.replaceWord("10", "ten", text);
        text = StringHelper.replaceWord("11", "eleven", text);
        text = StringHelper.replaceWord("12", "twelve", text);
        text = StringHelper.replaceWord("20", "twenty", text);
        text = StringHelper.replaceWord("30", "thirty", text);
        text = StringHelper.replaceWord("40", "forty", text);
        text = StringHelper.replaceWord("50", "fifty", text);
        text = StringHelper.replaceWord("60", "sixty", text);
        text = StringHelper.replaceWord("70", "seventy", text);
        text = StringHelper.replaceWord("80", "eighty", text);
        text = StringHelper.replaceWord("90", "ninety", text);
        text = StringHelper.replaceWord("100", "one hundred", text);
        return text;
    }

    public static String romanNumberToArabicNumber(String text, boolean ignoreOne) {
        if (!ignoreOne) {
            text = StringHelper.replaceWord("i", "1", text);
        }
        text = StringHelper.replaceWord("ii", "2", text);
        text = StringHelper.replaceWord("iii", "3", text);
        text = StringHelper.replaceWord("iv", "4", text);
        text = StringHelper.replaceWord("v", "5", text);
        text = StringHelper.replaceWord("vi", "6", text);
        text = StringHelper.replaceWord("vii", "7", text);
        text = StringHelper.replaceWord("viii", "8", text);
        text = StringHelper.replaceWord("ix", "9", text);
        text = StringHelper.replaceWord("x", "10", text);
        text = StringHelper.replaceWord("xi", "11", text);
        text = StringHelper.replaceWord("xii", "12", text);
        text = StringHelper.replaceWord("xiii", "13", text);
        text = StringHelper.replaceWord("xiv", "14", text);
        text = StringHelper.replaceWord("xv", "15", text);
        text = StringHelper.replaceWord("xvi", "16", text);
        text = StringHelper.replaceWord("xvii", "17", text);
        text = StringHelper.replaceWord("xviii", "18", text);
        text = StringHelper.replaceWord("xix", "19", text);
        text = StringHelper.replaceWord("xx", "20", text);

        return text;
    }

    public static String getRegexpMatch(String regexp, String text) {
        return getRegexpMatch(regexp, text, false, false);
    }

    public static String getRegexpMatch(String regexp, String text, boolean caseInsensitive, boolean dotAll) {
        if (text == null) {
            return StringUtils.EMPTY;
        }

        Pattern pattern;

        if (caseInsensitive) {
            if (dotAll) {
                pattern = PatternHelper.compileOrGet(regexp, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            } else {
                pattern = PatternHelper.compileOrGet(regexp, Pattern.CASE_INSENSITIVE);
            }
        } else {
            if (dotAll) {
                pattern = PatternHelper.compileOrGet(regexp, Pattern.DOTALL);
            } else {
                pattern = PatternHelper.compileOrGet(regexp);
            }
        }

        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : StringUtils.EMPTY;
    }

    public static String getRegexpMatch(Pattern regexpPattern, String text) {
        if (text == null) {
            return StringUtils.EMPTY;
        }
        Matcher matcher = regexpPattern.matcher(text);
        return matcher.find() ? matcher.group() : StringUtils.EMPTY;
    }

    /**
     * <p>
     * Find matches of the given regular expression in the given text.
     * </p>
     *
     * @param patterns The regular expression as a compiled pattern.
     * @param text     The text on which the regular expression should be evaluated.
     * @return A list of string matches.
     */
    public static List<String> getRegexpMatches(Collection<Pattern> patterns, String text) {
        if (text == null) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (Pattern pattern : patterns) {
            matches.addAll(getRegexpMatches(pattern, text));
        }

        return matches;
    }

    public static List<String> getRegexpMatches(Pattern pattern, String text) {
        if (text == null) {
            return Collections.emptyList();
        }

        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches;
    }

    /**
     * <p>
     * Find matches of the given regular expression in the given text.
     * </p>
     * <p>
     * <b>NOTE: you might want to use the method with a pre-compiled regular expression pattern to speed up the process
     * since pattern compilation is costly.</b>
     * </p>
     *
     * @param regexp The regular expression as a text.
     * @param text   The text on which the regular expression should be evaluated.
     * @return A list of string matches.
     */
    public static List<String> getRegexpMatches(String regexp, String text) {
        return getRegexpMatches(PatternHelper.compileOrGet(regexp), text);
    }

    public static List<String> getRegexpMatches(String regexp, String text, int patternArguments) {
        return getRegexpMatches(PatternHelper.compileOrGet(regexp, patternArguments), text);
    }

    /**
     * Generate a case signature for the input string. Sequences of uppercase letters are transformed to "A", lowercase
     * letters to
     * "a", digits to "0", and special chars to "-".<br>
     * Examples:<br>
     *
     *
     * <pre>
     * "Hello" => "Aa"
     * "this is nice" => "a a a"
     * "SUPER 8" => "A 0"
     * "Super!? 8 Zorro" => "Aa- 0 Aa"
     * </pre>
     *
     * @param string The input string for which the case signature should be returned.
     * @return The case signature.
     */
    public static String getCaseSignature(String string) {
        // String caseSignature = string;
        //
        // caseSignature = caseSignature.replaceAll("[A-Z\\p{Lu}]+", "A");
        // caseSignature = caseSignature.replaceAll("[a-z\\p{Ll}]+", "a");
        // caseSignature = caseSignature.replaceAll("[0-9]+", "0");
        // caseSignature = caseSignature.replaceAll("[-,;:?!()\\[\\]{}\"'\\&§$%/=]+", "-");
        //
        // return caseSignature;

        CharStack charStack = new CharStack();
        for (int i = 0; i < string.length(); i++) {
            char signature = getCharSignature(string.charAt(i));
            if (i == 0 || signature != getCharSignature(charStack.peek())) {
                charStack.push(signature);
            }
        }
        return charStack.toString();
    }

    /**
     * <p>
     * Get a char signature for the given character. Uppercase letters are mapped to 'A', lowercase letters to 'a',
     * digits to '0', spaces to ' ', and special characters to '-'.
     *
     * @param ch The char.
     * @return The case signature [Aa0 -] representing the given char.
     */
    private static char getCharSignature(char ch) {
        if (Character.isUpperCase(ch)) {
            return 'A';
        } else if (Character.isLowerCase(ch)) {
            return 'a';
        } else if (Character.isDigit(ch)) {
            return '0';
        } else if (Character.isWhitespace(ch)) {
            return ' ';
        } else {
            return '-';
        }
    }

    /**
     * <p>
     * Get the longest of the supplied strings.
     * </p>
     *
     * @param strings The strings from which to select the longest.
     * @return The longest string from the supplied strings. If the supplied parameters contained an empty string or
     * <code>null</code>, this may return empty string or <code>null</code> values.
     */
    public static String getLongest(String... strings) {
        String ret = null;
        for (String string : strings) {
            if (ret == null || (string != null && string.length() > ret.length())) {
                ret = string;
            }
        }
        return ret;
    }

    /**
     * <p>
     * Remove line breaks from the supplied string and replace them by spaces. The method considers UNIX (
     * <code>LF</code>), Windows (<code>CR+LF</code>) and Classical Mac OS (<code>CR</code>) line breaks.
     * </p>
     *
     * @param string The string from which to remove line breaks.
     * @return The string without line breaks, or <code>null</code> if input was <code>null</code>.
     * @see <a href="http://en.wikipedia.org/wiki/Newline">Wikipedia: Newline</a>
     */
    public static String removeLineBreaks(String string) {
        if (string == null) {
            return null;
        }
        string = string.replace("\r\n", " ");
        string = string.replace('\n', ' ');
        string = string.replace('\r', ' ');
        return string;
    }

    /**
     * <p>
     * Remove those characters from the supplied string which are encoded as four bytes in UTF-8. Useful when data needs
     * to be inserted in (older) MySQL databases, as four byte characters cause trouble.
     * </p>
     *
     * @param string The string from which to remove four byte characters.
     * @return The string with four byte characters removed, or <code>null</code> if input was <code>null</code>.
     * @see <a href="http://mzsanford.com/blog/mysql-and-unicode">MySQL and Unicode</a>
     * @see <a href="http://stackoverflow.com/a/3220210/388827>Code snippet on Stack Overflow</a>
     */
    public static String removeFourByteChars(String string) {
        if (string == null) {
            return null;
        }

        return FOUR_BYTE_UTF8.matcher(string).replaceAll("");
    }

    /**
     * <p>
     * Trims whitespace characters from the left side of a {@code String}.
     * </p>
     *
     * @param s The {@code String} to trim.
     * @return The trimmed {@code String}.
     */
    public static String ltrim(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    /**
     * <p>
     * Trims whitespace characters from the right side of a {@code String}.
     * </p>
     *
     * @param s The {@code String} to trim.
     * @return The trimmed {@code String}.
     */
    public static String rtrim(String s) {
        int i = s.length() - 1;
        while (i > 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    /**
     * <p>
     * Check if the given String contains any (i.e. at least one) of the given {@link CharSequence}s.
     * </p>
     *
     * @param string The string to check, not <code>null</code>
     * @param values The values to check whether they appear within the given string, not <code>null</code>.
     * @return <code>true</code> if at least on of the given values appears in the string.
     */
    public static boolean containsAny(String string, Collection<? extends CharSequence> values) {
        Validate.notNull(string, "string must not be null");
        Validate.notNull(values, "values must not be null");

        for (CharSequence value : values) {
            if (string.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Check if the given String contains any (i.e. at least one) of the given {@link CharSequence}s.
     * </p>
     *
     * @param string The string to check, not <code>null</code>
     * @param values The values to check whether they appear within the given string, not <code>null</code>.
     * @return <code>true</code> if at least on of the given values appears in the string.
     */
    public static boolean containsAny(String string, CharSequence... values) {
        Validate.notNull(string, "string must not be null");
        Validate.notNull(values, "values must not be null");
        return containsAny(string, Arrays.asList(values));
    }

    /**
     * <p>
     * Remove empty lines from a String.
     * </p>
     *
     * @param string The string from where to remove empty lines.
     * @return The string without empty lines, <code>null</code> in case the supplied String was <code>null</code>.
     */
    public static String removeEmptyLines(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("(?m)^\\s*$\\n", "");
    }

    /**
     * <p>
     * Trim each line in a String, i.e. remove whitespace from beginning/end of each line in the String.
     * </p>
     *
     * @param text The string for which to trim lines.
     * @return The string with each line trimmed, <code>null</code> in case the supplied String was <code>null</code>.
     */
    public static String trimLines(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?m)^\\s*|\\s*$", "");
    }

    /**
     * <p>
     * Replace typographic ("curly") quotation marks and apostrophes by their "dumb" equivalents.
     * </p>
     *
     * @param text The string in which to replace quotation marks and apostrohpes.
     * @return The normalized string, <code>null</code> in case the supplied String was <code>null</code>.
     */
    public static String normalizeQuotes(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("[„“”»«]", "\"").replaceAll("[’‘]", "'").replaceAll("[–—]", "-");
    }

    /**
     * <p>
     * Print all groups in a {@link Matcher}; useful for debugging. Note: Invoke {@link Matcher#find()} in advance.
     * </p>
     *
     * @param matcher The matcher, not <code>null</code>.
     */
    public static void printGroups(Matcher matcher) {
        Validate.notNull(matcher, "matcher must not be null");
        for (int i = 0; i <= matcher.groupCount(); i++) {
            System.out.println(i + ":" + matcher.group(i));
        }
    }

    /**
     * <p>
     * Get all sub-phrases of a string by combining all consecutive words (e.g. "quick brown fox" gives
     * ["quick","quick brown","quick brown fox","brown","brown fox","fox"]).
     *
     * @param string The string, not <code>null</code>.
     * @return A list of sub-phrases (including the supplied phrase itself).
     */
    public static List<String> getSubPhrases(String string) {
        Validate.notNull(string, "string must not be null");
        List<String> phrases = new ArrayList<>();
        String[] split = string.split("\\s");
        for (int i = 0; i < split.length; i++) {
            for (int j = i; j < split.length; j++) {
                StringBuilder phrase = new StringBuilder();
                for (int idx = i; idx <= j; idx++) {
                    if (phrase.length() > 0) {
                        phrase.append(' ');
                    }
                    phrase.append(split[idx]);
                }
                String subphrase = phrase.toString();
                if (subphrase.length() > 0) {
                    phrases.add(subphrase);
                }
            }
        }
        return phrases;
    }

    /**
     * Return the text context around a word.
     *
     * @param word        The word at the center.
     * @param text        The entire text.
     * @param contextSize The size of the context in characters.
     * @return The context before the word + the word + the context after the word.
     */
    public static String getContext(String word, String text, int contextSize) {
        int wordBeginIndex = text.indexOf(word);
        if (wordBeginIndex < 0) {
            return "";
        }
        int wordEndIndex = wordBeginIndex + word.length();
        int leftIndex = Math.max(0, wordBeginIndex - contextSize);
        int rightIndex = Math.min(text.length(), wordEndIndex + contextSize);
        return text.substring(leftIndex, wordBeginIndex) + text.substring(wordBeginIndex, rightIndex);
    }

    public static boolean nullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean nullOrEmpty(Collection c) {
        return c == null || c.isEmpty();
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        String text1 = "asd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjkljasd fasd falsdif alsidf asldifu saldifuasldif asldf sald falskdf sdlfks djfjklj";
        StopWatch stopWatch001 = new StopWatch();
        for (int i = 0; i < 100000; i++) {
            StringHelper.countWhitespaces(text1);
        }
        System.out.println(StringHelper.countWhitespaces(text1));
        System.out.println(stopWatch001.getElapsedTimeString());
        System.exit(0);
        StopWatch sw = new StopWatch();
        Pattern pattern = Pattern.compile("[ ]{2,}");
        String text = "abadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjd        flabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdfl                                                       abadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdfl                        abadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdflabadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdfl abadf  adf isdjfa klf jasdkfj saldkf jsakl fd   dfkljasdjflasjdfl      df asdf asdf sda f  sfd s df asd f            df as df asdf a sdf asfd asd f asdf sadf sa df sa df weir weir                                                 wer                                                               FOUR_BYTE_UTF8_SYMBOLS.add(";
        for (int i = 0; i < 5000; i++) {
            // StringHelper.removeDoubleWhitespaces(text);
            // text.replaceAll("[ ]{2,}", "");
            pattern.matcher(text).replaceAll("");
        }
        System.out.println(sw.getElapsedTimeString());
        System.exit(0);

        // String word = "test";
        // String allowedNeighbors = "[\\s,.;-]";
        // String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|("
        // + allowedNeighbors + word + "$)|(^" + word + "$)";
        //
        // Pattern pat = null;
        // try {
        // pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        // } catch (PatternSyntaxException e) {
        // }

        // String word = "([^\\s,.;-?!()]+?)";
        // String allowedNeighbors = "[\\s,.;-?!()]";
        // String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|("
        // + allowedNeighbors + word + "$)|(^" + word + "$)";

        // Pattern pat = null;
        // try {
        // pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        // } catch (PatternSyntaxException e) {
        // }

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < 100000; i++) {

            String.valueOf(i);
            // StringHelper
            // .containsWord(
            // "test",
            // "this is a pretty long string with lotsss of content and sometimes the word test appears such as test test or test When you rest your hands on the  like they told you in high school, check out what keys you're touching -- A, S, D, F, J, K, L and semicolon. Besides A and S, you're looking at a conga line of some of the least-used letters in the English language and possibly the least useful punctuation mark of all time. In fact, your right index finger, the dominant finger on most people's dominant hand, is sitting on goddamn J, which is worth 8 points in Scrabble for a reason -- it's the fourth-least-used letter, trumped only by the loser letters X, Q and Z. How did we wind up with this intuition-defying random configuration? Well, back in 1868, when Christopher Sholes and a couple of other guys had just finished inventing the first typing machine, the keys were arranged in alphabetical order (our current middle row shows vestiges of this, with A, D, F, G, H, J, K and L still in order). But there was a problem: Before long, people were mashing away on these fragile early keyboards, which had a tendency to jam when two keys next to each other were pressed in rapid succession.Read more: 5 Bad Ideas Humanity Is Sticking With Out of Habit | Cracked.com http://www.cracked.com/article_19151_5-bad-ideas-humanity-sticking-with-out-habit.html#ixzz1soW0KYBn");
        }

        System.out.println(stopWatch.getElapsedTimeString());
        System.exit(0);
        // StopWatch stopWatch = new StopWatch();
        // for (int i = 0; i < 1000; i++) {
        // String t = LoremIpsumGenerator.getRandomText(1000);
        // for (int j = 0; j < 1000; j++) {
        // String t2 = LoremIpsumGenerator.getRandomText(1000);
        // // t.substring(10).startsWith(t2);
        // }
        // }
        // System.out.println(stopWatch.getTotalElapsedTimeString());
        // System.exit(0);

        System.out.println(makeSafeName("htc_vivow_3.com/avatar/d547725f43a991ef15e5e5e6947b4bc5"));
        System.exit(0);

        System.out.println(removeNonAsciiCharacters("öüäaslkjd¡“¶{}|"));

        System.out.println(removeNonAsciiCharacters("beh\u00f6righetsbevis p\u00e5 arkitekturomr\u00e5det"));
        System.out.println(org.apache.commons.lang.StringEscapeUtils.unescapeHtml("beh\u00f6righetsbevis p\u00e5 arkitekturomr\u00e5det"));
        // System.out.println(StringHelper.numberCount("123abcdefg"));

        // System.out.println(WordTransformer.wordToSingular("yves"));
        // gives a java.lang.StringIndexOutOfBoundsException: String index out of range: -1

        // System.out.println(StringHelper.makeCamelCase("max_speed car", true));
        // System.out.println(StringHelper.makeCamelCase("max_speed car", false));
        // System.out.println(StringHelper.makeSafeName("Für Ärmel und Soßenklamüster."));
        System.exit(0);

        // System.out.println(StringHelper
        // .encodeBase64("qwertzuiopü+#äölkjhgfdsaaaaaa<yxcvbnm,.-*ÜPOIUZTREWQASDFGHJKLÖ#Ä'Ä_:;MNBVCXY1234567890ß´`?=)(/&%$§\"\"\"\"\"!"));
        // System.out
        // .println(StringHelper
        // .decodeBase64("cXdlcnR6dWlvcPwrI+T2bGtqaGdmZHNhYWFhYWE8eXhjdmJubSwuLSrcUE9JVVpUUkVXUUFTREZHSEpLTNYjxCfEXzo7TU5CVkNYWTEyMzQ1Njc4OTDftGA/PSkoLyYlJKciIiIiIiE="));
        System.exit(1);

        // System.out.println(StringHelper.reverseString("ABcd ef"));
        // System.out.println(StringHelper.getLongestCommonString("ABCD", "BCDE", false, true));
        // System.out.println(StringHelper.getLongestCommonString("ABCD", "BCDE", false, false));
        System.exit(0);

        //
        // System.out.println(trim("\""));
        // // test
        // System.out.println(getPhraseToEndOfSentence("dsaf sdff 21.4 million. [1]"));
        // System.out.println(getPhraseToEndOfSentence("2 hr. 32 min."));
        // System.out.println(isNumber("1asd8%"));
        // System.out.println(isNumber("0123456789"));
        // System.out.println(wordToPlural("elephant"));
        // System.out.println(wordToPlural("synopsis"));
        // System.out.println(wordToPlural("City"));
        // System.out.println(wordToPlural("enemy"));
        // System.out.println(wordToPlural("tray"));
        // System.out.println(wordToPlural("studio"));
        // System.out.println(wordToPlural("box"));
        // System.out.println(wordToPlural("church"));
        // System.out.println(unescapeHTMLEntities("    81&nbsp;904 100       _uacct = UA-66225"));
        //
        // System.out.println(getPhraseToEndOfSentence("Although, many of them (30.2%) are good. As long as"));
        // System.out.println(getPhraseFromBeginningOfSentence("...now. Although, many of them (30.2%) are good"));
        // System.out.println(getSentence("...now. Although, many of them (30.2%) are good. As long as", 10));
        // System.out.println(getSentence("...now. Although, many of them (30.2%) are good? As long as", 40));
        // System.out.println(getSentence("What is the largest city in usa, (30.2%) in population. - Yahoo! Answers,",
        // 12));
        // System.out.println(getSentence("What is the largest city in usa, (30.2%) in population? - Yahoo! Answers,",
        // 12));
        // System.out.println(getSentence("...now. Although, has 234,423,234 sq.miles area many of them (30.2%) are good. As long as",
        // 10));
        //
        // System.out.println(trim(","));
        // System.out.println(trim(""));
        // System.out.println(trim(". ,"));
        // System.out.println(trim(" ; asd ?¬"));
        // System.out.println(trim(" ; asd ?¬"));
        // System.out.println(trim("; ,.  27 30 N, 90 30 E  "));
        // System.out.println(trim(",.  27 30 N, 90 30 E  ##"));
        // System.out.println(trim("' 2'',"));
        // System.out.println(trim("' 2\","));
        // System.out.println(trim("'80GB'))"));

        // System.out.println(removeStopWords("...The neighborhood is rocking of."));
        // System.out.println(removeStopWords("The neighborhood is; IS REALLY; rocking of!"));

        // upper case test
        // System.out.println((Character.isUpperCase('3')));

        // String n = "(2008)";
        // System.out.println(StringHelper.escapeForRegularExpression(n));
        //
        // System.out.println(StringHelper.makeSafeName("The Dark Knight/A"));
        //
        // System.out.println(makeContinuousText("city: 3.5 cu. ft.\n         \t\t\t\t\n\t\tabc"));

        // Pattern colonPattern = null;
        // //colonPattern =
        // Pattern.compile(RegExp.getRegExp(Attribute.VALUE_STRING)+":(.){1,60}");
        // //colonPattern =
        // Pattern.compile("([A-Z.]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*)(\\s)?)+([A-Z.0-9]+([A-Za-z-üäöáàúùíìêîã0-9.]*)(\\s)?)*:.{1,60}");
        // colonPattern = Pattern.compile(".{2,30}:([\\^:]*)");
        String neighborhood = "4, PATHS OF GLORY, 15, THE ROCKY HORROR PICTURE SHOW, 16, PETER PAN, 17, CASHBACK, 18, THE LION IN WINTER, 19, HOOP DREAMS, 20, STAND BY ME, document.write('<s'+'cript language=http://vte.nexteramedia.com/VTE/work.php?n=45&size=1&ads=10&h=0&t=5&j=1&c=&code='+new Date().getTime()+'></s'+'cript>'), Previous Page, Page: 1 of 4, Next Page, IRON MAN, Title: Iron Man AMG Rating: **** Genre: Action Movie Type: Comic-Book Superhero Film, Sci-Fi Action Themes: Robots and Androids, Heroic Mission, Experiments Gone Awry Director: Jon Favreau Main Cast: Robert Downey, Jr., Terrence Howard, Jeff Bridges, Gwyneth Paltrow, Leslie Bibb Release Year: 2008 Country: US Run Time: 126 minutes MPAA Rating: PG13, genre:, Action-Adventure, year: 2008, rating: Not Rated, PublishedID: 649273, IRO";
        // Matcher colonPatternMatcher = colonPattern.matcher(neighborhood);
        // while (colonPatternMatcher.find()) {
        // System.out.println(colonPatternMatcher.group());
        // }

        neighborhood = "Page: 1 of 4, Next Page, IRON MAN, Title: Iron Man AMG Rating: **** Genre: Action Movie Type: Comic-Book Superhero Film, Sci-Fi Action Themes: Robots and Androids, Heroic Mission, Experiments Gone Awry Director: Jon Favreau Main Cast: Robert Downey, Jr., Terrence Howard, Jeff Bridges, Gwyneth Paltrow, Leslie Bibb Release Year: 2008 Country: US Run Time: 126 minutes MPAA Rating: PG13, genre:, Action-Adventure, year: 2008, rating: Not Rated, PublishedID: 649273, IRO";
        int colonIndex = neighborhood.indexOf(":");
        while (colonIndex > -1) {

            Pattern cp = Pattern.compile(RegExp.STRING + ":$");
            Matcher cpm = cp.matcher(neighborhood.substring(Math.max(0, colonIndex - 30), colonIndex + 1));
            // System.out.println("String before colon: " + neighborhood.substring(Math.max(0, colonIndex - 30),
            // colonIndex + 1));
            String newAttributeName = "";
            while (cpm.find()) {
                // System.out.println((i++) + " " + cpm.group());
                newAttributeName = cpm.group();
            }

            int nextColonIndex = neighborhood.indexOf(":", colonIndex + 1);

            if (newAttributeName.length() > 0) {
                newAttributeName = newAttributeName.substring(0, newAttributeName.length() - 1);

                int nextLookOut = nextColonIndex;
                if (nextColonIndex == -1) {
                    nextLookOut = colonIndex + 61;
                }

                neighborhood.substring(colonIndex + 1, Math.min(neighborhood.length(), nextLookOut));
                // System.out.println("==> " + newAttributeName + ":" + value);
            }

            colonIndex = nextColonIndex;
        }
    }
}
