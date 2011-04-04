package ws.palladian.daterecognition.technique;

import java.util.ArrayList;

import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.retrieval.DocumentRetriever;

public class MetaDateGetter extends TechniqueDateGetter<MetaDate>{

	private boolean lookHttpDates = true;
	
	private HTTPDateGetter httpDateGetter = new HTTPDateGetter();
	private HeadDateGetter headDateGetter = new HeadDateGetter();
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<MetaDate> getDates() {
		ArrayList<MetaDate> dates = new ArrayList<MetaDate>();
		if(checkDocAndUrl()){
			if(lookHttpDates){
				httpDateGetter.setUrl(this.url);
				dates.addAll(httpDateGetter.getDates());
			}
			headDateGetter.setDocument(this.document);
			dates.addAll(headDateGetter.getDates());
		}
		return dates;
	}

	public void setHttpDateGetter(HTTPDateGetter httpDateGetter) {
		this.httpDateGetter = httpDateGetter;
	}

	public HTTPDateGetter getHttpDateGetter() {
		return httpDateGetter;
	}

	public void setHeadDateGetter(HeadDateGetter headDateGetter) {
		this.headDateGetter = headDateGetter;
	}

	public HeadDateGetter getHeadDateGetter() {
		return headDateGetter;
	}

	private boolean checkDocAndUrl(){
		boolean result;
		if(this.url == null && this.document == null){
			result = false;
		}else {
			result = true;
			if(this.url == null){
				this.url = document.getBaseURI();
			}else if(this.document == null){
				DocumentRetriever crawler = new DocumentRetriever();
				this.document = crawler.getWebDocument(this.url);
			}
		}
		return result;
	}
	
	public void setLookHttpDates(boolean lookHttpDates){
		this.lookHttpDates = lookHttpDates;
	}
	
}
