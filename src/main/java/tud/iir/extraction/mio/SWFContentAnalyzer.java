package tud.iir.extraction.mio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import swf9.SWFHeader;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

import com.anotherbigidea.flash.interfaces.SWFText;
import com.anotherbigidea.flash.interfaces.SWFVectors;
import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;
import com.anotherbigidea.flash.structs.AlphaColor;
import com.anotherbigidea.flash.structs.Color;
import com.anotherbigidea.flash.structs.Matrix;
import com.anotherbigidea.flash.structs.Rect;
import com.anotherbigidea.flash.writers.SWFTagTypesImpl;

/**
 * Parse a Flash movie and extract all the text in Text symbols
 * 
 * A "pipeline" is set up:
 * 
 * SWFReader-->TagParser-->SWFContentAnalyzer
 * 
 * SWFReader reads the input SWF file and separates out the header
 * and the tags. The separated contents are passed to TagParser which
 * parses out the individual tag types and passes them to SWFContentAnalyzer.
 * 
 * SWFContentAnalyzer extends SWFTagTypesImpl and overrides some methods.
 * 
 * @author Martin Werner
 */
public class SWFContentAnalyzer extends SWFTagTypesImpl {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(SWFContentAnalyzer.class);

    /**
     * Store font info keyed by the font symbol id Each entry is an int[] of character codes for the corresponding font
     * glyphs (An empty array denotes a System Font).
     */
    // protected HashMap<Integer, int[]> fontCodes = new HashMap<Integer, int[]>();
    protected Map<Integer, int[]> fontCodes = new HashMap<Integer, int[]>();

    /** The text content. */
    private StringBuffer textContent = new StringBuffer();

    /** The Constant DOWNLOADPATH. */
    private String downloadPath;

    /**
     * Instantiates a new MIOContentAnalyzer.
     */
    public SWFContentAnalyzer() {
        super(null);
        this.downloadPath = InCoFiConfiguration.getInstance().tempDirPath;

    }

    /**
     * SWFTagTypes interface
     * Save the Text Font character code info.
     * 
     * @param fontId the font id
     * @param fontName the font name
     * @param flags the flags
     * @param codes the codes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void tagDefineFontInfo(final int fontId, final String fontName, final int flags, final int[] codes)
            throws IOException {
        // fontCodes.put(new Integer(fontId), codes);
        fontCodes.put(Integer.valueOf(fontId), codes);
    }

    /**
     * SWFTagTypes interface
     * Save the character code info.
     * 
     * @param tagID the id
     * @param flags the flags
     * @param name the name
     * @param numGlyphs the num glyphs
     * @param ascent the ascent
     * @param descent the descent
     * @param leading the leading
     * @param codes the codes
     * @param advances the advances
     * @param bounds the bounds
     * @param kernCodes1 the kern codes1
     * @param kernCodes2 the kern codes2
     * @param kernAdjustments the kern adjustments
     * @return the sWF vectors
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SWFVectors tagDefineFont2(final int tagID, final int flags, final String name, int numGlyphs,
            final int ascent, final int descent, int leading, int[] codes, int[] advances, Rect[] bounds,
            int[] kernCodes1, int[] kernCodes2, int[] kernAdjustments) throws IOException {
        // fontCodes.put(new Integer(id), (codes != null) ? codes : new int[0]);
        fontCodes.put(Integer.valueOf(tagID), (codes != null) ? codes : new int[0]);

        return null;
    }

    /**
     * SWFTagTypes interface
     * Dump any initial text in the field.
     * 
     * @param fieldId the field id
     * @param fieldName the field name
     * @param initialText the initial text
     * @param boundary the boundary
     * @param flags the flags
     * @param textColor the text color
     * @param alignment the alignment
     * @param fontId the font id
     * @param fontSize the font size
     * @param charLimit the char limit
     * @param leftMargin the left margin
     * @param rightMargin the right margin
     * @param indentation the indentation
     * @param lineSpacing the line spacing
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void tagDefineTextField(int fieldId, String fieldName, String initialText, Rect boundary, int flags,
            AlphaColor textColor, int alignment, int fontId, int fontSize, int charLimit, int leftMargin,
            int rightMargin, int indentation, int lineSpacing) throws IOException {
        // if (initialText != null) {
        // // System.out.println(initialText);
        // }
    }

    /**
     * SWFTagTypes interface.
     * 
     * @param someId the some id
     * @param bounds the bounds
     * @param matrix the matrix
     * @return the sWF text
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SWFText tagDefineText(int someId, Rect bounds, Matrix matrix) throws IOException {
        return new TextDumper();
    }

    public class TextDumper implements SWFText {

        /** The font id. */
        protected Integer fontId;

