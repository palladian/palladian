package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.functional.Factory;

/**
 * A {@link LazyMatrix} is a matrix which never returns <code>null</code>, when invoking {@link #get(Object, Object)}.
 * Instead, when the requested cell is absent, a {@link Factory} is used to create it and put it in the matrix.
 *
 * @param <K>
 * @param <V>
 * @author Philipp Katz
 * @see LazyMap
 */
public class LazyMatrix<K, V> extends MatrixDecorator<K, V> {
    private static final long serialVersionUID = 1L;

    private final Factory<V> factory;

    public LazyMatrix(Matrix<K, V> matrix, Factory<V> factory) {
        super(matrix);
        Validate.notNull(matrix, "matrix must not be null");
        Validate.notNull(factory, "factory must not be null");
        this.factory = factory;
    }

    public LazyMatrix(Factory<V> factory) {
        this(new MapMatrix<>(), factory);
    }

    /**
     * @return The wrapped {@link Matrix}.
     */
    public Matrix<K, V> getMatrix() {
        return matrix;
    }

    @Override
    public V get(K x, K y) {
        V value = matrix.get(x, y);
        if (value == null) {
            value = factory.create();
            matrix.set(x, y, value);
        }
        return value;
    }
}
