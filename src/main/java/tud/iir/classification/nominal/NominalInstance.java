package tud.iir.classification.nominal;

import java.util.ArrayList;
import java.util.List;

import tud.iir.classification.Instance;

public class NominalInstance extends Instance {

    private List<String> nominalFeatures = new ArrayList<String>();

    public void setNominalFeatures(List<String> nominalFeatures) {
        this.nominalFeatures = nominalFeatures;
    }

    public List<String> getNominalFeatures() {
        return nominalFeatures;
    }

}
