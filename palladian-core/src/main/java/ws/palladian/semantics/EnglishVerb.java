package ws.palladian.semantics;

public class EnglishVerb {

    private final String present;
    private final String simplePast;
    private final String pastParticiple;

    public EnglishVerb(String present, String simplePast, String pastParticiple) {
        this.present = present;
        this.simplePast = simplePast;
        this.pastParticiple = pastParticiple;
    }

    public String getPresent() {
        return present;
    }

    public String getSimplePast() {
        return simplePast;
    }

    public String getPastParticiple() {
        return pastParticiple;
    }

}
