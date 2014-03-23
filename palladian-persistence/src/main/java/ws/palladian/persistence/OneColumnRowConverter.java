package ws.palladian.persistence;


/**
 * <p>
 * A set of predefined {@link RowConverter}s which retrieve one column from the result of the desired type. The
 * converters are singletons, their instance can be retrieved using the constants, e.g. {@link RowConverters#STRING}.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated Use {@link RowConverters} instead.
 */
@Deprecated
public final class OneColumnRowConverter {

    private OneColumnRowConverter() {
        // prevent instances.
    }

    /**
     * <p>
     * A {@link RowConverter} for {@link Boolean} types.
     * </p>
     * @deprecated Use {@link RowConverters#BOOLEAN} instead
     */
    public final static RowConverter<Boolean> BOOLEAN = RowConverters.BOOLEAN;

    /**
     * <p>
     * A {@link RowConverter} for {@link Integer} types.
     * </p>
     * @deprecated Use {@link RowConverters#INTEGER} instead
     */
    public final static RowConverter<Integer> INTEGER = RowConverters.INTEGER;

    /**
     * <p>
     * A {@link RowConverter} for {@link Double} types.
     * </p>
     * @deprecated Use {@link RowConverters#DOUBLE} instead
     */
    public final static RowConverter<Double> DOUBLE = RowConverters.DOUBLE;

    /**
     * <p>
     * A {@link RowConverter} for {@link Long} types.
     * </p>
     * @deprecated Use {@link RowConverters#LONG} instead
     */
    public final static RowConverter<Long> LONG = RowConverters.LONG;

    /**
     * <p>
     * A {@link RowConverter} for {@link String} types.
     * </p>
     * @deprecated Use {@link RowConverters#STRING} instead
     */
    public final static RowConverter<String> STRING = RowConverters.STRING;

}
