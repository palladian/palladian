package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>This class converts various datasets to our Palladian internal format.</p>
 * 
 * @author Philipp Katz
 * 
 */
public class DatasetWriter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetWriter.class);

    public static void main(String[] args) {

        // createCiteULike(
        //        "/home/pk/PalladianData/datasets/KeyphraseExtraction/citeulike180/taggers",
        //        "/home/pk/PalladianData/datasets/KeyphraseExtraction/citeulike180/citeulike180index.txt");
        // createFAO("/Users/pk/temp/fao780", "/Users/pk/temp/fao780.txt");
        
        
        
        // createDeliciousT140("/home/pk/DATASETS/delicioust140", "/home/pk/temp/deliciousT140");
        createDeliciousT140("/Users/pk/Studium/Diplomarbeit/delicioust140", "/Users/pk/temp/deliciousT140");

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
        final int MINIMUM_USERS = 50;
        final float MINIMUM_USER_TAG_RATIO = 0.05f;
        final String SEPARATOR_CHARACTER = " ";
        final Pattern TAG_MATCH_PATTERN = Pattern.compile("[a-z0-9\\-\\.\\+\\#]+"); // we are not interested in tags with foreign characters


        final String pathToIndexFile = pathToResultDirectory + "/deliciousT140index.txt"; // index file to create
        final String pathToDocsSubdirectory = pathToResultDirectory + "/docs/"; // subdirectory where to place the txt files

        // clean up in advance ...
        if (FileHelper.fileExists(pathToIndexFile)) {
            FileHelper.delete(pathToIndexFile);
        }
        if (FileHelper.directoryExists(pathToDocsSubdirectory)) {
            FileHelper.delete(pathToDocsSubdirectory);
        }

        final MutableInt parseCounter = new MutableInt();
        final MutableInt acceptCounter = new MutableInt();
        
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
                        filename = getText();
                    } else if (qName.equals("filetype")) {
                        filetype = getText();
                    } else if (qName.equals("users")) {
                        users = Integer.parseInt(getText());
                    } else if (qName.equals("name")) {
                        currentTag = getText();
                    } else if (qName.equals("weight")) {
                        currentWeight = Integer.parseInt(getText());
                    } else if (qName.equals("tag")) {
                        boolean accept = (float) currentWeight / users >= MINIMUM_USER_TAG_RATIO;
                        accept = accept && TAG_MATCH_PATTERN.matcher(currentTag).matches();
                        if (accept) {
                            tags.add(currentTag);
                        }
                    } else if (qName.equals("document")) {
                        writeEntry();
                        tags.clear();
                        parseCounter.increment();
                    }
                }

                private void writeEntry() throws SAXException {

                    String pathToSubdirectory = filename.substring(0, 2) + "/" + filename;
                    String pathToHtmlFile = pathToDatasetDirectory + "/fdocuments/" + pathToSubdirectory;

                    boolean accept = filetype.equals("html");
                    accept = accept && users >= MINIMUM_USERS;
                    accept = accept && !tags.isEmpty();
                    accept = accept && !(new File(pathToHtmlFile).length() > 60000);

                    if (!accept) {
                        return;
                    }

                    // parse the HTML file
                    String content = FileHelper.readFileToString(pathToHtmlFile);
                    String cleanContent = HtmlHelper.documentToReadableText(content, false);
                    
                    if (cleanContent.length() < 100) {
                        return;
                    }
                    
                    // //////////// write line to the index file ///////////////////
                    StringBuilder lineToWrite = new StringBuilder();
                    lineToWrite.append(pathToSubdirectory.replace(".html", ".txt")).append(SEPARATOR_CHARACTER);
                    lineToWrite.append(StringUtils.join(tags, SEPARATOR_CHARACTER));
                    lineToWrite.append("\n");

                    FileHelper.appendFile(pathToIndexFile, lineToWrite);

                    // //////////// create .txt files from HTML pages ///////////////
                    FileHelper.writeToFile(pathToDocsSubdirectory + pathToSubdirectory.replace(".html", ".txt"), cleanContent);
                    
                    acceptCounter.increment();
                    
                    if (acceptCounter.intValue() % 1000 == 0) {
                        LOGGER.info("wrote " + acceptCounter + " entries; parsed " + parseCounter + " entries.");
                    }

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

        LOGGER.info("done. wrote " + acceptCounter + " lines to " + pathToIndexFile);

    }

}
