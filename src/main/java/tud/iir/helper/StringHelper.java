package tud.iir.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import tud.iir.preprocessing.normalization.StringNormalizer;
import tud.iir.preprocessing.normalization.UnitNormalizer;

/**
 * The StringHelper adds string functionality.
 * 
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 * @author Martin Gregor
 */
public class StringHelper {

    /** The Constant BRACKETS. A list of bracket types. */
    private static final char[] BRACKETS = { '(', ')', '{', '}', '[', ']' };

    /**
     * In ontologies names can not have certain characters so they have to be changed.
     * 
     * @param name The name.
     * @param maxLength The maximum length of the string. -1 means no maximum length.
     * @return The safe name.
     */
    public static String makeSafeName(String name, int maxLength) {
        String safeName = name.replaceAll(" ", "_").replaceAll("/", "_").replaceAll("'", "").replaceAll("\"", "")
        .replaceAll(",", "_").replaceAll("\\*", "_").replaceAll("\\.", "_").replaceAll(";", "_").replaceAll(
                "\\:", "_").replaceAll("\\!", "").replaceAll("\\?", "").replaceAll("\\ä", "ae").replaceAll(
                        "\\Ä", "Ae").replaceAll("\\ö", "oe").replaceAll("\\Ö", "Oe").replaceAll("\\ü", "ue")
                        .replaceAll("\\Ü", "Ue").replaceAll("\\ß", "ss");

        if (maxLength > 0) {
            safeName = safeName.substring(0, Math.min(safeName.length(), maxLength));
        }

        return safeName;
    }

    public static String makeSafeName(String name) {
        return makeSafeName(name, -1);
    }

    /**
     * This function wraps the string to integer conversion in order to prevent the exception catching in other
     * functions.
     * 
     * @param text The text that is a number.
     * @return The integer presentation of the text.
     */
    public static Integer toInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (Exception e) {
            Logger.getRootLogger().error("could not parse string to integer, " + e.getMessage());
        }

