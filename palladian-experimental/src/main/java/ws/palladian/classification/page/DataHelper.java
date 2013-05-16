package ws.palladian.classification.page;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.retrieval.DocumentRetriever;

final class DataHelper {

    private static final String XML_PART_NAME = "part";
    private static final String URL_PART_NAME = "urls";

    // private static final int MAX_URLS = 2446790;

    /**
     * Break the content.rdf.u8 in several small parts while keeping the XML correct.
     */
    public void breakODPFile() {
        int linesPerFile = 100000;

        String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><RDF xmlns:r=\"http://www.w3.org/TR/RDF/\" xmlns:d=\"http://purl.org/dc/elements/1.0/\" xmlns=\"http://dmoz.org/rdf/\">\n";
        String filePath = "data/temp/odp/content.rdf.u8";

        try {
            FileReader in = new FileReader(filePath);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            int lineNumber = 1;
            StringBuilder filePart = new StringBuilder();
            boolean waitToBreak = false;
            int fileCount = 0;
            do {
                line = br.readLine();
                if (line == null) {
                    break;
                }

                filePart.append(line).append("\n");

                if (lineNumber > 0 && lineNumber % linesPerFile == 0 || waitToBreak) {
                    waitToBreak = true;

                    if (line.endsWith("</Topic>") || line.endsWith("</ExternalPage>")) {
                        filePart.append("</RDF>");
                        FileHelper.writeToFile("data/temp/odp/" + XML_PART_NAME + fileCount++ + ".xml", filePart);
                        System.out.println("save file number " + fileCount);
                        filePart = new StringBuilder(head);
                        waitToBreak = false;
                    }

                }

                lineNumber++;

            } while (line != null);

            FileHelper.writeToFile("data/temp/odp/" + XML_PART_NAME + fileCount + ".xml", filePart);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    /**
     * Merge URL files to one big file.
     */
    public void mergeURLFiles() {

        try {
            FileWriter fileWriter = new FileWriter("data/temp/odp/list.txt");

            File[] files = FileHelper.getFiles("data/temp/odp/", URL_PART_NAME);
            for (File file : files) {
                fileWriter.write(FileHelper.readFileToString("data/temp/odp/" + file.getName()));
                fileWriter.flush();
            }

            fileWriter.close();

        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

    }

    /**
     * Read ODP RDF dump and create a file for classification. Format: URL Class1/Class2[/ClassX]* Save URLs in several files that can be merged later.
     * 
     * @param language If set to something other than "english", the Top/World/X categories are used.
     */
    public void parseODP(String language) {

        long t1 = System.currentTimeMillis();

        StringBuilder transformedString = new StringBuilder();
        DocumentRetriever c = new DocumentRetriever();

        boolean useTopWorldCategories = false;
        if (!language.equalsIgnoreCase("english")) {
            useTopWorldCategories = true;
        }

        int count = 1;
        int fileLines = 0;
        int urlFileCount = 0;

        // iterate over all chunk files in ODP directory
        File[] files = FileHelper.getFiles("data/temp/odp/", XML_PART_NAME);
        for (File file : files) {

            Document documentPart = c.getWebDocument("data/temp/odp/" + file.getName());

            // get all topic tags with their links
            List<Node> topicNodes = XPathHelper.getNodes(documentPart, "//TOPIC");

            for (Node topicNode : topicNodes) {
                if (topicNode.getAttributes().getNamedItem("r:id") != null) {

                    String categoryString = topicNode.getAttributes().getNamedItem("r:id").getTextContent();
                    String[] categories = categoryString.split("/");

                    if (categories.length < 2) {
                        continue;
                    }

                    // add to file if the language is english and category not world or category is world and language matches
                    if (!categories[1].equalsIgnoreCase("world") && !useTopWorldCategories
                            || useTopWorldCategories && categories.length > 2 && categories[2].equalsIgnoreCase(language)) {

                        // remove "Top" category
                        categoryString = categoryString.substring(4);

                        if (categoryString.startsWith("World")) {
                            categoryString = categoryString.substring(6);
                            if (categoryString.toLowerCase().startsWith(language.toLowerCase())) {
                                if (categoryString.length() <= language.length() + 1) {
                                    continue;
                                }
                                categoryString = categoryString.substring(language.length() + 1);
                            }
                        }

                        // get all links from the topic
                        Node linkNode = topicNode.getNextSibling();
                        while (true) {

                            linkNode = linkNode.getNextSibling();
                            // System.out.println(linkNode.getNodeName()+","+linkNode.getNodeType());
                            if (linkNode == null || linkNode.getNodeType() != Node.TEXT_NODE && !linkNode.getNodeName().equals("LINK")) {
                                break;
                            }

                            if (linkNode.getNodeType() == Node.TEXT_NODE) {
                                continue;
                            }

                            if (linkNode.getAttributes().getNamedItem("r:resource") != null) {
                                String link = linkNode.getAttributes().getNamedItem("r:resource").getTextContent();
                                link = UrlHelper.getCleanUrl(link);
                                transformedString.append(link).append(" ").append(categoryString).append("\n");
                                fileLines++;
                                // System.out.println(fileLines);

                                if (fileLines > 0 && fileLines % 50000 == 0) {
                                    FileHelper.writeToFile("data/temp/odp/" + URL_PART_NAME + urlFileCount++ + ".txt", transformedString);
                                    transformedString = new StringBuilder();
                                }
                                // System.out.println(link + " " + categoryString);
                                // System.out.print(".");
                            }
                        }
                    }
                }
            }

            System.out.println("loaded document number " + count + " of " + files.length + " / " + fileLines + " lines");
            count++;
        }

        FileHelper.writeToFile("data/temp/odp/" + URL_PART_NAME + urlFileCount++ + ".txt", transformedString);
        // FileHelper.writeToFile("data/benchmarkSelection/page/" + DateHelper.getCurrentDatetime() + "_odp.txt", transformedString);

        mergeURLFiles();
        DateHelper.formatDuration(t1);
    }

    /**
     * Create a random sample of URLs.
     * 
     * @param sourceFile The file with the URLs to sample from.
     * @param sampleSize The size of the sample.
     */
    public void createRandomSample(String sourceFile, int sampleSize) {

        int maximumLineNumber = FileHelper.getNumberOfLines("data/temp/odp/" + sourceFile);

        StringBuilder sampleFile = new StringBuilder();
        HashSet<Integer> randomNumbers = new HashSet<Integer>();

        while (randomNumbers.size() < sampleSize) {
            Integer lineNumber = (int) (Math.random() * maximumLineNumber) + 1;
            randomNumbers.add(lineNumber);
        }

        final Object[] obj = new Object[2];
        obj[0] = sampleFile;
        obj[1] = randomNumbers;

        LineAction la = new LineAction() {

            @SuppressWarnings("unchecked")
            @Override
            public void performAction(String line, int lineNumber) {

                if (((HashSet<Integer>) obj[1]).contains(lineNumber)) {
                    ((StringBuilder) obj[0]).append(line).append("\n");
                }

            }
        };
        FileHelper.performActionOnEveryLine("data/temp/odp/" + sourceFile, la);

        // FileHelper.writeToFile("data/temp/odp/" + FileHelper.getFilePath(sourceFile) + "_sample" + sampleSize +
        // ".txt", sampleFile);
        FileHelper.writeToFile("data/temp/odp/" + FileHelper.appendToFileName(sourceFile, "_sample" + sampleSize),
                sampleFile);
    }

    /**
     * Delete helper files.
     * 
     * @param deleteXMLParts If true, the xml parts will be deleted as well.
     */
    public void cleanup(boolean deleteXMLParts) {
        if (deleteXMLParts) {
            File[] files = FileHelper.getFiles("data/temp/odp/", XML_PART_NAME);
            for (File file : files) {
                file.delete();
            }
        }
        File[] files2 = FileHelper.getFiles("data/temp/odp/", URL_PART_NAME);
        for (File file : files2) {
            file.delete();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        DataHelper dh = new DataHelper();
        // dh.breakODPFile();
        // dh.parseODP("deutsch");
        dh.createRandomSample("list_german.txt", 20000);
        dh.cleanup(false);
    }

}