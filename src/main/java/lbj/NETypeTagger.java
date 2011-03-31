// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D4C813B02C038144FFACD0D1A58A261C1D5C1AB58028507E4B93264B4A1EB44B8FFED8838E27BCDB7FCAF4390331ABF1E5193817E825BEEF2B885CA56A1C1638996424144991933E19C707053EDEB475FFA65D165F9FA80C517E8642153379ECD5D72152DFB3E8F4A8174B72AC4C8956905F6BB61BB61BF6A1038D737E781B8AC9000000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.FeatureVectorReturner;
import LBJ2.classify.LabelVectorReturner;
import LBJ2.learn.SparseAveragedPerceptron;
import LBJ2.learn.SparseNetworkLearner;
import LBJ2.learn.TestingMetric;
import LBJ2.parse.Parser;

public class NETypeTagger extends SparseNetworkLearner
{
  public static boolean isTraining = false;
  private static java.net.URL lcFilePath;
  private static NETypeTagger instance;
  public static NETypeTagger getInstance()
  {
    if (instance == null) {
        instance = (NETypeTagger) Classifier.binaryRead(lcFilePath, "NETypeTagger");
    }
    return instance;
  }

  static
  {
    lcFilePath = NETypeTagger.class.getResource("NETypeTagger.lc");

    if (lcFilePath == null)
    {
      System.err.println("ERROR: Can't locate NETypeTagger.lc in the class path.");
      System.exit(1);
    }
  }

  @Override
public void save()
  {
    if (instance == null) {
        return;
    }

    if (lcFilePath.toString().indexOf(".jar!" + java.io.File.separator) != -1)
    {
      System.err.println("WARNING: NETypeTagger.lc is part of a jar file.  It will be written to the current directory.  Use 'jar -u' to update the jar file.  To avoid seeing this message in the future, unpack the jar file and put the unpacked files on your class path instead.");
      instance.binaryWrite(System.getProperty("user.dir") + java.io.File.separator + "NETypeTagger.lc", "NETypeTagger");
    } else {
        instance.binaryWrite(lcFilePath.getPath(), "NETypeTagger");
    }
  }

  public static Parser getParser() { return null; }

  public static TestingMetric getTestingMetric() { return null; }

  private static FeatureVector cache;
  private static Object exampleCache;

  private boolean isClone;

  public NETypeTagger()
  {
    super("lbj.NETypeTagger");
    isClone = true;
    if (instance == null) {
        instance = (NETypeTagger) Classifier.binaryRead(lcFilePath, "NETypeTagger");
    }
  }

  private NETypeTagger(boolean b)
  {
    super(new SparseAveragedPerceptron(.1, 0, 4));
    containingPackage = "lbj";
    name = "NETypeTagger";
    setLabeler(new NELabel());
    setExtractor(new LbjTagger$NETypeTagger$1());
    isClone = false;
  }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete"; }

