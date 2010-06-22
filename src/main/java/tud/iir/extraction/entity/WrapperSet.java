package tud.iir.extraction.entity;

import java.util.HashSet;
import java.util.Iterator;

/**
 * A set of wrappers.
 * 
 * @author David Urbansky
 * @param <W>
 */
class WrapperSet<W> extends HashSet<AffixWrapper> {

    private static final long serialVersionUID = 821249889810177086L;
    private int minWrapperSize = 6;

    @Override
    public boolean add(AffixWrapper e) {
        if (contains(e))
            return false;
        return super.add(e);
    }

    @Override
    public boolean contains(Object obj) {
        AffixWrapper wrapper = (AffixWrapper) obj;

        Iterator<AffixWrapper> i = this.iterator();
        while (i.hasNext()) {
            AffixWrapper w = i.next();
            if (w.equals(wrapper))
                return true;
        }

        return false;
    }

    public void removeShortWrappers() {
        removeShortWrappers(getMinWrapperSize());
    }

    public void removeShortWrappers(int minWrapperSize) {

        WrapperSet<AffixWrapper> clearedSet = new WrapperSet<AffixWrapper>();

        Iterator<AffixWrapper> wrapperIterator = this.iterator();
        while (wrapperIterator.hasNext()) {
            AffixWrapper w = wrapperIterator.next();
            if (w.getPrefix().length() >= minWrapperSize && w.getSuffix().length() >= minWrapperSize) {
                clearedSet.add(w);
            }
        }

        this.clear();
        this.addAll(clearedSet);
    }

    /**
     * No prefix can have another prefix as suffix. No suffix can have another suffix as prefix.
     */
    public void removeSubWrappers() {
        WrapperSet<AffixWrapper> clearedSet = new WrapperSet<AffixWrapper>();
        clearedSet.addAll(this);

        Iterator<AffixWrapper> wrapperIterator = this.iterator();
        while (wrapperIterator.hasNext()) {
            AffixWrapper w = wrapperIterator.next();

            Iterator<AffixWrapper> wrapperIterator2 = this.iterator();
            while (wrapperIterator2.hasNext()) {
                AffixWrapper w2 = wrapperIterator2.next();

                if (w.equals(w2))
                    continue;

                int deleteVote = 0;

                if (w.getPrefix().toLowerCase().endsWith(w2.getPrefix().toLowerCase())) {
                    deleteVote++;
                }
                if (w.getSuffix().toLowerCase().startsWith(w2.getSuffix().toLowerCase())) {
                    deleteVote++;
                }

                if (deleteVote == 2 && clearedSet.contains(w2)) {
                    clearedSet.remove(w2);
                }
            }
        }
        this.clear();
        this.addAll(clearedSet);
    }

    public int getMinWrapperSize() {
        return minWrapperSize;
    }

    public void setMinWrapperSize(int minWrapperSize) {
        this.minWrapperSize = minWrapperSize;
    }
}