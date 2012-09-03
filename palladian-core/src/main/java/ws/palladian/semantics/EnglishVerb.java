package ws.palladian.semantics;

public class EnglishVerb {

    private String present;
    private String simplePast;
    private String pastParticiple;

    public EnglishVerb(String present, String simplePast, String pastParticiple) {
        this.present = present;
        this.simplePast = simplePast;
        this.pastParticiple = pastParticiple;
    }

    public String getPresent() {
        return present;
    }

    public void setPresent(String present) {
        this.present = present;
    }

    public String getSimplePast() {
        return simplePast;
    }

    public void setSimplePast(String simplePast) {
        this.simplePast = simplePast;
    }

    public String getPastParticiple() {
        return pastParticiple;
    }

    public void setPastParticiple(String pastParticiple) {
        this.pastParticiple = pastParticiple;
    }

}
