package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * Reader for the L3S-GN1 Dataset, which was used for the evaluation by Christian Kohlschuetter, Peter Fankhauser,
 * Wolfgang Nejdl; 'Boilerplate Detection using Shallow Text Features', WSDM 2010: Third ACM International Conference on
 * Web Search and Data Mining New York City, NY USA.
 * 
 * @author pk
 */
public final class BoilerpipeDataset implements ContentExtractionDataset {

    public static enum Mode {
        /** Only consider main content as ground truth; i.e. elements within span class='x-nc-sel2'. */
        MAIN_CONTENT("//text()[ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel2']"),
        /**
         * Consider main content and comments as ground truth; i.e. elements within span class='x-nc-sel2' and
         * 'x-nc-sel5'.
         */
        WHOLE_CONTENT(
                "//text()[ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel2' or ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel5']");
        private final String xPath;

        Mode(String xPath) {
            this.xPath = xPath;
        }
    }

    private final File datasetDirectory;
    private final Map<String, String> uuidUrlMapping;
    private final Mode mode;
    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();

    public BoilerpipeDataset(File boilerpipeDatasetDirectory, Mode mode) {
        Validate.notNull(boilerpipeDatasetDirectory, "boilerpipeDatasetDirectory must not be null");
        Validate.notNull(mode, "mode must not be null");
        if (!boilerpipeDatasetDirectory.isDirectory()) {
            throw new IllegalArgumentException(boilerpipeDatasetDirectory + " is not a directory.");
        }
        this.datasetDirectory = boilerpipeDatasetDirectory;
        this.uuidUrlMapping = readFileUrlMapping(boilerpipeDatasetDirectory);
        this.mode = mode;
    }

    /**
     * Read the file "url-mapping.txt", which contains a list of UUIDs with their URLs.
     * 
     * @param boilerpipeDatasetDirectory The path to the dataset directory.
     * @return A {@link Map} with UUIDs as keys, URLs as values.
     */
    private static Map<String, String> readFileUrlMapping(File boilerpipeDatasetDirectory) {
        File urlMappingFile = new File(boilerpipeDatasetDirectory, "url-mapping.txt");
        if (!urlMappingFile.isFile()) {
            throw new IllegalStateException(urlMappingFile + " does not exist.");
        }
        final Map<String, String> mapping = CollectionHelper.newHashMap();
        final Pattern split = Pattern.compile("<urn:uuid:([a-z0-9\\-]*?)>\\s(.*?)");
        FileHelper.performActionOnEveryLine(urlMappingFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                Matcher matcher = split.matcher(line);
                if (matcher.matches() && matcher.groupCount() == 2) {
                    String uuid = matcher.group(1);
                    String url = matcher.group(2);
                    mapping.put(uuid, url);
                }
            }
        });
        return mapping;
    }

    @Override
    public Iterator<ContentExtractionPage> iterator() {

        return new AbstractIterator<ContentExtractionPage>() {
            Iterator<String> uuidIterator = uuidUrlMapping.keySet().iterator();

            @Override
            protected ContentExtractionPage getNext() throws Finished {
                if (uuidIterator.hasNext()) {
                    final String uuid = uuidIterator.next();
                    return new ContentExtractionPage() {

                        @Override
                        public File getHtmlFile() {
                            return new File(datasetDirectory, "/original/" + uuid + ".html");
                        }

                        @Override
                        public String getExpectedText() {
                            File annotatedFile = new File(datasetDirectory, "/annotated/" + uuid + ".html");
                            try {
                                Document annotatedDocument = htmlParser.parse(annotatedFile);
                                StringBuilder expectedText = new StringBuilder();
                                List<Node> nodes = XPathHelper.getXhtmlNodes(annotatedDocument, mode.xPath);
                                for (Node node : nodes) {
                                    expectedText.append(node.getTextContent()).append(" ");
                                }
                                return expectedText.toString();
                            } catch (ParserException e) {
                                throw new IllegalStateException("Could not read or parse " + annotatedFile + ".");
                            }
                        }

                        @Override
                        public String toString() {
                            return getHtmlFile().getPath();
                        }

                        @Override
                        public String getUrl() {
                            return uuidUrlMapping.get(uuid);
                        }
                    };
                }
                throw FINISHED;
            }
        };
    }

    @Override
    public int size() {
        return uuidUrlMapping.size();
    }

    @Override
    public String toString() {
        return "L3S-GN1-Dataset";
    }

}
