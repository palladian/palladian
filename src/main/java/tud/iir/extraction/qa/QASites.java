package tud.iir.extraction.qa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import tud.iir.helper.FileHelper;

/**
 * A list of question answer site definitions.
 * 
 * @author David Urbansky
 * 
 */
public class QASites extends ArrayList<QASite> implements Serializable {

    private static final long serialVersionUID = 7038194843336481216L;

    public int getTotalURLStackSize() {
        int stackSize = 0;

        for (Iterator<QASite> iterator = iterator(); iterator.hasNext();) {
            stackSize += iterator.next().getURLStackSize();
        }

        return stackSize;
    }

    /**
     * Serialize state of QASite extraction to resume later on.
     */
    public void serialize() {
        FileHelper.serialize(this, "data/status/qaSites.ser");
    }

}