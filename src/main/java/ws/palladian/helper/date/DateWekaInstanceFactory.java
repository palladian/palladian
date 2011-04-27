package ws.palladian.helper.date;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.technique.PageDateType;

/**
 * Creates an instance for WEKA classifier out of a ContentDate.
 * 
 * @author Martin Gregor
 * 
 */
public class DateWekaInstanceFactory {

	private PageDateType pageDateType;

	private Attribute classIndex;

	private Attribute hour;
	private Attribute minute;
	private Attribute second;

	private Attribute relDocPos;
	private Attribute ordDocPos;
	private Attribute ordAgePos;

	private Attribute keyClass;
	private Attribute keyLoc;
	private Attribute keyDiff;

	private Attribute simpleTag;
	private Attribute hTag;
	private Attribute tagName;

	private Attribute hasStructureDate;
	private Attribute inMetaDates;
	private Attribute inUrl;

	private Attribute relCntSame;
	private Attribute relSize;

	private Attribute distPosBefore;
	private Attribute distPosAfter;
	private Attribute distAgeBefore;
	// private Attribute distAgeAfter = new private Attribute("distAgeAfter");

	private Attribute format;
	private Attribute keyword;
	private Attribute excatness;

	private Attribute keyLoc201;
	private Attribute keyLoc202;

	private Attribute isKeyClass1;
	private Attribute isKeyClass2;
	private Attribute isKeyClass3;

	public DateWekaInstanceFactory(PageDateType pageDateType) {
		this.pageDateType = pageDateType;
		setAttributes();
	}

