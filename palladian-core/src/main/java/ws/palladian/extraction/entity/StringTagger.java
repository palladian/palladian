package ws.palladian.extraction.entity;

import java.util.List;


/**
 * <p>
 * Tag possible named entities in an English text.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class StringTagger {

    public static final String CANDIDATE_TAG = "<CANDIDATE>$0</CANDIDATE>";

    private static String tagString(String s, String regexp) {
        return s.replaceAll(regexp, CANDIDATE_TAG);
    }

    private static String tagString(String s) {

        String regexp = "";

        String camelCaseWords = "(GmbH|LLC)";
        String companySuffixes = "((?<=(Inc)|(Corp)|(Co))\\.)?";

        // dashes (such as "Ontario-based" "Victor" or St. Louis-based)
        regexp += "([A-Z][a-z]\\. )?([A-Z]{1}[A-Za-z]+(-[a-z]+)(-[A-Za-z]+)*)";

        // names
        regexp += "|";
        regexp += "([A-Z]\\.)( )?[A-Z]{1}[['’]A-Za-z]{1,100}";
        regexp += "|";
        regexp += "[A-Z][a-z]+ [A-Z]{1}\\. [A-Za-z]{1,100}";
        regexp += "|";
        regexp += "([A-Z][a-z]{0,2}\\.) [A-Z]{1}[A-Za-z]{1,100}( [A-Z]{1}[A-Za-z]{1,100})?";
        regexp += "|";
        // regexp +=
        // "([A-Z]\\.)+ (([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ ])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})";
        regexp += "([A-Z]\\.)+( ([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ ])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})*";
        // regexp += "|";
        // regexp +=
        // "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9']+))+(( )?[A-Z]+('[A-Z])?([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // ending with dash (Real- Rumble => should be two words, TOTALLY FREE- Abc => also two matches)
        regexp += "|";
        regexp += "([A-Z][A-Za-z]+ )*[A-Z][A-Za-z]+(?=-+? )";

        // small with dash (ex-President)
        regexp += "|";
        regexp += "([A-Z][A-Za-z]+ )?([a-z]+-[A-Z][A-Za-z0-9]+)";

        // ___ of ___ (such as "National Bank of Scotland" or "Duke of South Carolina") always in the form of X Y of Z
        // OR X of Y Z
        regexp += "|";
        regexp += "(([A-Z]{1}[A-Za-z]+ ){2,}of (([A-Z]{1}[A-Za-z-]+)(?!([a-z-]{0,20}\\s[A-Z]))))|([A-Z]{1}[A-Za-z-]+ of( [A-Z]{1}[A-Za-z]+){1,})";

        // prevent mixtures of mix camel cases => "Veronica Swenston VENICE" should be two matches
        regexp += "|";
        regexp += "([A-Z]{1}([a-z-0-9®]+)(( " + camelCaseWords + ")?(([ &])*([A-Z]['’])?[A-Z]{1}([a-z-0-9®]+))?)*)"
                + companySuffixes;

        // names (such as "O'Sullivan"), compounds such as "D&G"
        regexp += "|";
        regexp += "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+|['’][A-Z][A-Za-z]{2,20}))+(([ &])*[A-Z]+(['’][A-Z])?([A-Za-z-üäößãáàúùíìîéèê0-9®]*)){0,10})(?!(\\.[A-Z])+))"
                + companySuffixes;

        // regexp += "|";
        // regexp +=
        // "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ &])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // camel case (iPhone 4)
        regexp += "|";
        regexp += "([a-z][A-Z][A-Za-z0-9]+( [A-Z0-9][A-Za-z0-9]{0,20}){0,20})";

        s = s.replaceAll(regexp, CANDIDATE_TAG);

        return s;
    }

    public static List<Annotation> getTaggedEntities(String text, String regexp) {
        String taggedText = tagString(text, regexp);
        return FileFormatParser.getAnnotationsFromXmlText(taggedText);
    }

    public static List<Annotation> getTaggedEntities(String text) {
        String taggedText = tagString(text);
        return FileFormatParser.getAnnotationsFromXmlText(taggedText);
    }
}