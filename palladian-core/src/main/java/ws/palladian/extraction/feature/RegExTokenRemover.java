package ws.palladian.extraction.feature;

import java.util.regex.Pattern;

public class RegExTokenRemover extends TokenRemover {
    
    private static final long serialVersionUID = 1L;
    private final Pattern pattern;

    public RegExTokenRemover(Pattern pattern) {
        this.pattern = pattern;
    }
    
    public RegExTokenRemover(String pattern) {
        this(Pattern.compile(pattern));
    }
    
    @Override
    protected boolean remove(Annotation annotation) {
        return pattern.matcher(annotation.getValue()).matches();
    }

}