        /** The first y. */
        protected boolean firstY = true;

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#font(int, int)
         */
        public void font(int fontId, int textHeight) {
            // this.fontId = new Integer(fontId);
            this.fontId = Integer.valueOf(fontId);

        }

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#setY(int)
         */
        public void setY(int yvar) {
            if (firstY) {
                firstY = false;
            }

            // else System.out.println(); //Change in Y - dump a new line
        }

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#text(int[], int[])
         */
        public void text(int[] glyphIndices, int[] glyphAdvances) {

            final int[] codes = fontCodes.get(fontId);
            if (codes == null) {
                // System.out.println("\n**** COULD NOT FIND FONT INFO FOR TEXT ****\n");
                return;
            }

            // --Translate the glyph indices to character codes
            char[] chars = new char[glyphIndices.length];

            for (int i = 0; i < chars.length; i++) {
                final int index = glyphIndices[i];

                if (index >= codes.length) // System Font ?
                {
                    chars[i] = (char) index;
                } else {
                    chars[i] = (char) (codes[index]);
                }
            }

            textContent.append(chars);

        }

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#color(com.anotherbigidea.flash.structs.Color)
         */
        public void color(Color color) {
        }

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#setX(int)
         */
        public void setX(int xvar) {
            // do something
        }

        /*
         * (non-Javadoc)
         * @see com.anotherbigidea.flash.interfaces.SWFText#done()
         */
        public void done() {
            // System.out.println();
        }
    }

    /**
     * Extract the header of an SWFFile.
     * 
     * @param file the swfFile
     * @return the SWFHeader
     */
    private SWFHeader extractHeader(File file) {

        SWFHeader swfHeader = null;
        try {
            swfHeader = new SWFHeader(file);

        } catch (Exception e) {
            LOGGER.error("SWFHeader extraction failed! " + e.getMessage());
        }

        return swfHeader;
    }

    /**
     * This is the central method of this class and allows to completely analyze the content of a given SWF-MIO and add
     * some relevant parameter and features to that MIO.
     * 
     * @param mio the SWF-MIO
     * @param entity the entity
     */
    public void analyzeContentAndSetFeatures(final MIO mio, Entity entity) {

        String url = mio.getDirectURL();

        if (!url.toLowerCase(Locale.ENGLISH).endsWith(".swf")) {
            if (url.toLowerCase(Locale.ENGLISH).contains(".swf")) {
                final int swfIndex = (url.toLowerCase(Locale.ENGLISH).indexOf(".swf")) + 4;
                url = url.substring(0, swfIndex);
            } else {
                LOGGER.info("MIO-URL contains no swf");
                url = "";
            }

        }
        if (!("").equals(url)) {
            // make directory if not exists
            File tempFile = new File(downloadPath);
            if (!tempFile.isDirectory()) {
                tempFile.mkdir();
            }
            final File mioFile = Crawler.downloadBinaryFile(url, downloadPath + mio.getFileName());

            if (mioFile != null) {

//                double size = (double) mioFile.length();
//                System.out.println(mio.getFileName() + " --- " + size);
                try {
                    final String textContent = extractTextContent(mioFile);
                    final SWFHeader header = extractHeader(mioFile);
                    // setTextContentLength(mio, textContent);

                    setFileSize(mio, header);
//                    setFileSize(mio, mioFile);
                    setFeatures(mio, entity, textContent, header);

                } catch (Exception exception) {
                    LOGGER.error("Analyzing SWFContent failed for "+entity.getName() + " and FileName: " + mio.getFileName() + exception.getMessage());
                } catch (Error error) {
                    LOGGER.error("Analyzing SWFContent failed for "+entity.getName() + " and FileName: " + mio.getFileName() + error.getMessage());
                }
            }
        }

    }

    /**
     * Sets the textContentLength.
     * 
     * @param mio the mio
     * @param textContent the text content
     */
    // private void setTextContentLength(MIO mio, String textContent) {
    //
    // mio.setTextContentLength(textContent.length());
    // }

    /**
     * Sets the fileSize.
     * 
     * @param mio the mio
     * @param header the header
     */
    private void setFileSize(MIO mio, SWFHeader header) {

        if (header != null) {
            mio.setFileSize(header.getSize());

        }

    }

    // An alternative without header-usage.
    //
