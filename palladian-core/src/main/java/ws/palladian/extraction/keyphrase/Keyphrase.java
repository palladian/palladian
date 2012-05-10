package ws.palladian.extraction.keyphrase;

public class Keyphrase implements Comparable<Keyphrase>{
    
    public Keyphrase(String value) {
        this.value = value;
    }
    
    public Keyphrase(String value, double weight) {
        this.value = value;
        this.weight = weight;
    }
    
    private String value;
    private double weight = -1;
    
    
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

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
//        StringBuilder builder = new StringBuilder();
//        // builder.append("Keyphrase [value=");
//        builder.append(value);
//         builder.append(", weight=");
//         builder.append(weight);
//         builder.append("]");
//        return builder.toString();
        return value;
    }

    @Override
    public int compareTo(Keyphrase o) {
        return Double.compare(o.getWeight(), this.getWeight());
    }
    
}