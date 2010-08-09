package tud.iir.classification.controlledtagging;

/**
 * Represents a Tag.
 * 
 * @author Philipp Katz
 * 
 */
public class Tag {

    private String name;
    private float weight;

    public Tag() {

    }

    public Tag(String name, float weight) {
        this.name = name;
        this.weight = weight;
    }
    
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
    
    public void increaseWeight(float by) {
        this.weight += by;
    }

    @Override
    public String toString() {
        return name + ":" + weight;
    }

}