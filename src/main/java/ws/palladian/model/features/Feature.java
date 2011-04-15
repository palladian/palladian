package ws.palladian.model.features;

public abstract class Feature<T> {

    private String name = "";
    private T value = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}
