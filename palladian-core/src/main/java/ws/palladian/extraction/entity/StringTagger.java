package ws.palladian.extraction.entity;

import java.io.File;

import ws.palladian.helper.io.FileHelper;

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

    public static void tagAndSaveString(File input) {
        String text = FileHelper.readFileToString(input.getAbsolutePath());
        String taggedText = tagString(text);
        FileHelper.writeToFile(
                FileHelper.getRenamedFilename(input, input.getName().replaceAll("\\..*", "") + "_tagged"), taggedText);
    }

    public static String tagString(File f) {
        String text = FileHelper.readFileToString(f.getAbsolutePath());
        return tagString(text);
    }

    public static String tagString(String s, String regexp) {
        return s.replaceAll(regexp, CANDIDATE_TAG);
    }

    public static String tagString(String s) {

        String regexp = "";

        String camelCaseWords = "(GmbH)";
        String companySuffixes = "((?<=(Inc)|(Corp))\\.)?";

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
        regexp += "([A-Z]{1}([a-z-0-9]+)(( " + camelCaseWords + ")?(([ &])*([A-Z]['’])?[A-Z]{1}([a-z-0-9]+))?)*)"
                + companySuffixes;

        // names (such as "O'Sullivan"), compounds such as "D&G"
        regexp += "|";
        regexp += "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+|['’][A-Z][A-Za-z]{2,20}))+(([ &])*[A-Z]+(['’][A-Z])?([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))"
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

    public static Annotations getTaggedEntities(String text, String regexp) {
        Annotations annotations = new Annotations();
        String taggedText = tagString(text, regexp);
        annotations = FileFormatParser.getAnnotationsFromXmlText(taggedText);
        return annotations;
    }

    public static Annotations getTaggedEntities(String text) {
        Annotations annotations = new Annotations();
        String taggedText = tagString(text);
        annotations = FileFormatParser.getAnnotationsFromXmlText(taggedText);
        return annotations;
    }
}