package ws.palladian.helper;

import java.util.HashMap;
import java.util.Map;

/**
 * The WordTransformer transforms an input word.
 * Currently it can transform English singular to plural and vice versa.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * 
 */
public class WordTransformer {

    /** The Constant IRREGULAR_NOUNS <singular, plural>. */
    private static final Map<String, String> IRREGULAR_NOUNS = new HashMap<String, String>();
    
    static {
        
        IRREGULAR_NOUNS.put("addendum", "addenda");
        IRREGULAR_NOUNS.put("alga", "algae");
        IRREGULAR_NOUNS.put("alumna", "alumnae");
        IRREGULAR_NOUNS.put("alumnus", "alumni");
        IRREGULAR_NOUNS.put("analysis", "analyses");
        IRREGULAR_NOUNS.put("antennas", "antenna");
        IRREGULAR_NOUNS.put("apparatus", "apparatuses");
        IRREGULAR_NOUNS.put("appendix", "appendices");
        IRREGULAR_NOUNS.put("archive", "archives"); // would be converted to singular "archife"
        IRREGULAR_NOUNS.put("automaton", "automata");
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
        IRREGULAR_NOUNS.put("service", "services"); // would be converted to singular "servix"
        IRREGULAR_NOUNS.put("sheep", "sheep");
        IRREGULAR_NOUNS.put("shelf", "shelves");
        IRREGULAR_NOUNS.put("shrimp", "shrimp");
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
    }

    /**
     * Get a map of irregular nouns.
     * 
     * @return The map of irregular nouns.
     */
    private static Map<String, String> getIrregularNouns() {
        // moved initialization to static block, only initialize once -> speed up.
        return IRREGULAR_NOUNS;
    }

    /**
     * Transform an English plural word to its singular form.<br>
     * Rules:
     * http://www.englisch-hilfen.de/en/grammar/plural.htm,
     * http://en.wikipedia.org/wiki/English_plural
     * 
     * @param pluralForm The plural form of the word.
     * @return The singular form of the word.
     */
    public static String wordToSingular(String pluralForm) {

        String plural = pluralForm;

        if (plural == null) {
            return "";
        }

        String singular = plural;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsValue(plural)) {
            singular = CollectionHelper.getKeyByValue(getIrregularNouns(), singular);

            if (StringHelper.startsUppercase(plural)) {
                singular = StringHelper.upperCaseFirstLetter(singular);
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
            char letterBeforeVES = plural.substring(plural.length() - 3, plural.length() - 2).charAt(0);
            plural = plural.substring(0, plural.length() - 3) + "f";
            if (!StringHelper.isVowel(letterBeforeVES)
                    && StringHelper.isVowel(plural.substring(plural.length() - 2, plural.length() - 1).charAt(0))) {
                plural += "e";
            }
            return plural;
        }

        // remove es
        if (plural.toLowerCase().endsWith("es") && plural.length() >= 5) {
            String lettersBeforeES = plural.substring(plural.length() - 4, plural.length() - 2);
            String letterBeforeES = lettersBeforeES.substring(1);
            if (lettersBeforeES.equalsIgnoreCase("ss") || lettersBeforeES.equalsIgnoreCase("ch")
                    || lettersBeforeES.equalsIgnoreCase("sh") || letterBeforeES.equalsIgnoreCase("x")
                    || StringHelper.isVowel(letterBeforeES.charAt(0))) {
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
                plural = StringHelper.upperCaseFirstLetter(plural);
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
    
    public static void main(String[] args) {
        
        // 335ms
        
        StopWatch sw = new StopWatch();
        System.out.println(WordTransformer.wordToSingular("women"));
        System.out.println(WordTransformer.wordToSingular("services")); // wrong
        System.out.println(WordTransformer.wordToSingular("series"));
        System.out.println(WordTransformer.wordToSingular("species"));
        System.out.println(WordTransformer.wordToSingular("automata")); // wrong
        System.out.println(WordTransformer.wordToSingular("archives")); // wrong
        System.out.println(sw.getElapsedTimeString());
    }

}
