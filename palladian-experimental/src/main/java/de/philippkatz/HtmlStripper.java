package de.philippkatz;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * Helper for stripping tags from HTML strings.
 * </p>
 * 
 * @author Philipp Katz
 */
public class HtmlStripper {

    /** Enum necessary for the {@link #stripHtmlTagAndContent(String, String)} state machine. */
    private enum StripTagState {
        READ, TAG_NAME_1, IGNORE, TAG_NAME_2, CLOSE_TAG_NAME
    };

    /** Enum necessary for the {@link #stripHtmlComments(String)} state machine. */
    private enum StripCommentState {
        READ, LT, EXCLAMATION_MARK, COLON_1, IGNORE, COLON_3, COLON_4
    }

    /**
     * <p>
     * Removes all HTML tags from the supplied string, i.e. everything between the characters &lt; and &gt;, including
     * themselves. For example <code>&lt;b&gt;This&lt;/b&gt; is some &lt;i&gt;text&lt;/i&gt; with tags</code> is
     * transformed to <code>This is some text with tags</code>.
     * </p>
     * 
     * @param htmlText The string with HTML tags to remove.
     * @return The string without HTML tags.
     */
    public static String stripHtmlTags(String htmlText) {
        boolean inTag = false;
        StringBuilder result = new StringBuilder(htmlText.length());
        for (int i = 0; i < htmlText.length(); i++) {
            char c = htmlText.charAt(i);
            if (c == '<') {
                inTag = true;
            } else if (c == '>' && inTag) {
                inTag = false;
            } else if (!inTag) {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * <p>
     * Removes HTML comments and their content from the supplied string, i.e. everything between the characters &lt;!--
     * and --&gt;, including themselves. For example <code>This is a text &lt;!-- with a comment --&gt;</code> is
     * transformed to <code>This is a text</code>.
     * </p>
     * 
     * @param htmlText The string with HTML comments to remove.
     * @return The string without HTML comments.
     */
    public static String stripHtmlComments(String htmlText) {

        // store the result
        StringBuilder result = new StringBuilder(htmlText.length());
        
        // store temporary text, where we are not yet sure whether to keep it,
        // this is either discarded or added to the result, when we know that we
        // want to keep it
        StringBuilder stack = new StringBuilder();
        
        StripCommentState state = StripCommentState.READ;

        for (int i = 0; i < htmlText.length(); i++) {

            char c = htmlText.charAt(i);
            // System.out.println("state=" + state + " c=" + c + " stack=" + stack);

            switch (state) {
                case READ:
                    if (c == '<') {
                        state = StripCommentState.LT;
                        stack.append(c);
                    } else {
                        result.append(c);
                    }
                    break;
                case LT:
                    stack.append(c);
                    if (c == '!') {
                        state = StripCommentState.EXCLAMATION_MARK;
                    } else {
                        result.append(stack.toString());
                        stack = new StringBuilder();
                        state = StripCommentState.READ;
                    }
                    break;
                case EXCLAMATION_MARK:
                    stack.append(c);
                    if (c == '-') {
                        state = StripCommentState.COLON_1;
                    } else {
                        result.append(stack.toString());
                        stack = new StringBuilder();
                        state = StripCommentState.READ;
                    }
                    break;
                case COLON_1:
                    if (c == '-') {
                        state = StripCommentState.IGNORE;
                        stack = new StringBuilder();
                    } else {
                        stack.append(c);
                        result.append(stack.toString());
                        stack = new StringBuilder();
                        state = StripCommentState.READ;
                    }
                    break;
                case IGNORE:
                    if (c == '-') {
                        state = StripCommentState.COLON_3;
                    }
                    break;
                case COLON_3:
                    if (c == '-') {
                        state = StripCommentState.COLON_4;
                    } else {
                        state = StripCommentState.IGNORE;
                    }
                    break;
                case COLON_4:
                    if (c == '>') {
                        state = StripCommentState.READ;
                    } else {
                        state = StripCommentState.IGNORE;
                    }
                    break;
            }

        }
        return result.toString();
    }

    /**
     * <p>
     * Removes all instances of a specific HTML tag including its content from the supplied string, i.e. everything
     * within the specified tag is removed. For example <code>This is a text &lt;b&gt;with bold words&lt;/b&gt;</code>
     * is transformed to <code>This is a text</code>.
     * </p>
     * 
     * @param htmlText The string with HTML tags to remove.
     * @param tagName The name of the specific tag to remove.
     * @return The string without the specified tag.
     */
    public static String stripHtmlTagAndContent(String htmlText, String tagName) {

        // store the result
        StringBuilder result = new StringBuilder(htmlText.length());
        // store temporary text, where we are not yet sure whether to keep it
        StringBuilder stack = new StringBuilder();
        StripTagState stripTagState = StripTagState.READ;

        for (int i = 0; i < htmlText.length(); i++) {

            char c = htmlText.charAt(i);
            // System.out.println("state=" + state + " c=" + c + " stack=" + stack);

            switch (stripTagState) {

                case READ:
                    if (c == '<') {
                        stripTagState = StripTagState.TAG_NAME_1;
                    } else {
                        result.append(c);
                    }
                    break;

                // we are inside a tag and read its name
                case TAG_NAME_1:
                    if (c == ' ' || c == '>') {
                        if (stack.toString().equals(tagName)) {
                            stripTagState = StripTagState.IGNORE;
                        } else {
                            result.append('<').append(stack.toString()).append(c);
                            stripTagState = StripTagState.READ;
                        }
                        stack = new StringBuilder();
                    } else {
                        stack.append(c);
                    }
                    break;

                // we ignore content
                case IGNORE:
                    if (c == '<') {
                        stripTagState = StripTagState.TAG_NAME_2;
                    }
                    break;

                // we read a tag name in ignore mode, we are only interested in closing tags here, if its not a closing
                // tag, stay in ignore mode
                case TAG_NAME_2:
                    if (c == '/') {
                        stripTagState = StripTagState.CLOSE_TAG_NAME;
                    } else {
                        stripTagState = StripTagState.IGNORE;
                    }
                    break;

                // we have a closing tag in ignore mode, if the ignored tag is closed, switch to read mode, elsewise
                // stay in ignorance mode
                case CLOSE_TAG_NAME:
                    if (c == '>') {
                        if (stack.toString().equals(tagName)) {
                            stripTagState = StripTagState.READ;
                        } else {
                            stripTagState = StripTagState.IGNORE;
                        }
                        stack = new StringBuilder();
                    } else {
                        stack.append(c);
                    }
                    break;
            }

        }
        return result.toString();
    }
    
    public static void main(String[] abc) {
//        String text = new DocumentRetriever().getText("http://blog.fefe.de/?q=noch");
         String text = new DocumentRetriever().getText("http://cinefreaks.com");
        StopWatch stopWatch = new StopWatch();
        System.out.println(text.length() / 1024.0 + " KB");
//        String t = HtmlHelper.stripHtmlTags(text, true, true, false, false);
        String t = HtmlStripper.stripHtmlTags(text);
        System.out.println(t.length() / 1024.0 + " KB");
        System.out.println(stopWatch.getTotalElapsedTimeString());
        System.out.println(StringHelper.shorten(t, 2000));
        System.exit(0);
    }

}
