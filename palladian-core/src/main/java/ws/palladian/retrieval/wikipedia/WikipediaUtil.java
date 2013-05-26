package ws.palladian.retrieval.wikipedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Utility functionality for working with <a href="http://www.mediawiki.org/">MediaWiki</a> markup.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class WikipediaUtil {

    public static String stripMediaWikiMarkup(String markup) {

        // strip everything in <ref> tags
        Pattern refPattern = Pattern.compile("<ref(?:\\s[^>]*)?>[^<]*</ref>|<ref[^/>]*/>", Pattern.MULTILINE);
        String result = refPattern.matcher(markup).replaceAll("");

        // strip HTML comments
        result = result.replaceAll("<!--[^>]*-->", "");

        // resolve HTML entities
        result = StringEscapeUtils.unescapeHtml4(result);

        // replace headlines
        Pattern titlePattern = Pattern.compile("^={1,4}([^=]*)={1,4}$", Pattern.MULTILINE);
        result = titlePattern.matcher(result).replaceAll("$1\n");

        // replace formatting
        result = result.replaceAll("'''|''", "");

        // replace {{convert|...}} tags
        Pattern convertPattern = Pattern.compile("\\{\\{convert\\|([\\d.]+)\\|([\\w°]+)(\\|[^}]*)?\\}\\}");
        result = convertPattern.matcher(result).replaceAll("$1 $2");

        // replace links
        // FIXME not working for internal links
        Pattern linkPattern = Pattern.compile("\\[\\[?([^|\\]]*)(?:\\|([^|\\]]*))?\\]\\]?");
        Matcher linkMatcher = linkPattern.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (linkMatcher.find()) {
            String target = linkMatcher.group(1);
            String text = linkMatcher.group(2);
            String replacement = StringUtils.EMPTY;
            if (!target.toLowerCase().startsWith("category:")) {
                replacement = text != null ? text : target;
            }
            linkMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(buffer);
        result = buffer.toString();

        // remove everything left in between { ... } and [ ... ]
        result = removeArea(result, '{', '}');
        result = removeArea(result, '[', ']');

        // remove single line breaks; but keep lists (lines starting with *)
        result = result.replaceAll("(?<!\n)\n(?![*\n])", " ");

        result = StringHelper.removeDoubleWhitespaces(result);
        result = result.replaceAll("\n{2,}", "\n\n");
        result = result.trim();
        return result;
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
        String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/sample.wikipedia");
        String text = stripMediaWikiMarkup(wikipediaPage);
        System.out.println(text);
    }

}
