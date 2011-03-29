package ws.palladian.helper;

public class LoremIpsumGenerator {

    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    public static String generateText(int words) {
        String[] possibleWords = LOREM_IPSUM.split(" ");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words; i++) {
            sb.append(possibleWords[i % 50]).append(" ");
        }

        sb.deleteCharAt(sb.length() - 1).append(".");

        return sb.toString();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(LoremIpsumGenerator.generateText(400));
    }

}
