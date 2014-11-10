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

    /** @deprecated Use {@link RowConverters#BOOLEAN} instead */
    @Deprecated
    public final static RowConverter<Boolean> BOOLEAN = RowConverters.BOOLEAN;

    /** @deprecated Use {@link RowConverters#INTEGER} instead */
    @Deprecated
    public final static RowConverter<Integer> INTEGER = RowConverters.INTEGER;

    /** @deprecated Use {@link RowConverters#DOUBLE} instead */
    @Deprecated
    public final static RowConverter<Double> DOUBLE = RowConverters.DOUBLE;

    /** @deprecated Use {@link RowConverters#LONG} instead */
    @Deprecated
    public final static RowConverter<Long> LONG = RowConverters.LONG;

    /** @deprecated Use {@link RowConverters#STRING} instead */
    @Deprecated
    public final static RowConverter<String> STRING = RowConverters.STRING;

}
