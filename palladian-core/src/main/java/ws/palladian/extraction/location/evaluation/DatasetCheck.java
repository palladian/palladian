package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.io.FileHelper;
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

    private static final Pattern TAG_REGEX = Pattern.compile("<([^>]*)>([^<]*)<(/[^>]*)>");

    private static final Set<String> allowedTags;

    static {
        allowedTags = CollectionHelper.newHashSet();
        for (LocationType type : LocationType.values()) {
            allowedTags.add(type.toString());
        }
    }

    private static void performCheck(File datasetDirectory) {
        if (!datasetDirectory.isDirectory()) {
            throw new IllegalStateException("Specified path '" + datasetDirectory
                    + "' does not exist or is no directory.");
        }

        File[] datasetFiles = FileHelper.getFiles(datasetDirectory.getPath(), "text");
        if (datasetFiles.length == 0) {
            throw new IllegalStateException("No text files found in '" + datasetDirectory + "'");
        }
        
        // keep tag -> values
        Map<String, CountMap<String>> assignedTagCounts = LazyMap.create(new Factory<CountMap<String>>() {
            @Override
            public CountMap<String> create() {
                return CountMap.create();
            }
        });

        int tokenCount = 0;

        for (File file : datasetFiles) {
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            String stringContent = FileHelper.readFileToString(filePath);
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

                String openingTag = matcher.group(1);
                if (openingTag.contains("role=\"main\"")) {
                    openingTag = openingTag.substring(0, openingTag.indexOf("role=\"main\"")).trim();
                }
                String content = matcher.group(2);
                String closingTag = matcher.group(3);

                // closing tag does not start with slash
                if (!closingTag.startsWith("/")) {
                    System.out.println("[error] " + closingTag + " does not start with '/' in " + fileName);
                }

                closingTag = closingTag.substring(1);

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

                valueTags.get(content.toLowerCase()).add(openingTag);
                assignedTagCounts.get(openingTag).add(content.toLowerCase());
            }

            // check, whether all annotations with a specific value in the text have the same tag; if not, this is not
            // necessarily an error, as there might be different meanings (e.g. Mississippi, New York, ...)
            for (String value : valueTags.keySet()) {
                if (valueTags.get(value).size() > 1) {
                    System.out.println("[warn] ambiguous annotations for " + value + ": " + valueTags.get(value)
                            + " in " + fileName);
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
            int count = assignedTagCounts.get(tag).totalSize();
            int uniqueCount = assignedTagCounts.get(tag).uniqueSize();
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
    }

    public static void main(String[] args) {
        performCheck(new File("/Users/pk/Desktop/LocationLab/LocationDatasetUliana"));
    }

}
