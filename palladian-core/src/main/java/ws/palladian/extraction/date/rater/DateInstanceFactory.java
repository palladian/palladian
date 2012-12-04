package ws.palladian.extraction.date.rater;

import java.util.Arrays;

import ws.palladian.classification.InstanceBuilder;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Creates an instance for WEKA classifier out of a ContentDate.
 * </p>
 * 
 * @author Martin Gregor
 * @author David Urbansky
 * @author Philipp Katz
 */
final class DateInstanceFactory {
    
    private DateInstanceFactory() {
        // no instances.
    }

    public static FeatureVector createFeatureVector(ContentDate date) {

        String formatString = date.getFormat();
        if (!isNormalFormat(formatString)) {
            formatString = changeFormat(formatString);
        }

        String tagNameString = date.getTag().toUpperCase();
        if (!isNormalTag(tagNameString)) {
            tagNameString = "A";
        }

        String keywordString = date.getKeyword();
        if (keywordString != null) {
            keywordString = keywordString.toLowerCase();
            if (!isNormalKeyword(keywordString)) {
                keywordString = getNormalKeyword(keywordString);
            }
        } else {
            keywordString = "null";
        }

        if (formatString.indexOf(",") > -1 || formatString.indexOf(" ") > -1) {
            formatString = "'" + formatString + "'";
        }

        String keyword = keywordString.indexOf(" ") > -1 ? "'" + keywordString + "'" : keywordString;
        
        // XXX replace numeric to nominal where applicable (needs new model).
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        instanceBuilder.set("hour", date.get(ExtractedDate.HOUR) > -1 ? 1.0 : 0.0);
        instanceBuilder.set("minute", date.get(ExtractedDate.HOUR) > -1 ? 1.0 : 0.0);
        instanceBuilder.set("second", date.get(ExtractedDate.HOUR) > -1 ? 1.0 : 0.0);
        instanceBuilder.set("relDocPos", date.getRelDocPos());
        instanceBuilder.set("ordDocPos", date.getOrdDocPos());
        instanceBuilder.set("ordAgePos", date.getOrdAgePos());
        instanceBuilder.set("keyClass", (double)Math.max(0, KeyWords.getKeywordPriority(date.getKeyword())));
        instanceBuilder.set("keyLoc", (double)date.getKeyLoc());
        instanceBuilder.set("keyDiff", date.getKeyDiff());
        instanceBuilder.set("simpleTag", date.isSimpleTag() ? 1.0 : 0.0);
        instanceBuilder.set("hTag", date.isHTag() ? 1.0 : 0.0);
        instanceBuilder.set("tagName", tagNameString);
        instanceBuilder.set("hasStructureDate", date.hasStructureDate() ? 1.0 : 0.0);
        instanceBuilder.set("inMetaDates", date.isInMetaDates() ? 1.0 : 0.0);
        instanceBuilder.set("inUrl", date.isInUrl() ? 1.0 : 0.0);
        instanceBuilder.set("relCntSame", date.getRelCntSame());
        instanceBuilder.set("relSize", date.getRelSize());
        instanceBuilder.set("distPosBefore", (double)date.getDistPosBefore());
        instanceBuilder.set("distPosAfter", (double)date.getDistPosAfter());
        instanceBuilder.set("distAgeBefore", (double)date.getDistAgeBefore());
        instanceBuilder.set("format", formatString);
        instanceBuilder.set("keyword", keyword);
        instanceBuilder.set("excatness", (double)date.getExactness().getValue());
        instanceBuilder.set("keyLoc201", date.getKeyLoc() == 1 ? 1.0 : 0.0);
        instanceBuilder.set("keyLoc202", date.getKeyLoc() == 2 ? 1.0 : 0.0);
        instanceBuilder.set("isKeyClass1", date.getKeywordPriority() == 1 ? 1.0 : 0.0);
        instanceBuilder.set("isKeyClass2", date.getKeywordPriority() == 2 ? 1.0 : 0.0);
        instanceBuilder.set("isKeyClass3", date.getKeywordPriority() == 3 ? 1.0 : 0.0);
        return instanceBuilder.create();

    }

    private static boolean isNormalKeyword(String keyword) {
        return Arrays.asList(KeyWords.ARFF_KEYWORDS).contains(keyword.toLowerCase());
    }

