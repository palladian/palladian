// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B880000000000000005AF814B028040158FFACB480CD38B057CCC3A7A22A381D973D15616B55674B888EFB7B6A1679AB4C0CC9EB9FEDB92437EE8A5A936FE8E2ABEE83355D27B47123B80779E1B6750EAE790CA32CD1AB448B75E4D9DF98369529A6B374C95D7062997D6B55AD2F68E6160F9AB0488EF6D6B0D13ED2542F217CF27E556332F89520BD91326C4B07C61203132EB2DBB4D7E554E68704A6ECC9F64AD65D30C46990144BC00B2C4EDFD9024A5ADAA319981F0091F28F3449DDA1A1AA5F6B17E9E34A385738100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class PreviousTag2Level1 extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public PreviousTag2Level1() { super("lbj.PreviousTag2Level1"); }

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
      System.err.println("Classifier 'PreviousTag2Level1(NEWord)' defined on line 216 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == PreviousTag2Level1.exampleCache) {
        return PreviousTag2Level1.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("PreviousTag2"))
    {
      int i;
      NEWord w = word;
      if (w.previous != null)
      {
        if (((NEWord) w.previous).previous != null)
        {
          if (NETaggerLevel1.isTraining)
          {
            __id = this.name + "-2";
            __value = "" + ((NEWord) ((NEWord) w.previous).previous).neLabel;
            __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          }
          else
          {
            __id = this.name + "-2";
            __value = "" + ((NEWord) ((NEWord) w.previous).previous).neTypeLevel1;
            __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          }
        }
      }
    }

    PreviousTag2Level1.exampleCache = __example;
    PreviousTag2Level1.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'PreviousTag2Level1(NEWord)' defined on line 216 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "PreviousTag2Level1".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof PreviousTag2Level1; }
}

