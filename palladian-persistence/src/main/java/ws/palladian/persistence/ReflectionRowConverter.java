package ws.palladian.persistence;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * {@link RowConverter} using reflection. The classes to be converter to need to conform to the bean conventions, i.e.
 * provide a zero-argument constructor and getters/setters for their properties. This is a quick and dirty
 * implementation, has not yet been tested extensively, and can be regarded as experimental currently.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T>
 */
public final class ReflectionRowConverter<T> implements RowConverter<T> {

    /** The class of instances, which this RowConverter creates. */
    private final Class<T> clazz;

    /** The class' properties which are set by this RowConverter. */
    private final Map<String, Method> properties;

    /**
     * <p>
     * Create a new {@link ReflectionRowConverter} of the specified type.
     * </p>
     * 
     * @param clazz The type for which to create the {@link ReflectionRowConverter}.
     * @return A new {@link ReflectionRowConverter} instance for the specified type.
     */
    public static <T> ReflectionRowConverter<T> create(Class<T> clazz) {
        return new ReflectionRowConverter<T>(clazz);
    }

    private ReflectionRowConverter(Class<T> clazz) {
        this.clazz = clazz;
        this.properties = getProperties(clazz);
    }

    @Override
    public T convert(ResultSet resultSet) throws SQLException {
        T instance = createInstance(clazz);
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = resultSet.getMetaData().getColumnName(i).toLowerCase();
            Method method = properties.get(columnName);
            Object resultValue = resultSet.getObject(i);
            Class<?> parameterType = method.getParameterTypes()[0];
            if (resultValue == null && parameterType.isPrimitive()) {
                // if result is null and the property is of primitive type,
                // do not set it, but leave bean's default value
                continue;
            }
            try {
                method.invoke(instance, resultValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Error setting value \"" + resultValue + "\" for property \""
                        + columnName + "\" (" + parameterType + ")");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Error setting value \"" + resultValue + "\" for property \""
                        + columnName + "\" (" + parameterType + ")");
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Error setting value \"" + resultValue + "\" for property \""
                        + columnName + "\" (" + parameterType + ")");
            }
        }
        return instance;
    }

    /**
     * <p>
     * Get the properties which can be set via setters for the specified class. These are typical Java beans setters,
     * i.e. their names start with "set" and they take one argument.
     * </p>
     * 
     * @param clazz
     * @return
     */
    private static Map<String, Method> getProperties(Class<?> clazz) {
        Map<String, Method> ret = new HashMap<String, Method>();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean setter = methodName.startsWith("set");
            boolean oneArgument = parameterTypes.length == 1;
            if (setter && oneArgument) {
                String propertyName = methodName.substring(3).toLowerCase();
                ret.put(propertyName, method);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Create a new instance of the specified class.
     * </p>
     * 
     * @param clazz The class to create.
     * @return A new instance of the specified class, created by using the default constructor.
     * @throws IllegalStateException When instantiation fails.
     */
    private static <T> T createInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Could not instantiate " + clazz.getName() + ": " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not instantiate " + clazz.getName() + ": " + e.getMessage());
        }
    }

}
