package tud.iir.knowledge;

import java.util.ArrayList;

/**
 * 
 * This class holds HTML-symbols and its conversion to normal text.
 * 
 * @author Martin Gregor
 * 
 */
public class HTMLSymbols {
    /** Protected whitespace. */
    public static final String[] emptyWhitsp = { "&#8203;", " " };
    /** Protected whitespace. */
    public static final String[] NBSP = { "&nbsp;", " " };
    /** Protected whitespace. */
    public static final String[] NBSP2 = { "\u00A0", " " };
    /** Qutemark &quot;. */
    public static final String[] QUOT = { "&quot;", "\"" };
    /** Paragraph &amp;. */
    public static final String[] AMP = { "&amp;", "&" };
    /** Less then &lt;. */
    public static final String[] LT = { "&lt;", "<" };
    /** Greater then &gt;. */
    public static final String[] GT = { "&gt;", ">" };
    /** Letter ä. */
    public static final String[] AUML = { "&auml;", "ä" };
    /** Letter Ä. */
    public static final String[] AAUML = { "&Auml;", "Ä" };
    /** Letter ö. */
    public static final String[] OUML = { "&ouml;", "ö" };
    /** Letter Ö. */
    public static final String[] OOUML = { "&Ouml;", "Ö" };
    /** Letter ü. */
    public static final String[] UUML = { "&uuml;", "ü" };
    /** Letter Ü. */
    public static final String[] UUUML = { "&Uuml;", "Ü" };
    /** Letter ß. */
    public static final String[] SZLIG = { "&szlig;", "ß" };
    /** New Line \n */
    public static final String[] NL = { "\n", " " };
    /** Tabulator */
    public static final String[] TABHTML = { "&#09;", " " };
    /** Tabulator */
    public static final String[] TAB = { "\t", " " };
    /**  */
    public static final String[] COM = { " ,", " " };

    // TODO complete HTML-symbols.

    /**
     * Returns all string arrays of HTML symbols. <br>
     * A HTML symbol for example is &nbsp; which stands for a whitespace. <br>
     * A list-element consist of the HTML-code and the corresponding symbol.
     */
    public static ArrayList<String[]> getHTMLSymboles() {
        ArrayList<String[]> array = new ArrayList<String[]>();
        array.add(TABHTML);
        array.add(NL);
        array.add(NBSP);
        array.add(NBSP2);
        array.add(TAB);
        array.add(QUOT);
        array.add(AMP);
        array.add(LT);
        array.add(GT);
        array.add(AUML);
        array.add(AAUML);
        array.add(OUML);
        array.add(OOUML);
        array.add(UUML);
        array.add(UUUML);
        array.add(SZLIG);
        array.add(emptyWhitsp);
        array.add(COM);
        // TODO enter all HTML-symboles.
        return array;
    }
}
