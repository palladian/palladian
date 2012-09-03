package de.philippkatz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class PreisroboterQueryScraper {

    private final static long MAX_SLEEP = TimeUnit.MINUTES.toMillis(1);
    private static final String OUTPUT_FILE = "preisroboter_queries.txt";

    public static void main(String[] args) {

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        DocumentParser htmlParser = ParserFactory.createHtmlParser();

        while (true) {

            try {
                
                HttpResult httpResult = httpRetriever.httpGet("http://www.preisroboter.de/livesearch");
                Document document = htmlParser.parse(httpResult);
                
                List<Node> nodes = XPathHelper.getXhtmlNodes(document, "//@title");
                List<String> queries = new ArrayList<String>();
                for (Node node : nodes) {
                    queries.add(node.getTextContent());
                }
                
                appendToFile(OUTPUT_FILE, queries);
                
            } catch (HttpException e) {
                System.out.println("HttpException " + e);
            } catch (ParserException e) {
                System.out.println("ParserException " + e);
            }


            long sleep = (long) (Math.random() * MAX_SLEEP);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }

        }

    }

    private static void appendToFile(String filePath, List<String> lines) {
        StringBuilder linesBuilder = new StringBuilder();
        for (String line : lines) {
            linesBuilder.append(line).append("\n");
        }
        FileHelper.appendFile(filePath, linesBuilder);
    }

}
