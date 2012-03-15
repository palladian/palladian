/**
 * Created on: 27.09.2011 08:13:55
 */
package ws.palladian.iirmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import ws.palladian.iirmodel.persistence.ModelPersistenceLayer;

/**
 * <p>
 * Each {@link Label} has a type that is described by an instance of this class.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
@Entity
@Table(name = "LabelType")
public final class LabelType implements Comparable<LabelType> {
    /**
     * <p>
     * The database identifier assigned to this {@code LabelType} by the {@link ModelPersistenceLayer}.
     * </p>
     */
    @Id
    @GeneratedValue
    private final Integer identifier;
    /**
     * <p>
     * A human readable name given to this {@code LabelType}.
     * </p>
     */
    private final String name;
    /**
     * <p>
     * Some text giving a detailed explanation for this {@code LabelType}. This is useful if human {@link Labeler}s
     * should label {@link Item}s and need to know exactly what type a {@link Label} should get.
     * </p>
     */
    private final String explanation;

    /**
     * <p>
     * Creates a new instance of {@code LabelType}. This method is protected and should never be called directly. It is
     * only used by the {@link ModelPersistenceLayer}.
     * </p>
     */
    protected LabelType() {
        super();
        identifier = null;
        name = null;
        explanation = null;
    }

    /**
     * <p>
     * Creates a new instance of {@code LabelType} completely initialized with a name and an explanation.
     * </p>
     * 
     * @param name The name of the new {@code LabelType}
     * @param explanation Some text giving a detailed explanation for the new {@code LabelType}.
     */
    public LabelType(final String name, final String explanation) {
        super();
        this.identifier = null;
        this.name = name;
        this.explanation = explanation;
    }

    /**
     * @return the identifier given to this {@code LabelType} by the {@link ModelPersistenceLayer}.
     */
    public final Integer getIdentifier() {
        return identifier;
    }

    /**
     * @return the name given to this {@code LabelType}.
     */
    public final String getName() {
        return name;
    }

    /**
     * @return Some text giving a detailed explanation for this {@link LabelType}.
     */
    public final String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(LabelType labelType) {
        return this.getName().compareTo(labelType.getName());
    }
}
