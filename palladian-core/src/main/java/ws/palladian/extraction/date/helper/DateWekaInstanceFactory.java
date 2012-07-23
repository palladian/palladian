package ws.palladian.extraction.date.helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringOutputStream;

/**
 * Creates an instance for WEKA classifier out of a ContentDate.
 * 
 * @author Martin Gregor
 * @author David Urbansky
 * 
 */
public class DateWekaInstanceFactory {

    private String templateFile;
    private StringBuilder template = new StringBuilder();

    public DateWekaInstanceFactory(PageDateType pageDateType) {
        if (pageDateType.equals(PageDateType.publish)) {
            this.templateFile = "/wekaClassifier/pubTemplate.arff";
        } else {
            this.templateFile = "/wekaClassifier/modTemplate.arff";
        }
        readTemplate();
    }

    private void readTemplate() {
        InputStream inputStream = DateWekaInstanceFactory.class.getResourceAsStream(this.templateFile);
        BufferedInputStream stream = null;
        StringOutputStream templateStream = null;

        try {
            stream = new BufferedInputStream(inputStream);
            templateStream = new StringOutputStream();

            byte[] buffer = new byte[4096];

            int n = 0;
            while ((n = stream.read(buffer)) != -1) {
                templateStream.write(buffer, 0, n);
            }
            template = new StringBuilder(templateStream.toString() + "\n");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream, stream, templateStream);
        }
    }

    public Instance getDateInstanceByArffTemplate(ContentDate date) {
        Instance instance = null;
        StringBuilder dateInstanceBuffer = new StringBuilder(this.template.toString());
        String dateFeatures = datefeaturesToString(date);
        dateInstanceBuffer.append("0," + dateFeatures + "\n");
//        System.out.println(dateFeatures);
        BufferedReader reader = new BufferedReader(new StringReader(
                dateInstanceBuffer.toString()));
        try {
//        	System.out.println(dateInstanceBuffer.toString());
        	Instances instances = new Instances(reader);
        	instances.setClassIndex(0);
            instance = instances.firstInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instance;
    }

    private String datefeaturesToString(ContentDate date) {
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
        }else {
        	keywordString = "null";
        }
        

        String hour = (date.get(ContentDate.HOUR) > -1) ? "1.0" : "0.0";
        String minute = (date.get(ContentDate.HOUR) > -1) ? "1.0" : "0.0";
        String second = (date.get(ContentDate.HOUR) > -1) ? "1.0" : "0.0";

        String relDocPos = String.valueOf(date.getRelDocPos());
        String ordDocPos = String.valueOf(date.getOrdDocPos());
        String ordAgePos = String.valueOf(date.getOrdAgePos());

        int keyPrio = KeyWords.getKeywordPriority(date.getKeyword());
        keyPrio = keyPrio == -1 ? 0 : keyPrio;
        String keyClassString = String.valueOf(keyPrio);
        String keyLoc = date.getKeyLoc();
        String keyDiff = String.valueOf(date.getKeyDiff());

        String simpleTag = date.getSimpleTag()+ ".0";
        String hTag = date.gethTag()+ ".0";
        String tagName = tagNameString;

        String hasStructureDate = date.hasStrucutreDate() ?"1.0" : "0.0";
        String inMetaDates = date.isInMetaDates() ? "1.0" : "0.0";
        String inUrl = date.isInUrl() ? "1.0" : "0.0";

        String relCntSame = String.valueOf(date.getRelCntSame());
        String relSize = String.valueOf(date.getRelSize());

        String distPosBefore = String.valueOf(date.getDistPosBefore());
        String distPosAfter = String.valueOf(date.getDistPosAfter());
        String distAgeBefore = String.valueOf(date.getDistAgeBefore());
        String distAgeAfter = String.valueOf(date.getDistAgeAfter());

        // String distAgeBefore = date.getDistAgeBefore();
        // String distAgeAfter = date.getDistAgeAfter();

        // if formatString .index(,) > -1 then ''
        // else if fomratstring.ind (" " > -1 then ''
        String format = formatString;
        if (formatString.indexOf(",") > -1 || formatString.indexOf(" ") > -1) {
            format = "'" + formatString + "'";
        }

        String keyword = (keywordString.indexOf(" ") > -1) ? "'"
                + keywordString + "'" : keywordString;
        String excatness = String.valueOf(date.getExactness());

        String keyLoc201 = date.getKeyLoc201() + ".0";
        String keyLoc202 = date.getKeyLoc202() + ".0";

        String isKeyClass1 = date.getIsKeyClass1() + ".0";
        String isKeyClass2 = date.getIsKeyClass2() + ".0";
        String isKeyClass3 = date.getIsKeyClass3() + ".0";

        String dateString = hour + "," + minute + "," + second + ","
        + relDocPos + "," + ordDocPos + "," + ordAgePos + ","
        + keyClassString + "," + keyLoc + "," + keyDiff + ","
        + simpleTag + "," + hTag + "," + tagName + ","
        + hasStructureDate + "," + inMetaDates + "," + inUrl + ","
        + relCntSame + "," + relSize + "," + distPosBefore + ","
        + distPosAfter + "," + distAgeBefore + "," + distAgeAfter + "," + format + ","
        + keyword + "," + excatness + "," + keyLoc201 + "," + keyLoc202
        + "," + isKeyClass1 + "," + isKeyClass2 + "," + isKeyClass3;
        return dateString;
    }

    private static boolean isInstance(ContentDate date) {
        boolean resultExactness = date.get(ExtractedDate.EXACTENESS) >= 3 ? true
                : false;
        boolean resultFormat = isNormalFormat(date.getFormat()) ? true
                : ((changeFormat(date.getFormat()) != null) ? true : false);

        return resultExactness && resultFormat;
    }

    private static boolean isNormalKeyword(String keyword) {
    	return KeyWords.hasKeyword(keyword, KeyWords.ARFF_KEYWORDS);
    }

    private static String getNormalKeyword(String keyword){
    	String normalKeyword = null;
    	int prio = KeyWords.getKeywordPriority(keyword);
    	switch(prio){
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
        return tagName.equalsIgnoreCase("TD")
        || tagName.equalsIgnoreCase("OPTION")
        || tagName.equalsIgnoreCase("CITE")
        || tagName.equalsIgnoreCase("A")
        || tagName.equalsIgnoreCase("B")
        || tagName.equalsIgnoreCase("DT")
        || tagName.equalsIgnoreCase("I")
        || tagName.equalsIgnoreCase("U")
        || tagName.equalsIgnoreCase("DL")
        || tagName.equalsIgnoreCase("PRE")
        || tagName.equalsIgnoreCase("P")
        || tagName.equalsIgnoreCase("DD")
        || tagName.equalsIgnoreCase("CENTER")
        || tagName.equalsIgnoreCase("ABBR")
        || tagName.equalsIgnoreCase("CODE")
        || tagName.equalsIgnoreCase("TIME")
        || tagName.equalsIgnoreCase("SMALL")
        || tagName.equalsIgnoreCase("ADDRESS")
        || tagName.equalsIgnoreCase("PAUSING")
        || tagName.equalsIgnoreCase("STRONG")
        || tagName.equalsIgnoreCase("SPAN")
        || tagName.equalsIgnoreCase("NOBR")
        || tagName.equalsIgnoreCase("H6")
        || tagName.equalsIgnoreCase("H5")
        || tagName.equalsIgnoreCase("H4")
        || tagName.equalsIgnoreCase("EM")
        || tagName.equalsIgnoreCase("EDITDATE")
        || tagName.equalsIgnoreCase("FONT")
        || tagName.equalsIgnoreCase("H3")
        || tagName.equalsIgnoreCase("H2")
        || tagName.equalsIgnoreCase("BODY")
        || tagName.equalsIgnoreCase("H1")
        || tagName.equalsIgnoreCase("DIV")
        || tagName.equalsIgnoreCase("LI")
        || tagName.equalsIgnoreCase("HTML:ABBR")
        || tagName.equalsIgnoreCase("BLOCKQUOTE")
        || tagName.equalsIgnoreCase("TITLE");
    }

    private static boolean isNormalFormat(String format) {
        return format.equals("MM/DD/YYYY")
        || format.equals("MM/DD/YYYY HH:MM:SS +UTC")
        || format.equals("DD. MMMM YYYY")
        || format.equals("DD.MM.YYYY") || format.equals("MMMM-DD-YYYY")
        || format.equals("YYYYDDD")
        || format.equals("YYYY-MM-DDTHH:MM:SS+HH:MM")
        || format.equals("YYYY-MM-DD")
        || format.equals("MMMM DD, YYYY") || format.equals("YYYYMMDD")
        || format.equals("WD MMM DD_1 HH:MM:SS YYYY")
        || format.equals("MMMM DD, YYYY YYYY HH:MM:SS +UTC")
        || format.equals("YYYY_MM_DD")
        || format.equals("WD, DD MMM YYYY HH:MM:SS +UTC")
        || format.equals("YYYY-MMM-D")
        || format.equals("DD. MMMM YYYY HH:MM:SS +UTC")
        || format.equals("DD.MM.YYYY HH:MM:SS +UTC")
        || format.equals("YYYY-DDD");
    }

    private static String changeFormat(String format) {
        String result = null;
        if (format.equals("YYYY-DDDTHH:MM:SS+HH:MM")
                || format.equals("YYYY-WW-DTHH:MM:SS+HH:MM")) {
            result = "YYYY-MM-DDTHH:MM:SS+HH:MM";
        } else if (format.equals("WWD, DD-MMM-YY HH:MM:SS TZ")
                || format.equals("WD, DD MMM YYYY HH:MM:SS TZ")
                || format.equals("WD MMM DD_1 HH:MM:SS YYYY +UTC")
                || format.equals("WWD, DD-MMM-YY HH:MM:SS +UTC")) {
            result = "WD, DD MMM YYYY HH:MM:SS +UTC";
        } else if (format.equals("YYYY-WW-D")) {
            result = "YYYY-MM-DD";
        }
        return result;
    }

}
