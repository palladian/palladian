package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

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
        allowedTags = CollectionHelper.newHashSet();
        for (LocationType type : LocationType.values()) {
            allowedTags.add(type.toString());
        }
    }

    static void performCheck(File datasetDirectory) {
        if (!datasetDirectory.isDirectory()) {
            throw new IllegalStateException("Specified path '" + datasetDirectory
                    + "' does not exist or is no directory.");
        }

        File[] datasetFiles = FileHelper.getFiles(datasetDirectory.getPath(), "text");
        if (datasetFiles.length == 0) {
            throw new IllegalStateException("No text files found in '" + datasetDirectory + "'");
        }

        // keep tag -> values
        Map<String, Bag<String>> assignedTagCounts = LazyMap.create(new Bag.BagFactory<String>());

        int tokenCount = 0;
        int scopedDocCount = 0;

        for (File file : datasetFiles) {
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            String stringContent = FileHelper.tryReadFileToString(filePath);
            Matcher matcher = TAG_REGEX.matcher(stringContent);

            // keep value -> assigned tags
            Map<String, Set<String>> valueTags = LazyMap.create(new Factory<Set<String>>() {
                @Override
                public Set<String> create() {
                    return CollectionHelper.newHashSet();
                }
            });

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
                }

                // opening does not match closing tag
                if (!openingTag.equals(closingTag)) {
                    System.out.println("[error] " + openingTag + " does not match " + closingTag + " in " + fileName);
                }

                // unknown tag type
                if (!allowedTags.contains(openingTag)) {
                    System.out.println("[error] unknown tag " + openingTag + " in " + fileName);
                }

                // check if text in between is rather long
                if (content.length() > 50) {
                    System.out.println("[warn] " + content + " seems rather long for an annotation in " + fileName);
                }

                // annotation value should not start/end with punctuation
                if (StringHelper.isPunctuation(content.charAt(0))) {
                    System.out.println("[warn] '" + content + "' starts with punctuation in " + fileName);
                }
                if (StringHelper.isPunctuation(content.charAt(content.length() - 1))) {
                    System.out.println("[warn] '" + content + "' ends with punctuation in " + fileName);
                }

                // annotation value should not start/end with white space
                if (Character.isWhitespace(content.charAt(0))) {
                    System.out.println("[warn] '" + content + "' starts with white space in " + fileName);
                }
                if (Character.isWhitespace(content.charAt(content.length() - 1))) {
                    System.out.println("[warn] '" + content + "' ends with white space in " + fileName);
                }

                valueTags.get(content/* .toLowerCase() */).add(openingTag);
                assignedTagCounts.get(openingTag).add(content/* .toLowerCase() */);
            }

            // check, whether all annotations with a specific value in the text have the same tag; if not, this is not
            // necessarily an error, as there might be different meanings (e.g. Mississippi, New York, ...)
            for (String value : valueTags.keySet()) {
                if (valueTags.get(value).size() > 1) {
                    System.out.println("[warn] ambiguous annotations for " + value + ": " + valueTags.get(value)
                            + " in " + fileName);
                }
            }

            // check for potentially missed annotations
            for (String value : valueTags.keySet()) {
                for (String tag : valueTags.get(value)) {
                    Pattern pattern = Pattern.compile(String.format("(?<!<%s>)(?<=[\\s\"])%s(?!</%s>)(?=[\\s.,:;?!])",
                            tag, Pattern.quote(value), tag));
                    Matcher matcher2 = pattern.matcher(stringContent);
                    while (matcher2.find()) {
                        int start = matcher2.start();
                        int end = matcher2.end();
                        String context = stringContent.substring(Math.max(0, start - 15),
                                Math.min(stringContent.length(), end + 15)).replace('\n', ' ');
                        System.out.println("[warn] potentially missed annotation for '" + value + "' (context '"
                                + context + "' in " + fileName);
                    }
                }
            }

            if (valueTags.isEmpty()) {
                System.out.println("[warn] no annotations in " + fileName);
            }

        }

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
        Map<String, Map<Integer, GeoCoordinate>> coordinates = TudLoc2013DatasetIterable
                .readCoordinates(coordinatesFile);
        Bag<String> totalTypeCounts = Bag.create();
        Bag<String> disambiguatedTypeCounts = Bag.create();
        int mainRoleCount = 0;

        File[] files = FileHelper.getFiles(datasetPath.getPath(), "text");
        for (File file : files) {
            String inputText = FileHelper.tryReadFileToString(file);
            if (inputText.contains(MAIN_ROLE_ATTRIBUTE)) {
                mainRoleCount++;
            }
            inputText = inputText.replace(MAIN_ROLE_ATTRIBUTE, "");
            Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlText(inputText);
            for (ContextAnnotation annotation : annotations) {
                totalTypeCounts.add(annotation.getTag());
                GeoCoordinate coordinate = coordinates.get(file.getName()).get(annotation.getStartPosition());
                if (coordinate != null) {
                    disambiguatedTypeCounts.add(annotation.getTag());
                }
            }
        }
        for (String tag : disambiguatedTypeCounts.uniqueItems()) {
            int count = totalTypeCounts.count(tag);
            int disambiguatedCount = disambiguatedTypeCounts.count(tag);
            float disambiguatedPercentage = (float)disambiguatedCount / count * 100;
            System.out.println(tag + " total: " + count + ", disambiguated: " + disambiguatedCount + ", percentage: "
                    + MathHelper.round(disambiguatedPercentage, 2));
        }
        System.out.println();
        System.out.println("# total disambiguated: " + disambiguatedTypeCounts.size());
        System.out.println("% total disambiguated: "
                + MathHelper.round((float)disambiguatedTypeCounts.size() / totalTypeCounts.size() * 100, 2));
        System.out.println("# role='main' annotations: " + mainRoleCount);
    }

    public static void main(String[] args) {
        File datasetPath = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/0-all");
        getNonDisambiguatedStatistics(datasetPath);
        // performCheck(datasetPath);
    }

}
