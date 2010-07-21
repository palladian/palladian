package tud.iir.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLHelper {

    /**
     * Count the tags.
     * 
     * @param htmlText The html text.
     * @return The number of tags.
     */
    public static int countTags(String htmlText) {
        return countTags(htmlText, false);
    }

    /**
     * Count tags.
     * 
     * @param htmlText The html text.
     * @param distinct If true, count multiple occurrences of the same tag only once.
     * @return The number of tags.
     */
    public static int countTags(String htmlText, boolean distinct) {
        Set<String> tags = new HashSet<String>();

        int tagCount = 0;

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            tagCount++;
            tags.add(matcher.group());
        }

        if (distinct) {
            tagCount = tags.size();
        }

        return tagCount;
    }

    /**
     * Remove all style and script tags including their content (css, javascript). Remove all other tags as well. Close
     * gaps.
     * 
     * @param htmlContent the html content
     * @param stripTags the strip tags
     * @param stripComments the strip comments
     * @param stripJSAndCSS the strip js and css
     * @param joinTagsAndRemoveNewlines the join tags and remove newlines
     * @return The text of the web page.
     */
    public static String removeHTMLTags(String htmlContent, boolean stripTags, boolean stripComments,
            boolean stripJSAndCSS, boolean joinTagsAndRemoveNewlines) {

        String htmlText = htmlContent;
        // modified by Martin Werner, 2010-06-02

        if (joinTagsAndRemoveNewlines) {
            htmlText = htmlText.replaceAll(">\\s*?<", "><");
            htmlText = htmlText.replaceAll("\n", "");
        }

        // String regExp = "";

        if (stripComments) {
            // regExp += "(\\<!--.*?-->)|";
            htmlText = htmlText.replaceAll("<!--.*?-->", "");
        }

        if (stripJSAndCSS) {
            // regExp += "(<style.*?>.*?</style>)|(<script.*?>.*?</script>)|";
            htmlText = removeConcreteHTMLTag(htmlText, "style");
            htmlText = removeConcreteHTMLTag(htmlText, "script");
        }

        if (stripTags) {
            // regExp += "(\\<.*?>)";
            // htmlText = removeConcreteHTMLTag(htmlText, "\\<", ">", true);
            htmlText = htmlText.replaceAll("<.*?>", "");
        }

        // if (regExp.length() == 0) {
        // return htmlText;
        // }

        // if (regExp.endsWith("|")) {
        // regExp = regExp.substring(0, regExp.length() - 1);
        // }
        //
        // // Pattern pattern =
        // //
        // Pattern.compile("((\\<!--.*?-->)|(\\<style.*?>.*?\\</style>)|(\\<script.*?>.*?\\</script>)|(\\<.*?>))",Pattern.DOTALL);
        // Pattern pattern = Pattern.compile("(" + regExp + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        // Matcher matcher = pattern.matcher(htmlText);
        //
        // while (matcher.find()) {
        // htmlText = htmlText.replace(matcher.group(), " "); // TODO changed
        // // and untested
        // // 16/06/2009
        // // replace with
        // // whitespace
        // // instead of
        // // nothing
        // }

        // close gaps
        htmlText = htmlText.replaceAll("(\\s){2,}", " ");

        return htmlText.trim();
    }

    /**
     * Removes the concrete html tag.
     * 
     * @param pageContent The html text.
     * @param tag The tag that should be removed.
     * @return The html text without the tag.
     */
    public static String removeConcreteHTMLTag(String pageString, String tag) {
        return removeConcreteHTMLTag(pageString, tag, tag);
    }

    /**
     * Remove concrete HTMLTags from a string; set isSpecial=true for special-tags like <!-- -->.
     * 
     * @param pageContent The html text.
     * @param beginTag The begin tag.
     * @param endTag The end tag.
     * @return The string without the specified html tag.
     */
    public static String removeConcreteHTMLTag(String pageContent, String beginTag, String endTag) {
        String pageString = pageContent;
        List<String> removeList;
        removeList = getConcreteTags(pageString, beginTag, endTag);
        for (String removeTag : removeList) {
            pageString = pageString.replace(removeTag, "");
        }
        return pageString;
    }

    /**
     * Get a list of concrete HTMLTags; begin- and endtag are not different.
     * 
     * @param pageContent The html text.
     * @param tag The tag.
     * @return A list of concrete tags.
     */
    public static List<String> getConcreteTags(String pageString, String tag) {
        return getConcreteTags(pageString, tag, tag);
    }

    /**
     * Get a list of concrete HTMLTags; its possible that begin- and endtag are different like <!-- -->.
     * 
     * @param pageString The html text.
     * @param beginTag The begin tag.
     * @param endTag The end tag.
     * @return A list of concrete tag names.
     */
    public static List<String> getConcreteTags(String pageString, String beginTag, String endTag) {

        List<String> tagList = new ArrayList<String>();
        String regExp = "";
        if (beginTag.equals(endTag)) {
            // regExp = "<"+beginTag+".*?>.*?</"+endTag+">";
            regExp = "<" + beginTag + ".*?>(.*?</" + endTag + ">)?";

        } else {
            regExp = beginTag + ".*?" + endTag;
        }

        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(pageString);
        while (matcher.find()) {
            tagList.add(matcher.group(0));
        }

        return tagList;
    }

}
