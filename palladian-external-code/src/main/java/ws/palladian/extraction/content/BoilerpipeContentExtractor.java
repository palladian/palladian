package ws.palladian.extraction.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * <p>
 * Boilerpipe, as described in "Boilerplate Detection using Shallow Text Features"; Kohlschütter, Christian; Fankhauser,
 * Peter; Nejdl, Wolfgang; 2010.
 * </p>
 * 
 * @see http://code.google.com/p/boilerpipe/ and
 * @see http://www.l3s.de/~kohlschuetter/boilerplate/
 * 
 * @author Ruchit Beri
 * @author Philipp Katz
 * 
 */
public class BoilerpipeContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BoilerpipeContentExtractor.class);

    // private final ExtractorBase extractor;
    // private TextDocument textDocument;
    //
    // public BoilerpipeContentExtractor() {
    // this(ArticleExtractor.INSTANCE);
    // }
    //
    // public BoilerpipeContentExtractor(ExtractorBase extractor) {
    // this.extractor = extractor;
    // }
    //
    // @Override
    // public WebPageContentExtractor setDocument(File file) throws PageContentExtractorException {
    // try {
    // InputSource inputSource = new InputSource(new FileInputStream(file));
    // setDocument(inputSource);
    // } catch (FileNotFoundException e) {
    // throw new PageContentExtractorException(e);
    // }
    // return this;
    // }
    //
    // @Override
    // public BoilerpipeContentExtractor setDocument(Document document) throws PageContentExtractorException {
    // StringReader stringReader = new StringReader(HtmlHelper.xmlToString(document, false));
    // InputSource inputSource = new InputSource(stringReader);
    // setDocument(inputSource);
    // return this;
    // }
    //
    // // TODO pull up?
    // public BoilerpipeContentExtractor setDocument(InputSource inputSource) throws PageContentExtractorException {
    // try {
    // BoilerpipeSAXInput boilerpipeInput = new BoilerpipeSAXInput(inputSource);
    // textDocument = boilerpipeInput.getTextDocument();
    // extractor.process(textDocument);
    // } catch (SAXException e) {
    // throw new PageContentExtractorException(e);
    // } catch (BoilerpipeProcessingException e) {
    // throw new PageContentExtractorException(e);
    // }
    // return this;
    // }
    //
    // @Override
    // public Node getResultNode() {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public String getResultText() {
    // return textDocument.getContent();
    //
    // }
    //
    // @Override
    // public String getResultTitle() {
    // return textDocument.getTitle();
    // }

    @Override
    public String getExtractorName() {
        return "boilerpipe";
    }

    public static void main(String[] args) throws Exception {
        BoilerpipeContentExtractor bpce = new BoilerpipeContentExtractor();
        bpce.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
        // bpce.setDocument("http://www.bbc.co.uk/news/world/europe/");
        LOGGER.info("ResultText: " + bpce.getResultText());
        LOGGER.info("ResultTitle: " + bpce.getResultTitle());
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getResultNode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getResultText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getResultTitle() {
        // TODO Auto-generated method stub
        return null;
    }

}
