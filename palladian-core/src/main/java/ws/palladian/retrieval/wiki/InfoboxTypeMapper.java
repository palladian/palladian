package ws.palladian.retrieval.wiki;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.importers.WikipediaLocationImporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * Maps infobox types from the Wikipedia to CoNLL or TUD-Loc-2013 entity types.
 * 
 * @author pk
 */
public final class InfoboxTypeMapper {

    private static final String MAPPING_FILE = "/WikipediaInfoboxTypeMapping.csv";

    private static final Map<String, String> CONLL = loadMapping(1);

    private static final Map<String, String> TUD_LOC = loadMapping(2);

    private static final Map<String, String> loadMapping(final int colIdx) {
        InputStream inputStream = null;
        try {
            final Map<String, String> result = CollectionHelper.newHashMap();
            inputStream = WikipediaLocationImporter.class.getResourceAsStream(MAPPING_FILE);
            int numLines = FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    if (lineNumber == 0 || line.isEmpty()) {
                        return;
                    }
                    String[] split = StringUtils.splitPreserveAllTokens(line, ';');
                    String infoboxType = split[0];
                    String mappedType = split[colIdx];
                    if (mappedType.length() > 0) {
                        result.put(infoboxType, mappedType);
                    }
                }
            });
            if (numLines == 0) {
                throw new IllegalStateException("Could not read any mappings from '" + MAPPING_FILE + "'.");
            }
            return result;
        } finally {
            FileHelper.close(inputStream);
        }
    }

    public static LocationType getLocationType(String infoboxName) {
        Validate.notNull(infoboxName, "infoboxName must not be null");
        String tudTypeString = TUD_LOC.get(infoboxName);
        return tudTypeString != null ? LocationType.map(tudTypeString) : null;
    }

    public static String getConLLType(String infoboxName) {
        Validate.notNull(infoboxName, "infoboxName must not be null");
        return CONLL.get(infoboxName);
    }

    private InfoboxTypeMapper() {
        // no instance
    }

}
