/**
 * 
 */
package ws.palladian.daterecognition.dates;

/**
 * @author Martin Gregor
 * 
 */
public class ContentDate extends AbstractBodyDate {

	private DateType dateType = DateType.ContentDate;
	
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

    /**
     * Related Structure-Date for this Content-Date.
     */
    private StructureDate structureDate = null;
    
    private boolean hasStrucutreDate = false;
    private boolean inMetaDates = false;
    private boolean inUrl = false;

    
    /**
     * 
     */
    public ContentDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public ContentDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public ContentDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
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
        return super.toString() + " Position in doc: " + positionInDocument + " Position in tag: " + positionInTagtext
                + "\n" + " Distance date-keyword: " + distanceToContext + " Keyword-location: " + getKeyLocToString();
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
	
	public String getStructureDateString(){
		String string = "no structure date";
		if(this.structureDate != null){
			string = this.structureDate.getNormalizedDateString();
		}
		return string;
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



}
