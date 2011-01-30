package tud.iir.persistence;

import java.util.Map;

/**
 * Provides a simple {@link ResultCallback}, typed as {@link Map} with <code>String</code> key, <code>Object</code>
 * value. The intention of this class is to make code more readable, by writing <code>SimpleResultCallback</code>
 * instead of <code>ResultCallback&lt;Map&lt;String, Object&gt;&gt;</code>
 * 
 * @author Philipp Katz
 * 
 */
public abstract class SimpleResultCallback extends ResultCallback<Map<String, Object>> {

}
