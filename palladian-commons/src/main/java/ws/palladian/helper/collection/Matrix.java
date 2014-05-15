package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * A data matrix. The data can have arbitrary types for keys (i.e. indices for rows/columns) and cell content.
 * </p>
 * 
 * @author pk
 * 
 * @param <K> Type of the indices.
 * @param <V> Type of the data.
 */
public interface Matrix<K, V> extends Serializable {

    /**
     * <p>
     * Get data at the specified x/y position.
     * </p>
     * 
     * @param x Key/index of the column.
     * @param y Key/index of the row.
     * @return The value.
     */
    V get(K x, K y);

    /**
     * <p>
     * Set data at the specified x/y position.
     * </p>
     * 
     * @param x Key/index of the column.
     * @param y Key/index of the row.
     * @param value The value.
     */
    void set(K x, K y, V value);

    /**
     * @return The column keys in this matrix.
     */
    Set<K> getColumnKeys();

    /**
     * @return The row keys in this matrix.
     */
    Set<K> getRowKeys();

    /**
     * @return The number of columns in this matrix.
     */
    int columnCount();

    /**
     * @return The number of rows in this matrix.
     */
    int rowCount();

    /**
     * <p>
     * Clears the matrix of all existing entries.
     * </p>
     */
    void clear();

    /**
     * <p>
     * Get a row from this matrix.
     * </p>
     * 
     * @param y Key/index of the row, not <code>null</code>.
     * @return The row.
     */
    Vector<K, V> getRow(K y);

    /**
     * <p>
     * Get a column from this matrix.
     * </p>
     * 
     * @param x Key/index of the column, not <code>null</code>.
     * @return The column.
     */
    Vector<K, V> getColumn(K x);

    /**
     * @return A CSV representation of this matrix.
     */
    String toCsv();

}