	public Instances getDateInstanceByArffTemplate(ContentDate date) {
		Instances instances = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					"data/wekaclassifier/template.arff"));

			File file = new File("data/wekaclassifier/temp.arff");
			FileWriter fileWriter = new FileWriter(file, false);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			String line;
			while ((line = reader.readLine()) != null) {
				bufferedWriter.write(line + "\n");
			}
			bufferedWriter.write("0," + datefeaturesToString(date));
			bufferedWriter.close();
			fileWriter.close();
			reader.close();
			reader = new BufferedReader(new FileReader(file));
			instances = new Instances(reader);
			instances.setClassIndex(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instances;
	}

	private String datefeaturesToString(ContentDate date) {
		String formatString = date.getFormat();
		if (!isNormalFormat(formatString)) {
			formatString = changeFormat(formatString);
		}

		String tagNameString = date.getTag();
		if (!isNormalTag(tagNameString)) {
			tagNameString = "A";
		}

		String keywordString = date.getKeyword();
		if (keywordString != null) {
			keywordString = keywordString.toLowerCase();
		}
		if (!isNormalKeyword(keywordString)) {
			keywordString = "null";
		}

		String hour = (date.get(ContentDate.HOUR) > -1) ? "1" : "0";
		String minute = (date.get(ContentDate.HOUR) > -1) ? "1" : "0";
		String second = (date.get(ContentDate.HOUR) > -1) ? "1" : "0";

		String relDocPos = String.valueOf(date.getRelDocPos());
		String ordDocPos = String.valueOf(date.getOrdDocPos());
		String ordAgePos = String.valueOf(date.getOrdAgePos());

		int keyPrio = KeyWords.getKeywordPriority(date.getKeyword());
		keyPrio = keyPrio == -1 ? 0 : keyPrio;
		String keyClassString = String.valueOf(keyPrio);
		String keyLoc = date.getKeyLoc();
		String keyDiff = String.valueOf(date.getKeyDiff());

		String simpleTag = date.getSimpleTag();
		String hTag = date.gethTag();
		String tagName = tagNameString;

		String hasStructureDate = date.hasStrucutreDate() ? "1" : "0";
		String inMetaDates = date.isInMetaDates() ? "1" : "0";
		String inUrl = date.isInUrl() ? "1" : "0";

		String relCntSame = String.valueOf(date.getRelCntSame());
		String relSize = String.valueOf(date.getRelSize());

		String distPosBefore = String.valueOf(date.getDistPosBefore());
		String distPosAfter = String.valueOf(date.getDistPosAfter());
		String distAgeBefore = String.valueOf(date.getDistAgeAfter());

		// String distAgeBefore = date.getDistAgeBefore();
		// String distAgeAfter = date.getDistAgeAfter();

		//if formatString .index(,) > -1 then ''
		// else if fomratstring.ind (" " > -1 then ''
		String format = formatString;
		if (formatString.indexOf(",") > -1 || formatString.indexOf(" ") > -1){
			format = "'" + formatString + "'" ;
		}
		
		String keyword = (keywordString.indexOf(" ") > -1) ? "'"
				+ keywordString + "'" : keywordString;
		String excatness = String.valueOf(date.getExactness());

		String keyLoc201 = date.getKeyLoc201();
		String keyLoc202 = date.getKeyLoc202();

		String isKeyClass1 = date.getIsKeyClass1();
		String isKeyClass2 = date.getIsKeyClass2();
		String isKeyClass3 = date.getIsKeyClass3();

		String dateString = hour + "," + minute + "," + second + ","
				+ relDocPos + "," + ordDocPos + "," + ordAgePos + ","
				+ keyClassString + "," + keyLoc + "," + keyDiff + ","
				+ simpleTag + "," + hTag + "," + tagName + ","
				+ hasStructureDate + "," + inMetaDates + "," + inUrl + ","
				+ relCntSame + "," + relSize + "," + distPosBefore + ","
				+ distPosAfter + "," + distAgeBefore + "," + format + ","
				+ keyword + "," + excatness + "," + keyLoc201 + "," + keyLoc202
				+ "," + isKeyClass1 + "," + isKeyClass2 + "," + isKeyClass3;
		return dateString;
	}

	public Instance instantiate(ContentDate date) {
		Instance instance = null;

		if (isInstance(date)) {

			String formatString = date.getFormat();
			if (!isNormalFormat(formatString)) {
				formatString = changeFormat(formatString);
			}

			String tagNameString = date.getTag();
			if (!isNormalTag(tagNameString)) {
				tagNameString = "A";
			}

			String keywordString = date.getKeyword();
			if (keywordString != null) {
				keywordString = keywordString.toLowerCase();
			}
			if (!isNormalKeyword(keywordString)) {
				keywordString = "null";
			}

			instance = new Instance(28);
			instance.setDataset(getFeatureSet());

			String hourString = (date.get(ExtractedDate.HOUR) == -1) ? "0"
					: "1";
			String minuteString = date.get(ExtractedDate.MINUTE) == -1 ? "0"
					: "1";
			String secondString = date.get(ExtractedDate.SECOND) == -1 ? "0"
					: "1";

			instance.setValue(hour, String.valueOf(hourString));
			instance.setValue(minute, minuteString);
			instance.setValue(second, secondString);

			instance.setValue(relDocPos, date.getRelDocPos());
			instance.setValue(ordDocPos, date.getOrdDocPos());
			instance.setValue(ordAgePos, date.getOrdAgePos());

			int keyPrio = KeyWords.getKeywordPriority(date.getKeyword());
			keyPrio = keyPrio == -1 ? 0 : keyPrio;
			String keyClassString = String.valueOf(keyPrio);
			instance.setValue(keyClass, keyClassString);
			instance.setValue(keyLoc, date.getKeyLoc());
			instance.setValue(keyDiff, date.getKeyDiff());

			instance.setValue(simpleTag, date.getSimpleTag());
			instance.setValue(hTag, date.gethTag());
			instance.setValue(tagName, tagNameString);

			instance.setValue(hasStructureDate, date.hasStrucutreDate() ? "1"
					: "0");
			instance.setValue(inMetaDates, date.isInMetaDates() ? "1" : "0");
			instance.setValue(inUrl, date.isInUrl() ? "1" : "0");

			instance.setValue(relCntSame, date.getRelCntSame());
			instance.setValue(relSize, date.getRelSize());

			instance.setValue(distPosBefore, date.getDistPosBefore());
			instance.setValue(distPosAfter, date.getDistPosAfter());
			instance.setValue(distAgeBefore, date.getDistAgeAfter());

			// instance.setValue(distAgeBefore, date.getDistAgeBefore());
			// instance.setValue(distAgeAfter, date.getDistAgeAfter());

			instance.setValue(format, formatString);
			instance.setValue(keyword, keywordString);
			instance.setValue(excatness, String.valueOf(date.getExactness()));

			instance.setValue(keyLoc201, date.getKeyLoc201());
			instance.setValue(keyLoc202, date.getKeyLoc202());

			instance.setValue(isKeyClass1, date.getIsKeyClass1());
			instance.setValue(isKeyClass2, date.getIsKeyClass2());
			instance.setValue(isKeyClass3, date.getIsKeyClass3());
		}
		return instance;
	}

	private Instances getFeatureSet() {
		FastVector attributeVec = new FastVector(29);
		attributeVec.addElement(hour);
		attributeVec.addElement(minute);
		attributeVec.addElement(second);

		attributeVec.addElement(relDocPos);
		attributeVec.addElement(ordDocPos);
		attributeVec.addElement(ordAgePos);

		attributeVec.addElement(keyClass);
		attributeVec.addElement(keyLoc);
		attributeVec.addElement(keyDiff);

		attributeVec.addElement(simpleTag);
		attributeVec.addElement(hTag);
		attributeVec.addElement(tagName);

		attributeVec.addElement(hasStructureDate);
		attributeVec.addElement(inMetaDates);
		attributeVec.addElement(inUrl);

		attributeVec.addElement(relCntSame);
		attributeVec.addElement(relSize);

		attributeVec.addElement(distPosBefore);
		attributeVec.addElement(distPosAfter);
		attributeVec.addElement(distAgeBefore);
		// attributeVec.addElement(distAgeAfter);

		attributeVec.addElement(format);
		attributeVec.addElement(keyword);
		attributeVec.addElement(excatness);

		attributeVec.addElement(keyLoc201);
		attributeVec.addElement(keyLoc202);

		attributeVec.addElement(isKeyClass1);
		attributeVec.addElement(isKeyClass2);
		attributeVec.addElement(isKeyClass3);

		attributeVec.addElement(classIndex);

		Instances featureSet = new Instances("featureSet", attributeVec, 0);
		featureSet.setClass(classIndex);

		return featureSet;
	}

	private static FastVector getVector(String numString) {
		String[] nums = numString.split(";");
		FastVector vector = new FastVector(nums.length);
		for (int i = 0; i < nums.length; i++) {
			vector.addElement(nums[i]);
		}
		return vector;
	}

	/** TODO numeric values in richtiger reihenfolge hinzufügen */
	private static FastVector getTagVector() {
		// FastVector vector = new FastVector();
		//		
		// vector.addElement("SPAN");
		// vector.addElement("P");
		// vector.addElement("LI");
		// vector.addElement("FONT");
		// vector.addElement("DIV");
		// vector.addElement("H2");
		// vector.addElement("A");
		// vector.addElement("B");
		// vector.addElement("OPTION");
		// vector.addElement("SMALL");
		// vector.addElement("EM");
		// vector.addElement("H3");
		// vector.addElement("CENTER");
		// vector.addElement("ABBR");
		// vector.addElement("NOBR");
		// vector.addElement("STRONG");
		// vector.addElement("TD");
		// vector.addElement("CITE");
		// vector.addElement("BODY");
		// vector.addElement("I");
		// vector.addElement("PRE");
		// vector.addElement("DD");
		// vector.addElement("DT");
		// vector.addElement("BLOCKQUOTE");
		// vector.addElement("HTML:ABBR");
		// vector.addElement("H4");
		// vector.addElement("H5");
		// vector.addElement("TITLE");
		// vector.addElement("PAUSING");
		// vector.addElement("DL");
		// vector.addElement("U");
		// vector.addElement("ADDRESS");
		// vector.addElement("EDITDATE");
		// vector.addElement("H6");
		// vector.addElement("CODE");
		// vector.addElement("H1");
		// vector.addElement("TIME");
		// return vector;
		return getVector("SPAN;STRONG;P;EM;LI;H5;FONT;DIV;H2;A;B;I;CENTER;OPTION;"
				+ "SMALL;TD;TITLE;H3;ADDRESS;BODY;ABBR;NOBR;EDITDATE;CITE;"
				+ "H6;PRE;H4;CODE;H1;DD;BLOCKQUOTE;DT;TIME;HTML:ABBR;PAUSING;DL;U");
	}

	private static FastVector getFormatVector() {
		// FastVector vector = new FastVector();
		// vector.addElement("MMMM DD, YYYY");
		// vector.addElement("MMMM DD, YYYY YYYY HH:MM:SS +UTC");
		// vector.addElement("MM/DD/YYYY") ;
		// vector.addElement("DD. MMMM YYYY") ;
		// vector.addElement("DD.MM.YYYY") ;
		// vector.addElement("YYYY-MM-DD") ;
		// vector.addElement("YYYY_MM_DD") ;
		// vector.addElement("MM/DD/YYYY HH:MM:SS +UTC") ;
		// vector.addElement("DD. MMMM YYYY HH:MM:SS +UTC");
		// vector.addElement("YYYYMMDD") ;
		// vector.addElement("YYYY-DDD");
		// vector.addElement("WD, DD MMM YYYY HH:MM:SS +UTC") ;
		// vector.addElement("MMMM-DD-YYYY") ;
		// vector.addElement("YYYY-MMM-D") ;
		// vector.addElement("DD.MM.YYYY HH:MM:SS +UTC") ;
		// vector.addElement("YYYY-MM-DDTHH:MM:SS+HH:MM");
		// vector.addElement("YYYYDDD") ;
		// vector.addElement("WD MMM DD_1 HH:MM:SS YYYY") ;
		// return vector;
		return getVector("MMMM DD, YYYY;" + "MMMM DD, YYYY YYYY HH:MM:SS +UTC;"
				+ "MM/DD/YYYY;" + "DD. MMMM YYYY;" + "DD.MM.YYYY;"
				+ "YYYY-MM-DD;" + "YYYY_MM_DD;" + "YYYYMMDD;"
				+ "MM/DD/YYYY HH:MM:SS +UTC;" + "DD. MMMM YYYY HH:MM:SS +UTC;"
				+ "DD.MM.YYYY HH:MM:SS +UTC;" + "MMMM-DD-YYYY;"
				+ "YYYY-MM-DDTHH:MM:SS+HH:MM;" + "YYYY-DDD;"
				+ "WD, DD MMM YYYY HH:MM:SS +UTC;" + "YYYY-MMM-D;"
				+ "WD MMM DD_1 HH:MM:SS YYYY;" + "YYYYDDD");
	}

	private static FastVector getKeywordVector() {
		// FastVector vector = new FastVector();
		// vector.addElement("date");
		// vector.addElement("null");
		// vector.addElement("posted");
		// vector.addElement("update");
		// vector.addElement("release");
		// vector.addElement("added");
		// vector.addElement("updated");
		// vector.addElement("create");
		// vector.addElement("publish");
		// vector.addElement("released");
		// vector.addElement("published");
		// vector.addElement("revised");
		// vector.addElement("created");
		// vector.addElement("pdate");
		// vector.addElement("revise");
		// vector.addElement("last modified");
		// vector.addElement("date-header");
		// vector.addElement("pubdate");
		// vector.addElement("datetime");
		// vector.addElement("geschrieben");
		// return vector;
		return getVector("null;updated;date;created;posted;published;"
				+ "release;update;added;released;last modified;"
				+ "create;publish;pubdate;pdate;revised;"
				+ "date-header;revise;datetime;geschrieben");
	}

	private static boolean isInstance(ContentDate date) {
		boolean resultExactness = date.get(ExtractedDate.EXACTENESS) >= 3 ? true
				: false;
		boolean resultFormat = isNormalFormat(date.getFormat()) ? true
				: ((changeFormat(date.getFormat()) != null) ? true : false);

		return resultExactness && resultFormat;
	}

	private static boolean isNormalKeyword(String keyword) {
		return KeyWords.getKeywordPriority(keyword) == -1 ? false : true;
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

	private void setAttributes() {
		FastVector vector01 = getVector("0;1");

		String typeString;
		if (pageDateType.equals(PageDateType.publish)) {
			typeString = "pub";
		} else {
			typeString = "mod";
		}

		classIndex = new Attribute(typeString, getVector("1;0"));

		hour = new Attribute("hour", vector01);
		minute = new Attribute("minute", vector01);
		second = new Attribute("second", vector01);

		relDocPos = new Attribute("relDocPos");
		ordDocPos = new Attribute("ordDocPos");
		ordAgePos = new Attribute("ordAgePos");

		keyClass = new Attribute("keyClass", getVector("0;2;3;1"));
		keyLoc = new Attribute("keyLoc", getVector("0;2;1"));
		keyDiff = new Attribute("keyDiff");

		simpleTag = new Attribute("simpleTag", vector01);
		hTag = new Attribute("hTag", vector01);
		tagName = new Attribute("tagName", getTagVector());

		hasStructureDate = new Attribute("hasStructureDate", vector01);
		inMetaDates = new Attribute("inMetaDates", vector01);
		inUrl = new Attribute("inUrl", vector01);

		relCntSame = new Attribute("relCntSame");
		relSize = new Attribute("relSize");

		distPosBefore = new Attribute("distPosBefore");
		distPosAfter = new Attribute("distPosAfter");
		distAgeBefore = new Attribute("distAgeBefore");
		// distAgeAfter = new Attribute("distAgeAfter");

		format = new Attribute("format", getFormatVector());
		keyword = new Attribute("keyword", getKeywordVector());
		excatness = new Attribute("excatness", getVector("3;5;6;4"));

		keyLoc201 = new Attribute("keyLoc201", vector01);
		keyLoc202 = new Attribute("keyLoc202", vector01);

		isKeyClass1 = new Attribute("isKeyClass1", vector01);
		isKeyClass2 = new Attribute("isKeyClass2", vector01);
		isKeyClass3 = new Attribute("isKeyClass3", vector01);
	}
}
