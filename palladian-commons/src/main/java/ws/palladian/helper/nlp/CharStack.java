package ws.palladian.helper.nlp;

/**
 * A character-based stack, implemented using a {@link StringBuilder}.
 * 
 * @author pk
 */
public class CharStack {

    private final StringBuilder builder = new StringBuilder();

    public void push(char ch) {
        builder.append(ch);
    }

    public char pop() {
        int last = builder.length() - 1;
        char c = builder.charAt(last);
        builder.setLength(last);
        return c;
    }

    public char peek() {
        return builder.charAt(builder.length() - 1);
    }

    public int size() {
        return builder.length();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

}