    private static String getNormalKeyword(String keyword) {
        String normalKeyword = null;
        int prio = KeyWords.getKeywordPriority(keyword);
        switch (prio) {
            case 1:
                normalKeyword = "publish";
                break;
            case 2:
                normalKeyword = "update";
                break;
            case 3:
                normalKeyword = "date";
                break;
        }
        return normalKeyword;
    }

    private static boolean isNormalTag(String tagName) {
        return tagName.equalsIgnoreCase("TD") || tagName.equalsIgnoreCase("OPTION") || tagName.equalsIgnoreCase("CITE")
                || tagName.equalsIgnoreCase("A") || tagName.equalsIgnoreCase("B") || tagName.equalsIgnoreCase("DT")
                || tagName.equalsIgnoreCase("I") || tagName.equalsIgnoreCase("U") || tagName.equalsIgnoreCase("DL")
                || tagName.equalsIgnoreCase("PRE") || tagName.equalsIgnoreCase("P") || tagName.equalsIgnoreCase("DD")
                || tagName.equalsIgnoreCase("CENTER") || tagName.equalsIgnoreCase("ABBR")
                || tagName.equalsIgnoreCase("CODE") || tagName.equalsIgnoreCase("TIME")
                || tagName.equalsIgnoreCase("SMALL") || tagName.equalsIgnoreCase("ADDRESS")
                || tagName.equalsIgnoreCase("PAUSING") || tagName.equalsIgnoreCase("STRONG")
                || tagName.equalsIgnoreCase("SPAN") || tagName.equalsIgnoreCase("NOBR")
                || tagName.equalsIgnoreCase("H6") || tagName.equalsIgnoreCase("H5") || tagName.equalsIgnoreCase("H4")
                || tagName.equalsIgnoreCase("EM") || tagName.equalsIgnoreCase("EDITDATE")
                || tagName.equalsIgnoreCase("FONT") || tagName.equalsIgnoreCase("H3") || tagName.equalsIgnoreCase("H2")
                || tagName.equalsIgnoreCase("BODY") || tagName.equalsIgnoreCase("H1")
                || tagName.equalsIgnoreCase("DIV") || tagName.equalsIgnoreCase("LI")
                || tagName.equalsIgnoreCase("HTML:ABBR") || tagName.equalsIgnoreCase("BLOCKQUOTE")
                || tagName.equalsIgnoreCase("TITLE");
    }

    private static boolean isNormalFormat(String format) {
        return format.equals("MM/DD/YYYY") || format.equals("MM/DD/YYYY HH:MM:SS +UTC")
                || format.equals("DD. MMMM YYYY") || format.equals("DD.MM.YYYY") || format.equals("MMMM-DD-YYYY")
                || format.equals("YYYYDDD") || format.equals("YYYY-MM-DDTHH:MM:SS+HH:MM")
                || format.equals("YYYY-MM-DD") || format.equals("MMMM DD, YYYY") || format.equals("YYYYMMDD")
                || format.equals("WD MMM DD_1 HH:MM:SS YYYY") || format.equals("MMMM DD, YYYY YYYY HH:MM:SS +UTC")
                || format.equals("YYYY_MM_DD") || format.equals("WD, DD MMM YYYY HH:MM:SS +UTC")
                || format.equals("YYYY-MMM-D") || format.equals("DD. MMMM YYYY HH:MM:SS +UTC")
                || format.equals("DD.MM.YYYY HH:MM:SS +UTC") || format.equals("YYYY-DDD");
    }

    private static String changeFormat(String format) {
        String result = null;
        if (format.equals("YYYY-DDDTHH:MM:SS+HH:MM") || format.equals("YYYY-WW-DTHH:MM:SS+HH:MM")) {
            result = "YYYY-MM-DDTHH:MM:SS+HH:MM";
        } else if (format.equals("WWD, DD-MMM-YY HH:MM:SS TZ") || format.equals("WD, DD MMM YYYY HH:MM:SS TZ")
                || format.equals("WD MMM DD_1 HH:MM:SS YYYY +UTC") || format.equals("WWD, DD-MMM-YY HH:MM:SS +UTC")) {
            result = "WD, DD MMM YYYY HH:MM:SS +UTC";
        } else if (format.equals("YYYY-WW-D")) {
            result = "YYYY-MM-DD";
        }
        return result;
    }

}
