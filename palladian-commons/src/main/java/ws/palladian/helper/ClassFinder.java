package ws.palladian.helper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.io.Action;
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

    private ClassFinder() {
        // no instances
    }
    
//    public static <T> Collection<Class<? extends T>> findClasses(Class<T> type) {
//        return findClasses(type, Filter.ACCEPT);
//    }
    
    /**
     * <p>
     * Find classes on the class path which derive from the given class (usually, one would provide an interface here,
     * to get classes implementing this interface). <b>Important:</b> This mechanism <b>loads</b> all classes which are
     * scanned (i.e. all static initialization is performed); this is a damn fucking expensive operation, as all touched
     * classes go to the perm gen and there is usually no way of getting them out of there again (like uninvited
     * guests). But there is a rescue: Use the parameter parameter for filtering the namespace wisely, to only check as
     * little classes as possible (e.g. when you assume, that the classes you are interested in are located in
     * <code>foo.bar.baz</code>, specify this namespace as parameter.
     * </p>
     * 
     * @param type The type for which to search implementors/subclasses.
     * @param namespaceFilter The filter to determine in which namespace to search, not <code>null</code>.
     * @return A {@link Collection} with {@link Class} objects implementing/deriving from the given type.
     */
    public static <T> Collection<Class<? extends T>> findClasses(final Class<T> type,
            final Filter<? super String> namespaceFilter) {
        // return Collections.emptySet();
        
        StopWatch stopWatch = new StopWatch();
        final Collection<Class<? extends T>> result = CollectionHelper.newHashSet();
        
        String classPath = System.getProperty("java.class.path");
        String[] classPathItems = classPath.split(File.pathSeparator);

        for (final String classPathItem : classPathItems) {
            if (classPathItem.endsWith(".jar")) {
                // continue; // implement me
                
                try {
                    JarFile jar = new JarFile(new File(classPathItem));
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry currentEntry = entries.nextElement();
                        if (currentEntry.isDirectory()) {
                            continue;
                        }
                        String name = currentEntry.getName();
                        if (name.endsWith(".class")) {
                            String className = name.replace(File.separatorChar, '.');
                            className = className.replace(".class", "");
                            
                            if (namespaceFilter.accept(className)){
                            
                            try {
                                Class<?> clazz = Class.forName(className);
                                if (type.isAssignableFrom(clazz)) {
//                                    System.out.println(clazz);
                                    result.add((Class<T>)clazz);
                                }
                            } catch (ClassNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }catch (Throwable e){
                                e.printStackTrace();
                            }
                            }
                            
                        }
                        // System.out.println(name);
                    }
                    System.out.println(jar);
                    // System.exit(0);
                    
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                continue;
            }
            
            FileHelper.traverseFiles(new File(classPathItem), new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String namespaceName = pathname.getPath().substring(classPathItem.length() + 1);
                    namespaceName = namespaceName.replace(File.separatorChar, '.');
                    return pathname.getName().endsWith(".class") && /*pathname.getPath().contains("ws/palladian/retrieval/search");*/
                            namespaceFilter.accept(namespaceName);
                }
            }, new Action<File>() {
                @SuppressWarnings("unchecked")
                @Override
                public void process(File file) {
                    
                    String className = file.getPath().substring(classPathItem.length() + 1);
                    className = className.replace(File.separatorChar, '.');
//                    className = className.replace("/", ".");
                    className = className.replace(".class", "");
                    // System.out.println(className);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (type.isAssignableFrom(clazz)) {
//                            System.out.println(clazz);
                            result.add((Class<T>)clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });

        }
        
        // CollectionHelper.print(classPathItems);
        
        System.out.println("took : " + stopWatch);
        return Collections.unmodifiableCollection(result);
        
    }

    public static void main(String[] args) {
//        Collection<Class<? extends Searcher>> classes = findClasses(Searcher.class, new RegexFilter("ws.palladian.retrieval.search.*"));
//        Collection<Class<? extends AbstractMultifacetSearcher>> classes = findClasses(AbstractMultifacetSearcher.class, new RegexFilter("ws.palladian.retrieval.search.*"));
//        CollectionHelper.print(classes);
    }

}
