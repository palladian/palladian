package ws.palladian.helper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Allows to dynamically search for classes, which implement a specific interface or which inherit from a specific type.
 * This way, one can implement a simple plugin mechanism. However, keep in mind, that this most certainly will not work
 * in edge cases with non-standard class loading mechanisms (WAR files, application containers, OSGi etc.).
 * </p>
 * 
 * @author pk
 * 
 */
public final class ClassFinder {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFinder.class);

    /** The class path. */
    private static final String CLASSPATH = System.getProperty("java.class.path");

    /** File extension of JAR files. */
    private static final String JAR_FILE_EXTENSION = ".jar";

    /** File extension of Java class files. */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /** Filter classes compatible to the given type, no interfaces, no abstract classes. */
    private static final class ConcreteClassFilter implements Filter<Class<?>> {
        private final Class<?> type;

        public ConcreteClassFilter(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean accept(Class<?> item) {
            return type.isAssignableFrom(item) && !item.isInterface() && !Modifier.isAbstract(item.getModifiers());
        }
    }

    private ClassFinder() {
        // no instances
    }

    /**
     * <p>
     * Find classes on the class path which derive from the given class (usually, one would provide an interface here,
     * to get classes implementing this interface). <b>Important:</b> This mechanism <b>loads</b> all classes which are
     * scanned (i.e. all static initialization is performed); this is a damn fucking expensive operation, as all touched
     * classes go to the perm gen and there is usually no way of getting them out of there again (like uninvited
     * guests). But there is a rescue: Use the namespace filter parameter for filtering the namespace wisely, to only
     * check as little classes as possible (e.g. when you assume, that the classes you are interested in are located in
     * <code>foo.bar.baz</code>, specify this namespace as parameter.
     * </p>
     * 
     * @param type The type for which to search implementors/subclasses, not <code>null</code>.
     * @param namespaceFilter The filter to determine in which namespace to search, not <code>null</code>.
     * @return A {@link Collection} with concrete {@link Class} objects implementing/deriving from the given type (no
     *         interfaces, no abstract classes), or an empty {@link Collection} if no matches were found, never
     *         <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<Class<? extends T>> findClasses(Class<T> type,
            final Filter<? super String> namespaceFilter) {
        Validate.notNull(type, "type must not be null");
        Validate.notNull(namespaceFilter, "namespaceFilter must not be null");

        final Collection<Class<? extends T>> result = CollectionHelper.newHashSet();
        final ConcreteClassFilter classFilter = new ConcreteClassFilter(type);

        LOGGER.debug("Classpath = {}", CLASSPATH);
        String[] classPathItems = CLASSPATH.split(File.pathSeparator);
        for (final String classPathItem : classPathItems) {
            if (classPathItem.endsWith(JAR_FILE_EXTENSION)) { // we're in a JAR file
                LOGGER.debug("Scanning JAR {}", classPathItem);
                try {
                    JarFile jar = new JarFile(new File(classPathItem));
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry currentEntry = entries.nextElement();
                        String name = currentEntry.getName();
                        String className = pathToClassName(name);
                        if (currentEntry.isDirectory() || !name.endsWith(CLASS_FILE_EXTENSION)
                                || !namespaceFilter.accept(className)) {
                            continue;
                        }
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (classFilter.accept(clazz)) {
                                result.add((Class<T>)clazz);
                            }
                        } catch (ClassNotFoundException e) {
                            LOGGER.debug("Encountered ClassNotFoundException for {}", className);
                        } catch (NoClassDefFoundError e) {
                            LOGGER.debug("Encountered NoClassDefFoundError for {}", className);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException when trying to read {}", classPathItem, e);
                }
            } else { // we're checking .class files
                FileHelper.traverseFiles(new File(classPathItem), new Filter<File>() {
                    @Override
                    public boolean accept(File pathname) {
                        String namespaceName = pathname.getPath().substring(classPathItem.length() + 1);
                        namespaceName = namespaceName.replace(File.separatorChar, '.');
                        return pathname.getName().endsWith(CLASS_FILE_EXTENSION)
                                && namespaceFilter.accept(namespaceName);
                    }
                }, new Consumer<File>() {
                    @Override
                    public void process(File file) {
                        String className = pathToClassName(file.getPath().substring(classPathItem.length() + 1));
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (classFilter.accept(clazz)) {
                                result.add((Class<T>)clazz);
                            }
                        } catch (ClassNotFoundException e) {
                            LOGGER.debug("Encountered ClassNotFoundException for {}", className);
                        } catch (NoClassDefFoundError e) {
                            LOGGER.debug("Encountered NoClassDefFoundError for {}", className);
                        }
                    }
                });
            }
        }
        return result;
    }

    private static String pathToClassName(String name) {
        return name.replace(File.separatorChar, '.').replace(CLASS_FILE_EXTENSION, "");
    }

}
