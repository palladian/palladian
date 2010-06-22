package tud.iir.extraction;

/**
 * The Filter class specifies thresholds for entity and fact trusts.
 * 
 * @author David Urbansky
 */
public class Filter {

    private static Filter instance = null;

    public static double minEntityCorroboration = 1.0; // how much corroboration an entity needs to pass the filter
    public static double minFactCorroboration = 1.0;

    private Filter() {
    }

    public static Filter getInstance() {
        if (instance == null)
            instance = new Filter();
        return instance;
    }

}
