package de.philippkatz;

public class HtmlStripper {

    private enum State {
        READ, TAG_NAME_1, IGNORE, TAG_NAME_2, CLOSE_TAG_NAME
    };
    
    public static String stripHtmlTags(String htmlText) {
        boolean inTag = false;
        StringBuilder result = new StringBuilder(htmlText.length());
        for (int i = 0; i < htmlText.length(); i++) {
            char c = htmlText.charAt(i);
            if (c == '<') {
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            } else {
                if (!inTag) {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    /**
     * <p>
     * State machine for the win! Happy debugging! Everybody will hate me for this.
     * </p>
     * 
     * @param htmlText
     * @param tagName
     * @return
     */
    public static String stripHtmlTagsWithContent(String htmlText, String tagName) {

        StringBuilder result = new StringBuilder(htmlText.length());
        StringBuilder stack = new StringBuilder();
        State state = State.READ;

        for (int i = 0; i < htmlText.length(); i++) {

            char c = htmlText.charAt(i);
            // System.out.println("state=" + state + " c=" + c + " stack=" + stack);

            switch (state) {

                case READ:
                    // normal read mode
                    if (c == '<') {
                        state = State.TAG_NAME_1;
                    } else {
                        result.append(c);
                    }
                    break;

                // we are inside a tag and read its name
                case TAG_NAME_1:
                    if (c == ' ' || c == '>') {
                        if (stack.toString().equals(tagName)) {
                            state = State.IGNORE;
                        } else {
                            result.append('<').append(stack.toString()).append(c);
                            state = State.READ;
                        }
                        stack = new StringBuilder();
                    } else {
                        stack.append(c);
                    }
                    break;

                // we ignore content
                case IGNORE:
                    if (c == '<') {
                        state = State.TAG_NAME_2;
                    }
                    break;

                // we read a tag name in ignore mode, we are only interested in closing tags here, if its not a closing
                // tag, stay in ignore mode
                case TAG_NAME_2:
                    if (c == '/') {
                        state = State.CLOSE_TAG_NAME;
                    } else {
                        state = State.IGNORE;
                    }
                    break;

                // we have a closing tag in ignore mode, if the ignored tag is closed, switch to read mode, elsewise
                // stay in ignorance mode
                case CLOSE_TAG_NAME:
                    if (c == '>') {
                        if (stack.toString().equals(tagName)) {
                            state = State.READ;
                        } else {
                            state = State.IGNORE;
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

    public static void main(String[] args) {
        String htmlText = "one <b>two</b> three <style>xxx</style><b>yyyy <style/>";
        String strippedText = stripHtmlTagsWithContent(htmlText, "style");
        System.out.println(strippedText);
    }

}
