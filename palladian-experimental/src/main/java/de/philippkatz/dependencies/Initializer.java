package de.philippkatz.dependencies;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Initializer {

    private final Map<String, String> configuration;

    public Initializer() {
        this.configuration = new HashMap<String, String>();
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(Class<T> initClass) {
        
        T instance = null;
        
        Constructor<?>[] constructors = initClass.getConstructors();
        for (int j = 0 ; j < constructors.length; j++) {
            
            Constructor<?> constructor = constructors[j];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {

                // Class<?> parameterType = parameterTypes[i];
                
                Annotation[] parameterAnnotations = constructor.getParameterAnnotations()[i];
                for (Annotation parameterAnnotation : parameterAnnotations) {
                    
                    if (parameterAnnotation instanceof ConfigurationParameter) {
                        ConfigurationParameter configurationParameter = (ConfigurationParameter) parameterAnnotation;
                        String key = configurationParameter.key();
                        String value = configuration.get(key);
                        parameters[i] = value;
                    }
                }
            }
            
            try {
                instance = (T) constructor.newInstance(parameters);
                break;
                
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        return instance;
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public String put(String arg0, String arg1) {
        return configuration.put(arg0, arg1);
    }

    public static void main(String[] args) {
        
        Initializer initializer = new Initializer();
        initializer.put("demo.username", "philipp");
        initializer.put("demo.password", "password");
        
        DemoClass demoObject = initializer.initialize(DemoClass.class);
        System.out.println(demoObject);
        
    }

}
