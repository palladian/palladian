package ws.palladian.helper.nlp;

import java.util.EmptyStackException;

/**
 * A character-based stack, implemented using a {@link StringBuilder}.
 * 
 * @author pk
 */
public class CharStack implements CharSequence {

    private final StringBuilder builder = new StringBuilder();

    public void push(char ch) {
        builder.append(ch);
    }

    public char pop() {
        char c = peek();
        builder.setLength(builder.length() - 1);
        return c;
    }

    public char peek() {
        int last = builder.length() - 1;
        if (last < 0) {
            throw new EmptyStackException();
        }
        return builder.charAt(last);
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    public char charAt(int index) {
        return builder.charAt(index);
    }

    @Override
    public int length() {
        return builder.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return builder.subSequence(start, end);
    }

}
