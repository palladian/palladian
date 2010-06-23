package tud.iir.helper;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.RegExp;
import tud.iir.normalization.StringNormalizer;
import tud.iir.normalization.UnitNormalizer;

/**
 * The StringHelper adds string functionality.
 * 
 * @author David Urbansky
 * @author Martin Werner
 */
public class StringHelper {

    // list of brackets
    /** The Constant BRACKETS. */
    private static final char[] BRACKETS = { '(', ')', '{', '}', '[', ']' };

    // irregular nouns| singular, plural
    /** The Constant IRREGULAR_NOUNS. */
    private static final HashMap<String, String> IRREGULAR_NOUNS = new HashMap<String, String>();

    /**
     * Gets the irregular nouns.
     * 
     * @return the irregular nouns
     */
    private static HashMap<String, String> getIrregularNouns() {

        IRREGULAR_NOUNS.put("addendum", "addenda");
        IRREGULAR_NOUNS.put("alga", "algae");
        IRREGULAR_NOUNS.put("alumna", "alumnae");
        IRREGULAR_NOUNS.put("alumnus", "alumni");
        IRREGULAR_NOUNS.put("analysis", "analyses");
        IRREGULAR_NOUNS.put("antennas", "antenna");
        IRREGULAR_NOUNS.put("apparatus", "apparatuses");
        IRREGULAR_NOUNS.put("appendix", "appendices");
        IRREGULAR_NOUNS.put("axis", "axes");
        IRREGULAR_NOUNS.put("bacillus", "bacilli");
        IRREGULAR_NOUNS.put("bacterium", "bacteria");
        IRREGULAR_NOUNS.put("basis", "bases");
        IRREGULAR_NOUNS.put("beau", "beaux");
        IRREGULAR_NOUNS.put("bison", "bison");
        IRREGULAR_NOUNS.put("calf", "calves");
        IRREGULAR_NOUNS.put("child", "children");
        IRREGULAR_NOUNS.put("corps", "corps");
        IRREGULAR_NOUNS.put("crisis", "crises");
        IRREGULAR_NOUNS.put("criterion", "criteria");
        IRREGULAR_NOUNS.put("curriculum", "curricula");
        IRREGULAR_NOUNS.put("datum", "data");
        IRREGULAR_NOUNS.put("deer", "deer");
        IRREGULAR_NOUNS.put("die", "dice");
        IRREGULAR_NOUNS.put("diagnosis", "diagnoses");
        IRREGULAR_NOUNS.put("echo", "echoes");
        IRREGULAR_NOUNS.put("elf", "elves");
        IRREGULAR_NOUNS.put("ellipsis", "ellipses");
        IRREGULAR_NOUNS.put("embargo", "embargoes");
        IRREGULAR_NOUNS.put("emphasis", "emphases");
        IRREGULAR_NOUNS.put("erratum", "errata");
        IRREGULAR_NOUNS.put("fireman", "firemen");
        IRREGULAR_NOUNS.put("fish", "fish");
        IRREGULAR_NOUNS.put("foot", "feet");
        IRREGULAR_NOUNS.put("fungus", "fungi");
        IRREGULAR_NOUNS.put("genus", "genera");
        IRREGULAR_NOUNS.put("goose", "geese");
        IRREGULAR_NOUNS.put("half", "halves");
        IRREGULAR_NOUNS.put("hero", "heroes");
        IRREGULAR_NOUNS.put("hippopotamus", "hippopotami");
        IRREGULAR_NOUNS.put("hypothesis", "hypotheses");
        IRREGULAR_NOUNS.put("index", "indices");
        IRREGULAR_NOUNS.put("information", "information");
        IRREGULAR_NOUNS.put("knife", "knives");
        IRREGULAR_NOUNS.put("leaf", "leaves");
        IRREGULAR_NOUNS.put("life", "lives");
        IRREGULAR_NOUNS.put("loaf", "loaves");
        IRREGULAR_NOUNS.put("louse", "lice");
        IRREGULAR_NOUNS.put("man", "men");
        IRREGULAR_NOUNS.put("matrix", "matrices");
        IRREGULAR_NOUNS.put("means", "means");
        IRREGULAR_NOUNS.put("medium", "media");
        IRREGULAR_NOUNS.put("memorandum", "memoranda");
        IRREGULAR_NOUNS.put("millennium", "milennia");
        IRREGULAR_NOUNS.put("moose", "moose");
        IRREGULAR_NOUNS.put("mosquito", "mosquitoes");
        IRREGULAR_NOUNS.put("mouse", "mice");
        IRREGULAR_NOUNS.put("movie", "movies");
        IRREGULAR_NOUNS.put("neurosis", "neuroses");
        IRREGULAR_NOUNS.put("news", "news");
        IRREGULAR_NOUNS.put("nucleus", "nuclei");
        IRREGULAR_NOUNS.put("oasis", "oases");
        IRREGULAR_NOUNS.put("ovum", "ova");
        IRREGULAR_NOUNS.put("ox", "oxen");
        IRREGULAR_NOUNS.put("paralysis", "paralyses");
        IRREGULAR_NOUNS.put("parenthesis", "parentheses");
        IRREGULAR_NOUNS.put("person", "people");
        IRREGULAR_NOUNS.put("phenomenon", "phenomena");
        IRREGULAR_NOUNS.put("pike", "pike");
        IRREGULAR_NOUNS.put("potato", "potatoes");
        IRREGULAR_NOUNS.put("radius", "radiuses");
        IRREGULAR_NOUNS.put("salmon", "salmon");
        IRREGULAR_NOUNS.put("scissors", "scissors");
        IRREGULAR_NOUNS.put("series", "series");
        IRREGULAR_NOUNS.put("sheep", "sheep");
        IRREGULAR_NOUNS.put("shelf", "shelves");
        IRREGULAR_NOUNS.put("species", "species");
        IRREGULAR_NOUNS.put("status", "status");
        IRREGULAR_NOUNS.put("stimulus", "stimuli");
        IRREGULAR_NOUNS.put("stratum", "strata");
        IRREGULAR_NOUNS.put("swine", "swine");
        IRREGULAR_NOUNS.put("syllabus", "syllabuses");
        IRREGULAR_NOUNS.put("symposium", "symposia");
        IRREGULAR_NOUNS.put("synthesis", "syntheses");
        IRREGULAR_NOUNS.put("synopsis", "synopses");
        IRREGULAR_NOUNS.put("tableau", "tableaux");
        IRREGULAR_NOUNS.put("thesis", "theses");
        IRREGULAR_NOUNS.put("thief", "thieves");
        IRREGULAR_NOUNS.put("tomato", "tomatoes");
        IRREGULAR_NOUNS.put("tooth", "teeth");
        IRREGULAR_NOUNS.put("torpedo", "torpedoes");
        IRREGULAR_NOUNS.put("trout", "trout");
        IRREGULAR_NOUNS.put("vertebra", "vertebrae");
        IRREGULAR_NOUNS.put("vertex", "vertices");
        IRREGULAR_NOUNS.put("veto", "vetoes");
        IRREGULAR_NOUNS.put("vita", "vitae");
        IRREGULAR_NOUNS.put("wife", "wives");
        IRREGULAR_NOUNS.put("wolf", "wolves");
        IRREGULAR_NOUNS.put("woman", "women");

        return IRREGULAR_NOUNS;
    }

