package tud.iir.extraction.keyphrase;

import java.util.Set;

public abstract class AbstractKeyphraseExtractor {
    
    public abstract Set<Keyphrase> extract(String inputText);

}

// TODO move to own file.
class Keyphrase {
    
    public Keyphrase(String value) {
        this.value = value;
    }
    
    public Keyphrase(String value, double weight) {
        this.value = value;
        this.weight = weight;
    }
    
    String value;
    double weight = -1;
    
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Keyphrase other = (Keyphrase) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Keyphrase [value=");
        builder.append(value);
        builder.append(", weight=");
        builder.append(weight);
        builder.append("]");
        return builder.toString();
    }
    
}