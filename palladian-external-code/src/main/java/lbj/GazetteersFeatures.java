// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000560914F44C020158FFAC8B98B1806981F8677D4C38A70313E143E99C67AA43C28168A8EA9DFFE24B4757BE58460EDC7FED3A1D4B1F810F41EE4DE034044F47B8A24FE198D3CDCB83FD04C470785550FD0AB506F8ACBAD6A51F42BD2AC727FC48273EC605AD2DD3E71B5CF7CD283FC40B1043CA1ECB6890F96AC61760641584341F5E3CDA4B8F91A686D970656F43CA0E2069BC15F72B60BDB1353801A3B1CC1297E209DFC037505D0C982FDD3E7867D3DF2DAAA60A5C3AC370D56653EAC78449FA782B3102FEE8669AF673A167927FF5DA42DB346C3D380195F4869035CABC3651A1CD2AD07D1857C3BFEE14B015FC39D4D03EB96389235E029BAF8B2F308B425E367E100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class GazetteersFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public GazetteersFeatures() { super("lbj.GazetteersFeatures"); }

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
      System.err.println("Classifier 'GazetteersFeatures(NEWord)' defined on line 15 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == GazetteersFeatures.exampleCache) {
        return GazetteersFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("GazetteersFeatures"))
    {
      int i = 0;
      NEWord w = word, last = (NEWord) word.next;
      for (i = 0; i < 2 && last != null; ++i)
      {
        last = (NEWord) last.next;
      }
      for (i = 0; i > -2 && w.previous != null; --i)
      {
        w = (NEWord) w.previous;
      }
      do
      {
        if (w.gazetteers != null)
        {
          for (int j = 0; j < w.gazetteers.size(); j++)
          {
            __id = this.name + i;
            __value = "" + w.gazetteers.elementAt(j);
            __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          }
        }
        i++;
        w = (NEWord) w.next;
      }      while (w != last);

    }

    GazetteersFeatures.exampleCache = __example;
    GazetteersFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'GazetteersFeatures(NEWord)' defined on line 15 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "GazetteersFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof GazetteersFeatures; }
}

