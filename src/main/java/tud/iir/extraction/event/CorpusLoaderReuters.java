package tud.iir.extraction.event;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * loads the reuters corpus using the SAX parser... (see comments for more
 * details)
 * 
 */

public class CorpusLoaderReuters extends DefaultHandler {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(CorpusLoaderReuters.class);

    private final static String CORPORA_PATH = "data/corpora/reuters/";

    /**
     * the test set which will be generated for evaluation
     */
    private ArrayList<Event> testSet;

    private ArrayList<Event> trainingSet;

    /**
     * indicates if the currently processed document is a test or training
     * document
     */
    private boolean inTRAINDOCUMENT;

    /**
     * if true, the current document will be added
     */
    private boolean inREUTERS;

    /**
     * if true, the next character string will be a label (the parser is within
     * a TOPICS and a D tag)
     */
    private boolean inTOPICSD;

    /**
     * the parser is currently within a TOPICS tag
     */
    private boolean inTOPICS;

    /**
     * the next character string will be the content of the document
     */
    private boolean inBODY;

    private boolean inTITLE;
    /**
     * a string temporarily holding the label and the content of the document,
     * respectively
     */
    private String text;

    private String title;
    /**
     * the configuration of the corpus, e.g., a subset, the split etc.
     * 
     * current values:<br>
     * 0: reuters 21578 ModApt? split <br>
     * 1: reuters 21578 ModApt? split (subset) <br>
     * 2: reuters 21578 ModApt? [10]
     * 
     */
    private int corpusConfiguration;

    /** initialize the loader */
    public CorpusLoaderReuters(int configuration) {

        /**
         * the test set
         */
        testSet = new ArrayList<Event>();
        trainingSet = new ArrayList<Event>();

        /**
         * defines some boolean variables for every TAG which is of importance
         */
        inREUTERS = false;
        inTOPICSD = false;
        inBODY = false;
        inTOPICS = false;
        inTRAINDOCUMENT = true;
        inTITLE = false;

        text = "";
        title = "";
        // useStopWordRemoval = useSWR;
        // stemmerMethod = stemMethod;

        corpusConfiguration = configuration;

    }

    /**
     * Read XML from input stream and parse, generating SAX events
     * 
     * @param inStream
     */
    public final void readXML(InputStream inStream) {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(this);
            reader.parse(new InputSource(new InputStreamReader(inStream)));

        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    // SAX ContentHandler methods: --

    /* Receive notice of start of document. */
    public void startDocument() throws SAXException {

    }

    /* Receive notice of end of document. */
    public void endDocument() throws SAXException {

    }

    /* Receive notice of start of XML element. */
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {

        if (qName.equalsIgnoreCase("reuters")
        // && atts.getValue("TOPICS").equalsIgnoreCase("yes")
                && atts.getValue("LEWISSPLIT").equalsIgnoreCase("train")) {
            // a new reuters tag started (for the TRAININGSET!!!)
            // just change "yes" and "train" to other values to load a different
            // split!

            inTRAINDOCUMENT = true;
            inREUTERS = true;

            // currentID = Integer.parseInt(atts.getValue("NEWID"));

        } else if (qName.equalsIgnoreCase("reuters")
        // && atts.getValue("TOPICS").equalsIgnoreCase("yes")
                && atts.getValue("LEWISSPLIT").equalsIgnoreCase("test")) {
            // a new reuters tag started (for the TESTSET!!!)
            // just change "yes" and "train" to other values to load a different
            // split!

            inTRAINDOCUMENT = false;
            inREUTERS = true;

        } else if (qName.equalsIgnoreCase("reuter")) {
            LOGGER.info("something went wrong");
        }

        if (qName.equalsIgnoreCase("topics") && inREUTERS) {
            // the parser is now within a TOPICS tag
            inTOPICS = true;
        }

        if ((qName.equalsIgnoreCase("d")) && inREUTERS && inTOPICS) {
            // the parser is now within a D tag which is within a TOPICS tag
            inTOPICSD = true;
            text = "";
        }

        if (qName.equalsIgnoreCase("title") && inREUTERS) {

            inTITLE = true;
            title = "";

        }

        if (qName.equalsIgnoreCase("body") && inREUTERS) {
            // the parser is now within a BODY tag
            inBODY = true;

            // initialize the string to hold the body
            text = "";
        }

    }

    /* Receive notice of end of XML element. */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        if ((qName.equalsIgnoreCase("reuters")) && inREUTERS) {
            // a REUTERS tag was just closed -> set everything to false
            // (just to make sure...)

            inREUTERS = false;
            inBODY = false;
            inTOPICS = false;
            inTOPICSD = false;
            inTITLE = false;
        }
        if ((qName.equalsIgnoreCase("title")) && inREUTERS && inTITLE) {
            inTITLE = false;

        }
        if ((qName.equalsIgnoreCase("body")) && inREUTERS && inBODY) {
            // end of a BODY tag
            // here the text body can be added to the document
            // and the document can be added to the corpus
            inBODY = false;

            // check if the parser ir running to create the corpus or the
            // training set

            if ((inTRAINDOCUMENT) && (corpusConfiguration != 2)) {

                Event event = new Event(title, text);
                trainingSet.add(event);

            } else if ((!inTRAINDOCUMENT) && (corpusConfiguration != 2)) {
                Event event = new Event(title, text);
                testSet.add(event);

            }
        }

        if ((qName.equalsIgnoreCase("topics")) && inREUTERS && inTOPICS) {
            // the parser just left a TOPICS tag

            inTOPICS = false;
        }

        if (qName.equalsIgnoreCase("d") && inREUTERS && inTOPICS) {
            // the parser just left a D tag

            inTOPICSD = false;
        }
    }

