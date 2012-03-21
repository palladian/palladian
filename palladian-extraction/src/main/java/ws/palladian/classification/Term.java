package ws.palladian.classification;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A term is a sequence of characters.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * 
 */
public class Term implements Serializable {

    private static final long serialVersionUID = 149355295388274193L;

    /** The text of the term. */
    private String text = "";

    public Term(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    // removed the setter, as an assumption of immutable Terms allows us speed improvements inside the Matrix(tm)
    // Philipp, 2010-08-04
    
    // public void setText(String text) {
    // this.text = text;
    // }

    public void lowerCaseText() {
        this.text = this.text.toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Term)) {
            return false;
        }
        Term t = (Term) obj;
        return this.text.equals(t.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "Term [text=" + text + "]";
    }

    public static void main(String[] a) {
        Term t1 = new Term("abc");
        // Term t2 = new Term("abc");
        Term t2 = new Term("ab" + "c");
        Term t3 = new Term("def");
        System.out.println(t1.equals(t2));
        System.out.println(t1.equals(t3));
        HashMap<Term, Integer> termMap = new HashMap<Term, Integer>();
        termMap.put(t1, 1);
        termMap.put(t3, 3);
        System.out.println(termMap.keySet().contains(t2));

    }
}