package ws.palladian.extraction.location;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Perform some simple integrity checks on the location dataset. Do opening tags match closing tags? Did we only use
 * allowed tags (as specified in {@link LocationType})?
 * </p>
 * 
 * @author Philipp Katz
 */
final class DatasetCheck {

    private static final String DATASET_PATH = "/Users/pk/Desktop/LocationLab/LocationExtractionDataset";
    private static final Pattern TAG_REGEX = Pattern.compile("<([^>]*)>([^<]*)</([^>]*)>");

    private static final Set<String> allowedTags;

    static {
        allowedTags = CollectionHelper.newHashSet();
        for (LocationType type : LocationType.values()) {
            allowedTags.add(type.toString());
        }
    }

    public static void main(String[] args) {
        File[] datasetFiles = FileHelper.getFiles(DATASET_PATH, "text");
        CountMap<String> assignedTags = CountMap.create();
        
        for (File file : datasetFiles) {
            String filePath = file.getAbsolutePath();
            String stringContent = FileHelper.readFileToString(filePath);
            Matcher matcher = TAG_REGEX.matcher(stringContent);

            while (matcher.find()) {

                String openingTag = matcher.group(1);
                if (openingTag.contains("role=\"main\"")) {
                    openingTag = openingTag.substring(0, openingTag.indexOf("role=\"main\"")).trim();
                }
                String content = matcher.group(2);
                String closingTag = matcher.group(3);

                // opening does not match closing tag
                if (!openingTag.equals(closingTag)) {
                    System.out.println(openingTag + " does not match " + closingTag + " in " + filePath);
                }

                // unknown tag type
                if (!allowedTags.contains(openingTag)) {
                    System.out.println("unknown tag " + openingTag + " in " + filePath);
                }
                
                // check if text in between is rather long
                if (content.length() > 50) {
                    System.out.println(content + " seems rather long for an annotation");
                }
                
                assignedTags.add(openingTag);
            }

        }
        
        System.out.println("Assigned tags:");
        for (String tag : assignedTags) {
            System.out.println(tag + " " + assignedTags.getCount(tag));
        }

    }

}
