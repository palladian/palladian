package ws.palladian.core;

import ws.palladian.helper.functional.Function;

public interface Token extends Comparable<Token> {

    /** Function to convert a {@link Token} to its String value. */
    Function<Token, String> STRING_CONVERTER = new Function<Token, String>() {
        @Override
        public String compute(Token input) {
            return input.getValue();
        }
    };

    /**
     * @return The start offset of this annotation in the text (first character in text is zero).
     */
    int getStartPosition();

    /**
     * @return The end offset of this annotation in the text (this is startPosition + value.length()).
     */
    int getEndPosition();

    /**
     * @return The string value of this annotation.
     */
    String getValue();

    /**
     * <p>
     * Determine, whether this annotation overlaps another given annotation (i.e. start/end boundaries are within/on the
     * on the other annotation).
     * </p>
     * 
     * @param other The other annotation, not <code>null</code>.
     * @return <code>true</code> in case this annotation overlaps the given one, <code>false</code> otherwise.
     */
    boolean overlaps(Token other);

    /**
     * <p>
     * Determine, whether this and the given annotation are congruent (i.e. start and end position are the same).
     * <p>
     * 
     * @param other The other annotation, not <code>null</code>.
     * @return <code>true</code> in case this annotation and the given are congruent, <code>false</code> otherwise.
     */
    boolean congruent(Token other);

}
