package ws.palladian.retrieval.wikipedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

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
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\[\\[([^|\\]]*)(?:\\|([^|\\]]*))?\\]\\]");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[http([^\\s]+)(?:\\s([^\\]]+))\\]");

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

    private WikipediaUtil() {
        // leave me alone!
    }

    public static void main(String[] args) {
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/newYork.wikipedia");
        String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/sample2.wikipedia");
        String text = stripMediaWikiMarkup(wikipediaPage);
        System.out.println(text);
    }

}
