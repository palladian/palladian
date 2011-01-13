package tud.iir.classification.controlledtagging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.helper.Counter;

// TODO this should go into another package?
/**
 * Parser for Delicious data set from http://nlp.uned.es/social-tagging/delicioust140/
 * 
 * See main method for usage example.
 * 
 * @author Philipp Katz
 */
@Deprecated
public class DeliciousDatasetReader {

    private String dataPath = "";

    private DatasetFilter filter = new DatasetFilter();

    public static abstract class DatasetCallback {
        private boolean stop = false;

        public abstract void callback(DatasetEntry entry);

        public final void stop() {
            stop = true;
        }
    }

    /**
     * Represents an entry in the data set.
     * 
     * TODO use Tag class instead of Bag, also change return type.
     * 
     * @author Philipp Katz
     * 
     */
    public class DatasetEntry {

        private String url;
        private String filename;
        private String filetype;
        private int numUsers;
        private List<Tag> tags = new ArrayList<Tag>();

        @Override
        public String toString() {
            return url + " type:" + filetype + " numUsers:" + numUsers + " tags:" + tags;
        }

        /** get entry's url. */
        public String getUrl() {
            return url;
        }

        /** get file type of associated file. */
        public String getFiletype() {
            return filetype;
        }

        /** get the number of users who bookmarked this entry. */
        public int getNumUsers() {
            return numUsers;
        }

        /** get the path to the associated file. */
        public String getPath() {
            return dataPath + "/fdocuments/" + filename.substring(0, 2) + "/" + filename;
        }

        /** get associated file. */
        public File getFile() {
            return new File(getPath());
        }

        @Deprecated
        public Bag<String> getTags() {
            Bag<String> resultBag = new HashBag<String>();
            for (Tag tag : tags) {
                resultBag.add(tag.getName(), (int) tag.getWeight());
            }
            return resultBag;
        }

        public List<Tag> getAssignedTags() {
            return tags;
        }

    }

    /**
     * Allows to filter DatasetEntries based on their attributes.
     * Available Filetypes are html, pdf, xml or swf.
     * 
     * @author Philipp Katz
     * 
     */
    public static class DatasetFilter {

        protected Collection<String> allowedFiletypes = new LinkedList<String>();
        protected int minUsers = -1;
        protected double minUserTagRatio = -1;
        protected int maxFileSize = -1;

        public void setAllowedFiletypes(Collection<String> allowedFiletypes) {
            this.allowedFiletypes = allowedFiletypes;
        }

        public void addAllowedFiletype(String allowedFiletype) {
            this.allowedFiletypes.add(allowedFiletype);
        }

        public void setMinUsers(int minUsers) {
            this.minUsers = minUsers;
        }

        public void setMinUserTagRatio(double minUserTagRatio) {
            this.minUserTagRatio = minUserTagRatio;
        }

        /**
         * Limit for maximum accepted file size in bytes. This is useful, because very big HTML files can cause the
         * HTML parser to stall. I usually set this to 600.000, to skip files above 600 kB. Set to -1 for no limit.
         * 
         * @param maxFileSize
         */
        public void setMaxFileSize(int maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        protected boolean accept(DatasetEntry entry) {
            boolean accept = allowedFiletypes.isEmpty() || allowedFiletypes.contains(entry.filetype);
            accept = accept && entry.numUsers >= minUsers;
            accept = accept && (maxFileSize == -1 || entry.getFile().length() <= maxFileSize);
            return accept;
        }

        protected boolean accept(Tag tag, DatasetEntry entry) {
            boolean accept = tag.getWeight() / entry.getNumUsers() >= minUserTagRatio;
            return accept;
        }

    }

    // SAX handler for parsing the XML data set.
    private class DatasetHandler extends DefaultHandler {

        DatasetCallback callback;
        int offset; // where to start parsing.
        int limit; // when to break parsing.
        int currentIndex; // current position in XML file.

