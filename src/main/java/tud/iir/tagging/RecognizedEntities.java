package tud.iir.tagging;

import java.util.HashSet;

public class RecognizedEntities extends HashSet<RecognizedEntity> {

    private static final long serialVersionUID = 2807611986363171430L;

    /**
     * Check whether ArrayList contains obj.
     * 
     * @return True if the obj is contained, false otherwise.
     */
    @Override
    public boolean contains(Object obj) {
        String entityName = (String) obj;

        for (RecognizedEntity e : this) {
            if (e.getName().equalsIgnoreCase(entityName))
                return true;
        }

        return false;
    }

}
