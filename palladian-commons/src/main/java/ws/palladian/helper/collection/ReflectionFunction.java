package ws.palladian.helper.collection;

import java.lang.reflect.Method;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A {@link ReflectionFunction} invokes a given method on a given type and returns the result value. Saves some lines of
 * code, as we avoid implementing an anonymous method for retrieving an Object's properties. For example, instead of:
 * 
 * <pre>
 * new Function&lt;NameObject, String&gt;() {
 *     &#064;Override
 *     public String compute(NameObject item) {
 *         return item.getName();
 *     }
 * };
 * </pre>
 * 
 * we can now write:
 * 
 * <pre>
 * // invoke NameObject#getName method which returns a String
 * ReflectionFunction.create(NameObject.class, &quot;getName&quot;, String.class);
 * </pre>
 * 
 * Thats five lines less code! Keep in mind, <a
 * href="http://en.wikipedia.org/wiki/There_ain't_no_such_thing_as_a_free_lunch">that there is no such thing as a free
 * lunch</a>. Reflection has a performance cost. However, if you do not run this function in a loop bazillions of times
 * every two and a half seconds, the overhead is negligable.
 * </p>
 * 
 * @param <I> Input type.
 * @param <O> Output type.
 * @author pk
 */
public final class ReflectionFunction<I, O> implements Function<I, O> {

    // TODO it would be cool, if we can chain method calls, similar to JPaths, like:
    // ReflectionFunction.create(NameObject.class, "getAddress/getHome/getCoordinate/getLatitude", Double.class);

    /** The method which is invoked on the {@link Function}'s input. */
    private final Method method;

    /** The desired return type. Keep it here at runtime for {@link #toString()}. */
    private final Class<O> returnType;

    /**
     * <p>
     * Create a new {@link ReflectionFunction}.
     * </p>
     * 
     * @param inputType The input type, not <code>null</code>.
     * @param name The name of the method to invoke on the input type, not <code>null</code> or empty. This method must
     *            have the specified return type.
     * @param returnType The output type, not <code>null</code>.
     * @return A new {@link ReflectionFunction} instance.
     * @throws IllegalStateException In case the specified method does not exist, the return type of the given method is
     *             not compatible to the provided return type, or a security exception occurs during reflection.
     */
    public static <I, O> ReflectionFunction<I, O> create(Class<I> inputType, String name, Class<O> returnType) {
        return new ReflectionFunction<I, O>(inputType, returnType, name);
    }

    /** Should be constructed using the static method (type inference). */
    private ReflectionFunction(Class<I> inputType, Class<O> returnType, String name) {
        Validate.notNull(inputType, "inputType must not be null");
        Validate.notNull(returnType, "returnType must not be null");
        Validate.notEmpty(name, "name must not be empty");

        try {
            Method method = inputType.getMethod(name);
            // check that actual return type is assignment compatible
            Class<?> actualReturnType = method.getReturnType();
            if (!ClassUtils.isAssignable(actualReturnType, returnType)) {
                throw new IllegalStateException("The type " + actualReturnType.getName() + " cannot be assigned to "
                        + returnType.getName());
            }
            this.method = method;
            this.returnType = returnType;
        } catch (SecurityException e) {
            throw new IllegalStateException("Encountered SecurityException for " + inputType.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Encountered NoSuchMethod exception for " + inputType.getName()
                    + " and method " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public O compute(I input) {
        if (input == null) {
            return null;
        }
        try {
            // cast is safe, since we checked compatibility already in constructor
            return (O)method.invoke(input);
        } catch (Exception e) {
            throw new IllegalStateException("Encountered " + e.getClass().getSimpleName() + " when trying to invoke "
                    + method.getName() + ".", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(ReflectionFunction.class.getSimpleName());
        builder.append(" [");
        builder.append(method.getDeclaringClass().getName()).append('.');
        builder.append(method.getName()).append(" : ");
        builder.append(returnType.getName());
        builder.append(']');
        return builder.toString();
    }

}
