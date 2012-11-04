package ws.palladian.helper.nlp;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Jaro–Winkler_distance">Jaro—Winkler distance</a> metric,
 * an extension of the Jaro metric. For more information, see
 * "String Comparator Metrics and Enhanced Decision Rules in the Fellegi-Sunter Model of Record Linkage", William E.
 * Winkler, 1990 and "Overview of Record Linkage and Current Research Directions", William E. Winkler, 2006.
 * </p>
 * 
 * @see <a href="http://web.archive.org/web/20100227020019/http://www.census.gov/geo/msb/stand/strcmp.c">Original
 *      Jaro-Winkler implementation in C</a>
 * @author Philipp Katz
 */
public class JaroWinklerSimilarity implements StringSimilarity {

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");

        // trim and transform both strings to UPPERCASE
        s1 = s1.trim().toUpperCase();
        s2 = s2.trim().toUpperCase();

        // strings are exactly equal
        if (s1.equals(s2)) {
            return 1;
        }

        int l1 = s1.length();
        int l2 = s2.length();

        if (l1 == 0 || l2 == 0) {
            return 0;
        }

        // first string must not be longer, so exchange the two if necessary
        if (l1 > l2) {
            String temp = s1;
            s1 = s2;
            s2 = temp;
            l1 = s1.length();
            l2 = s2.length();
        }

        boolean[] s1flag = new boolean[l2];
        boolean[] s2flag = new boolean[l2];
        Arrays.fill(s1flag, false);
        Arrays.fill(s2flag, false);

        // # of matching characters
        int m = 0;
        int range = l2 / 2;
        for (int i = 0; i < l1; i++) {
            for (int j = Math.max(0, i - range); j < Math.min(l2, i + range); j++) {
                if (!s2flag[j] && s1.charAt(i) == s2.charAt(j)) {
                    s2flag[j] = true;
                    s1flag[i] = true;
                    m++;
                    break;
                }
            }
        }

        if (m == 0) {
            return 0;
        }

        // get # of transpositions
        int t = 0;
        int k = 0;
        for (int i = 0; i < l1; i++) {
            if (s1flag[i]) {
                int j;
                for (j = k; j < l2; j++) {
                    if (s2flag[j]) {
                        k = j + 1;
                        break;
                    }
                }
                if (s1.charAt(i) != s2.charAt(j)) {
                    t++;
                }
            }
        }
        t /= 2;

        double jaro = ((double)m / l1 + (double)m / l2 + (double)(m - t) / m) / 3;

        // length of common prefix, up to 4 characters max.
        int l = 0;
        for (l = 0; l < Math.min(4, s1.length()) && s1.charAt(l) == s2.charAt(l); l++) {
        }
        return jaro + l * 0.1 * (1. - jaro);
    }

}
