package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Content handler for parsing the Delcious T140 data set and transforming it to Palladian data set format.
 * </p>
 * 
 * @see <a href="http://nlp.uned.es/social-tagging/delicioust140/">DeliciousT140 Dataset</a> and <i>Arkaitz Zubiaga,
 *      Alberto P. García-Plaza, Víctor Fresno, and Raquel Martínez. Content-based Clustering for Tag Cloud
 *      Visualization. Proceedings of ASONAM 2009, International Conference on Advances in Social Networks Analysis and
 *      Mining. 2009.</i>
 * @author Philipp Katz
 */
final class DeliciousT140Handler extends DefaultHandler {
    
    // tag names in the XML file which we are interested in, i.e. when the parser starts catching characters
    private static final String TAG_TAG = "tag";
    private static final String TAG_WEIGHT = "weight";
    private static final String TAG_NAME = "name";
    private static final String TAG_USERS = "users";
    private static final String TAG_FILETYPE = "filetype";
    private static final String TAG_FILENAME = "filename";
    private static final List<String> TAGS = Arrays.asList(TAG_WEIGHT, TAG_NAME, TAG_USERS, TAG_FILETYPE, TAG_FILENAME);

    // filter variables

    /** The minimum number of users who need to bookmark an URL to be accepted. */
    private final int minimumUsers;
    /**
     * The ratio of users who need set a specific tag to be accepted, e.g. a value of 0.05 means that 5 of 100 users
     * need to assign this specific tag.
     */
    private final float minimumUserTagRatio;

    /** Pattern, which tags have to match, elsewise they are ignored. */
    private static final Pattern TAG_MATCH_PATTERN = Pattern.compile("[a-z0-9\\-\\.\\+\\#]+");

    /** The index file which will be written. */
    private final File indexFileOutput;

    // state variables for the ContentHandler

    /** Buffer for all text. */
    private StringBuffer textBuffer = new StringBuffer();
    /** Whether we are interested in the currently read character data. */
    private boolean catchText = false;
    /** Counter for the number of entries written. */
    private int entriesWritten;

    // content specific variables

    /** The filename of the current document. */
    private String filename;
    /** The file type of the current document. */
    private String filetype;
    /** The number of users who have bookmarked the current document. */
    private int users;
    /** The set of tags assigned for the current document. */
    private Set<String> tags = new HashSet<String>();
    /** The value of the current tag. */
    private String currentTag;
    /** The number of users having assigned the current tag. */
    private int currentWeight;

    public DeliciousT140Handler(File indexFileOutput, int minimumUsers, float minimumUserTagRatio) {
        if (indexFileOutput.exists()) {
            indexFileOutput.delete();
        }
        this.indexFileOutput = indexFileOutput;
        this.minimumUsers = minimumUsers;
        this.minimumUserTagRatio = minimumUserTagRatio;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (TAGS.contains(qName)) {
            catchText = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        catchText = false;
        if (qName.equals(TAG_FILENAME)) {
            filename = getText();
        } else if (qName.equals(TAG_FILETYPE)) {
            filetype = getText();
        } else if (qName.equals(TAG_USERS)) {
            users = Integer.parseInt(getText());
        } else if (qName.equals(TAG_NAME)) {
            currentTag = getText();
        } else if (qName.equals(TAG_WEIGHT)) {
            currentWeight = Integer.parseInt(getText());
        } else if (qName.equals(TAG_TAG)) {
            boolean accept = (float)currentWeight / users >= minimumUserTagRatio;
            accept = accept && TAG_MATCH_PATTERN.matcher(currentTag).matches();
            if (accept) {
                tags.add(currentTag);
            }
        } else if (qName.equals("document")) {
            writeEntry();
            tags.clear();
        }
    }

    private void writeEntry() throws SAXException {

        String pathToSubdirectory = filename.substring(0, 2) + "/" + filename;

        boolean accept = filetype.equals("html");
        accept &= users >= minimumUsers;
        accept &= !tags.isEmpty();
        // accept &= !(new File(pathToHtmlFile).length() > 60000);

        if (!accept) {
            return;
        }

        StringBuilder line = new StringBuilder();
        line.append(pathToSubdirectory).append(DatasetConverter.SEPARATOR);
        line.append(StringUtils.join(tags, DatasetConverter.SEPARATOR));
        line.append(DatasetConverter.NEWLINE);
        FileHelper.appendFile(indexFileOutput.getAbsolutePath(), line);

        entriesWritten++;
        if (entriesWritten % 100 == 0) {
            System.out.println(entriesWritten);
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

}
