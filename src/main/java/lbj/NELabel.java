// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000B49CC2E4E2A4D294550F375F94C4A4DC1D0F37D0FCF2A415827021A9A063ABA05DA0049E2D2AC30B88E5E5A285595B24D200FA24D7F193000000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class NELabel extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public NELabel() { super("lbj.NELabel"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete"; }

  @Override
public String discreteValue(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'NELabel(NEWord)' defined on line 184 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    NEWord word = (NEWord) __example;
    return "" + word.neLabel;
  }

  @Override
public FeatureVector classify(Object example)
  {
    if (example == exampleCache) {
        return cache;
    }
    cache = new FeatureVector(new DiscreteFeature(containingPackage, name, discreteValue(example)));
    exampleCache = example;
    return cache;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'NELabel(NEWord)' defined on line 184 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "NELabel".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof NELabel; }
}

