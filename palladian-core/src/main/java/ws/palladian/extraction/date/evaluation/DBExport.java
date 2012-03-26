package ws.palladian.extraction.date.evaluation;


public class DBExport {
	private String url = "";
	private String filePath = "";
	private String pubDate = "";
	private String modDate ="";
	private boolean pubSureness = false;
	private boolean modSureness = false;
	private String googleDate = "";
	private String hakiaDate = "";
	private String askDate = "";
	private String lastModDate = "";
	private String dateDate = "";
	private boolean downloaded = false;
	private String actDate = "";
	
	public static final int URL = 0;
	public static final int PATH = 1;
	public static final int PUB_DATE = 2;
	public static final int MOD_DATE = 3;
	public static final int PUB_SURE = 4;
	public static final int MOD_SURE = 5;
	public static final int GOOGLE = 6;
	public static final int HAKIA = 7;
	public static final int ASK = 8;
	public static final int HEADER_LAST = 9;
	public static final int HEADER_DATE = 10;
	public static final int DOWNLOADED = 11;
	public static final int ACTUAL_DATE = 12;
	
	
	public DBExport(String url){
		this.url = url;
	}
	
	public DBExport(String url, String pubDate, String modDate, boolean pubSureness, boolean modSureness){
		this.url = url;
		this.pubDate = pubDate;
		this.modDate = modDate;
		this.pubSureness = pubSureness;
		this.modSureness = modSureness;
	}
	
	public DBExport(String url, String filePath, String pubDate, String modDate, boolean pubSureness, boolean modSureness, String googleDate,
				String hakiaDate, String askDAte, String lastModDate, String dateDate, String actDate){
		this.url = url;
		this.filePath = filePath;
		this.pubDate = pubDate;
		this.modDate = modDate;
		this.pubSureness = pubSureness;
		this.modSureness = modSureness;
		this.googleDate = googleDate;
		this.hakiaDate = hakiaDate;
		this.askDate = askDAte;
		this.lastModDate = lastModDate;
		this.dateDate = dateDate;
		this.actDate = actDate;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	public String getPubDate() {
		return pubDate;
	}
	public void setModDate(String modDate) {
		this.modDate = modDate;
	}
	public String getModDate() {
		return modDate;
	}
	public void setPubSureness(boolean pubSureness) {
		this.pubSureness = pubSureness;
	}
	public boolean isPubSureness() {
		return pubSureness;
	}
	public void setModSureness(boolean modSureness) {
		this.modSureness = modSureness;
	}
	public boolean isModSureness() {
		return modSureness;
	}
	
	public void setDateDate(String dateDate) {
		this.dateDate = dateDate;
	}
	public String getDateDate() {
		return dateDate;
	}
	public void setLastModDate(String lastModDate) {
		this.lastModDate = lastModDate;
	}
	public String getLastModDate() {
		return lastModDate;
	}
	public void setAskDate(String askDate) {
		this.askDate = askDate;
	}
	public String getAskDate() {
		return askDate;
	}
	public void setHakiaDate(String hakiaDate) {
		this.hakiaDate = hakiaDate;
	}
	public String getHakiaDate() {
		return hakiaDate;
	}
	public void setGoogleDate(String googleDate) {
		this.googleDate = googleDate;
	}
	public String getGoogleDate() {
		return googleDate;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	public boolean isDownloaded() {
		return downloaded;
	}

	public void setActDate(String actDate) {
		this.actDate = actDate;
	}

	public String getActDate() {
		return actDate;
	}
	
	public String get(int field){
		String value = ""; 
		switch(field){
		case URL: 
			value = url;
			break;
		case PATH:
			value = filePath;
			break;
		case PUB_DATE:
			value = pubDate;
			break;
		case MOD_DATE:
			value = modDate;
			break;
		case PUB_SURE:
			value = String.valueOf(pubSureness);
			break;
		case MOD_SURE:
			value = String.valueOf(modSureness);
			break;
		case GOOGLE:
			value = googleDate;
			break;
		case HAKIA:
			value = hakiaDate;
			break;
		case ASK:
			value = askDate;
			break;
		case HEADER_LAST:
			value = lastModDate;
			break;
		case HEADER_DATE:
			value = dateDate;
			break;
		case DOWNLOADED:
			value = String.valueOf(downloaded);
			break;
		case ACTUAL_DATE:
			value = actDate;
			break;
		}
		return value;
		
	}
	@Override
    public String toString(){
		String separator = EvaluationHelper.SEPARATOR;
		String write =getUrl() + separator
		+ getFilePath() + separator
		+ getPubDate() + separator
		+ String.valueOf(isPubSureness()) + separator
		+ getModDate() + separator
		+ String.valueOf(isModSureness()) + separator
		+ getGoogleDate() + separator
		+ getHakiaDate() + separator
		+ getAskDate() + separator
		+ getLastModDate() + separator
		+ getDateDate() + separator
		+ getActDate();
				
		return write;
	}
}
