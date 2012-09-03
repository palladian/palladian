package ws.palladian.helper.normalization;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * The string normalizer normalizes strings.
 * 
 * @author David Urbansky
 */
public class StringNormalizer {

    /**
     * <p>Different number formats do not match if compared, thus they have to be normalized before. e.g. 40,000 = 40000 and 4.00 = 4.0 = 4 but 6,6 should be equal
     * to 6.6.</p>
     * 
     * @param numberString The string with a number.
     * @return The normalized number as a string.
     */
    public static String normalizeNumber(String numberString) {

        if (numberString.length() == 0) {
            return "";
        }

        Locale.setDefault(Locale.ENGLISH);
        DecimalFormat formatter = new DecimalFormat("#.###");

        try {
            numberString = formatter.format(Double.valueOf(numberString));
        } catch (NumberFormatException e) {
            // e.printStackTrace();
        }

        numberString = numberString.replaceAll("\\.(0){1,}(?!(\\d))", ""); // if only zeros after . remove all of these
        // System.out.println(numberString);
        // numberString = numberString.replaceAll("(?<=[1-9])0{1,}(?!(,|\\.|(\\d)))",""); // delete all trailed zeros
        // System.out.println(numberString);
        numberString = numberString.replaceAll(",(?=((\\d){3}(\\.|,|(\\W)|(\\Z))))", "").replaceAll(" ", ""); // remove thousand separators (space or comma)
        // System.out.println(numberString);
        numberString = numberString.replaceAll(",", "."); // for well-formatted numbers only max. one , or . should be in the string => tranform it to .

        int pointIndex = numberString.indexOf(".");
        if (pointIndex > -1) {
            String afterPointPart = numberString.substring(pointIndex + 1);
            afterPointPart = afterPointPart.replaceAll("(?<=[1-9])0{1,}(?!(,|\\.|(\\d)))", ""); // delete all trailed zeros
            numberString = numberString.substring(0, pointIndex) + "." + afterPointPart;
        }

        return numberString;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        System.out.println(normalizeNumber("30,000,000.00"));
        System.out.println(normalizeNumber("30,000,000.10"));
        System.out.println(normalizeNumber("30,000,000?"));
        System.out.println(normalizeNumber("30 000 000!"));
        System.out.println(normalizeNumber("30,000,000.004500"));
        System.out.println(normalizeNumber("30,234523000"));
        System.out.println(normalizeNumber("4,07000"));
        System.out.println(normalizeNumber("4.4560000"));
        System.out.println(normalizeNumber("7,500,000"));
        System.out.println(normalizeNumber("7,500,400"));
        System.out.println(normalizeNumber("1990"));

        // upper case test
        System.out.println((Character.isUpperCase('3')));

    }
}