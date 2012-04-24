package ws.palladian.extraction.keyphrase.extractors;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import quickdt.Attributes;
import quickdt.Instance;
import quickdt.Leaf;
import quickdt.Node;
import quickdt.TreeBuilder;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

import com.google.common.collect.Sets;

public class DecisionTreeClassifier {
    
    private final String classColumn;
    final Set<Instance> instances = Sets.newHashSet();
    private Node tree;

    public DecisionTreeClassifier(String classColumn) {
        this.classColumn = classColumn;
    }
    
    public void train(FeatureVector fv) {
        Feature<?>[] fvValueArray = fv.toArray();
        List<Serializable> atts = getAtts(fvValueArray);
        Serializable cls = (Serializable)fv.get(classColumn).getValue();
        if (cls == null) {
            throw new IllegalStateException("class is mssing");
        }
        Serializable[] a = atts.toArray(new Serializable[0]);
        Instance instance = Attributes.create(a).classification(cls);
        instances.add(instance);
    }

    private List<Serializable> getAtts(Feature<?>[] fvValueArray) {
        List<Serializable> atts = new ArrayList<Serializable>();
        for (Feature<?> feature : fvValueArray) {
            String fName = feature.getName();
            Serializable fValue = (Serializable)feature.getValue();
            if (fName.equals(classColumn)) {
                continue;
            }
            atts.add(fName);
            atts.add(fValue);
        }
        return atts;
    }
    
    public void build() {
        tree = new TreeBuilder().buildTree(instances);
    }
    
    public void build2() {
        tree = new TreeBuilder().buildTree(instances, 20, 1);
    }
    
    
    public Serializable classify(FeatureVector fv) {
        if (tree == null) {
            throw new IllegalStateException();
        }
        List<Serializable> atts = getAtts(fv.toArray());
        Attributes attributes = Attributes.create(atts.toArray(new Serializable[0]));
        
        return tree.getLeaf(attributes).classification;
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        tree.dump(printStream);
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void main(String[] args) {
        FeatureVector fv1 = new FeatureVector();
        fv1.add(new NumericFeature("height", 55.));
        fv1.add(new NumericFeature("weight", 168.));
        fv1.add(new NominalFeature("gender", "male"));
        fv1.add(new NominalFeature("class", "overweight"));
        DecisionTreeClassifier dtCl = new DecisionTreeClassifier("class");
        dtCl.train(fv1);
        dtCl.build();
        
        FeatureVector fv2 = new FeatureVector();
        // "height", 62, "weight", 201, "gender", "female"
        fv2.add(new NumericFeature("height", 62.));
        fv2.add(new NumericFeature("weight", 168.));
        fv2.add(new NominalFeature("gender", "female"));
        Serializable ret = dtCl.classify(fv2);
        System.out.println(ret);
    }

}
