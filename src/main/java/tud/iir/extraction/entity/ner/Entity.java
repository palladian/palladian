package tud.iir.extraction.entity.ner;

public class Entity {

    private String name = "";
    private String tagName = "";

    public Entity(String name) {
        this.name = name;
    }

    public Entity(String name, String tagName) {
        this.name = name;
        this.tagName = tagName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

}
