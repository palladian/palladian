// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B8800000000000000054BC1BA02C0341581E759B362055A6767178A358050507EB637881C0969B94B97D732E0E2769FFC7E24E95050467CB3B7F0138D1170567C7C22E8A6B1D47E30540B4A6723CF444AD2784E9EA0E2B902FF805D05E549059E6B2B4685496AFD6EB012AEF1E2B348D3CD40991B619529A3E9A3AEB3A1A7DA909CD70B70384DE59000000

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




public class NETaggerLevel2 extends SparseNetworkLearner
{
  public static boolean isTraining = false;
  private static java.net.URL lcFilePath;
  private static NETaggerLevel2 instance;
  public static NETaggerLevel2 getInstance()
  {
    if (instance == null) {
        instance = (NETaggerLevel2) Classifier.binaryRead(lcFilePath, "NETaggerLevel2");
    }
    return instance;
  }

  static
  {
    lcFilePath = NETaggerLevel2.class.getResource("NETaggerLevel2.lc");

    if (lcFilePath == null)
    {
      System.err.println("ERROR: Can't locate NETaggerLevel2.lc in the class path.");
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
      System.err.println("WARNING: NETaggerLevel2.lc is part of a jar file.  It will be written to the current directory.  Use 'jar -u' to update the jar file.  To avoid seeing this message in the future, unpack the jar file and put the unpacked files on your class path instead.");
      instance.binaryWrite(System.getProperty("user.dir") + java.io.File.separator + "NETaggerLevel2.lc", "NETaggerLevel2");
    } else {
        instance.binaryWrite(lcFilePath.getPath(), "NETaggerLevel2");
    }
  }

  public static Parser getParser() { return null; }

  public static TestingMetric getTestingMetric() { return null; }

  private static FeatureVector cache;
  private static Object exampleCache;

  private boolean isClone;

  public NETaggerLevel2()
  {
        super("ws.palladian.external.lbj.NETaggerLevel2");
    isClone = true;
    if (instance == null) {
        instance = (NETaggerLevel2) Classifier.binaryRead(lcFilePath, "NETaggerLevel2");
    }
  }

  private NETaggerLevel2(boolean b)
  {
    super(new SparseAveragedPerceptron(.1, 0, 20));
        containingPackage = "ws.palladian.external.lbj";
    name = "NETaggerLevel2";
    setLabeler(new NELabel());
    setExtractor(new FeaturesLevel2());
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
        System.err.println("Classifier 'NETaggerLevel2(NEWord)' defined on line 476 of LbjTagger.lbj received '" + type + "' as input.");
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
        System.err.println("Classifier 'NETaggerLevel2(NEWord)' defined on line 476 of LbjTagger.lbj received '" + type + "' as input.");
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
        System.err.println("Classifier 'NETaggerLevel2(NEWord)' defined on line 476 of LbjTagger.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

__classify:
    {
      if (__example == NETaggerLevel2.exampleCache) {
        break __classify;
    }
      NETaggerLevel2.exampleCache = __example;

      NETaggerLevel2.cache = super.classify(__example);
    }

    if (__saveExtractor != this.extractor) {
        setExtractor(__saveExtractor);
    }
    return NETaggerLevel2.cache;
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
        System.err.println("Classifier 'NETaggerLevel2(NEWord)' defined on line 476 of LbjTagger.lbj received '" + type + "' as input.");
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
public int hashCode() { return "NETaggerLevel2".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof NETaggerLevel2; }

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

