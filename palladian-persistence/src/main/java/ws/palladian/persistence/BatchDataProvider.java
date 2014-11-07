package ws.palladian.persistence;

import java.util.List;

/**
 * <p>
 * This is a callback interface which can be used to provide data for batch database updates.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface BatchDataProvider {

    /**
     * <p>
     * Retrieve one data item for the batch. The list provides the parameters for the SQL statement.
     * </p>
     * 
     * @param number Number in the data, starting with 0.
     * @return List with parameters for the SQL statement, not <code>null</code>.
     */
    List<? extends Object> getData(int number);

    /**
     * <p>
     * Returns the generated ID for an inserted item. This is triggered after the data has been retrieved via
     * {@link #getData(int)} and has been inserted successfully.
     * </p>
     * 
     * @param number Number in the data, starting with 0.
     * @param generatedId The generated ID for the inserted item, <code>-1</code>, in case no ID was generated.
     */
    void insertedItem(int number, int generatedId);

    /**
     * <p>
     * Retrieve the number of items in the batch. This is the number of instances which have to be delivered via
     * {@link #getData(int)}. This method is invoked once, before the the data is retrieved.
     * </p>
     * 
     * @return The number of items in the batch.
     */
    int getCount();

}