    /* Receive notice of character data (text not in an XML element). */
    public void characters(char[] ch, int start, int length)
            throws SAXException {

        String s = new String(ch, start, length);
        s = s.trim();
        s = s.toLowerCase();

        if (inREUTERS && inTITLE) {
            title += s;
        } else if (inREUTERS && inBODY) {
            // read the text between a BODY tag

            text += s;
        } else if (inREUTERS && inTOPICS && inTOPICSD) {
            // every s is one of the categories this document was assigned to

            // add these strings to an ArrayList

            text += s;
        }
    }

    // end SAX ContentHandler methods

    public ArrayList<Event> getTestSet() {

        return testSet;
    }

    public ArrayList<Event> getTrainingSet() {

        return trainingSet;
    }

    public void loadCorpus() {

        // parse all the XML files...

        // corpus = new TCCorpus();
        testSet = new ArrayList<Event>();
        trainingSet = new ArrayList<Event>();

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                    + "reut2-000.xml"));
            readXML(in);
            in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                    + "reut2-001.xml"));
            readXML(in);
            in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                    + "reut2-002.xml"));
            readXML(in);
            in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                    + "reut2-003.xml"));
            readXML(in);

            if (corpusConfiguration != 1) {
                // the entire reuters corpus mod apte split (no subset)

                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-004.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-005.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-006.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-007.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-008.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-009.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-010.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-011.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-012.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-013.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-014.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-015.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-016.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-017.xml"));
                readXML(in);
            }

            in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                    + "reut2-018.xml"));
            readXML(in);

            if (corpusConfiguration == 0) {

                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-019.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-020.xml"));
                readXML(in);
                in = new BufferedInputStream(new FileInputStream(CORPORA_PATH
                        + "reut2-021.xml"));
                readXML(in);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        CorpusLoaderReuters clr_training = new CorpusLoaderReuters(0);
        clr_training.loadCorpus();

        CorpusLoaderReuters clr_test = new CorpusLoaderReuters(1);
        clr_test.loadCorpus();

        LOGGER.info("number of test events:  " + clr_test.getTestSet().size());
        LOGGER.info("number of training events:  "
                + clr_training.getTestSet().size());
    }
}
