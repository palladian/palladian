package ws.palladian.retrieval.wikipedia;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlElement;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage.WikipediaInfobox;
import ws.palladian.retrieval.wikipedia.WikipediaPage.WikipediaLink;

/**
 * <p>
 * Utility functionality for working with <a href="http://www.mediawiki.org/">MediaWiki</a> markup.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Help:Wiki_markup">Wiki markup</a>
 * @author Philipp Katz
 */
public final class WikipediaUtil {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaUtil.class);

    /**
     * Utility class representing a location extracted from Wikipedia coordinate markup.
     */
    public static final class MarkupLocation implements GeoCoordinate {
        double lat;
        double lng;
        Long population;
        String display;
        String name;
        String type;
        String region;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MarkupLocation [lat=");
            builder.append(lat);
            builder.append(", lng=");
            builder.append(lng);
            builder.append(", population=");
            builder.append(population);
            builder.append(", display=");
            builder.append(display);
            builder.append(", name=");
            builder.append(name);
            builder.append(", type=");
            builder.append(type);
            builder.append(", region=");
            builder.append(region);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public Double getLatitude() {
            return lat;
        }

        @Override
        public Double getLongitude() {
            return lng;
        }

        public String getDisplay() {
            return display;
        }

        public Long getPopulation() {
            return population;
        }
    }

    private static final Pattern REF_PATTERN = Pattern.compile("<ref(?:\\s[^>]*)?>[^<]*</ref>|<ref[^/>]*/>",
            Pattern.MULTILINE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^={1,6}([^=]*)={1,6}$", Pattern.MULTILINE);
    private static final Pattern CONVERT_PATTERN = Pattern
            .compile("\\{\\{convert\\|([\\d.]+)\\|([\\w°]+)(\\|[^}]*)?\\}\\}");
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\[\\[([^|\\]]*)(?:\\|([^|\\]]*))?\\]\\]");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[http([^\\s]+)(?:\\s([^\\]]+))\\]");

    private static final Pattern REDIRECT_PATTERN = Pattern.compile("#redirect\\s*:?\\s*\\[\\[(.*)\\]\\]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern OPEN_TAG_PATTERN = Pattern.compile("<\\w+[^>/]*>");
    private static final Pattern CLOSE_TAG_PATTERN = Pattern.compile("</\\w+[^>]*>");

    /**
     * matcher for coordinate template: {{Coord|47|33|27|N|10|45|00|E|display=title}}
     */
    private static final Pattern COORDINATE_TAG_PATTERN = Pattern.compile("\\{\\{Coord" + //
            // match latitude, either DMS or decimal, N/S is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([NS]))?" +
            // ..-(1)--------------.......-(2)------------........-(3)---------------.........-(4)--
            // match longitude, either DMS or decimal, W/E is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([WE]))?" +
            // ..-(5)--------------.......-(6)--------------......-(7)---------------.........-(8)--
            // additional data
            "((?:\\|[^}|<]+(?:<\\w+>[^<]*</\\w+>)?)*)" + //
            // -(9)----------------------------------
            "\\}\\}", Pattern.CASE_INSENSITIVE);//

    public static String stripMediaWikiMarkup(String markup) {
        Validate.notNull(markup, "markup must not be null");

        // strip everything in <ref> tags
        String result = REF_PATTERN.matcher(markup).replaceAll("");

        // resolve HTML entities
        result = StringEscapeUtils.unescapeHtml4(result);

        // remove HTML markup
        result = HtmlHelper.stripHtmlTags(result);

        // replace headlines
        result = HEADING_PATTERN.matcher(result).replaceAll("$1\n");

        // replace formatting
        result = result.replaceAll("'''''|'''|''", "");

        // replace {{convert|...}} tags
        result = CONVERT_PATTERN.matcher(result).replaceAll("$1 $2");

        // replace internal links
        result = processLinks(result, INTERNAL_LINK_PATTERN);
        result = processLinks(result, EXTERNAL_LINK_PATTERN);

        // remove everything left in between { ... } and [ ... ]
        result = removeArea(result, '{', '}');

        // result = removeArea(result, '[', ']');
        // XXX replaced by RegEx, not sure if accurate
        result = result.replaceAll("\\[\\[[^]]*\\]\\]", "");

        // remove single line breaks; but keep lists (lines starting with *)
        result = result.replaceAll("(?<!\n)\n(?![*\n])", " ");

        // remove double whitespace and line breaks, trim
        result = StringHelper.removeDoubleWhitespaces(result);
        result = result.replaceAll("\n{2,}", "\n\n");
        result = result.trim();

        return result;
    }

    private static String processLinks(String string, Pattern linkPattern) {
        Matcher linkMatcher = linkPattern.matcher(string);
        StringBuffer buffer = new StringBuffer();
        while (linkMatcher.find()) {
            String target = linkMatcher.group(1);
            String text = linkMatcher.group(2);
            String replacement = StringUtils.EMPTY;
            // special treatment: ignore category: links completely for now
            if (!target.toLowerCase().startsWith("category:")) {
                replacement = text != null ? text : target;
            }
            linkMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String removeArea(String string, char begin, char end) {
        StringBuilder builder = new StringBuilder();
        int brackets = 0;
        for (int idx = 0; idx < string.length(); idx++) {
            char current = string.charAt(idx);
            if (current == begin) {
                brackets++;
            } else if (current == end) {
                brackets--;
            } else if (brackets == 0) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String extractSentences(String text) {
        // remove lines which do not contain a sentence and bulleted items
        Pattern pattern = Pattern.compile("^(\\*.*|.*\\w)$", Pattern.MULTILINE);
        String result = pattern.matcher(text).replaceAll("");
        result = result.replaceAll("\n{2,}", "\n\n");
        result = result.trim();
        return result;
    }

    public static String cleanTitle(String title) {
        String clean = title.replaceAll("\\s\\([^)]*\\)", "");
        clean = clean.replaceAll(",.*", ""); // XXX comma should not be here! (makes sense for locations only)
        return clean;
    }

    static String getRedirect(String text) {
        Matcher matcher = REDIRECT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * <p>
     * Retrieve a {@link WikipediaPage} directly from the web.
     * </p>
     * 
     * @param title The title of the article to retrieve, not <code>null</code> or empty. Escaping with underscores is
     *            done automatically.
     * @param language The langugae of the Wikipedia to check, not <code>null</code>.
     * @return The {@link WikipediaPage} for the given title, or <code>null</code> in case no article with that title
     *         was given.
     */
    public static final WikipediaPage retrieveArticle(String title, Language language) {
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

        // http://de.wikipedia.org/w/api.php?action=query&prop=revisions&rvlimit=1&rvprop=content&format=json&titles=Dresden
        String escapedTitle = title.replace(" ", "_");
        escapedTitle = UrlHelper.encodeParameter(escapedTitle);
        String url = String
                .format("http://%s.wikipedia.org/w/api.php?action=query"
                        + "&prop=revisions&rvlimit=1&rvprop=content&format=json&titles=%s", language.getIso6391(),
                        escapedTitle);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }

        String stringResult = HttpHelper.getStringContent(httpResult);
        try {
            JSONObject jsonResult = new JSONObject(stringResult);
            JSONObject queryJson = jsonResult.getJSONObject("query");
            JSONObject pagesJson = queryJson.getJSONObject("pages");
            @SuppressWarnings("rawtypes")
            Iterator keys = pagesJson.keys();
            while (keys.hasNext()) {
                String key = (String)keys.next();
                JSONObject pageJson = pagesJson.getJSONObject(key);
                // System.out.println(pageJson);

                if (pageJson.has("missing")) {
                    return null;
                }

                String pageTitle = pageJson.getString("title");
                int namespaceId = pageJson.getInt("ns");
                int pageId = pageJson.getInt("pageid");

                JSONArray revisionsJson = pageJson.getJSONArray("revisions");
                JSONObject firstRevision = revisionsJson.getJSONObject(0);
                String pageText = firstRevision.getString("*");
                return new WikipediaPage(pageId, namespaceId, pageTitle, pageText);
            }
            return null;
        } catch (JSONException e) {
            throw new IllegalStateException("Error while parsing the JSON: " + e.getMessage() + ", JSON='"
                    + stringResult + "'", e);
        }
    }

    /**
     * <p>
     * Extract key-value pairs from Wikipedia template markup.
     * </p>
     * 
     * @param markup The markup, not <code>null</code>.
     * @return A {@link Map} containing extracted key-value pairs from the template, entries in the map have the same
     *         order as in the markup. Entries without a key are are indexed by running numbers as strings (0,1,2…).
     */
    static Map<String, String> extractTemplate(String markup) {
        Validate.notNull(markup, "markup must not be null");
        Map<String, String> properties = new LinkedHashMap<String, String>();
        // trim surrounding {{ and }}
        String content = markup.substring(2, markup.length() - 2);

        // in case of geobox, we must remove the first | character
        if (markup.toLowerCase().startsWith("{{geobox")) {
            content = markup.substring(markup.indexOf('|') + 1, markup.length() - 2);
        }

        int i = 0;
        for (String part : splitTemplateMarkup(content)) {
            String key = String.valueOf(i++);
            int equalIdx = part.indexOf('=');
            if (equalIdx > 0) {
                String potentialKey = part.substring(0, equalIdx);
                if (isBracketBalanced(potentialKey) && isTagBalanced(potentialKey)) {
                    key = part.substring(0, equalIdx).trim();
                } else {
                    equalIdx = -1;
                }
            }
            properties.put(key, part.substring(equalIdx + 1).trim());
        }
        return properties;
    }

    static final List<String> splitTemplateMarkup(String markup) {
        List<String> result = CollectionHelper.newArrayList();
        int startIdx = markup.indexOf('|') + 1;
        for (int currentIdx = startIdx; currentIdx < markup.length(); currentIdx++) {
            char currentChar = markup.charAt(currentIdx);
            String substring = markup.substring(0, currentIdx);
            if (currentChar == '|' && isBracketBalanced(substring)) {
                result.add(markup.substring(startIdx, currentIdx));
                startIdx = currentIdx + 1;
            }
        }
        result.add(markup.substring(startIdx));
        return result;
    }

    /**
     * Check, whether opening/closing brackets are in balance.
     * 
     * @param markup The markup.
     * @return <code>true</code>, if number of opening = number of closing characters.
     */
    private static final boolean isBracketBalanced(String markup) {
        // check the balance of bracket-like characters
        int open = markup.replace("{{", "").replace("[", "").replace("<", "").length();
        int close = markup.replace("}}", "").replace("]", "").replace(">", "").length();
        return open - close == 0;
    }

    private static final boolean isTagBalanced(String markup) {
        // check the balance of HTML tags
        int openTags = StringHelper.countRegexMatches(markup, OPEN_TAG_PATTERN);
        int closeTags = StringHelper.countRegexMatches(markup, CLOSE_TAG_PATTERN);
        return openTags - closeTags == 0;
    }

    /**
     * <p>
     * Extract geographical data from Wikipedia page markup.
     * </p>
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates">WikiProject
     *      Geographical coordinates</a>
     * @param text The markup, not <code>null</code>.
     * @return {@link List} of extracted {@link MarkupLocation}s, or an empty list, never <code>null</code>.
     */
    public static List<MarkupLocation> extractCoordinateTag(String text) {
        Validate.notNull(text, "text must not be null");
        List<MarkupLocation> result = CollectionHelper.newArrayList();
        Matcher m = COORDINATE_TAG_PATTERN.matcher(text);
        while (m.find()) {
            MarkupLocation coordMarkup = new MarkupLocation();
            coordMarkup.lat = parseComponents(m.group(1), m.group(2), m.group(3), m.group(4));
            coordMarkup.lng = parseComponents(m.group(5), m.group(6), m.group(7), m.group(8));

            // get coordinate parameters
            String data = m.group(9);
            String type = getCoordinateParam(data, "type");
            if (type != null) {
                coordMarkup.population = getNumberInBrackets(type);
                type = type.replaceAll("\\(.*\\)", ""); // remove population
            }
            coordMarkup.type = type;
            coordMarkup.region = getCoordinateParam(data, "region");
            // get other parameters
            coordMarkup.display = getOtherParam(data, "display");
            coordMarkup.name = getOtherParam(data, "name");

            result.add(coordMarkup);
        }
        return result;
    }

    /**
     * <p>
     * Try to parse {@link GeoCoordinate}s in a given info box.
     * </p>
     * 
     * @param parsedTemplate The parsed template, not <code>null</code>.
     * @return Set with extracted {@link GeoCoordinate}s, or an empty list in case nothing could be extracted, never
     *         <code>null</code>.
     * @see #extractTemplate(String)
     */
    public static Set<GeoCoordinate> extractCoordinatesFromInfobox(WikipediaInfobox infobox) {
        Validate.notNull(infobox, "parsedTemplate must not be null");
        Set<GeoCoordinate> coordinates = CollectionHelper.newHashSet();

        // try lat/long_deg/min_sec
        try {
            String latDeg = infobox.getEntry("lat_deg", "latd", "lat_d", "lat_degrees", "source_lat_d", "mouth_lat_d");
            String lngDeg = infobox.getEntry("lon_deg", "longd", "long_d", "long_degrees", "source_long_d",
                    "mouth_long_d");
            if (StringUtils.isNotBlank(latDeg) && StringUtils.isNotBlank(lngDeg)) {
                String latMin = infobox.getEntry("lat_min", "latm", "lat_m", "lat_minutes", "source_lat_m",
                        "mouth_lat_m");
                String latSec = infobox.getEntry("lat_sec", "lats", "lat_s", "lat_seconds", "source_lat_s",
                        "mouth_lat_s");
                String lngMin = infobox.getEntry("lon_min", "longm", "long_m", "long_minutes", "source_long_m",
                        "mouth_long_m");
                String lngSec = infobox.getEntry("lon_sec", "longs", "long_s", "long_seconds", "source_long_s",
                        "mouth_long_s");
                String latNS = infobox.getEntry("latNS", "lat_direction", "lat_NS", "source_lat_NS", "mouth_lat_NS");
                String lngEW = infobox.getEntry("longEW", "long_direction", "long_EW", "source_long_EW",
                        "mouth_long_EW");
                double lat = parseComponents(latDeg, latMin, latSec, latNS);
                double lng = parseComponents(lngDeg, lngMin, lngSec, lngEW);
                coordinates.add(new ImmutableGeoCoordinate(lat, lng));
            }
        } catch (Exception e) {
            LOGGER.warn("Error while parsing: {}", e.getMessage());
        }

        // try all-in-one format
        String lat = infobox.getEntry("latitude");
        String lng = infobox.getEntry("longitude");
        if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lng)) {
            try {
                // try decimal format
                coordinates.add(new ImmutableGeoCoordinate(Double.valueOf(lat), Double.valueOf(lng)));
            } catch (Exception e) {
                try {
                    // try DMS format
                    double latDec = GeoUtils.parseDms(lat);
                    double lngDec = GeoUtils.parseDms(lng);
                    coordinates.add(new ImmutableGeoCoordinate(latDec, lngDec));
                } catch (Exception e1) {
                    // try decdeg markup
                    try {
                        double latDec = WikipediaUtil.parseDecDeg(lat);
                        double lngDec = WikipediaUtil.parseDecDeg(lng);
                        coordinates.add(new ImmutableGeoCoordinate(latDec, lngDec));
                    } catch (Exception e2) {
                        LOGGER.warn("Error while parsing: {} and/or {}: {}", lat, lng, e.getMessage());
                    }
                }
            }
        }
        return coordinates;
    }

    private static Long getNumberInBrackets(String string) {
        Matcher matcher = Pattern.compile("\\(([\\d,]+)\\)").matcher(string);
        if (matcher.find()) {
            String temp = matcher.group(1).replace(",", "");
            try {
                return Long.valueOf(temp);
            } catch (NumberFormatException e) {
                LOGGER.error("Error parsing {}", temp);
            }
        }
        return null;
    }

    private static String getOtherParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            String[] keyValue = temp1.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(name)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }

    private static String getCoordinateParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            for (String temp2 : temp1.split("_")) {
                String[] keyValue = temp2.split(":");
                if (keyValue.length == 2 && keyValue[0].equals(name)) {
                    return keyValue[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse DMS components. The only part which must not be <code>null</code> is deg.
     * 
     * @param deg Degree part, not <code>null</code> or empty.
     * @param min Minute part, may be <code>null</code>.
     * @param sec Second part, may be <code>null</code>.
     * @param nsew NSEW modifier, should be in [NSEW], may be <code>null</code>.
     * @return Parsed double value.
     */
    private static double parseComponents(String deg, String min, String sec, String nsew) {
        Validate.notEmpty(deg, "deg must not be null or empty");
        double parsedDeg = Double.valueOf(deg);
        double parsedMin = StringUtils.isNotBlank(min) ? Double.valueOf(min) : 0;
        double parsedSec = StringUtils.isNotBlank(sec) ? Double.valueOf(sec) : 0;
        int sgn = ("S".equals(nsew) || "W".equals(nsew)) ? -1 : 1;
        return sgn * (parsedDeg + parsedMin / 60. + parsedSec / 3600.);
    }

    /**
     * @param markup The markup, nor <code>null</code>.
     * @return A {@link List} with all internal links on the page (sans "category:" links; they can be retrieved using
     *         {@link #getCategories()}). Empty list, in case no links are on the page, never <code>null</code>.
     */
    static final List<WikipediaLink> getLinks(String markup) {
        Validate.notNull(markup, "markup must not be null");
        List<WikipediaLink> result = CollectionHelper.newArrayList();
        Matcher matcher = WikipediaUtil.INTERNAL_LINK_PATTERN.matcher(markup);
        while (matcher.find()) {
            String target = matcher.group(1);
            // strip fragments
            int idx = target.indexOf('#');
            if (idx >= 0) {
                target = target.substring(0, idx);
            }
            String text = matcher.group(2);
            // ignore category links here
            if (target.toLowerCase().startsWith("category:")) {
                continue;
            }
            result.add(new WikipediaLink(target, text));
        }

        return result;
    }
    
    /**
     * <p>
     * Get the content of markup area between double curly braces, like {{infobox …}}, {{quote …}}, etc.
     * </p>
     * 
     * @param markup The media wiki markup, not <code>null</code>.
     * @param name The name, like infobox, quote, etc.
     * @return The content in the markup, or an empty list of not found, never <code>null</code>.
     */
    static List<String> getNamedMarkup(String markup, String... names) {
        List<String> result = CollectionHelper.newArrayList();
        int startIdx = 0;
        String cleanMarkup = HtmlHelper.stripHtmlTags(markup, HtmlElement.COMMENTS);
        String namesSeparated = StringUtils.join(names, "|").toLowerCase();
        Pattern pattern = Pattern.compile("\\{\\{(?:" + namesSeparated + ")(?:\\s|\\|)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cleanMarkup);
        while (matcher.find()) {
            startIdx = matcher.start();
            int brackets = 0;
            int endIdx;
            for (endIdx = startIdx; startIdx < cleanMarkup.length(); endIdx++) {
                char current = cleanMarkup.charAt(endIdx);
                if (current == '{') {
                    brackets++;
                } else if (current == '}') {
                    brackets--;
                }
                if (brackets == 0) {
                    break;
                }
            }
            try {
                result.add(cleanMarkup.substring(startIdx, endIdx + 1));
            } catch (StringIndexOutOfBoundsException e) {
                LOGGER.warn("Encountered {}, potentially caused by invalid markup.");
            }
            startIdx = endIdx;
        }
        return result;
    }

    /**
     * <p>
     * Split the given MediaWiki markup into individual sections. The beginning of the article is also added to the
     * result, even if it does not start with a section heading.
     * </p>
     * 
     * @param markup The MediaWiki markup, not <code>null</code>.
     * @return List with sections, starting with the original section headings, or empty list if no sections were found,
     *         never <code>null</code> however.
     */
    static List<String> getSections(String markup) {
        Validate.notNull(markup, "markup must not be null");
        List<String> result = CollectionHelper.newArrayList();
        Matcher matcher = HEADING_PATTERN.matcher(markup);
        int start = 0;
        while (matcher.find()) {
            int end = matcher.start();
            result.add(markup.substring(start, end));
            start = end;
        }
        result.add(markup.substring(start));
        return result;
    }

    /**
     * <p>
     * Parse markup in {{decdeg|...}} element (decimal degrees for geographic coordinates). See <a
     * href="http://en.wikipedia.org/wiki/Template:Decdeg/sandbox">here</a> for more details.
     * </p>
     * 
     * @param docDegMarkup The markup, not <code>null</code>.
     * @return The double value with the coordinates.
     * @throws NumberFormatException in case the string could not be parsed.
     */
    public static double parseDecDeg(String docDegMarkup) {
        Validate.notNull(docDegMarkup, "string must not be null");
        Map<String, String> templateData = extractTemplate(docDegMarkup);
        String degStr = CollectionHelper.getTrying(templateData, "deg", "0");
        String minStr = CollectionHelper.getTrying(templateData, "min", "1");
        String secStr = CollectionHelper.getTrying(templateData, "sec", "2");
        String hem = CollectionHelper.getTrying(templateData, "hem", "3");
        try {
            double deg = StringUtils.isNotBlank(degStr) ? Double.valueOf(degStr) : 0;
            double min = StringUtils.isNotBlank(minStr) ? Double.valueOf(minStr) : 0;
            double sec = StringUtils.isNotBlank(secStr) ? Double.valueOf(secStr) : 0;
            int sgn;
            if (StringUtils.isNotBlank(hem)) {
                sgn = "W".equals(hem) || "S".equals(hem) ? -1 : 1;
            } else {
                sgn = degStr.startsWith("-") ? -1 : 1;
            }
            double result = sgn * (Math.abs(deg) + min / 60. + sec / 3600.);
            String rndStr = CollectionHelper.getTrying(templateData, "rnd", "4");
            if (StringUtils.isNotBlank(rndStr)) {
                int rnd = Integer.valueOf(rndStr);
                result = MathHelper.round(result, rnd);
            }
            return result;
        } catch (Exception e) {
            throw new NumberFormatException("The coordinate data from \"" + docDegMarkup + "\" could not be parsed.");
        }
    }

    private WikipediaUtil() {
        // leave me alone!
    }

    public static void main(String[] args) {
        // System.out.println(getDoubleBracketBalance("{{xx{{{{"));
        // System.exit(0);
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/newYork.wikipedia");
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/sample2.wikipedia");
        // String text = stripMediaWikiMarkup(wikipediaPage);
        // System.out.println(text);

        // WikipediaPage page = getArticle("Mit Schirm, Charme und Melone (Film)", Language.GERMAN);
        WikipediaPage page = retrieveArticle("Charles River", Language.ENGLISH);
        Map<String, String> infoboxData = extractTemplate(getNamedMarkup(page.getText(), "geobox").get(0));
        CollectionHelper.print(infoboxData);
        System.out.println(page);

    }

}
