package ws.palladian.preprocessing.featureextraction;

import java.util.regex.Pattern;

public class RegExTokenRemover extends TokenRemover {
    
    private Pattern pattern;

    public RegExTokenRemover(Pattern pattern) {
        this.pattern = pattern;
    }
    
    public RegExTokenRemover(String pattern) {
        this(Pattern.compile(pattern));
    }
    
    @Override
    protected boolean remove(String tokenValue) {
        return pattern.matcher(tokenValue).matches();
    }
    
    public static void main(String[] args) {
        //RegExTokenRemover remover = new RegExTokenRemover("\\p{Punct}");
        RegExTokenRemover remover = new RegExTokenRemover(".");
        System.out.println(remover.remove("."));
        System.out.println(remover.remove("a"));
        System.out.println(remover.remove("ab"));
    }

}