        private DatasetEntry entry = new DatasetEntry();
        private StringBuffer textBuffer = new StringBuffer();
        private boolean catchText = false;
        private Tag tag = new Tag();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("url") || qName.equals("filename") || qName.equals("filetype") || qName.equals("users")
                    || qName.equals("name") || qName.equals("weight")) {
                catchText = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            catchText = false;
            if (qName.equals("url")) {
                entry.url = getText();
            } else if (qName.equals("filename")) {
                entry.filename = getText();
            } else if (qName.equals("filetype")) {
                entry.filetype = getText();
            } else if (qName.equals("users")) {
                entry.numUsers = Integer.parseInt(getText());
            } else if (qName.equals("name")) {
                tag.setName(getText());
            } else if (qName.equals("weight")) {
                tag.setWeight(Integer.parseInt(getText()));
            } else if (qName.equals("tag")) {
                if (filter.accept(tag, entry)) {
                    entry.tags.add(tag);
                }
                tag = new Tag();
            } else if (qName.equals("document")) {
                if (filter.accept(entry)) {
                    if (++currentIndex > offset) {
                        callback.callback(entry);
                    }
                    if (callback.stop || (limit != -1 && currentIndex == limit + offset)) {
                        throw new StopParsingException();
                    }
                }
                entry = new DatasetEntry();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (catchText) {
                textBuffer.append(ch, start, length);
            }
        }

        private String getText() {
            try {
                return textBuffer.toString();
            } finally {
                textBuffer = new StringBuffer();
            }
        }

    }

    /**
     * Subclass of SAXException to allow stopping the SAX parser, we catch this Exception explicity when parsing.
     */
    private class StopParsingException extends SAXException {
        private static final long serialVersionUID = 1L;
    }

    public DeliciousDatasetReader() {
        try {
            PropertiesConfiguration configuration = new PropertiesConfiguration("config/deliciousDatasetReader.conf");
            setDataPath(configuration.getString("dataPath", dataPath));
        } catch (ConfigurationException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Start reading the dataset, using the specified callback.
     * 
     * @param callback
     */
    public void read(DatasetCallback callback) {
        read(callback, -1, 0);
    }

    /**
     * Start reading the dataset, using the specified callback.
     * 
     * @param callback
     * @param limit the number of entries to read, or -1 for no limit.
     */
    public void read(DatasetCallback callback, int limit) {
        read(callback, limit, 0);
    }

    /**
     * Start reading the dataset, using the specified callback.
     * 
     * @param callback
     * @param limit the number of entries to read, or -1 for no limit.
     * @param offset the offset where to start reading, or 0 for no offset.
     */
    public void read(DatasetCallback callback, int limit, int offset) {

        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            DatasetHandler handler = new DatasetHandler();
            handler.callback = callback;
            handler.limit = limit;
            handler.offset = offset;
            parser.parse(new File(dataPath + "/taginfo.xml"), handler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (StopParsingException e) {
            // System.out.println("stopped parsing");
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Set the path to the data files. One can obtain them from http://nlp.uned.es/social-tagging/delicioust140/ --
     * download both ZIP files and put their contents "taginfo.xml" and "fdocuments" in one directory.
     * 
     * @param dataPath
     */
    public void setDataPath(String dataPath) {
        if (dataPath.endsWith("/")) {
            dataPath = dataPath.substring(0, dataPath.length() - 1);
        }
        this.dataPath = dataPath;
    }

    /**
     * Set filter for entries.
     * 
     * @param filter
     */
    public void setFilter(DatasetFilter filter) {
        this.filter = filter;
    }

    public static void main(String[] args) {

        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        // path to the data files, must contain "taginfo.xml" and "fdocuments" directory
        // this can also be configured in config/deliciousDatasetReader.conf
        // reader.setDataPath("/Users/pk/Studium/Diplomarbeit/delicioust140/");

        // configure filter to get only HTML files, bookmarked by at least 50 users,
        // only those tags which have been assigned by at least 10 % of the users,
        // and only files which have a maximum size of 600.000 bytes
        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        // filter.setMinUsers(50);
        // filter.setMinUserTagRatio(0.1);
        filter.setMaxFileSize(500000);
        reader.setFilter(filter);

        final Counter c = new Counter();
        // callback for every entry in the data set
        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                c.increment();

            }
        };


        // read ten entries, starting at 100th matching entry.
        // defining an offset is useful for evaluation purposes,
        // allowing to separate into train and test set
        reader.read(callback);

        System.out.println(c);

    }

}
