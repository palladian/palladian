package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;

/**
 * @author Martin Gregor
 * 
 */
public final class ContentDate extends AbstractBodyDate {

    /** Keyword found in attribute of surrounding tag. */
    public static final int KEY_LOC_ATTR = 201;
    /** Keyword found in text (content) of surrounding tag. */
    public static final int KEY_LOC_CONTENT = 202;

    /** Position of datestring in text of found tag. */
    public static final int DATEPOS_IN_TAGTEXT = 201;
    /** Distance between datestring and nearst found keyword. */
    public static final int DISTANCE_DATE_KEYWORD = 202;
    /** Location of keyword. In tagtext (content), atribute or tagname. */
    public static final int KEYWORDLOCATION = 203;
    /** Position of datestring in text of whole document. */
    public static final int DATEPOS_IN_DOC = 204;

    /** Position of datesting in the text of the surrounding tag. */
    private int positionInTagtext = -1;
    /** If a keyword was found in near content, this is the distance between keyword and datestring. */
    private int distanceToContext = -1;
    /** If a keyword was found in content or surrounding tag it will be set to correspond value (see above). */
    private int keywordLocation = -1;
    /** Position of datestring in the document it was found. */
    private int positionInDocument = -1;

    private double relDocPos = 0;
    private double ordDocPos = 0;
    private double ordAgePos = 0;

    private int keyLoc = 0;
    private double keyDiff = 0;

    private boolean simpleTag = false;
    private boolean hTag = false;

    private double relCntSame = 0;
    private double relSize = 0;

    private long distPosBefore = -1;
    private long distPosAfter = -1;
    private long distAgeBefore = -1;
    private long distAgeAfter = -1;

    private boolean keyLoc201 = false;
    private boolean keyLoc202 = false;

    private boolean isKeyClass1 = false;
    private boolean isKeyClass2 = false;
    private boolean isKeyClass3 = false;

    private boolean hasStructureDate = false;
    private boolean inMetaDates = false;
    private boolean inUrl = false;

    public ContentDate(ExtractedDate date) {
        super(date);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(", positionInDocument=").append(positionInDocument);
        builder.append(", positionInTagtext=").append(positionInTagtext);
        builder.append(", distanceToContext=").append(distanceToContext);
        builder.append(", keyLoc=").append(keywordLocation);
        return builder.toString();
    }

    @Override
    public int get(int field) {
        int value;
        switch (field) {
            case DATEPOS_IN_DOC:
                value = this.positionInDocument;
                break;
            case DATEPOS_IN_TAGTEXT:
                value = this.positionInTagtext;
                break;
            case DISTANCE_DATE_KEYWORD:
                value = this.distanceToContext;
                break;
            case KEYWORDLOCATION:
                value = this.keywordLocation;
                break;
            default:
                value = super.get(field);
        }
        return value;

    }

    @Override
    public void set(int field, int value) {
        switch (field) {
            case DATEPOS_IN_DOC:
                this.positionInDocument = value;
                break;
            case DATEPOS_IN_TAGTEXT:
                this.positionInTagtext = value;
                break;
            case DISTANCE_DATE_KEYWORD:
                this.distanceToContext = value;
                break;
            case KEYWORDLOCATION:
                this.keywordLocation = value;
                break;
            default:
                super.set(field, value);

        }

    }

    public void setHasStructureDate(boolean hasStructureDate) {
        this.hasStructureDate = hasStructureDate;
    }

    public boolean hasStructureDate() {
        return hasStructureDate;
    }

    public void setInMetaDates(boolean inMetaDates) {
        this.inMetaDates = inMetaDates;
    }

    public boolean isInMetaDates() {
        return inMetaDates;
    }

    public void setInUrl(boolean inUrl) {
        this.inUrl = inUrl;
    }

    public boolean isInUrl() {
        return inUrl;
    }

    public void setRelDocPos(double relDocPos) {
        this.relDocPos = relDocPos;
    }

    public double getRelDocPos() {
        return relDocPos;
    }

    public void setOrdDocPos(double ordDocPos) {
        this.ordDocPos = ordDocPos;
    }

    public double getOrdDocPos() {
        return ordDocPos;
    }

    public void setOrdAgePos(double ordAgePos) {
        this.ordAgePos = ordAgePos;
    }

    public double getOrdAgePos() {
        return ordAgePos;
    }

    public void setKeyLoc(int keyLoc) {
        this.keyLoc = keyLoc;
    }

    public int getKeyLoc() {
        return keyLoc;
    }

    public void setKeyDiff(double keyDiff) {
        this.keyDiff = keyDiff;
    }

    public double getKeyDiff() {
        return keyDiff;
    }

    public void setSimpleTag(boolean simpleTag) {
        this.simpleTag = simpleTag;
    }

    public boolean isSimpleTag() {
        return simpleTag;
    }

    public void setHTag(boolean hTag) {
        this.hTag = hTag;
    }

    public boolean isHTag() {
        return hTag;
    }

    public void setRelCntSame(double relCntSame) {
        this.relCntSame = relCntSame;
    }

    public double getRelCntSame() {
        return relCntSame;
    }

    public void setRelSize(double relSize) {
        this.relSize = relSize;
    }

    public double getRelSize() {
        return relSize;
    }

    public void setDistPosBefore(int distPosBefore) {
        this.distPosBefore = distPosBefore;
    }

    public long getDistPosBefore() {
        return distPosBefore;
    }

    public void setDistPosAfter(long distPosAfter) {
        this.distPosAfter = distPosAfter;
    }

    public long getDistPosAfter() {
        return distPosAfter;
    }

    public void setDistAgeBefore(long distAgeBefore) {
        this.distAgeBefore = distAgeBefore;
    }

    public long getDistAgeBefore() {
        return distAgeBefore;
    }

    public void setKeyClass1(boolean isKeyClass1) {
        this.isKeyClass1 = isKeyClass1;
    }

    public boolean isKeyClass1() {
        return isKeyClass1;
    }

    public void setKeyLoc202(boolean keyLoc202) {
        this.keyLoc202 = keyLoc202;
    }

    public boolean isKeyLoc202() {
        return keyLoc202;
    }

    public void setKeyClass2(boolean isKeyClass2) {
        this.isKeyClass2 = isKeyClass2;
    }

    public boolean isKeyClass2() {
        return isKeyClass2;
    }

    public void setKeyLoc201(boolean keyLoc201) {
        this.keyLoc201 = keyLoc201;
    }

    public boolean isKeyLoc201() {
        return keyLoc201;
    }

    public void setKeyClass3(boolean isKeyClass3) {
        this.isKeyClass3 = isKeyClass3;
    }

    public boolean isKeyClass3() {
        return isKeyClass3;
    }

    public void setDistAgeAfter(long distAgeAfter) {
        this.distAgeAfter = distAgeAfter;
    }

    public long getDistAgeAfter() {
        return distAgeAfter;
    }

}
