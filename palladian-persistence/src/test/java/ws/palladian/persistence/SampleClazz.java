package ws.palladian.persistence;

final class SampleClazz {
    private Integer id;
    private String name;
    private int age;
    private double weight;
    private boolean cool;

    /**
     * @param name
     * @param age
     * @param weight
     * @param cool
     */
    public SampleClazz(String name, int age, double weight, boolean cool) {
        super();
        this.name = name;
        this.age = age;
        this.weight = weight;
        this.cool = cool;
    }

    /**
     * 
     */
    public SampleClazz() {
        super();
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * @param age the age to set
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @return the cool
     */
    public boolean isCool() {
        return cool;
    }

    /**
     * @param cool the cool to set
     */
    public void setCool(boolean cool) {
        this.cool = cool;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TestClazz [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", age=");
        builder.append(age);
        builder.append(", weight=");
        builder.append(weight);
        builder.append(", cool=");
        builder.append(cool);
        builder.append("]");
        return builder.toString();
    }

}