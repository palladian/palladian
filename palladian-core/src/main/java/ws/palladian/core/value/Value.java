package ws.palladian.core.value;

/**
 * <p>
 * A generic value in Palladian's feature type system. This interface requires no functionality but just serves a common
 * parent in the inheritance hierarchy (think of {@link Object}).
 * 
 * <p>
 * The core package provides predefined sub-interfaces (e.g. {@link NominalValue}). When extending the hierarchy, ask
 * yourself thoroughly, where your new class fits in the existing hierarchy. Let's assume, you want to add a "color"
 * value: In this case, insert it as child to (i.e. inherit from) {@link NominalValue}, because "red", "green", "blue"
 * are nominal values. This way, existing clients can also process more specific "unknown" types (color in the given
 * example), because they know how to handle their more general abstraction and do not need to provide special
 * treatment.
 * 
 * <p>
 * All implementors should inherit from {@link AbstractValue}, which provides some common functionality concerning
 * concerning {@link #equals(Object)} and {@link #hashCode()}, and requires explicit implementation of the
 * {@link #toString()} method. A working example for implementing a hypothetical color value would look like this:
 * 
 * <pre>
 * public class ColorValue extends AbstractValue implements NominalValue {
 * 
 *     private final Color color;
 * 
 *     public ColorValue(Color color) {
 *         this.color = color;
 *     }
 * 
 *     &#064;Override
 *     public String getString() {
 *         return color.toString();
 *     }
 * 
 *     public Color getColor() {
 *         return color;
 *     }
 * 
 *     &#064;Override
 *     public int hashCode() {
 *         return color.hashCode();
 *     }
 * 
 *     &#064;Override
 *     protected boolean equalsValue(Value value) {
 *         ColorValue colorValue = (ColorValue)value; // cast is safe
 *         return this.color.equals(colorValue.color);
 *     }
 * 
 *     &#064;Override
 *     public String toString() {
 *         return getString();
 *     }
 * 
 * }
 * </pre>
 * 
 * @see NominalValue
 * @see NumericValue
 * @see BooleanValue
 * @see TextValue
 * 
 * @author pk
 * 
 */
public interface Value {

}
