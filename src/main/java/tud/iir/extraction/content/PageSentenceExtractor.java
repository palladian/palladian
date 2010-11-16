package tud.iir.extraction.content;

import java.util.List;

import org.w3c.dom.Document;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.Tokenizer;
import tud.iir.web.Crawler;

/**
 * The PageSentenceExtractor extracts clean sentences from (English) texts. That is, short phrases are not included in
 * the output. Consider the {@link PageContentExtractor} for general content. The main difference is that this class
 * also finds sentences in comment sections of web pages.
 * 
 * @author David Urbansky
 * 
 */
public class PageSentenceExtractor {

    public static List<String> getSentences(String url) {

        Crawler crawler = new Crawler();
        Document webPage = crawler.getWebDocument(url);

        String content = PageAnalyzer.getReadableTextDump(webPage);

        List<String> sentences = Tokenizer.getSentences(content, true);

        // CollectionHelper.print(sentences);

        return sentences;
    }

    public static String getText(String url) {
        StringBuilder text = new StringBuilder();
        List<String> sentences = getSentences(url);

        for (String string : sentences) {
            text.append(string).append(" ");
        }

        return text.toString();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // String url = "http://lifehacker.com/5690722/why-you-shouldnt-switch-your-email-to-facebook";
        String url = "http://stackoverflow.com/questions/2670082/web-crawler-that-can-interpret-javascript";
        System.out.println("SentenceExtractor: " + PageSentenceExtractor.getText(url));
        System.out.println("ContentExtractor:  " + new PageContentExtractor().getResultText(url));

    }

}