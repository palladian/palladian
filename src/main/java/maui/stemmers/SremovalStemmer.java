package maui.stemmers;

/**
 * A basic stemmer that only performs the first step of the 
 * PorterStemmer algorithm: removing of the plural endings.
 * @author olena
 *
 */

public class SremovalStemmer extends Stemmer {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String stem(String str)  {
        // check for zero length
	if (str.length() > 3) {
	    // all characters must be letters
	    char[] c = str.toCharArray();
	    for (int i = 0; i < c.length; i++) {
		if (!Character.isLetter(c[i])) {
		    return str.toLowerCase();
		}
	    }
	} else {            
	    return str.toLowerCase();
	}
	str = step1a(str);
	return str.toLowerCase();
    } // end stem

    protected String step1a (String str) {
        // SSES -> SS
        if (str.endsWith("sses")) {
            return str.substring(0, str.length() - 2);
        // IES -> I
        } else if (str.endsWith("ies")) {
            return str.substring(0, str.length() - 2);
        // SS -> S
        } else if (str.endsWith("ss")) {
            return str;
        // S ->
        } else if (str.endsWith("s")) {
            return str.substring(0, str.length() - 1);
        } else {
            return str;
        }
    } // end step1a

    /*
       -------------------------------------------------------
       The following are functions to help compute steps 1 - 5
       -------------------------------------------------------
    */

    // does string end with 's'?
    protected boolean endsWithS(String str) {
        return str.endsWith("s");
    } // end function

    // does string contain a vowel?
    protected boolean containsVowel(String str) {
        char[] strchars = str.toCharArray();
        for (int i = 0; i < strchars.length; i++) {
            if (isVowel(strchars[i]))
                return true;
        }
        // no aeiou but there is y
        if (str.indexOf('y') > -1)
            return true;
        else
            return false;
    } // end function

    // is char a vowel?
    public boolean isVowel(char c) {
        if ((c == 'a') ||
            (c == 'e') ||
            (c == 'i') ||
            (c == 'o') ||
            (c == 'u'))
            return true;
        else
            return false;
    } // end function

    // does string end with a double consonent?
    protected boolean endsWithDoubleConsonent(String str) {
        char c = str.charAt(str.length() - 1);
        if (c == str.charAt(str.length() - 2))
            if (!containsVowel(str.substring(str.length() - 2))) {
                return true;
        }
        return false;
    } // end function

    // returns a CVC measure for the string
    protected int stringMeasure(String str) {
        int count = 0;
        boolean vowelSeen = false;
        char[] strchars = str.toCharArray();

        for (int i = 0; i < strchars.length; i++) {
            if (isVowel(strchars[i])) {
                vowelSeen = true;
            } else if (vowelSeen) {
                count++;
                vowelSeen = false;
            }
        } // end for
        return count;
    } // end function

    // does stem end with CVC?
    protected boolean endsWithCVC (String str) {
        char c, v, c2 = ' ';
        if (str.length() >= 3) {
            c = str.charAt(str.length() - 1);
            v = str.charAt(str.length() - 2);
            c2 = str.charAt(str.length() - 3);
        } else {
            return false;
        }

        if ((c == 'w') || (c == 'x') || (c == 'y')) {
            return false;
        } else if (isVowel(c)) {
            return false;
        } else if (!isVowel(v)) {
            return false;
        } else if (isVowel(c2)) {
            return false;
        } else {
            return true;
        }
    } // end function
} // end class
