package ws.palladian.retrieval.apiwrapper;

class ParameterBinding {
    public String term = "";
    public String value = "";

    public ParameterBinding(String term, String value) {
        this.term = term;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ParameterBinding [term=" + term + ", value=" + value + "]";
    }

}