        return -1;
    }

    /**
     * Search for the indices of a search string in a text.<br>
     * For example given the text "This is a text" and the searchString " ", we would get [4,7,9], the indices of the
     * white spaces.
     * 
     * @param text The text to search in.
     * @param searchString The search string to find the indices of.
     * @return A list of indices in the text.
     */
    public static List<Integer> getOccurrenceIndices(String text, String searchString) {
        List<Integer> indexList = new ArrayList<Integer>();

        String subText = text;
        int position = 0;
        while (subText.indexOf(searchString) > -1) {
            int index = subText.indexOf(searchString);
            indexList.add(index + position);
            subText = subText.substring(index + 1);
            position += index + 1;
        }

        return indexList;
    }

    /**
     * This function wraps the string to double conversion in order to prevent the exception catching in other
     * functions.
     * 
     * @param text The text that is a number.
     * @return The double presentation of the text.
     */
    public static Double toDouble(String text) {
        try {
            return Double.valueOf(text.trim());
        } catch (Exception e) {
            Logger.getRootLogger().error("could not parse string to double, " + e.getMessage());
        }

        return -1.0;
    }

    /**
     * Transform a name to a camel case variable name. For example: car_speed => carSpeed or CarSpeed
     * 
     * @param name The name.
     * @param uppercaseFirst If true, the first letter will be uppercase.
     * @param toSingular If true, the last part is translated to its singular form.
     * @return The camel cased name.
     */
    public static String makeCamelCase(String name, boolean uppercaseFirst, boolean toSingular) {
        String camelCasedName = "";
        String modName = name.replaceAll("\\s", "_");

        String[] parts = modName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1 && toSingular) {
                part = WordTransformer.wordToSingular(part);
            }
            camelCasedName += upperCaseFirstLetter(part);
        }

        if (!uppercaseFirst) {
            camelCasedName = lowerCaseFirstLetter(camelCasedName);
        }

        return camelCasedName;
    }

    /**
     * Make camel case.
     * 
     * @param name the name
     * @param uppercaseFirst the uppercase first
     * @return the string
     */
    public static String makeCamelCase(String name, boolean uppercaseFirst) {
        return makeCamelCase(name, uppercaseFirst, false);
    }

    /**
     * Make first letter of word upper case.
     * 
     * @param term The term.
     * @return The term with an upper case first letter.
     */
    public static String upperCaseFirstLetter(String term) {
        if (term.length() == 0) {
            return term;
        }

        return term.substring(0, 1).toUpperCase() + term.substring(1);
    }

    /**
     * Make first letter of word lower case.
     * 
     * @param term The term.
     * @return The term with an lower case first letter.
     */
    public static String lowerCaseFirstLetter(String term) {
        if (term.length() == 0) {
            return term;
        }
        return term.substring(0, 1).toLowerCase() + term.substring(1);
    }

    /**
     * Replace number before a text. 1.1 Text => Text
     * 
     * @param numberedText The text that possibly has numbers before it starts.
     * @return The text without the numbers.
     */
    public static String removeNumbering(String numberedText) {
        String modText = numberedText.replaceAll("^\\s*\\d+(\\.?\\d?)*\\s*", "");
        return modText;
    }

    /**
     * Make name for view.
     * 
     * @param name The name.
     * @return The view name.
     */
    public static String makeViewName(String name) {
        return name.replaceAll("_", " ");
    }

    /**
     * Check whether a given string contains a proper noun.
     * 
     * @param searchString The search string.
     * @return True if the string contains a proper noun, else false.
     */
    public static boolean containsProperNoun(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.getRegExp(RegExp.VALUE_STRING));
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp "
                    + RegExp.getRegExp(RegExp.VALUE_STRING), e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }

        return false;
    }

    /**
     * Check whether a given string contains a numeric value.
     * 
     * @param searchString The search string.
     * @return True if the string contains a numeric value, else false.
     */
    public static boolean containsNumber(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.getRegExp(RegExp.VALUE_NUMERIC));
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp "
                    + RegExp.getRegExp(RegExp.VALUE_NUMERIC), e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }

        return false;
    }

    /**
     * Clean the given string from stop words, i.e. words that appear often but have no meaning itself.
     * 
     * @param string The string.
     * @return The string without the stop words.
     */
    public static String removeStopWords(String string) {
        String[] stopWords = { "the", "and", "of", "by", "as", "but", "not", "is", "it", "to", "in", "or", "for", "on",
                "at", "up", "what", "how", "why", "when", "where" };
        int stopWordsSize = stopWords.length;

        String modString = " " + string + " ";
        for (int i = 0; i < stopWordsSize; ++i) {
            // remove stop words followed by a space
            modString = modString.replaceAll("(?<![\\w])(?i)" + stopWords[i] + "\\s", "");
            // remove stop words followed by punctuation
            modString = modString.replaceAll("\\s" + stopWords[i] + "(?=(\\!|\\?|\\.|,|;))", "");
        }

        return modString.trim();
    }

    /**
     * Replace "non-breaking" aka. protected whitespace (unicode 0x00A0) with normal whitespace.
     * 
     * @param string the string
     * @return the string
     */
    public static String removeProtectedSpace(String string) {
        // String modString = string.replaceAll(" ", " ");
        // let's use this notation, which does the same, to make clear what's going on ...:
        String modString = string.replaceAll("\u00A0", " ");
        return modString;
    }

    /**
     * Strips all non-ASCII characters from the supplied string. Useful to remove asian characters, for example.
     * 
     * @param string
     * @return
     */
    public static String removeNonAsciiCharacters(String string) {
        // http://forums.sun.com/thread.jspa?threadID=5370865
        return string.replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Removes the brackets.
     * 
     * @param bracketString the bracket string
     * @return the string
     */
    public static String removeBrackets(String bracketString) {
        String string = bracketString;
        try {
            string = string.replaceAll("\\(.*?\\)", "");
            string = string.replaceAll("\\[.*?\\]", "");
            string = string.replaceAll("\\{.*?\\}", "");
        } catch (Exception e) {
            Logger.getRootLogger().error(string + ", " + e.getMessage());
        }
        return string;
    }

    /**
     * Escape for regular expression.
     * 
     * @param inputString the input string
     * @return the string
     */
    public static String escapeForRegularExpression(String inputString) {
        String string = inputString;
        try {
            // modified by Philipp Katz, 2010-05-14
            // added further meta characters like . ? -
            // using normal replace methods instead of replaceAll
            string = string.replace("\\", "\\\\");
            string = string.replace("(", "\\(");
            string = string.replace(")", "\\)");
            string = string.replace("[", "\\[");
            string = string.replace("]", "\\]");
            string = string.replace("{", "\\{");
            string = string.replace("}", "\\}");
            string = string.replace("|", "\\|");
            string = string.replace("+", "\\+");
            string = string.replace("*", "\\*");
            string = string.replace("$", "\\$");
            string = string.replace("^", "\\^");
            string = string.replace(".", "\\.");
            string = string.replace("?", "\\?");
            string = string.replace("-", "\\-");
            string = string.replaceAll("\\n", "\\\\n");
        } catch (Exception e) {
            Logger.getRootLogger().error(string + ", " + e.getMessage());
        }
        return string;
    }

    /**
     * Checks whether character is a bracket.
     * 
     * @param character The character.
     * @return True if character is a bracket, else false.
     */
    public static boolean isBracket(char character) {
        for (char element : BRACKETS) {
            if (element == character) {
                return true;
            }

        }
        return false;
    }

    /**
     * Check if the string is a number.
     * 
     * @param ch the ch
     * @return True if string is number, else false.
     */
    public static boolean isNumber(Character ch) {
        return isNumber(ch.toString());
    }

    /**
     * Checks if is number.
     * 
     * @param string the string
     * @return true, if is number
     */
    public static boolean isNumber(String string) {
        if (string.length() == 0) {
            return false;
        }

        boolean isNumber = true;
        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER && ch != '.') {
                isNumber = false;
            }
            // System.out.println(Character.getType(ch)+" "+Character.DECIMAL_DIGIT_NUMBER);
        }

        if (string.startsWith(".") || string.endsWith(".")) {
            return false;
        }

        return isNumber;
    }

    /**
     * Checks if is numeric expression.
     * 
     * @param string the string
     * @return true, if is numeric expression
     * @throws NumberFormatException the number format exception
     * @throws OutOfMemoryError the out of memory error
     */
    public static boolean isNumericExpression(String string) throws NumberFormatException, OutOfMemoryError {
        if (string.length() == 0) {
            return false;
        }

        boolean isNumericExpression = true;

        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER
                    && Character.getType(ch) != Character.DASH_PUNCTUATION
                    && Character.getType(ch) != Character.CONNECTOR_PUNCTUATION
                    && Character.getType(ch) != Character.CURRENCY_SYMBOL
                    && Character.getType(ch) != Character.DIRECTIONALITY_WHITESPACE && ch != '%' && ch != '.'
                        && ch != ',' && ch != ':') {
                isNumericExpression = false;
                break;
            }
            // System.out.println(Character.getType(ch)+" "+Character.DECIMAL_DIGIT_NUMBER);
        }

        Pattern pattern = Pattern.compile("^" + RegExp.getRegExp(RegExp.VALUE_NUMERIC));
        Matcher m = pattern.matcher(string);
        try {

            if (m.find()) {
                double number = Double.valueOf(StringNormalizer.normalizeNumber(m.group()));
                double convertedNumber = UnitNormalizer.getNormalizedNumber(number, string.substring(m.end(), string
                        .length()));
                if (number != convertedNumber) {
                    return true;
                }

            }
        } catch (NumberFormatException e) {
            Logger.getRootLogger().error(m.group() + ", " + e.getMessage());
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
        if (string.matches("(\\d){1,2}:(\\d){1,2}(\\s)?(am|pm)")) {
            return true;
        }

        return false;
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

        boolean isCompletelyUppercase = true;
        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.UPPERCASE_LETTER
                    && Character.getType(ch) != Character.INITIAL_QUOTE_PUNCTUATION
                    && Character.getType(ch) != Character.FINAL_QUOTE_PUNCTUATION && ch != ' ') {
                isCompletelyUppercase = false;
            }
            // System.out.println(Character.getType(ch)+" "+Character.DECIMAL_DIGIT_NUMBER);
        }
        return isCompletelyUppercase;
    }

    /**
     * Starts uppercase.
     * 
     * @param testString the test string
     * @return true, if successful
     */
    public static boolean startsUppercase(String testString) {
        String string = StringHelper.trim(testString);
        if (string.length() == 0) {
            return false;
        }

        if (Character.isUpperCase(string.charAt(0))) {
            return true;
        }

        return false;
    }

    /**
     * Letter number count.
     * 
     * @param string the string
     * @return the int
     */
    public static int letterNumberCount(String string) {
        return string.replaceAll("[^a-zA-Z0-9]", "").length();
    }

    public static int numberCount(String string) {
        return string.replaceAll("[^0-9]", "").length();
    }

    /**
     * Capitalized word count.
     * 
     * @param string the string
     * @return the int
     */
    public static int capitalizedWordCount(String string) {
        StringTokenizer st = new StringTokenizer(string);
        int capitalizedWordCount = 0;
        while (st.hasMoreTokens()) {
            String token = (String) st.nextElement();
            if (StringHelper.isCompletelyUppercase(token)) {
                capitalizedWordCount++;
            }
        }
        return capitalizedWordCount;
    }

    /**
     * Checks if is vowel.
     * 
     * @param inputCharacter the input character
     * @return true, if is vowel
     */
    public static boolean isVowel(Character inputCharacter) {

        Character character = Character.toUpperCase(inputCharacter);
        if (character == 'A' || character == 'E' || character == 'I' || character == 'O' || character == 'U') {
            return true;
        }
        return false;
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
     * @param inputString the input string
     * @param keepCharacters the keep characters
     * @return the string
     */
    public static String trim(String inputString, String keepCharacters) {

        String string = inputString.trim();
        if (string.length() == 0) {
            return string;
        }

        string = StringEscapeUtils.unescapeHtml(string);

        String[] unwanted = { ",", ".", ":", ";", "!", "|", "?", "¬", " ", " ", "#", "-", "\'", "\"", "*", "/", "\\",
                "@", "<", ">", "=", "·", "^", "_", "+", "»", "ￂ" }; // whitespace
        // is also
        // unwanted
        // but trim()
        // handles
        // that, " "
        // here is
        // another
        // character
        // (ASCII code
        // 160)

        // delete quotes only if it is unlikely to be a unit (foot and inches)
        // Pattern p = Pattern.compile("((\\d)+'')|('(\\s)?(\\d)+\")");
        Pattern p = Pattern.compile("(\\A\"([^\"]*)[^\"]$)|((\\d)+'')|('(\\s)?(\\d)+\")");
        if (p.matcher(string).find()) {
            unwanted[12] = " ";
            unwanted[13] = " ";
        }

        boolean deleteFirst = true;
        boolean deleteLast = true;
        while ((deleteFirst || deleteLast) && string.length() > 0) {
            deleteFirst = false;
            deleteLast = false;
            Character first = string.charAt(0);
            Character last = string.charAt(string.length() - 1);
            // System.out.println(Character.getType(last));
            for (int i = 0, l = unwanted.length; i < l; ++i) {
                if (keepCharacters.indexOf(unwanted[i]) > -1) {
                    continue;
                }

                // System.out.println(first.charValue());
                // System.out.println(Character.isSpaceChar(first));
                if (first == unwanted[i].charAt(0)
                        || Character.getType(first) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                        || Character.isSpaceChar(first)) {
                    deleteFirst = true;
                }
                if (last == unwanted[i].charAt(0)
                        || Character.getType(last) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                        || Character.isSpaceChar(last)) {
                    deleteLast = true;
                }
                if (deleteFirst && deleteLast) {
                    break;
                }

            }

            if (deleteFirst) {
                string = string.substring(1);
            }

            if (deleteLast && string.length() > 0) {
                string = string.substring(0, string.length() - 1);
            }

            string = string.trim();
        }

        // remove all control characters from string
        string = removeControlCharacters(string);

        // close spaces gap that might have arisen
        string = string.replaceAll("(\\s){1,}", " ");

        // string = string.replaceAll("'\\)\\)","").replaceAll("'\\)",""); //
        // values are in javascript text sometimes e.g. ...('80GB')

        return string;
    }

    /**
     * Removes unwanted control characters from the specified string.
     * 
     * @param string
     * @return
     */
    public static String removeControlCharacters(String string) {
        for (int i = 0, l = string.length(); i < l; ++i) {
            // < 33 means all control characters are not wanted as well
            if (string.charAt(i) < 33) {
                string = string.replace(string.charAt(i), ' ');
            }
        }
        return string;
    }

    /**
     * Trim.
     * 
     * @param strings the strings
     * @return the hash set
     */
    public static HashSet<String> trim(HashSet<String> strings) {
        HashSet<String> trimmedStrings = new HashSet<String>();
        for (String s : strings) {
            trimmedStrings.add(trim(s));
        }
        return trimmedStrings;
    }

    /**
     * Remove tabs, line breaks and double spaces.
     * 
     * @param text The text to be cleaned.
     * @return The cleaned text.
     */
    public static String makeContinuousText(String text) {

        // close multiple spaces
        String continuoustext = text.replaceAll("(\\s){1,}", " ");

        return continuoustext;
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
     * Count number of words, words are separated by a blank " ".
     * 
     * @param string The string.
     * @return The number of words in the string.
     */
    public static int countWords(String string) {
        String[] words = string.split(" ");
        return words.length;
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
     * @param string1 the string1
     * @param string2 the string2
     * @param caseSensitive the case sensitive
     * @return the double
     */
    public static double calculateSimilarity(String string1, String string2, boolean caseSensitive) {
        double longestCommonStringLength = getLongestCommonString(string1, string2, caseSensitive, true).length();
        if (longestCommonStringLength == 0) {
            return 0.0;
        }

        double similarity = longestCommonStringLength / Math.min(string1.length(), string2.length()); // TODO
        // changed
        // without
        // test
        // 26/06/2009
        // return similarity / Math.max(string1.length(), string2.length());
        // return similarity / string2.length();
        return similarity;
    }

    /**
     * Get the longest common character chain two strings have in common.
     * 
     * @param string1 The first string.
     * @param string2 The second string.
     * @param caseSensitive True if the check should be case sensitive, false otherwise.
     * @param shiftString If true, the shorter string will be shifted and checked against the longer string. The longest
     *            common string of two strings is found
     *            regardless whether they start with the same characters. If true, ABCD and BBCD have BCD in common, if
     *            false the longest common string is
     *            empty.
     * @return The longest common string.
     */
    public static String getLongestCommonString(String string1, String string2, boolean caseSensitive,
            boolean shiftString) {

        String string1Compare = string1;
        String string2Compare = string2;
        if (!caseSensitive) {
            string1Compare = string1.toLowerCase();
            string2Compare = string2.toLowerCase();
        }

        // string length, string
        TreeMap<Integer, String> commonStrings = new TreeMap<Integer, String>();

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
                int index = 0;
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
     * Gets the array as string.
     * 
     * @param array the array
     * @return the array as string
     * @deprecated There is {@link StringUtils#join(Object[])}, which is more flexible and also works for Collections.
     */
    @Deprecated
    public static String getArrayAsString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (String element : array) {
            sb.append(element).append(",");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    /**
     * Reverse a string. ABC => CBA.
     * 
     * @param string The string to be reversed.
     * @return The reversed string.
     */
    public static String reverseString(String string) {
        StringBuilder reversedString = new StringBuilder();
        for (int i = string.length() - 1; i >= 0; i--) {
            reversedString.append(string.charAt(i));
        }
        return reversedString.toString();
    }

    /**
     * Run a regular expression on a string and form a new string with the matched strings separated by the specified
     * separator.
     * 
     * @param inputString The input string for the matching.
     * @param separator The separator used to separate the matched strings.
     * @param regularExpression The regular expression that is matched on the input string.
     * @return the string
     */
    public static String concatMatchedString(String inputString, String separator, String regularExpression) {
        String modInputString = StringEscapeUtils.unescapeHtml(inputString);
        String string = "";
        Pattern pattern = Pattern.compile(regularExpression);
        Matcher matcher = pattern.matcher(modInputString);

        while (matcher.find()) {
            Logger.getRootLogger().debug(matcher.group());
            string += matcher.group() + separator;
        }

        return string.substring(0, Math.max(0, string.length() - separator.length())).trim();
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
            Logger.getRootLogger().debug("Algorithm used: " + md.getAlgorithm());

            // should be 20 bytes, 160 bits long
            Logger.getRootLogger().debug("Digest is " + digest.length + " bytes long.");

            // dump out the hash
            Logger.getRootLogger().debug("Digest: ");
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
     * Get the substring between the given sequences.
     * 
     * @param string The string where the substring belongs to.
     * @param leftBorder The left border.
     * @param rightBorder The right border.
     * @return The substring between the two given strings or an empty string in case of an error.
     */
    public static String getSubstringBetween(String string, String leftBorder, String rightBorder) {

        String substring = "";

        int index1 = string.indexOf(leftBorder);
        int index2 = string.indexOf(rightBorder, index1 + leftBorder.length());

        if (index2 > index1 && index1 > -1) {
            substring = string.substring(index1 + leftBorder.length(), index2);
        }

        return substring;
    }

    /**
     * Transforms a CamelCased String into a split String.
     * 
     * @param camelCasedString The String to split.
     * @param separator The separator to insert between the camelCased fragments.
     * @return The separated String.
     * @author Philipp Katz
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
     * @author Philipp Katz
     */
    public static String camelCaseToWords(String camelCasedString) {
        return camelCaseToWords(camelCasedString, " ");
    }

    /**
     * URLDecode a String.
     * 
     * @param string
     * @return
     */
    public static String urlDecode(String string) {
        try {
            string = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.getRootLogger().error("unsupportedEncodingException for " + string + ", " + e.getMessage());
        } catch (Exception e) {
            Logger.getRootLogger().error("exception at Crawler for " + string + ", " + e.getMessage());
        }
        return string;
    }

    /**
     * URLEncode a String.
     * 
     * @param string
     * @return
     */
    public static String urlEncode(String string) {
        String result;
        try {
            result = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.getRootLogger().error("urlEncodeUtf8 " + e.getMessage());
            result = string;
        }
        return result;
    }

    /**
     * Looks for a regular expression in string. Removes found substring from source-string. <br>
     * Only the first found match will be deleted. <br>
     * Return value consists of a two-field-array. First value is cleared string, second is removed substring.
     * 
     * @param string to be cleared.
     * @param regExp A regular expression.
     * @return Cleared string and removed string in an array.
     */
    public static String[] removeFirstStringpart(String string, String regExp) {
        String returnString = null;
        String removedString = null;
        Matcher matcher;
        Pattern pattern;

        pattern = Pattern.compile(regExp.toLowerCase());
        matcher = pattern.matcher(string.toLowerCase());
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            removedString = string.substring(start, end);
            returnString = string.replace(removedString, "");
            returnString = returnString.replaceAll("  ", " ");
        }
        String[] result = { returnString, removedString };
        return result;
    }

    /**
     * Removes trailing whitespace at the end.
     * 
     * @param dateString String to be cleared.
     * @return Cleared string.
     */
    public static String removeLastWhitespace(String dateString) {
        StringBuffer temp = new StringBuffer(dateString);

        while (temp.charAt(temp.length() - 1) == ' ') {
            temp.deleteCharAt(temp.length() - 1);
        }
        return temp.toString();
    }

    /**
     * Replaces two or more trailing white spaces by one.
     * 
     * @param text
     * @return
     */
    public static String removeDoubleWhitespaces(String text) {
        String temp = text;
        while (temp.indexOf("  ") != -1) {
            temp = temp.replaceAll("  ", " ");
        }
        return temp;
    }

    /**
     * Counts whitespace in a text.
     * 
     * @param text
     * @return
     */
    public static int countWhitespaces(String text) {
        int count = 0;
        String t = text;
        /*while (t.indexOf(" ") != -1) {
            t = t.replaceFirst(" ", "");
            count++;
        }*/
        String[] temp = t.split(" ");
        count= temp.length - 1;
        //System.out.println(count);
        return count;
    }

    /**
     * Shorten a String; returns the first num words.
     * 
     * @param string
     * @param num
     * @return
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
     * Count number of occurrences of pattern within text.
     * 
     * TODO this will fail if pattern contains RegEx meta characters. Need to escape.
     * 
     * @param text
     * @param pattern
     * @param ignoreCase
     * @return
     */
    public static int countOccurences(String text, String pattern, boolean ignoreCase) {
        if (ignoreCase) {
            text = text.toLowerCase();
            pattern = pattern.toLowerCase();
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        int occurs = 0;
        while (m.find()) {
            occurs++;
        }
        return occurs;
    }

    /**
     * Calculates Levenshtein similarity between the strings.
     * 
     * @param s1
     * @param s2
     * @return similarity between 0 and 1 (inclusive).
     */
    public static float getLevenshteinSim(String s1, String s2) {
        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        float similarity = 1 - (float) distance / Math.max(s1.length(), s2.length());
        return similarity;
    }

    /**
     * Determine similarity based on String lengths. We can use this as threshold before even calculating Levenshtein
     * similarity which is computationally expensive.
     * 
     * @param s1
     * @param s2
     * @return similarity between 0 and 1 (inclusive).
     */
    public static float getLengthSim(String s1, String s2) {
        int length1 = s1.length();
        int length2 = s2.length();
        if (length1 == 0 && length2 == 0) {
            return 1;
        }
        return (float) Math.min(length1, length2) / Math.max(length1, length2);
    }

    /**
     * This method ensures that the output String has only valid XML unicode characters as specified by the XML 1.0
     * standard. For reference, please see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty String if the input is null or empty.
     * 
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     * @see http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     */
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || "".equals(in)) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if (current == 0x9 || current == 0xA || current == 0xD || current >= 0x20 && current <= 0xD7FF
                    || current >= 0xE000 && current <= 0xFFFD || current >= 0x10000 && current <= 0x10FFFF) {
                out.append(current);
            }
        }
        return out.toString();
    }

    /**
     * Extract URLs from a given text. The used RegEx is very liberal, for example it will extract URLs with/without
     * protocol, mailto: links, etc. The result are the URLs, directly from the supplied text. There is no further post
     * processing of the extracted URLs.
     * 
     * The RegEx was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls
     * and alternative one can be found on http://flanders.co.nz/2009/11/08/a-good-url-regular-expression-repost/
     * 
     * @param text
     * @return List of extracted URLs, or empty List if no URLs were found, never <code>null</code>.
     */
    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<String>();
        Pattern p = Pattern
        // .compile("\\b(?:(?:ht|f)tp(?:s?)\\:\\/\\/|~\\/|\\/)?(?:\\w+:\\w+@)?(?:(?:[-\\w]+\\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(?::[\\d]{1,5})?(?:(?:(?:\\/(?:[-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?(?:(?:\\?(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(?:&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(?:#(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
        .compile("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");

        Matcher m = p.matcher(text);
        while (m.find()) {
            urls.add(m.group());
        }
        return urls;
    }

    public static String getRegexpMatch(String regexp, String text) {
        Pattern p = Pattern.compile(regexp);

        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group();
        }

        return "";
    }

    public static List<String> getRegexpMatches(String regexp, String text) {

        List<String> matches = new ArrayList<String>();

        Pattern p = Pattern.compile(regexp);

        Matcher m = p.matcher(text);
        if (m.find()) {
            matches.add(m.group());
        }

        return matches;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main_(String[] args) {

        System.out.println(removeNonAsciiCharacters("öüäaslkjd¡“¶{}|"));

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

            Pattern cp = Pattern.compile(RegExp.getRegExp(RegExp.VALUE_STRING) + ":$");
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