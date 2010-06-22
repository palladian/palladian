package tud.iir.tagging;

import java.util.ArrayList;
import java.util.Collection;

public class EntityList extends ArrayList<RecognizedEntity> {

    private static final long serialVersionUID = 5254024924684976487L;

    public RecognizedEntity getEntity(String name) {

        for (RecognizedEntity e : this) {
            if (e.getName().equalsIgnoreCase(name))
                return e;
        }

        return null;
    }

    @Override
    public boolean add(RecognizedEntity e) {

        for (RecognizedEntity re : this) {
            if (re.equals(e)) {
                re.addTrust(e.getTrust());
                re.addCategoryEntries(e.getCategoryEntries());
                return false;
            }
        }

        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends RecognizedEntity> c) {

        boolean allAdded = true;

        for (RecognizedEntity re : c) {
            if (!add(re)) {
                allAdded = false;
            }
        }

        return allAdded;
    }

}