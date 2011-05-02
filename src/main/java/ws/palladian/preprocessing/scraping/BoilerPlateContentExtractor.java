package ws.palladian.preprocessing.scraping;

import java.io.StringReader;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.retrieval.DocumentRetriever;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

public class BoilerPlateContentExtractor extends WebPageContentExtractor {
	
	 /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BoilerPlateContentExtractor.class);
    
    private Document document;
    private TextDocument textDocument;
    //private Node resultNode;

    //private String mainContentHTML = "";

    public BoilerPlateContentExtractor() {
    crawler = new DocumentRetriever();
    }
    
    @Override
    public BoilerPlateContentExtractor setDocument(String url) throws PageContentExtractorException {
   	
    	crawler.setFeedAutodiscovery(false);
    	
        setDocument(crawler.getWebDocument(url));
    	
		StringReader stringReader = new StringReader(HTMLHelper.getXmlDump(document));
		final InputSource is = new InputSource(stringReader);
		
		
		try {
			final BoilerpipeSAXInput in = new BoilerpipeSAXInput(is);
			
			 textDocument = in.getTextDocument();
		} catch (SAXException e) {
			throw new PageContentExtractorException(e);

		} catch (BoilerpipeProcessingException e) {
			throw new PageContentExtractorException(e);

		}

		//this.textDocument = textDocument;
		return this;


    }
   
	@Override
	public BoilerPlateContentExtractor setDocument(Document document)
			throws PageContentExtractorException {
		
		this.document = document;
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
		
		String resultText = textDocument.getText(true, true);
		
		return resultText;
	}

	@Override
	public String getResultTitle() throws PageContentExtractorException {
		// TODO Auto-generated method stub
				
	    String resultTitle = "";
	    resultTitle=textDocument.getTitle();
	    return resultTitle;

	}


    public static void main(String[] args) throws Exception {
    	
    	  BoilerPlateContentExtractor bp = new BoilerPlateContentExtractor();
          bp.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
          System.out.println("ResultText: " + bp.getResultText());
          System.out.println("ResultTitle: " +bp.getResultTitle());

    }

	
}
