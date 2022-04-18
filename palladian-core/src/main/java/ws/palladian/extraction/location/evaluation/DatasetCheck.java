package ws.palladian.extraction.location.evaluation;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Perform some simple integrity checks on the location dataset. Do opening tags match closing tags? Did we only use
 * allowed tags (as specified in {@link LocationType})?
 * </p>
 *
 * @author Philipp Katz
 */
final class DatasetCheck {

    private static final String MAIN_ROLE_ATTRIBUTE = " role=\"main\"";

    private static final Pattern TAG_REGEX = Pattern.compile("<([^>]*)>([^<]*)<(/?)([^>]*)>");

    private static final Set<String> allowedTags;

    static {
        allowedTags = new HashSet<>();
        for (LocationType type : LocationType.values()) {
            allowedTags.add(type.toString());
        }
    }

    static void performCheck(File datasetDirectory) {
        if (!datasetDirectory.isDirectory()) {
            throw new IllegalStateException("Specified path '" + datasetDirectory + "' does not exist or is no directory.");
        }

        File[] datasetFiles = FileHelper.getFiles(datasetDirectory.getPath(), "text");
        if (datasetFiles.length == 0) {
            throw new IllegalStateException("No text files found in '" + datasetDirectory + "'");
        }

        // keep tag -> values
        Map<String, Bag<String>> assignedTagCounts = new LazyMap<>(Bag::new);

        int tokenCount = 0;
        int scopedDocCount = 0;
        int warnCount = 0;
        int errorCount = 0;

        for (File file : datasetFiles) {
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            String stringContent = FileHelper.tryReadFileToString(filePath);
            Matcher matcher = TAG_REGEX.matcher(stringContent);

            // keep value -> assigned tags
            Map<String, Set<String>> valueTags = new LazyMap<>(HashSet::new);

            // get token count
            tokenCount += Tokenizer.tokenize(FileFormatParser.getText(filePath, TaggingFormat.XML)).size();

            while (matcher.find()) {

                // System.out.println(matcher.group());

                String openingTag = matcher.group(1);
                if (openingTag.contains("role=\"main\"")) {
                    openingTag = openingTag.substring(0, openingTag.indexOf("role=\"main\"")).trim();
                    scopedDocCount++;
                }
                String content = matcher.group(2);
                String closingSlash = matcher.group(3);
                String closingTag = matcher.group(4);

                // closing tag does not start with slash
                if (!"/".equals(closingSlash)) {
                    System.out.println("[error] " + closingTag + " does not start with '/' in " + fileName);
                    errorCount++;
                }

                // opening does not match closing tag
                if (!openingTag.equals(closingTag)) {
                    System.out.println("[error] " + openingTag + " does not match " + closingTag + " in " + fileName);
                    errorCount++;
                }

                // unknown tag type
                if (!allowedTags.contains(openingTag)) {
                    System.out.println("[error] unknown tag " + openingTag + " in " + fileName);
                    errorCount++;
                }

                // check if text in between is rather long
                if (content.length() > 50) {
                    System.out.println("[warn] " + content + " seems rather long for an annotation in " + fileName);
                    warnCount++;
                }

                // annotation value should not start/end with punctuation
                if (StringHelper.isPunctuation(content.charAt(0))) {
                    System.out.println("[warn] '" + content + "' starts with punctuation in " + fileName);
                    warnCount++;
                }
                if (StringHelper.isPunctuation(content.charAt(content.length() - 1))) {
                    System.out.println("[warn] '" + content + "' ends with punctuation in " + fileName);
                    warnCount++;
                }

                // annotation value should not start/end with white space
                if (Character.isWhitespace(content.charAt(0))) {
                    System.out.println("[warn] '" + content + "' starts with white space in " + fileName);
                    warnCount++;
                }
                if (Character.isWhitespace(content.charAt(content.length() - 1))) {
                    System.out.println("[warn] '" + content + "' ends with white space in " + fileName);
                    warnCount++;
                }

                valueTags.get(content/* .toLowerCase() */).add(openingTag);
                assignedTagCounts.get(openingTag).add(content/* .toLowerCase() */);
            }

            // check, whether all annotations with a specific value in the text have the same tag; if not, this is not
            // necessarily an error, as there might be different meanings (e.g. Mississippi, New York, ...)
            for (String value : valueTags.keySet()) {
                if (valueTags.get(value).size() > 1) {
                    System.out.println("[warn] ambiguous annotations for " + value + ": " + valueTags.get(value) + " in " + fileName);
                    warnCount++;
                }
            }

            // check for potentially missed annotations
            for (String value : valueTags.keySet()) {
                for (String tag : valueTags.get(value)) {
                    Pattern pattern = Pattern.compile(String.format("(?<!<%s>)(?<=[\\s\"])%s(?!</%s>)(?=[\\s.,:;?!])", tag, Pattern.quote(value), tag));
                    Matcher matcher2 = pattern.matcher(stringContent);
                    while (matcher2.find()) {
                        int start = matcher2.start();
                        int end = matcher2.end();
                        String context = stringContent.substring(Math.max(0, start - 15), Math.min(stringContent.length(), end + 15)).replace('\n', ' ');
                        System.out.println("[warn] potentially missed annotation for '" + value + "' (context '" + context + "' in " + fileName);
                        warnCount++;
                    }
                }
            }

            if (valueTags.isEmpty()) {
                System.out.println("[warn] no annotations in " + fileName);
                warnCount++;
            }

        }
        
        System.out.println("# errors: " + errorCount);
        System.out.println("# warnings: " + warnCount);

        System.out.println('\n');
        System.out.println("Assigned tags:");
        int totalTags = 0;
        int totalUniqueTags = 0;
        for (String tag : assignedTagCounts.keySet()) {
            int count = assignedTagCounts.get(tag).size();
            int uniqueCount = assignedTagCounts.get(tag).unique().size();
            System.out.println(tag + " total: " + count + ", unique: " + uniqueCount);
            totalTags += count;
            totalUniqueTags += uniqueCount;
        }
        System.out.println();
        System.out.println("# total: " + totalTags);
        System.out.println("# unique: " + totalUniqueTags);
        System.out.println("# tokens: " + tokenCount);
        System.out.println();
        System.out.println("# texts: " + datasetFiles.length);
        System.out.println();
        System.out.println("# text with role=\"main\": " + scopedDocCount);
    }

