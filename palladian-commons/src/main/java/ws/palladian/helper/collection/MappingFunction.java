package ws.palladian.helper.collection;

import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * {@link Function} which maps values as determined by a {@link Map}.
 * 
 * @author pk
 * 
 * @param <I> Input type.
 * @param <O> Output type.
 */
public final class MappingFunction<I, O> implements Function<I, O> {

    private final Map<? extends I, ? extends O> map;

    public MappingFunction(Map<? extends I, ? extends O> map) {
        Validate.notNull(map, "map must not be null");
        this.map = map;
    }

    @Override
    public O compute(I input) {
        return map.get(input);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MappingFunction [map=");
        builder.append(map);
        builder.append("]");
        return builder.toString();
    }

}
