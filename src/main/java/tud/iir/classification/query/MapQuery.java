package tud.iir.classification.query;

/**
 * Map a query to an entity.
 * 
 * @author David
 */
public class MapQuery {

    /**
     * @param args
     */
    public static void main(String[] args) {

        QueryWord qw = new QueryWord("i7110");
        qw.addWord("Samsung", QueryWord.LEFT, 0);
        qw.addWord("Samsung", QueryWord.LEFT, 0);
        qw.addWord("Samsung", QueryWord.LEFT, 0);
        qw.addWord("all", QueryWord.LEFT, 0);
        qw.addWord("the", QueryWord.LEFT, 0);
        qw.addWord("Samsung", QueryWord.LEFT, 0);
        qw.addWord("new", QueryWord.LEFT, 0);
        qw.addWord("Samsung", QueryWord.LEFT, 0);
        qw.addWord("Samsung", QueryWord.LEFT, 0);

        qw.addWord("by", QueryWord.RIGHT, 0);
        qw.addWord("from", QueryWord.RIGHT, 0);
        qw.addWord("review", QueryWord.RIGHT, 0);
        qw.addWord("features", QueryWord.RIGHT, 0);
        qw.addWord("can", QueryWord.RIGHT, 0);
        qw.addWord("is", QueryWord.RIGHT, 0);
        qw.addWord("is", QueryWord.RIGHT, 0);
        qw.addWord("is", QueryWord.RIGHT, 0);
        qw.addWord("is", QueryWord.RIGHT, 0);

        System.out.println(qw.getFullEntityName());
    }

}
