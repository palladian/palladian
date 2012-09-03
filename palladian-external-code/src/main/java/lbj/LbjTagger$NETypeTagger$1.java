// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000BCDCCA84D415550F94ACA094C4F4F4D2251F37D09AC2845827C053CFC53C3FB82541AC18486A28D8EA2407642614AAB5A626949615A61BE82427642619F5A71526E209D0004E4A9496B4000000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;

public class LbjTagger$NETypeTagger$1 extends Classifier
{
  private static final ShapeFeatures __ShapeFeatures = new ShapeFeatures();
  private static final charNgrams __charNgrams = new charNgrams();

  private static FeatureVector cache;
  private static Object exampleCache;

  public LbjTagger$NETypeTagger$1() { super("lbj.LbjTagger$NETypeTagger$1"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "mixed%"; }

  @Override
public FeatureVector classify(Object example)
  {
    if (!(example instanceof NEWord))
    {
      String type = example == null ? "null" : example.getClass().getName();
      System.err.println("Classifier 'LbjTagger$NETypeTagger$1(NEWord)' defined on line 523 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (example == exampleCache) {
        return cache;
    }

    FeatureVector result = new FeatureVector();
    result.addFeatures(__ShapeFeatures.classify(example));
    result.addFeatures(__charNgrams.classify(example));

    exampleCache = example;
    cache = result;

    return result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'LbjTagger$NETypeTagger$1(NEWord)' defined on line 523 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "LbjTagger$NETypeTagger$1".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof LbjTagger$NETypeTagger$1; }

  @Override
public java.util.LinkedList getCompositeChildren()
  {
    java.util.LinkedList result = new java.util.LinkedList();
    result.add(__ShapeFeatures);
    result.add(__charNgrams);
    return result;
  }
}

