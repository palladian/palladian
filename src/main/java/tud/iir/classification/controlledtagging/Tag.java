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
    
    // tfidf field to keep the original values, as weight is reranked.
    private float originalWeight;

    public Tag() {

    }

    public Tag(String name, float weight) {
        this.name = name;
        this.weight = weight;
        this.originalWeight = weight;
    }
    
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The Tag's weight, which is based on tf-idf, but which may be altered by various re-ranking processes.
     * 
     * @return
     */
    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
    
    public void increaseWeight(float by) {
        this.weight += by;
    }

    /**
     * The Tag's weight, determined by tf-idf. Immutable.
     * 
     * TODO I dont like this solution. Think this over again, remove this method/field to weight and the other one to
     * "internal"/"ranking" weight etc., make it accessible just for the tagger, as it makes no sense to expose this
     * to the outside.
     * 
     * @return
     */
    public float getOriginalWeight() {
        return originalWeight;
    }

    @Override
    public String toString() {
        return name + ":" + weight;
    }

}