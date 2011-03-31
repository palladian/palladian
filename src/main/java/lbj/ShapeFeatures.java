// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D9D813B02C030158FFAC3A02424D6154723DAB9E895C1C9BADB6B10D4B4E25C14BFFDD4B38822E227077CBBB7FEEB2D0F9D197A91E0D41D1DE9A0FDB3261A777C6D598B586219528B3C85F0D8C7A55A55BEEA9E58C6DEB11251284881D0628165A2C8C27CA512E8D8C13A514C77C0B5C47562BC488AB7448DCBD77EEF4CED91B5B85CC381516805F10A5EF702EF902D84E54A1E9A9F95947A1100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class ShapeFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public ShapeFeatures() { super("lbj.ShapeFeatures"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete%"; }

  @Override
public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'ShapeFeatures(NEWord)' defined on line 509 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == ShapeFeatures.exampleCache) {
        return ShapeFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    int N = word.form.length();
    for (int i = 3; i <= 4; ++i)
    {
      if (word.form.length() > i)
      {
        __id = this.name + "p|";
        __value = "" + word.form.substring(0, i);
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
      }
    }
    for (int i = 1; i <= 4; ++i)
    {
      if (word.form.length() > i)
      {
        __id = this.name + "s|";
        __value = "" + word.form.substring(N - i);
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
      }
    }

    ShapeFeatures.exampleCache = __example;
    ShapeFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'ShapeFeatures(NEWord)' defined on line 509 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "ShapeFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof ShapeFeatures; }
}

