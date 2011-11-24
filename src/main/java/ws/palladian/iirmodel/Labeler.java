/**
 * Created on: 21.11.2011 15:46:21
 */
package ws.palladian.iirmodel;

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * <p>
 * A person adding {@link Label}s to {@link Item}s. By knowing who has added which {@code Label} to an item it is
 * possible to manage multiple {@code Label}s per {@code Item} and thus calculate agreement measures like the kappa
 * measure for a dataset.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 3.0.0
 * @version 1.0.0
 */
@Entity
public final class Labeler {
    /**
     * <p>
     * The {@code name} of this {@code Labeler}. This might be some nickname or the real name. It is only important that
     * this name is a unique identifier for the {@code Labeler} to find the {@code Label}s he or she provided.
     * </p>
     */
    @Id
    private final String name;
    /**
     * <p>
     * The {@code Label}s this {@code Labeler} provided.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    private final Collection<Label> labels;

    /**
     * <p>
     * Creates a new {@code Labeler} with no {@code Label}s provided initially.
     * </p>
     * 
     * @param name The {@code name} of this {@code Labeler}.
     */
    public Labeler(final String name) {
        super();
        this.name = name;
        this.labels = new HashSet<Label>();
    }

    /**
     * <p>
     * This default constructor is only required by JPA to create objects of this class via reflection. It should never
     * be called directly.
     * </p>
     */
    protected Labeler() {
        super();
        this.name = null;
        this.labels = new HashSet<Label>();
    }

    /**
     * <p>
     * Provides the {@code name} of this {@code Labeler}. This might be some nickname or the real name. It is only
     * important that this name is a unique identifier for the {@code Labeler} to find the {@code Label}s he or she
     * provided.
     * </p>
     * 
     * @return the {@code name} of the {@code Labeler}.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Returns the {@link Lable}s this {@code Labeler} provided.
     * </p>
     * 
     * @return the {@code Label}s this {@code Labeler} provided.
     */
    public Collection<Label> getLabels() {
        Collection<Label> ret = new HashSet<Label>();
        ret.addAll(labels);
        return ret;
    }

    /**
     * <p>
     * Adds a new {@link Label} to the {@code Collection} of {@code Label}s this {@code Labeler} has provided.
     * </p>
     * 
     * @param label The new {@code Label} to add.
     * @return {@code true} if the label was successfully added; {@code false} otherwise.
     * @see Collection
     */
    public Boolean addLabel(final Label label) {
        return this.labels.add(label);
    }
}
