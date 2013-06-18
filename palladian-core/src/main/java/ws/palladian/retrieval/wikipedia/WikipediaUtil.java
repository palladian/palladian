package ws.palladian.retrieval.wikipedia;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * Utility functionality for working with <a href="http://www.mediawiki.org/">MediaWiki</a> markup.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Help:Wiki_markup">Wiki markup</a>
 * @author Philipp Katz
 */
public final class WikipediaUtil {

    private static final Pattern REF_PATTERN = Pattern.compile("<ref(?:\\s[^>]*)?>[^<]*</ref>|<ref[^/>]*/>",
            Pattern.MULTILINE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^={1,6}([^=]*)={1,6}$", Pattern.MULTILINE);
    private static final Pattern CONVERT_PATTERN = Pattern
            .compile("\\{\\{convert\\|([\\d.]+)\\|([\\w°]+)(\\|[^}]*)?\\}\\}");
    public static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\[\\[([^|\\]]*)(?:\\|([^|\\]]*))?\\]\\]");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[http([^\\s]+)(?:\\s([^\\]]+))\\]");

    private static final Pattern REDIRECT_PATTERN = Pattern.compile("#redirect\\s*:?\\s*\\[\\[(.*)\\]\\]",
            Pattern.CASE_INSENSITIVE);

    public static String stripMediaWikiMarkup(String markup) {

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
        result = removeArea(result, '[', ']');

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
        clean = clean.replaceAll(",.*", "");
        return clean;
    }

    public static String getRedirect(String text) {
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
        String underscoreTitle = title.replace(" ", "_");
        String url = String.format("http://%s.wikipedia.org/w/api.php?action=query"
                + "&prop=revisions&rvlimit=1&rvprop=content&format=json&titles=%s", language.getIso6391(),
                underscoreTitle);
        try {
            HttpResult httpResult = retriever.httpGet(url);
            String stringResult = HttpHelper.getStringContent(httpResult);
            JSONObject jsonResult = new JSONObject(stringResult);

            JSONObject queryJson = jsonResult.getJSONObject("query");
            JSONObject pagesJson = queryJson.getJSONObject("pages");
            @SuppressWarnings("rawtypes")
            Iterator keys = pagesJson.keys();
            while (keys.hasNext()) {
                String key = (String)keys.next();
                JSONObject pageJson = pagesJson.getJSONObject(key);
                // System.out.println(pageJson);

                String pageTitle = pageJson.getString("title");
                int namespaceId = pageJson.getInt("ns");
                int pageId = pageJson.getInt("pageid");

                JSONArray revisionsJson = pageJson.getJSONArray("revisions");
                JSONObject firstRevision = revisionsJson.getJSONObject(0);
                String pageText = firstRevision.getString("*");
                return new WikipediaPage(pageId, namespaceId, pageTitle, pageText);
            }
            return null;
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private WikipediaUtil() {
        // leave me alone!
    }

    public static void main(String[] args) {
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/newYork.wikipedia");
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/sample2.wikipedia");
        // String text = stripMediaWikiMarkup(wikipediaPage);
        // System.out.println(text);

        // WikipediaPage page = getArticle("Mit Schirm, Charme und Melone (Film)", Language.GERMAN);
        WikipediaPage page = retrieveArticle("Mac mini", Language.GERMAN);
        System.out.println(page);

    }

}
