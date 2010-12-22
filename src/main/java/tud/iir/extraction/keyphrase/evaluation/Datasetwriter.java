package tud.iir.extraction.keyphrase.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.web.Crawler;

/**
 * This class converts various datasets to our Palladian internal format.
 * 
 * @author Philipp Katz
 * 
 */
public class Datasetwriter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Datasetwriter.class);

    public static void main(String[] args) {

        createCiteULike("/home/pk/PalladianData/datasets/KeyphraseExtraction/citeulike180/taggers", "/home/pk/PalladianData/datasets/KeyphraseExtraction/citeulike180/citeulike180index.txt");
        // createFAO("/Users/pk/temp/fao780", "/Users/pk/temp/fao780.txt");
        // createDeliciousT140("/Users/pk/Studium/Diplomarbeit/delicioust140", "/Users/pk/temp/deliciousT140");

    }

    public static void createCiteULike(String pathToRawFiles, String resultFile) {

        Factory<Bag<String>> factory = new Factory<Bag<String>>() {
            @Override
            public Bag<String> create() {
                return new HashBag<String>();
            }
        };
        Map<Integer, Bag<String>> documentsTags = LazyMap.decorate(new TreeMap<Integer, Bag<String>>(), factory);

        // go through all .tags files and get the tags

        File[] taggerDirectories = FileHelper.getFiles(pathToRawFiles);
        for (File file : taggerDirectories) {

            if (!file.isDirectory()) {
                continue;
            }

            File[] tagFiles = FileHelper.getFiles(file.getPath());
            for (File tagFile : tagFiles) {

                List<String> tags = FileHelper.readFileToArray(tagFile);
                Bag<String> documentTags = documentsTags.get(Integer.valueOf(tagFile.getName().replace(".tags", "")));

                for (String tag : tags) {
                    if (tag.length() > 0) {

                        // some .tag files in the dataset contain junk,
                        // which we filter here
                        if (tag.contains("  ")) {
                            tag = tag.substring(tag.indexOf("  ") + 2, tag.length());
                        }

                        documentTags.add(tag.trim());
                    }
                }
            }
        }

        // write index file
        StringBuilder sb = new StringBuilder();

        Set<Entry<Integer, Bag<String>>> entrySet = documentsTags.entrySet();
        for (Entry<Integer, Bag<String>> entry : entrySet) {
            sb.append(entry.getKey()).append(".txt").append("#");
            sb.append(StringUtils.join(entry.getValue().uniqueSet(), "#"));
            sb.append("\n");
        }

        FileHelper.writeToFile(resultFile, sb);

    }

    public static void createFAO(String pathToRawFiles, String resultFile) {

        File[] files = FileHelper.getFiles(pathToRawFiles, ".key");
        StringBuilder sb = new StringBuilder();

        for (File file : files) {

            String keyFile = file.getName();
            List<String> keywords = FileHelper.readFileToArray(pathToRawFiles + "/" + keyFile);

            sb.append(keyFile.replace(".key", ".txt"));
            sb.append("#");
            sb.append(StringUtils.join(keywords, "#"));
            sb.append("\n");

        }

        FileHelper.writeToFile(resultFile, sb);

    }

    public static void createDeliciousT140(final String pathToDatasetDirectory, final String pathToResultDirectory) {

        // filter settings
        final int minimumUsers = 50;
        final float minimumUserTagRatio = 0.05f;
        // final List<String> filetypes = Arrays.asList("html");
        final String separatorCharacter = " ";

        final Counter counter = new Counter();
        final Crawler crawler = new Crawler();
        crawler.setFeedAutodiscovery(false);

        final String pathToIndexFile = pathToResultDirectory + "/deliciousT140index.txt";

        if (FileHelper.fileExists(pathToIndexFile)) {
            FileHelper.delete(pathToIndexFile);
        }

        try {

            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                private StringBuffer textBuffer = new StringBuffer();
                private boolean catchText = false;

                // data we are interested in
                private String filename;
                private String filetype;
                private int users;
                private Set<String> tags = new HashSet<String>();
                private String currentTag;
                private int currentWeight;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if (qName.equals("filename") || qName.equals("filetype") || qName.equals("users")
                            || qName.equals("name") || qName.equals("weight")) {
                        catchText = true;
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    catchText = false;
                    if (qName.equals("filename")) {
                        this.filename = getText();
                    } else if (qName.equals("filetype")) {
                        this.filetype = getText();
                    } else if (qName.equals("users")) {
                        this.users = Integer.parseInt(getText());
                    } else if (qName.equals("name")) {
                        this.currentTag = getText();
                    } else if (qName.equals("weight")) {
                        this.currentWeight = Integer.parseInt(getText());
                    } else if (qName.equals("tag")) {
                        if ((float) currentWeight / users >= minimumUserTagRatio) {
                            this.tags.add(this.currentTag);
                        }
                    } else if (qName.equals("document")) {
                        // boolean accept = filetypes.isEmpty() || filetypes.contains(filetype);
                        boolean accept = filetype.equals("html");
                        accept = accept && users >= minimumUsers;
                        accept = accept && !tags.isEmpty();

                        if (accept) {

                            counter.increment();

                            if (counter.getCount() % 1000 == 0) {
                                LOGGER.info("wrote " + counter + " lines.");
                            }

                            if (counter.getCount() == 10000) {
                                System.exit(0);
                            }

                            write();

                            // System.out.println(filename + "\t" + users + "\t" + filetype + "\t" + tags);
                            // System.out.println(lineToWrite);
                        }
                        this.tags.clear();
                    }
                }

                private void write() throws SAXException {

                    String pathToHtmlFile = pathToDatasetDirectory + "/fdocuments/" + filename.substring(0, 2) + "/"
                            + filename.replace(".html", ".txt");
                    if (new File(pathToHtmlFile).length() > 60000) {
                        return;
                    }

                    // //////////// write line to the index file ///////////////////

                    StringBuilder lineToWrite = new StringBuilder();
                    lineToWrite.append(filename).append(separatorCharacter);
                    lineToWrite.append(StringUtils.join(tags, separatorCharacter));
                    lineToWrite.append("\n");

                    try {
                        FileHelper.appendFile(pathToIndexFile, lineToWrite);
                    } catch (IOException e) {
                        throw new SAXException("Error writing result file. Stopping.");
                    }

                    // //////////// create .txt files from HTML pages ///////////////

                    // Document doc = crawler.getWebDocument(pathToHtmlFile);
                    // String content = HTMLHelper.htmlToString(doc);

                    String content = FileHelper.readFileToString(pathToHtmlFile);
                    String cleanContent = HTMLHelper.htmlToString(content, false);

                    FileHelper.writeToFile(pathToResultDirectory + "/" + filename.replace(".html", ".txt"),
                            cleanContent);

                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (catchText) {
                        textBuffer.append(ch, start, length);
                    }
                }

                // Get the text, clear Buffer.
                private String getText() {
                    try {
                        return textBuffer.toString();
                    } finally {
                        textBuffer = new StringBuffer();
                    }
                }
            };

            parser.parse(pathToDatasetDirectory + "/taginfo.xml", handler);

        } catch (ParserConfigurationException e) {
            LOGGER.error(e);
        } catch (SAXException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        LOGGER.info("done. wrote " + counter + " lines to " + pathToIndexFile);

    }

}
