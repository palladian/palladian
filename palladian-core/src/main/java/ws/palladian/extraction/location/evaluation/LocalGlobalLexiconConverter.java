package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
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

        final File coordinateFile = new File(outputDirectory, "coordinates.csv");
        if (coordinateFile.isFile()) {
            coordinateFile.delete();
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
                    topCount = Integer.valueOf(CollectionHelper.coalesce(attributes.getValue("count"),
                            attributes.getValue("toponymcount")));
                } else if (qName.equals("gaztag")) {
                    geonameId = Integer.valueOf(CollectionHelper.coalesce(attributes.getValue("geonameid"),
                            attributes.getValue("gazid")));
                }
                clearBuffer();
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (qName.equals("article")) {
                    if (topCount != null && annotations.size() != topCount) {
                        throw new IllegalStateException("Count mismatch; should be " + topCount + ", but is "
                                + annotations.size());
                    }
                    writeArticle(text, annotations, docId, outputDirectory);
                    appendCoordinatesFile(annotations, docId, coordinateFile);
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
                    GeoCoordinate coordinate = null;
                    if (lat != null && lng != null) {
                        coordinate = new ImmutableGeoCoordinate(lat, lng);
                    }
                    Location location = new ImmutableLocation(geonameId, topName, type, coordinate, null);
                    String value = text.substring(topStart, topEnd);
                    annotations.add(new LocationAnnotation(topStart, value, location));
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

    private static void writeArticle(String text, List<LocationAnnotation> annotations, String docId,
            File outputDirectory) {
        String taggedText = NerHelper.tag(text, annotations, TaggingFormat.XML);
        File outputFile = new File(outputDirectory, "text_" + docId + ".txt");
        FileHelper.writeToFile(outputFile.getPath(), taggedText);
    }

    private static void appendCoordinatesFile(List<LocationAnnotation> annotations, String docId, File coordinateFile) {
        StringBuilder builder = new StringBuilder();
        if (!coordinateFile.exists()) {
            // write header first
            builder.append("docId;idx;offset;latitude;longitude;sourceId\n");
        }
        int idx = 0;
        for (LocationAnnotation annotation : annotations) {
            Double lat = annotation.getLocation().getLatitude();
            Double lng = annotation.getLocation().getLongitude();
            int id = annotation.getLocation().getId();
            String fileName = String.format("text_%s.txt", docId);
            String sourceId = id != 0 ? "geonames:" + id : "";
            builder.append(fileName).append(';');
            builder.append(idx++).append(';');
            builder.append(annotation.getStartPosition()).append(';');
            builder.append(lat != null ? lat : "").append(';');
            builder.append(lng != null ? lng : "").append(';');
            builder.append(sourceId).append('\n');
        }
        FileHelper.appendFile(coordinateFile.getPath(), builder);
    }

    /**
     * <p>
     * Clean the CLUST dataset; ignore duplicate texts, ignore texts which were no annotated.
     * </p>
     * 
     * @param datasetPath
     */
    public static final void cleanClust(File datasetPath) {
        File[] files = FileHelper.getFiles(datasetPath.getPath(), "text_");
        File destinationDirectory = new File(datasetPath, "0-all");
        Set<Integer> deduplication = CollectionHelper.newHashSet();
        int annotated = 0;
        for (File file : files) {
            String fileContent = FileHelper.tryReadFileToString(file);
            if (deduplication.add(fileContent.hashCode())) {
                Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlText(fileContent);
                if (annotations.size() > 0) {
                    annotated++;
                    FileHelper.copyFileToDirectory(file, destinationDirectory);
                }
            }
        }
        System.out.println("# files: " + files.length);
        System.out.println("# unique: " + deduplication.size());
        System.out.println("# annotated: " + annotated);
    }

    public static void main(String[] args) throws Exception {
        // convert(new File("/Users/pk/Desktop/LocationLab/LGL/articles.xml"), new File("/Users/pk/Desktop/LGL-converted"));
        // convert(new File("/Users/pk/Desktop/CLUST/articles.xml"), new File("/Users/pk/Desktop/CLUST-converted"));
        cleanClust(new File("/Users/pk/Desktop/CLUST-converted"));
    }

}
