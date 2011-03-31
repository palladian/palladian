// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D7E8F4B02804015CFBACE1A0A0CE06D928E252A11888050D97AA1B6347756674DAE3D731A22E5ABCCF973FED0FB24FB0F635511207548E26CA137F7694876B4735D8499BADC2491B626B758CB7B97A6F0F14664427386195A22502EE64961049A9127DF106D6D87AE7F5F4FE21F062313472E63BC4024CD9216DAD65EE4077FB509196BCE5B8F2F495FE8439136F46DE9A25CF27472001853C8F2EE359487C70498D62708ED19D6C40975E819250E78BF3FFDF597AEBA9DEA29613100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;


public class FeaturesLevel1 extends Classifier
{
  private static final nonLocalFeatures __nonLocalFeatures = new nonLocalFeatures();
  private static final GazetteersFeatures __GazetteersFeatures = new GazetteersFeatures();
  private static final FormParts __FormParts = new FormParts();
  private static final Forms __Forms = new Forms();
  private static final Capitalization __Capitalization = new Capitalization();
  private static final WordTypeInformation __WordTypeInformation = new WordTypeInformation();
  private static final Affixes __Affixes = new Affixes();
  private static final PreviousTag1Level1 __PreviousTag1Level1 = new PreviousTag1Level1();
  private static final PreviousTag2Level1 __PreviousTag2Level1 = new PreviousTag2Level1();
  private static final LbjTagger$FeaturesLevel1$9 __LbjTagger$FeaturesLevel1$9 = new LbjTagger$FeaturesLevel1$9();
  private static final prevTagsForContextLevel1 __prevTagsForContextLevel1 = new prevTagsForContextLevel1();
  private static final NEShapeTaggerFeatures __NEShapeTaggerFeatures = new NEShapeTaggerFeatures();
  private static final BrownClusterPaths __BrownClusterPaths = new BrownClusterPaths();
  private static final LbjTagger$FeaturesLevel1$13 __LbjTagger$FeaturesLevel1$13 = new LbjTagger$FeaturesLevel1$13();

  private static FeatureVector cache;
  private static Object exampleCache;

  public FeaturesLevel1() { super("lbj.FeaturesLevel1"); }

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
      System.err.println("Classifier 'FeaturesLevel1(NEWord)' defined on line 275 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (example == exampleCache) {
        return cache;
    }

    FeatureVector result = new FeatureVector();
    result.addFeatures(__nonLocalFeatures.classify(example));
    result.addFeatures(__GazetteersFeatures.classify(example));
    result.addFeatures(__FormParts.classify(example));
    result.addFeatures(__Forms.classify(example));
    result.addFeatures(__Capitalization.classify(example));
    result.addFeatures(__WordTypeInformation.classify(example));
    result.addFeatures(__Affixes.classify(example));
    result.addFeatures(__PreviousTag1Level1.classify(example));
    result.addFeatures(__PreviousTag2Level1.classify(example));
    result.addFeatures(__LbjTagger$FeaturesLevel1$9.classify(example));
    result.addFeatures(__prevTagsForContextLevel1.classify(example));
    result.addFeatures(__NEShapeTaggerFeatures.classify(example));
    result.addFeatures(__BrownClusterPaths.classify(example));
    result.addFeatures(__LbjTagger$FeaturesLevel1$13.classify(example));

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
            System.err.println("Classifier 'FeaturesLevel1(NEWord)' defined on line 275 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "FeaturesLevel1".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof FeaturesLevel1; }

  @Override
public java.util.LinkedList getCompositeChildren()
  {
    java.util.LinkedList result = new java.util.LinkedList();
    result.add(__nonLocalFeatures);
    result.add(__GazetteersFeatures);
    result.add(__FormParts);
    result.add(__Forms);
    result.add(__Capitalization);
    result.add(__WordTypeInformation);
    result.add(__Affixes);
    result.add(__PreviousTag1Level1);
    result.add(__PreviousTag2Level1);
    result.add(__LbjTagger$FeaturesLevel1$9);
    result.add(__prevTagsForContextLevel1);
    result.add(__NEShapeTaggerFeatures);
    result.add(__BrownClusterPaths);
    result.add(__LbjTagger$FeaturesLevel1$13);
    return result;
  }
}

