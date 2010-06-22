package tud.iir.classification;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A term is a sequence of characters.
 * 
 * @author David Urbansky
 * 
 */
public class Term implements Serializable {

    private static final long serialVersionUID = 149355295388274193L;
    private String text = "";

    public Term(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

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
    public String toString() {
        return "Term [text=" + text + "]";
    }

    public static void main(String[] a) {
        Term t1 = new Term("abc");
        Term t2 = new Term("abc");
        Term t3 = new Term("def");
        System.out.println(t1.equals(t2));
        System.out.println(t1.equals(t3));
        HashMap<Term, Integer> termMap = new HashMap<Term, Integer>();
        termMap.put(t1, 1);
        termMap.put(t3, 3);
        System.out.println(termMap.keySet().contains(t2));

    }
}