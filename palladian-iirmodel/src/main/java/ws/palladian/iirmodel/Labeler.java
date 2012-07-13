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
import javax.persistence.Table;
import javax.persistence.Transient;

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
@Table(name = "Labeler")
public final class Labeler implements Comparable<Labeler> {
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
     * The {@code password} of this {@link Labeler}. This can be used for authentication at the web application. The
     * password should not be stored in plain text of course.
     * </p>
     */
    @Transient
    private final String password;
    /**
     * <p>
     * The {@link Label}s this {@code Labeler} provided.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    private final Collection<Label> labels;

    /**
     * <p>
     * The {@link ItemRelation}s this {@code Labeler} provided.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    private final Collection<ItemRelation> relations;

    /**
     * <p>
     * Creates a new {@code Labeler} with no {@code Label}s provided initially and no specified password.
     * </p>
     * 
     * @param name The {@code name} of this {@code Labeler}.
     * @param password The {@code password} of this {@link Labeler}.
     */
    public Labeler(final String name, final String password) {
        super();
        this.name = name;
        this.password = password;
        this.labels = new HashSet<Label>();
        this.relations = new HashSet<ItemRelation>();
    }

    /**
     * <p>
     * Creates a new {@code Labeler} with no {@code Label}s provided initially and no specified password.
     * </p>
     * 
     * @param name The {@code name} of this {@code Labeler}.
     */
    public Labeler(final String name) {
        this(name, null);
    }

    /**
     * <p>
     * This default constructor is only required by JPA to create objects of this class via reflection. It should never
     * be called directly. Please use {@link #Labeler(String)} instead.
     * </p>
     */
    protected Labeler() {
        this(null);
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
     * Provides the {@code password} of this {@link Labeler}.
     * </p>
     * 
     * @return
     */
    public String getPassword() {
        return password;
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

    /**
     * @return The {@link ItemRelation}s this {@code Labeler} provided.
     */
    public Collection<ItemRelation> getRelations() {
        Collection<ItemRelation> ret = new HashSet<ItemRelation>();
        ret.addAll(relations);
        return ret;
    }

    /**
     * <p>
     * Adds a new {@link ItemRelation} to the relations provided by this {@code Labeler}.
     * </p>
     * 
     * @param relation The new {@code ItemRelation}.
     * @return {@code true} if adding the relation was successful; {@code false} otherwise.
     */
    public Boolean addRelation(final ItemRelation relation) {
        return this.relations.add(relation);
    }

    /**
     * <p>
     * Order is based on the alphabetical order of {@code Labeler} names.
     * </p>
     * 
     * @param otherLabeler The {@code Labeler} to compare to.
     */
    @Override
    public int compareTo(Labeler otherLabeler) {
        return this.name.compareTo(otherLabeler.name);
    }
}
