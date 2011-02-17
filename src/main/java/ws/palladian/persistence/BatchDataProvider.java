package ws.palladian.persistence;

import java.util.List;

/**
 * This is a callback interface which can be used to provide data for batch database updates.
 * 
 * @author Philipp Katz
 */
public interface BatchDataProvider {

    /**
     * Return data for the batch.
     * 
     * @param number Number in the data, starting with 0.
     * @return
     */
    List<Object> getData(int number);

    /**
     * Return the number of items in the batch. This is the number of instances which have to be delivered via
     * {@link #getData(int)}.
     * 
     * @return
     */
    int getCount();

}