    /**
     * In ontologies names can not have certain characters so they have to be changed.
     * 
     * @param name The name.
     * @return The safe name.
     */
    public static String makeSafeName(String name) {
        return name.replaceAll(" ", "_").replaceAll("/", "_").replaceAll("'", "").replaceAll("\"", "")
                .replaceAll(",", "_").replaceAll("\\.", "_").replaceAll(";", "_").replaceAll("\\:", "_")
                .replaceAll("\\!", "").replaceAll("\\?", "").replaceAll("\\ä", "ae").replaceAll("\\Ä", "Ae")
                .replaceAll("\\ö", "oe").replaceAll("\\Ö", "Oe").replaceAll("\\ü", "ue").replaceAll("\\Ü", "Ue")
                .replaceAll("\\ß", "ss");
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
                part = wordToSingular(part);
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
     * Calculate n-grams for a given string. The size of the set can be calculated as: Size = stringLength - n + 1
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of characters for a gram.
     * @return A set of n-grams.
     */
    public static HashSet<String> calculateNGrams(String string, int n) {
        HashSet<String> nGrams = new HashSet<String>();

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
     * Calculate all n-grams for a string for different n. The size of the set can be calculated as: Size = SUM_n(n1,n2)
     * (stringLength - n + 1)
     * 
     * @param string The string the n-grams should be calculated for.
     * @param n1 The smallest n-gram size.
     * @param n2 The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static HashSet<String> calculateAllNGrams(String string, int n1, int n2) {
        HashSet<String> nGrams = new HashSet<String>();
        for (int n = n1; n <= n2; n++) {
            nGrams.addAll(calculateNGrams(string, n));
        }

        return nGrams;
    }

    /**
     * Make name for view.
     * 
     * @param name The name
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
            pat = Pattern.compile(RegExp.getRegExp(Attribute.VALUE_STRING));
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp "
                            + RegExp.getRegExp(Attribute.VALUE_STRING), e);
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
            pat = Pattern.compile(RegExp.getRegExp(Attribute.VALUE_NUMERIC));
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp "
                            + RegExp.getRegExp(Attribute.VALUE_NUMERIC), e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }

        return false;
    }

    /**
     * Transform an English plural word to its singular form. Rules:
     * http://www.englisch-hilfen.de/en/grammar/plural.htm,
     * http://en.wikipedia.org/wiki/English_plural
     * 
     * @param pluralForm the plural form
     * @return The singular.
     */
    public static String wordToSingular(String pluralForm) {

        String plural = pluralForm;

        if (plural == null) {
            return "";
        }

        String singular = plural;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsValue(plural)) {
            singular = (String) CollectionHelper.getKeyByValue(getIrregularNouns(), singular);

            if (StringHelper.startsUppercase(plural)) {
                singular = upperCaseFirstLetter(singular);
            }
            return singular;
        }

        if (singular.length() < 4) {
            return singular;
        }

        // substitute ices with x
        if (plural.toLowerCase().endsWith("ices")) {
            return plural.substring(0, plural.length() - 4) + "ix";
        }

        // substitute ies with y after consonants
        if (plural.toLowerCase().endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        }

        // substitute ves with f or fe
        if (plural.toLowerCase().endsWith("ves")) {
            plural = plural.substring(0, plural.length() - 3) + "f";
            if (!isVowel(plural.substring(plural.length() - 3, plural.length() - 2).charAt(0))
                    && isVowel(plural.substring(plural.length() - 2, plural.length() - 1).charAt(0))) {
                plural += "e";
            }
            return plural;
        }

        // remove es
        if ((plural.toLowerCase().endsWith("es") && plural.length() >= 5)) {
            String lettersBeforeES = plural.substring(plural.length() - 4, plural.length() - 2);
            String letterBeforeES = lettersBeforeES.substring(1);
            if (lettersBeforeES.equalsIgnoreCase("ss") || lettersBeforeES.equalsIgnoreCase("ch")
                    || lettersBeforeES.equalsIgnoreCase("sh") || letterBeforeES.equalsIgnoreCase("x")
                    || isVowel(letterBeforeES.charAt(0))) {
                return plural.substring(0, plural.length() - 2);
            }
        }

        // remove s
        if (plural.toLowerCase().endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        }

        return plural;
    }

    /**
     * Transform an English singular word to its plural form. rules:
     * http://owl.english.purdue.edu/handouts/grammar/g_spelnoun.html
     * 
     * @param singular The singular.
     * @return The plural.
     */
    public static String wordToPlural(String singular) {

        if (singular == null) {
            return "";
        }

        String plural = singular;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsKey(singular)) {
            plural = getIrregularNouns().get(singular);

            if (StringHelper.startsUppercase(singular)) {
                plural = upperCaseFirstLetter(plural);
            }
            return plural;
        }

        // word must be at least two characters long
        if (singular.length() < 3) {
            return singular;
        }

        // get last two letters
        String lastLetter = singular.substring(singular.length() - 1, singular.length());
        String secondLastLetter = singular.substring(singular.length() - 2, singular.length() - 1);
        String lastTwoLetters = secondLastLetter + lastLetter;

        // if word ends in a vowel plus -y (-ay, -ey, -iy, -oy, -uy), add an -s
        if (lastTwoLetters.equalsIgnoreCase("ay") || lastTwoLetters.equalsIgnoreCase("ey")
                || lastTwoLetters.equalsIgnoreCase("iy") || lastTwoLetters.equalsIgnoreCase("oy")
                || lastTwoLetters.equalsIgnoreCase("uy")) {
            return singular + "s";
        }

        // if word ends in a consonant plus -y, change the -y into -ie and add
        // an -s
        if (lastLetter.equalsIgnoreCase("y")) {
            return singular.substring(0, singular.length() - 1) + "ies";
        }

        // if words that end in -is, change the -is to -es
        if (lastTwoLetters.equalsIgnoreCase("is")) {
            return singular.substring(0, singular.length() - 2) + "es";
        }

        // if word ends on -s, -z, -x, -ch or -sh end add an -es
        if (lastLetter.equalsIgnoreCase("s") || lastLetter.equalsIgnoreCase("z") || lastLetter.equalsIgnoreCase("x")
                || lastTwoLetters.equalsIgnoreCase("ch") || lastTwoLetters.equalsIgnoreCase("sh")) {
            return singular + "es";
        }

        // some words that end in -f or -fe have plurals that end in -ves
        // if (lastTwoLetters.equalsIgnoreCase("is")) {
        // return singular.substring(0,singular.length()-2)+"es";
        // }

        // if no other rule applied just add an s
        return singular + "s";
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
     * Remove all style and script tags including their content (css, javascript). Remove all other tags as well. Close
     * gaps.
     * 
     * @param htmlContent the html content
     * @param stripTags the strip tags
     * @param stripComments the strip comments
     * @param stripJSAndCSS the strip js and css
     * @param joinTagsAndRemoveNewlines the join tags and remove newlines
     * @return The text of the web page.
     */
    public static String removeHTMLTags(String htmlContent, boolean stripTags, boolean stripComments,
            boolean stripJSAndCSS, boolean joinTagsAndRemoveNewlines) {

        String htmlText = htmlContent;
        // modified by Martin Werner, 2010-06-02

        if (joinTagsAndRemoveNewlines) {
            htmlText = htmlText.replaceAll(">\\s*?<", "><");
            htmlText = htmlText.replaceAll("\n", "");
        }

        // String regExp = "";

        if (stripComments) {
            // regExp += "(\\<!--.*?-->)|";
            htmlText = htmlText.replaceAll("<!--.*?-->", "");
        }

        if (stripJSAndCSS) {
            // regExp += "(<style.*?>.*?</style>)|(<script.*?>.*?</script>)|";
            htmlText = removeConcreteHTMLTag(htmlText, "style");
            htmlText = removeConcreteHTMLTag(htmlText, "script");
        }

        if (stripTags) {
            // regExp += "(\\<.*?>)";
            // htmlText = removeConcreteHTMLTag(htmlText, "\\<", ">", true);
            htmlText = htmlText.replaceAll("<.*?>", "");
        }

        // if (regExp.length() == 0) {
        // return htmlText;
        // }

        // if (regExp.endsWith("|")) {
        // regExp = regExp.substring(0, regExp.length() - 1);
        // }
        //
        // // Pattern pattern =
        // //
        // Pattern.compile("((\\<!--.*?-->)|(\\<style.*?>.*?\\</style>)|(\\<script.*?>.*?\\</script>)|(\\<.*?>))",Pattern.DOTALL);
        // Pattern pattern = Pattern.compile("(" + regExp + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        // Matcher matcher = pattern.matcher(htmlText);
        //
        // while (matcher.find()) {
        // htmlText = htmlText.replace(matcher.group(), " "); // TODO changed
        // // and untested
        // // 16/06/2009
        // // replace with
        // // whitespace
        // // instead of
        // // nothing
        // }

        // close gaps
        htmlText = htmlText.replaceAll("(\\s){2,}", " ");

        return htmlText.trim();
    }

    /**
     * Removes the concrete html tag.
     * 
     * @param pageString the page string
     * @param tag the tag
     * @return the string
     */
    public static String removeConcreteHTMLTag(String pageString, String tag) {
        return removeConcreteHTMLTag(pageString, tag, tag);
    }

    /**
     * Remove concrete HTMLTags from a string; set isSpecial=true for special-tags like <!-- -->.
     * 
     * @param pageContent the page content
     * @param beginTag the begin tag
     * @param endTag the end tag
     * @return the string
     */
    public static String removeConcreteHTMLTag(String pageContent, String beginTag, String endTag) {
        String pageString = pageContent;
        List<String> removeList;
        removeList = getConcreteTags(pageString, beginTag, endTag);
        for (String removeTag : removeList) {
            pageString = pageString.replace(removeTag, "");
        }
        return pageString;
    }

    /**
     * Get a list of concrete HTMLTags; begin- and endtag are not different.
     * 
     * @param pageString the page string
     * @param tag the tag
     * @return the concrete tags
     */
    public static List<String> getConcreteTags(String pageString, String tag) {
        return getConcreteTags(pageString, tag, tag);
    }

    /**
     * Get a list of concrete HTMLTags; its possible that begin- and endtag are different like <!-- -->.
     * 
     * @param pageString the page string
     * @param beginTag the begin tag
     * @param endTag the end tag
     * @return the concrete tags
     */
    public static List<String> getConcreteTags(String pageString, String beginTag, String endTag) {

        List<String> tagList = new ArrayList<String>();
        String regExp = "";
        if (beginTag.equals(endTag)) {
            // regExp = "<"+beginTag+".*?>.*?</"+endTag+">";
            regExp = "<" + beginTag + ".*?>(.*?</" + endTag + ">)?";

        } else {
            regExp = beginTag + ".*?" + endTag;
        }

        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(pageString);
        while (matcher.find()) {
            tagList.add(matcher.group(0));
        }

        return tagList;
    }

    /**
     * Removes the special chars.
     * 
     * @param string the string
     * @return the string
     */
    public static String removeSpecialChars(String string) {
        String modString = string.replaceAll(" ", " ");
        return modString;
    }

    /**
     * Count tags.
     * 
     * @param htmlText the html text
     * @return the int
     */
    public static int countTags(String htmlText) {
        return countTags(htmlText, false);
    }

    /**
     * Count tags.
     * 
     * @param htmlText the html text
     * @param distinct the distinct
     * @return the int
     */
    public static int countTags(String htmlText, boolean distinct) {
        Set<String> tags = new HashSet<String>();

        int tagCount = 0;

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            tagCount++;
            tags.add(matcher.group());
        }

        if (distinct) {
            tagCount = tags.size();
        }

        return tagCount;
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
        for (int i = 0; i < BRACKETS.length; i++) {
            if (BRACKETS[i] == character) {
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

        Pattern pattern = Pattern.compile("^" + RegExp.getRegExp(Attribute.VALUE_NUMERIC));
        Matcher m = pattern.matcher(string);
        try {

            if (m.find()) {
                double number = Double.valueOf(StringNormalizer.normalizeNumber(m.group()));
                double convertedNumber = UnitNormalizer.getNormalizedNumber(number,
                        string.substring(m.end(), string.length()));
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
     * Given a string, find the beginning of the sentence, e.g. "...now. Although, many of them" =>
     * "Although, many of them". consider !,?,. and : as end of
     * sentence TODO control character after delimiter makes it end of sentence
     * 
     * @param inputString the input string
     * @return The phrase from the beginning of the sentence.
     */
    public static String getPhraseFromBeginningOfSentence(String inputString) {

        String string = inputString;
        // find the beginning of the current sentence by finding the period at
        // the end
        int startIndex = string.lastIndexOf(".");

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        boolean pointIsSentenceDelimiter = false;
        while (!pointIsSentenceDelimiter && startIndex > -1) {
            if (startIndex >= string.length() - 1) {
                break;
            }

            if (startIndex > 0) {
                pointIsSentenceDelimiter = (!isNumber(string.charAt(startIndex - 1)) && Character.isUpperCase(string
                        .charAt(startIndex + 1)));
            }
            if (!pointIsSentenceDelimiter && startIndex < string.length() - 2) {
                pointIsSentenceDelimiter = (Character.isUpperCase(string.charAt(startIndex + 2)) && string
                        .charAt(startIndex + 1) == ' ');
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
                pointIsSentenceDelimiter = !isNumber(string.charAt(endIndex - 1));
            }
            // one digit after point
            if (endIndex < string.length() - 1) {
                pointIsSentenceDelimiter = (!isNumber(string.charAt(endIndex + 1))
                        && (Character.isUpperCase(string.charAt(endIndex + 1))) || isBracket(string
                        .charAt(endIndex + 1)));
            }
            // two digits after point
            if (!pointIsSentenceDelimiter && endIndex < string.length() - 2) {
                pointIsSentenceDelimiter = (!isNumber(string.charAt(endIndex + 2))
                        && (Character.isUpperCase(string.charAt(endIndex + 2)) || isBracket(string.charAt(endIndex + 2))) && string
                        .charAt(endIndex + 1) == ' ');
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
            if (string.length() > indexColon + 1 && !isNumber(string.charAt(indexColon + 1))) {
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

        string = unescapeHTMLEntities(string);

        String[] unwanted = { ",", ".", ":", ";", "!", "|", "?", "¬", " ", " ", "#", "-", "\'", "\"", "*", "/",
                "\\", "@", "<", ">", "=", "·", "^", "_", "+" }; // whitespace
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
        for (int i = 0, l = string.length(); i < l; ++i) {
            // < 33 means all control characters are not wanted as well
            if ((int) string.charAt(i) < 33) {
                string = string.replace(string.charAt(i), ' ');
            }

        }

        // close spaces gap that might have arisen
        string = string.replaceAll("(\\s){1,}", " ");

        // string = string.replaceAll("'\\)\\)","").replaceAll("'\\)",""); //
        // values are in javascript text sometimes e.g. ...('80GB')

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
     * Unescape html entities.
     * 
     * @param string the string
     * @return the string
     */
    public static String unescapeHTMLEntities(String string) {
        String escaped = StringEscapeUtils.unescapeHtml(string);
        escaped = escaped.replaceAll("â€™", "’");
        return escaped.replaceAll(" ", " ");
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
        double longestCommonStringLength = (double) getLongestCommonString(string1, string2, caseSensitive, true)
                .length();
        if (longestCommonStringLength == 0) {
            return 0.0;
        }

        double similarity = longestCommonStringLength / (double) Math.min(string1.length(), string2.length()); // TODO
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
     */
    public static String getArrayAsString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]).append(",");
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
        String modInputString = StringHelper.unescapeHTMLEntities(inputString);
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
     * Overlap.
     * 
     * @param start1 the start1
     * @param end1 the end1
     * @param start2 the start2
     * @param end2 the end2
     * @return true, if successful
     */
    public static boolean overlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {

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
        CollectionHelper.print(calculateNGrams("allthelilacsinohio", 3));
        CollectionHelper.print(calculateNGrams("hiatt", 3));
        CollectionHelper.print(calculateAllNGrams("allthelilacsinohio", 3, 8));
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

        String n = "(2008)";
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

            Pattern cp = Pattern.compile(RegExp.getRegExp(Attribute.VALUE_STRING) + ":$");
            Matcher cpm = cp.matcher(neighborhood.substring(Math.max(0, colonIndex - 30), colonIndex + 1));
            // System.out.println("String before colon: " + neighborhood.substring(Math.max(0, colonIndex - 30),
            // colonIndex + 1));
            int i = 1;
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

                String value = neighborhood.substring(colonIndex + 1, Math.min(neighborhood.length(), nextLookOut));
                // System.out.println("==> " + newAttributeName + ":" + value);
            }

            colonIndex = nextColonIndex;
        }

    }
}