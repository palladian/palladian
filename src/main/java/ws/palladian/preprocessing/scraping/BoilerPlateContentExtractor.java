package ws.palladian.preprocessing.scraping;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.retrieval.DocumentRetriever;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.sax.BoilerpipeHTMLContentHandler;

public class BoilerPlateContentExtractor extends WebPageContentExtractor {
	
	 /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BoilerPlateContentExtractor.class);
    
    private Document document;
    //private Node resultNode;

    //private String mainContentHTML = "";

    public BoilerPlateContentExtractor() {
    crawler = new DocumentRetriever();
    }
    
    @Override
    public BoilerPlateContentExtractor setDocument(String url) throws PageContentExtractorException {
        crawler.setFeedAutodiscovery(false);
        return setDocument(crawler.getWebDocument(url));
    	
    }
   
	@Override
	public BoilerPlateContentExtractor setDocument(Document document)
			throws PageContentExtractorException {
		
		this.document = document;
//		String content = document.toString();
//		BoilerpipeHTMLParser parser = new BoilerpipeHTMLParser();
//		parser.parse(content);
        
        return this;
		
	}
	
	 public DocumentRetriever getCrawler() {
	        return crawler;
	    }

	 public void setCrawler(DocumentRetriever crawler) {
	        this.crawler = crawler;
	    }


	 public Document getDocument() {
	        return document;
	    }


	@Override
	public Node getResultNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResultText() {
		// TODO Auto-generated method stub
		
		ExtractorBase boilerplateExtractor = ArticleExtractor.INSTANCE;
		
		
		//ExtractorBase boilerplateExtractor = DefaultExtractor.INSTANCE;
		//String content = boilerplateExtractor.
		
//		String docURI = document.getDocumentURI();
//		String content = crawler.download(docURI);

        //String mainContentHTML = HTMLHelper.documentToHTMLString(document);
        
        String mainContentHTML = HTMLHelper.getXmlDump(document);
        
        String mainContentText=null;
		try {			
			mainContentText = boilerplateExtractor.getText(mainContentHTML);
			//System.out.println(mainContentText);
		} catch (BoilerpipeProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mainContentText;
	}

	@Override
	public String getResultTitle() {
		// TODO Auto-generated method stub
		
		BoilerpipeHTMLContentHandler boilerPlateTitleExtractor = new BoilerpipeHTMLContentHandler();
		String resultTitle = boilerPlateTitleExtractor.getTitle();

        return resultTitle;

	}


    public static void main(String[] args) throws Exception {
    	
    	BoilerPlateContentExtractor bp = new BoilerPlateContentExtractor();
        bp.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
        System.out.println("ResultText: " + bp.getResultText());
        System.out.println("ResultTitle: " +bp.getResultTitle());

    }

	
}