  @Override
public void learn(Object example)
  {
    if (isClone)
    {
      instance.learn(example);
      return;
    }

    Classifier saveExtractor = extractor;
    Classifier saveLabeler = labeler;

    if (!(example instanceof NEWord))
    {
      if (example instanceof FeatureVector)
      {
        if (!(extractor instanceof FeatureVectorReturner)) {
            setExtractor(new FeatureVectorReturner());
        }
        if (!(labeler instanceof LabelVectorReturner)) {
            setLabeler(new LabelVectorReturner());
        }
      }
      else
      {
        String type = example == null ? "null" : example.getClass().getName();
        System.err.println("Classifier 'NETypeTagger(NEWord)' defined on line 522 of LbjTagger.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    super.learn(example);

    if (saveExtractor != extractor) {
        setExtractor(saveExtractor);
    }
    if (saveLabeler != labeler) {
        setLabeler(saveLabeler);
    }
  }

  @Override
public void learn(Object[] examples)
  {
    if (isClone)
    {
      instance.learn(examples);
      return;
    }

    Classifier saveExtractor = extractor;
    Classifier saveLabeler = labeler;

    if (!(examples instanceof NEWord[]))
    {
      if (examples instanceof FeatureVector[])
      {
        if (!(extractor instanceof FeatureVectorReturner)) {
            setExtractor(new FeatureVectorReturner());
        }
        if (!(labeler instanceof LabelVectorReturner)) {
            setLabeler(new LabelVectorReturner());
        }
      }
      else
      {
        String type = examples == null ? "null" : examples.getClass().getName();
        System.err.println("Classifier 'NETypeTagger(NEWord)' defined on line 522 of LbjTagger.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    super.learn(examples);

    if (saveExtractor != extractor) {
        setExtractor(saveExtractor);
    }
    if (saveLabeler != labeler) {
        setLabeler(saveLabeler);
    }
  }

  @Override
public FeatureVector classify(Object __example)
  {
    if (isClone) {
        return instance.classify(__example);
    }

    Classifier __saveExtractor = extractor;

    if (!(__example instanceof NEWord))
    {
      if (__example instanceof FeatureVector)
      {
        if (!(extractor instanceof FeatureVectorReturner)) {
            setExtractor(new FeatureVectorReturner());
        }
      }
      else
      {
        String type = __example == null ? "null" : __example.getClass().getName();
        System.err.println("Classifier 'NETypeTagger(NEWord)' defined on line 522 of LbjTagger.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

__classify:
    {
      if (__example == NETypeTagger.exampleCache) {
        break __classify;
    }
      NETypeTagger.exampleCache = __example;

      NETypeTagger.cache = super.classify(__example);
    }

    if (__saveExtractor != this.extractor) {
        setExtractor(__saveExtractor);
    }
    return NETypeTagger.cache;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    if (isClone) {
        return instance.classify(examples);
    }

    Classifier saveExtractor = extractor;

    if (!(examples instanceof NEWord[]))
    {
      if (examples instanceof FeatureVector[])
      {
        if (!(extractor instanceof FeatureVectorReturner)) {
            setExtractor(new FeatureVectorReturner());
        }
      }
      else
      {
        String type = examples == null ? "null" : examples.getClass().getName();
        System.err.println("Classifier 'NETypeTagger(NEWord)' defined on line 522 of LbjTagger.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    FeatureVector[] result = super.classify(examples);
    if (saveExtractor != extractor) {
        setExtractor(saveExtractor);
    }
    return result;
  }

  @Override
public String discreteValue(Object __example)
  {
    DiscreteFeature f = (DiscreteFeature) classify(__example).firstFeature();
    return f == null ? "" : f.getValue();
  }

  @Override
public int hashCode() { return "NETypeTagger".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof NETypeTagger; }

  @Override
public java.lang.String valueOf(java.lang.Object a0, java.util.Collection a1)
  {
    if (isClone) {
        return instance.valueOf(a0, a1);
    }
    return super.valueOf(a0, a1);
  }

  @Override
public void write(java.io.PrintStream a0)
  {
    if (isClone)
    {
      instance.write(a0);
      return;
    }

    super.write(a0);
  }

  @Override
public void setLTU(LBJ2.learn.LinearThresholdUnit a0)
  {
    if (isClone)
    {
      instance.setLTU(a0);
      return;
    }

    super.setLTU(a0);
  }

  @Override
public void setLabeler(LBJ2.classify.Classifier a0)
  {
    if (isClone)
    {
      instance.setLabeler(a0);
      return;
    }

    super.setLabeler(a0);
  }

  @Override
public void setExtractor(LBJ2.classify.Classifier a0)
  {
    if (isClone)
    {
      instance.setExtractor(a0);
      return;
    }

    super.setExtractor(a0);
  }

  @Override
public void doneLearning()
  {
    if (isClone)
    {
      instance.doneLearning();
      return;
    }

    super.doneLearning();
  }

  @Override
public void forget()
  {
    if (isClone)
    {
      instance.forget();
      return;
    }

    super.forget();
  }

  @Override
public LBJ2.classify.ScoreSet scores(java.lang.Object a0)
  {
    if (isClone) {
        return instance.scores(a0);
    }
    return super.scores(a0);
  }

  @Override
public LBJ2.classify.ScoreSet scores(java.lang.Object a0, java.util.Collection a1)
  {
    if (isClone) {
        return instance.scores(a0, a1);
    }
    return super.scores(a0, a1);
  }

  @Override
public LBJ2.classify.Classifier getLabeler()
  {
    if (isClone) {
        return instance.getLabeler();
    }
    return super.getLabeler();
  }

  @Override
public LBJ2.classify.Classifier getExtractor()
  {
    if (isClone) {
        return instance.getExtractor();
    }
    return super.getExtractor();
  }
}

