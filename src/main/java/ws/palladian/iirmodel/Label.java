/**
 * Created on: 27.09.2011 07:30:49
 */
package ws.palladian.iirmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <p>
 * An annotation on an item.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
@Entity
@Table(name = "ANNOTATION")
public final class Label {
    @Id
    @GeneratedValue
    private final Integer identifier;

    @ManyToOne
    private final Item annotatedItem;

    @ManyToOne
    private final LabelType annotation;

    private final String comment;

    /**
     * 
     */
    protected Label() {
        this(null, null, null);

    }

    public Label(final Item annotatedItem, final LabelType labelType, final String comment) {
        super();
        this.identifier = null;
        this.annotatedItem = annotatedItem;
        this.annotation = labelType;
        this.comment = comment;
    }

    /**
     * @return the annotatedItem
     */
    public final Item getAnnotatedItem() {
        return annotatedItem;
    }

    /**
     * @return the annotation
     */
    public final LabelType getAnnotation() {
        return annotation;
    }

    /**
     * @return the comment
     */
    public final String getComment() {
        return comment;
    }

    /**
     * @return the identifier
     */
    public final Integer getIdentifier() {
        return identifier;
    }
}
