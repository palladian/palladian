package ws.palladian.classification.nominal;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.Instance;

public class NominalInstance extends Instance {

    /** The serial versionID */
    private static final long serialVersionUID = -3124363200436762196L;

    private List<String> nominalFeatures = new ArrayList<String>();

    public void setNominalFeatures(List<String> nominalFeatures) {
        this.nominalFeatures = nominalFeatures;
    }

    public List<String> getNominalFeatures() {
        return nominalFeatures;
    }

}
