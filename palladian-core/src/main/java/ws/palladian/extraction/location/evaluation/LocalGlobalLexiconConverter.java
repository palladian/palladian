package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.importers.GeonamesUtil;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Converts Local Global Lexicon corpus to XML files. See 'Geotagging with Local Lexicons to Build Indexes for
 * Textually-Specified', Michael D. Lieberman and Hanan Samet and Jagan Sankaranarayanan, 2010.
 * </p>
 * 
 * @author Philipp Katz
 */
class LocalGlobalLexiconConverter {

    public static void convert(File inputFile, final File outputDirectory) throws Exception {

        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file " + inputFile + " does not exist.");
        }
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException(outputDirectory + " is not a directory.");
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler() {

            StringBuilder buffer = new StringBuilder();
            String docId = null;
            String text = null;
            Integer topCount = null;
            Integer topStart = null;
            Integer topEnd = null;
            String topName = null;
            Integer geonameId = null;
            String fclass = null;
            String fcode = null;
            Double lat = null;
            Double lng = null;
            List<LocationAnnotation> annotations = CollectionHelper.newArrayList();

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                if (qName.equals("article")) {
                    docId = attributes.getValue("docid");
                } else if (qName.equals("toponyms")) {
                    topCount = Integer.valueOf(attributes.getValue("count"));
                } else if (qName.equals("gaztag")) {
                    geonameId = Integer.valueOf(attributes.getValue("geonameid"));
                }
                clearBuffer();
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (qName.equals("article")) {
                    if (annotations.size() != topCount) {
                        throw new IllegalStateException("Count mismatch; should be " + topCount + ", but is "
                                + annotations.size());
                    }
                    writeArticle(text, annotations, docId, outputDirectory);
                    clearAll();
                } else if (qName.equals("text")) {
                    text = getBuffer();
                } else if (qName.equals("start")) {
                    topStart = Integer.valueOf(getBuffer());
                } else if (qName.equals("end")) {
                    topEnd = Integer.valueOf(getBuffer());
                } else if (qName.equals("fclass")) {
                    fclass = getBuffer();
                } else if (qName.equals("fcode")) {
                    fcode = getBuffer();
                } else if (qName.equals("lat")) {
                    lat = Double.valueOf(getBuffer());
                } else if (qName.equals("lon")) {
                    lng = Double.valueOf(getBuffer());
                } else if (qName.equals("name")) {
                    topName = getBuffer();
                } else if (qName.equals("toponym")) {
                    LocationType type = GeonamesUtil.mapType(fclass, fcode);
                    if (geonameId == null) {
                        geonameId = 0;
                    }
                    if (topName == null) {
                        topName = StringUtils.EMPTY;
                    }
                    Location location = new ImmutableLocation(geonameId, topName, type, lat, lng, null);
                    String value = text.substring(topStart, topEnd);
                    annotations.add(new LocationAnnotation(topStart, topEnd, value, location));
                    clearToponym();
                }
            }

            private void clearAll() {
                docId = null;
                text = null;
                topCount = null;
                annotations.clear();
                clearBuffer();
                clearToponym();
            }

            private void clearToponym() {
                topStart = null;
                topEnd = null;
                topName = null;
                geonameId = null;
                fclass = null;
                fcode = null;
                lat = null;
                lng = null;
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                buffer.append(ch, start, length);
            }

            private String getBuffer() {
                try {
                    return buffer.toString();
                } finally {
                    clearBuffer();
                }
            }

            private void clearBuffer() {
                buffer = new StringBuilder();
            }

        };

        parser.parse(inputFile, handler);

    }

    protected static void writeArticle(String text, List<LocationAnnotation> annotations, String docId,
            File outputDirectory) {
        String taggedText = NerHelper.tag(text, annotations, TaggingFormat.XML);
        File outputFile = new File(outputDirectory, "text_" + docId + ".txt");
        FileHelper.writeToFile(outputFile.getPath(), taggedText);
    }

    public static void main(String[] args) throws Exception {
        convert(new File("/Users/pk/Downloads/LGL/articles.xml"), new File("/Users/pk/Desktop/temp_lgl"));
    }

}
