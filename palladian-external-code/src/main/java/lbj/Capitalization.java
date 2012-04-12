// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D50D14B44C0301500EFB2FC507969E6B88743B5F22E9401F0A87E0DE4160262B466A65D56FFBB94DEABAB402031EDBF212D3B4719496F385724B186C19E0798B3BB365BE8FBCA270F5C3EDFB688D36A4B598D6D8D387041F463AD7B4DD82DC04657C842FC1E548A9E2875BCE5E18E3B85DF7A655699BDE51C607461DEC4F60ECA8E2381C0122A0E43E591036BD2EA1BE5FF44E2A58F1D9338AA2ECED2DB5ED95EC373E9E34D0E076ECD2AE9D99A9D54A77E03AC94BABE95B9EF257ACDFA562522572F526EC3C9F2DC680971207551E62D9677CF30AE3B296D73010277EA587100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class Capitalization extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public Capitalization() { super("lbj.Capitalization"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete%"; }

  @Override
public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  @Override
public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'Capitalization(NEWord)' defined on line 119 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == Capitalization.exampleCache) {
        return Capitalization.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("Capitalization"))
    {
      int i;
      NEWord w = word, last = word;
      for (i = 0; i <= 2 && last != null; ++i)
      {
        last = (NEWord) last.next;
      }
      for (i = 0; i > -2 && w.previous != null; --i)
      {
        w = (NEWord) w.previous;
      }
      for (; w != last; w = (NEWord) w.next)
      {
        __id = this.name + i++;
        __value = "" + w.capitalized;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
      }
    }

    Capitalization.exampleCache = __example;
    Capitalization.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'Capitalization(NEWord)' defined on line 119 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "Capitalization".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof Capitalization; }
}

