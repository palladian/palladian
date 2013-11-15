package ws.palladian.extraction.location.clavin;

import java.io.File;
import java.util.List;

import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.resolver.ResolvedLocation;
import com.bericotech.clavin.util.TextUtils;

public class ClavinSample {
    public static void main(String... args) throws Exception {
        GeoParser parser = GeoParserFactory.getDefault("./IndexDirectory");
        String input = TextUtils.fileToString(new File("src/test/resources/sample-docs/Somalia-doc.txt"));
        List<ResolvedLocation> resolvedLocations = parser.parse(input);
        for (ResolvedLocation resolvedLocation : resolvedLocations) {
            System.out.println(resolvedLocation);
        }
    }
}