    /**
     * Get statistics about non/disambiguated annotations in the dataset.
     *
     * @param datasetPath
     */
    static void getNonDisambiguatedStatistics(File datasetPath) {
        File coordinatesFile = new File(datasetPath, "coordinates.csv");
        Map<String, Map<Integer, GeoCoordinate>> coordinates = TudLoc2013DatasetIterable.readCoordinates(coordinatesFile);
        Bag<String> totalTypeCounts = new Bag<>();
        Bag<String> disambiguatedTypeCounts = new Bag<>();
        int mainRoleCount = 0;

        File[] files = FileHelper.getFiles(datasetPath.getPath(), "text");
        for (File file : files) {
            String inputText = FileHelper.tryReadFileToString(file);
            if (inputText.contains(MAIN_ROLE_ATTRIBUTE)) {
                mainRoleCount++;
            }
            inputText = inputText.replace(MAIN_ROLE_ATTRIBUTE, "");
            Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromXmlText(inputText);
            for (Annotation annotation : annotations) {
                String tag = annotation.getTag();
                int start = annotation.getStartPosition();
                totalTypeCounts.add(tag);
                if (coordinates.get(file.getName()).containsKey(start)) {
                    GeoCoordinate coordinate = coordinates.get(file.getName()).get(start);
                    if (coordinate != null) {
                        disambiguatedTypeCounts.add(tag);
                    }
                } else {
                    System.out.println("[warn] missing entry for " + file.getName() + ": " + annotation);
                }
            }
        }
        for (String tag : disambiguatedTypeCounts.uniqueItems()) {
            int count = totalTypeCounts.count(tag);
            int disambiguatedCount = disambiguatedTypeCounts.count(tag);
            float disambiguatedPercentage = (float) disambiguatedCount / count * 100;
            System.out.println(tag + " total: " + count + ", disambiguated: " + disambiguatedCount + ", percentage: " + MathHelper.round(disambiguatedPercentage, 2));
        }
        System.out.println();
        System.out.println("# total disambiguated: " + disambiguatedTypeCounts.size());
        System.out.println("% total disambiguated: " + MathHelper.round((float) disambiguatedTypeCounts.size() / totalTypeCounts.size() * 100, 2));
        System.out.println("# role='main' annotations: " + mainRoleCount);
    }

    public static void main(String[] args) {
        File datasetPath = new File("/Users/pk/Documents/tud-loc-2015-de");
        // getNonDisambiguatedStatistics(datasetPath);
        performCheck(datasetPath);
    }

}