//     private void setFileSize(MIO mio, File mioFile) {
//     if (mioFile != null) {
//    
//     double size = (double)mioFile.length();
//     mio.setFileSize(size);
//     }
//    
//     }

    /**
     * Sets the features for trust calculation.
     * 
     * @param mio the new features
     * @param entity the entity
     * @param textContent the text content
     * @param header the header
     */
    private void setFeatures(final MIO mio, Entity entity, String textContent, SWFHeader header) {

        // double textContentRelevance = 0;
        // double resolutionRelevance = 0;

        if (!("").equals(textContent)) {
            mio.setFeature("TextContentRelevance", calcTextContentRelevance(textContent, entity));
        }

        if (header != null) {
            mio.setFeature("ResolutionRelevance", calcResolutionRelevance(header));
        }

    }

    /**
     * Calculates text content relevance.
     * 
     * @param textContent the textContent
     * @param entity the entity
     * @return the relevance as double
     */
    private double calcTextContentRelevance(String textContent, Entity entity) {

        double textContentRelevance = 0;

        if (textContent.length() > 3) {
            textContentRelevance = RelevanceCalculator.calcStringRelevance(textContent, entity);
        }
        return textContentRelevance;
    }

    /**
     * Calculates resolution relevance.
     * 
     * @param header the header
     * @return the relevance as double
     */
    private double calcResolutionRelevance(SWFHeader header) {
        double result = 0;

        double width = header.getWidth();
        double height = header.getHeight();
        double relevance = (width * height);
        if (relevance > 70000) {
            result = 1;
        }

        return result;
    }

    /**
     * Extract text content of an SWFFile.
     * 
     * @param file the file
     * @return the string
     * @throws FileNotFoundException 
     */
    private String extractTextContent(File file) throws FileNotFoundException {
        String result = "";

        FileInputStream inStream = null;
       
            inStream = new FileInputStream(file);
      

        if (inStream != null) {
            try {

                // TagParser implements SWFTags and drives a SWFTagTypes interface
                TagParser parser = new TagParser(this);

                // SWFReader reads an input file and drives a SWFTags interface
                SWFReader reader = new SWFReader(parser, inStream);

                // read the input SWF file and pass it through the interface pipeline
                reader.readFile();
                result = textContent.toString();
                // FileHelper.appendFile("f:/filecontent.html", "------ " + textContent + "<br>");

                inStream.close();
            } catch (Exception e) {
                LOGGER.error("Extracting textContent from swf-file failed! " + file.getName() + " " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        //
        // if (Crawler.isValidURL("http://www.dexgo.com/sponsoren/Banner/klein/Cougar.swf", false)) {
        // System.out.println("is valid");
        // } else {
        // System.out.println("is not valid");
        // }
        //
        // // System.exit(1);
        //
        String urlString = "http://l.yimg.com/dv/i/izmo/2/16762/audiosystem.swf";
        // // String urlString = "http://www.pentagonstrike.co.uk/pentagon_ge.swf";
        // // URL url = new URL("http://www.sennheiser.com/flash/hotspots/EN/HD-800.swf");
        // // String urlString = "http://www2.razerzone.com/Megalodon/main.swf";
        // // String urlString = "http://www.vivalagames.com/play/antbuster/game.swf";
        // String urlString = "http://media.tigerdirect.com/swf/HP EliteBook Flash Presentation.swf";
        //
        SWFContentAnalyzer mioca = new SWFContentAnalyzer();
        // System.exit(1);
        File file = Crawler.downloadBinaryFile(urlString, "F:/Temp/audiosystem4.swf");
        // File file = Crawler.downloadBinaryFile("http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/index.swf",
        // "F:/Temp/index.swf");
        //
        // double size = (double)file.length();
        // System.out.println("FileFileSize: " +size);
        //
        System.out.println(mioca.extractHeader(file).getSize());

        //
        String textContent = mioca.extractTextContent(file);
        System.out.println(textContent);
        // System.out.println(textContent.length());
        //
        // System.out.println(mioca.extractHeader(file).toString());
    }
}
