package ws.palladian.retrieval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.parser.ParserFactory;

class ScrapeTldList {
    public static void main(String[] args) throws Exception {
        Set<String> temp = new HashSet<>();
        temp.addAll(extractFromTLDList());
        temp.addAll(extractFromWikipedia());
        temp.addAll(FileHelper.readFileToArray("../palladian-commons/src/main/resources/top-level-domains.txt"));
        temp.addAll(FileHelper.readFileToArray("../palladian-commons/src/main/resources/second-level-domains.txt"));
        List<String> domains = new ArrayList<>(temp);
        domains.sort(Comparator.comparing(s -> {
            List<String> split = Arrays.asList(s.split("\\."));
            Collections.reverse(split);
            return String.join(".", split);
        }));
        domains.add(0, "# last update: " + DateHelper.getCurrentDatetime()
                + " based on https://tld-list.com/tlds-from-a-z, https://en.wikipedia.org/wiki/List_of_Internet_top-level_domains, and hand-curated entries");
        FileHelper.writeToFile("../palladian-commons/src/main/resources/domains.txt", domains);
        System.out.println("Wrote " + domains.size() + " domains");

        // compare();
    }

    protected static List<String> extractFromTLDList() throws Exception {
        String url = "https://tld-list.com/tlds-from-a-z";
        HttpResult result = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
        Document document = ParserFactory.createHtmlParser().parse(result);
        List<Node> nodes = XPathHelper.getXhtmlNodes(document, "//ul[@class=\"feature-list\"]/li/a");
        List<String> domains = nodes.stream().map(node -> node.getTextContent()).collect(Collectors.toList());
        return domains;
    }

    protected static List<String> extractFromWikipedia() throws Exception {
        String url = "https://en.wikipedia.org/wiki/List_of_Internet_top-level_domains";
        HttpResult result = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
        Document document = ParserFactory.createHtmlParser().parse(result);
        List<Node> nodes = XPathHelper.getXhtmlNodes(document,
                "//table[contains(@class,\"wikitable\")]/tbody/tr/td[1]");
        return nodes.stream() //
                .map(node -> node.getTextContent()) //
                .map(String::trim) //
                .map(d -> d.replaceAll("\\[.*\\]", "")) //
                .map(d -> Arrays.asList(d.split(",?\\s"))) //
                .flatMap(i -> i.stream()).filter(d -> d.startsWith(".")) //
                .collect(Collectors.toList());
    }

    protected static void compare() {
        List<String> domains = FileHelper.readFileToArray("../palladian-commons/src/main/resources/domains.txt");
        domains = domains.stream().filter(l -> !l.startsWith("#")).collect(Collectors.toList());

        // compare with previous list
        List<String> oldTLDs = FileHelper
                .readFileToArray("../palladian-commons/src/main/resources/top-level-domains.txt");
        List<String> oldSLDs = FileHelper
                .readFileToArray("../palladian-commons/src/main/resources/second-level-domains.txt");
        List<String> oldAll = new ArrayList<>(oldTLDs);
        oldAll.addAll(oldSLDs);

        Set<String> intersect = CollectionHelper.intersect(new HashSet<>(domains), new HashSet<>(oldAll));

        List<String> missingInOld = new ArrayList<>(domains);
        missingInOld.removeAll(intersect);
        System.out.println("Missing in old list: " + missingInOld.size());

        List<String> missingInNew = new ArrayList<>(oldAll);
        missingInNew.removeAll(intersect);
        System.out.println("Missing in new list: " + missingInNew.size());
        CollectionHelper.print(missingInNew);
    }

}
