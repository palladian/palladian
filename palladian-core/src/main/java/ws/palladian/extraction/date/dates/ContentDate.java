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
    
    private String keyClass = "0";
    private String keyLoc = "0";
    private double keyDiff = 0;
	
    private String simpleTag = "0";
    private String hTag = "0";
	
    private double relCntSame = 0;
	private double relSize = 0;
	
	private long distPosBefore = -1;
	private long distPosAfter = -1;
	private long distAgeBefore = -1;
	private long distAgeAfter = -1;
	
	private String keyLoc201 = "0";
	private String keyLoc202 = "0";
	
	private String isKeyClass1 = "0";
	private String isKeyClass2 = "0";
	private String isKeyClass3 = "0";
    
    
    /**
     * Related Structure-Date for this Content-Date.
     */
    private StructureDate structureDate = null;
    
    private boolean hasStrucutreDate = false;
    private boolean inMetaDates = false;
    private boolean inUrl = false;
   
    public ContentDate(ExtractedDate date) {
        super(date);
    }

    /**
     * Returns location of found keyword as readable string.<br>
     * Field <b>keywordLocation</b> should be set.
     * To set location or get it as int, use get() and set() methods.
     * 
     * @return Attribute or Content if location is set. -1 for undefined location.
     */
    public String getKeyLocToString() {
        String keyPos = String.valueOf(keywordLocation);
        switch (keywordLocation) {
            case KEY_LOC_ATTR:
                keyPos = "Attribute";
                break;
            case KEY_LOC_CONTENT:
                keyPos = "Content";
                break;

        }
        return keyPos;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(", positionInDocument=").append(positionInDocument);
        builder.append(", positionInTagtext=").append(positionInTagtext);
        builder.append(", distanceToContext=").append(distanceToContext);
        builder.append(", keyLoc=").append(getKeyLocToString());
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

	public void setStructureDate(StructureDate structureDate) {
		this.structureDate = structureDate;
		if(structureDate != null){
			this.hasStrucutreDate = true;
		}
	}

	public StructureDate getStructureDate() {
		return structureDate;
	}

	public void setHasStrucutreDate(boolean hasStrucutreDate) {
		this.hasStrucutreDate = hasStrucutreDate;
	}

	public boolean hasStrucutreDate() {
		return hasStrucutreDate;
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

	public void setKeyClass(String keyClass) {
		this.keyClass = keyClass;
	}

	public String getKeyClass() {
		return keyClass;
	}

	public void setKeyLoc(String keyLoc) {
		this.keyLoc = keyLoc;
	}

	public String getKeyLoc() {
		return keyLoc;
	}

	public void setKeyDiff(double keyDiff) {
		this.keyDiff = keyDiff;
	}

	public double getKeyDiff() {
		return keyDiff;
	}

	public void setSimpleTag(String simpleTag) {
		this.simpleTag = simpleTag;
	}

	public String getSimpleTag() {
		return simpleTag;
	}

	public void sethTag(String hTag) {
		this.hTag = hTag;
	}

	public String gethTag() {
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

	public void setIsKeyClass1(String isKeyClass1) {
		this.isKeyClass1 = isKeyClass1;
	}

	public String getIsKeyClass1() {
		return isKeyClass1;
	}

	public void setKeyLoc202(String keyLoc202) {
		this.keyLoc202 = keyLoc202;
	}

	public String getKeyLoc202() {
		return keyLoc202;
	}

	public void setIsKeyClass2(String isKeyClass2) {
		this.isKeyClass2 = isKeyClass2;
	}

	public String getIsKeyClass2() {
		return isKeyClass2;
	}

	public void setKeyLoc201(String keyLoc201) {
		this.keyLoc201 = keyLoc201;
	}

	public String getKeyLoc201() {
		return keyLoc201;
	}

	public void setIsKeyClass3(String isKeyClass3) {
		this.isKeyClass3 = isKeyClass3;
	}

	public String getIsKeyClass3() {
		return isKeyClass3;
	}

	public void setDistAgeAfter(long distAgeAfter) {
		this.distAgeAfter = distAgeAfter;
	}

	public long getDistAgeAfter() {
		return distAgeAfter;
	}



}
