// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D709BCA02C030154F752B05058A06B8B473A5454825A0A0EA74D96C84C4AC46AEBEBED1F15AAB17397C9CDB3377272375CD775DC118B62C091E91D62DBC76B1F4B757195AFA6C3056F983E986D48A18D877D8E325ECBBCCFE0C6B46107746644A0D2B9B7A35104CF93AC69245681CA9BFBAE54AE9D07D7BA079EA411C70E4AC252F98AB02C3B1F578538E8FD91F7852D0BC6B749BA64AECFE44D987819AA4C02FA14214AED13E59B1B5E3B5D10A2C7B5BD4D352F7179ADA3032510C780FFA1C842118CCD4F5F93F0FD7CECC5E5100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;


public class FeaturesLevel2 extends Classifier
{
  private static final level1AggregationFeatures __level1AggregationFeatures = new level1AggregationFeatures();
  private static final nonLocalFeatures __nonLocalFeatures = new nonLocalFeatures();
  private static final GazetteersFeatures __GazetteersFeatures = new GazetteersFeatures();
  private static final FormParts __FormParts = new FormParts();
  private static final Forms __Forms = new Forms();
  private static final Capitalization __Capitalization = new Capitalization();
  private static final WordTypeInformation __WordTypeInformation = new WordTypeInformation();
  private static final Affixes __Affixes = new Affixes();
  private static final PreviousTag1Level2 __PreviousTag1Level2 = new PreviousTag1Level2();
  private static final PreviousTag2Level2 __PreviousTag2Level2 = new PreviousTag2Level2();
  private static final LbjTagger$FeaturesLevel2$10 __LbjTagger$FeaturesLevel2$10 = new LbjTagger$FeaturesLevel2$10();
  private static final prevTagsForContextLevel2 __prevTagsForContextLevel2 = new prevTagsForContextLevel2();
  private static final NEShapeTaggerFeatures __NEShapeTaggerFeatures = new NEShapeTaggerFeatures();
  private static final BrownClusterPaths __BrownClusterPaths = new BrownClusterPaths();
  private static final LbjTagger$FeaturesLevel2$14 __LbjTagger$FeaturesLevel2$14 = new LbjTagger$FeaturesLevel2$14();
  private static final PatternFeatures __PatternFeatures = new PatternFeatures();

  private static FeatureVector cache;
  private static Object exampleCache;

  public FeaturesLevel2() { super("lbj.FeaturesLevel2"); }

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
      System.err.println("Classifier 'FeaturesLevel2(NEWord)' defined on line 471 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (example == exampleCache) {
        return cache;
    }

    FeatureVector result = new FeatureVector();
    result.addFeatures(__level1AggregationFeatures.classify(example));
    result.addFeatures(__nonLocalFeatures.classify(example));
    result.addFeatures(__GazetteersFeatures.classify(example));
    result.addFeatures(__FormParts.classify(example));
    result.addFeatures(__Forms.classify(example));
    result.addFeatures(__Capitalization.classify(example));
    result.addFeatures(__WordTypeInformation.classify(example));
    result.addFeatures(__Affixes.classify(example));
    result.addFeatures(__PreviousTag1Level2.classify(example));
    result.addFeatures(__PreviousTag2Level2.classify(example));
    result.addFeatures(__LbjTagger$FeaturesLevel2$10.classify(example));
    result.addFeatures(__prevTagsForContextLevel2.classify(example));
    result.addFeatures(__NEShapeTaggerFeatures.classify(example));
    result.addFeatures(__BrownClusterPaths.classify(example));
    result.addFeatures(__LbjTagger$FeaturesLevel2$14.classify(example));
    result.addFeatures(__PatternFeatures.classify(example));

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
            System.err.println("Classifier 'FeaturesLevel2(NEWord)' defined on line 471 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "FeaturesLevel2".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof FeaturesLevel2; }

  @Override
public java.util.LinkedList getCompositeChildren()
  {
    java.util.LinkedList result = new java.util.LinkedList();
    result.add(__level1AggregationFeatures);
    result.add(__nonLocalFeatures);
    result.add(__GazetteersFeatures);
    result.add(__FormParts);
    result.add(__Forms);
    result.add(__Capitalization);
    result.add(__WordTypeInformation);
    result.add(__Affixes);
    result.add(__PreviousTag1Level2);
    result.add(__PreviousTag2Level2);
    result.add(__LbjTagger$FeaturesLevel2$10);
    result.add(__prevTagsForContextLevel2);
    result.add(__NEShapeTaggerFeatures);
    result.add(__BrownClusterPaths);
    result.add(__LbjTagger$FeaturesLevel2$14);
    result.add(__PatternFeatures);
    return result;
  }
}

