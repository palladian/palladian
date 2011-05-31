package ws.palladian.retrieval.parser;

public class ParserFactory {
    
    public DocumentParser createHtmlParser() {
        return new NekoHtmlParser();
    }
    
    public DocumentParser createXmlParser() {
        return new XmlParser();
    }

}
