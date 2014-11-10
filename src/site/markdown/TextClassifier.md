Text Classification
===================

Text classification is the process of assigning one or more categories to a given text document. A typical use case is spam filtering, where e-mails are categorized as being *spam* or *no-spam*. Another example is language detection, where the natural language of a supplied text is predicted, e.g. whether the text is likely in *English*, *German*, *French* or *Italian*.

In the following we will explain which features can be used for the classification, the basic theory of the classifier, and how the performance of a classifier can be evaluated. In each section, we will describe the underlying theory and how the classification components can be used programmatically.

This classifier won the first Research Garden competition where the goal was to classify product descriptions into eight different categories. See <a href="https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137">press release</a> (via archive.org).

Datasets
--------

This section describes how to prepare text input data to learn, evaluate, and use a text classifier. The classifier requires an `Iterable` with `Instance`s for learning. Each `Instance` is a training document which provides text and a training category. Instances for the text classifier can be created programmatically using the `InstanceBuilder` class:

```java
List<Instance> docs = CollectionHelper.newArrayList();
docs.add(new InstanceBuilder().setText("Chinese Beijing Chinese").create("yes"));
docs.add(new InstanceBuilder().setText("Chinese Chinese Shanghai").create("yes"));
docs.add(new InstanceBuilder().setText("Chinese Macao").create("yes"));
docs.add(new InstanceBuilder().setText("Tokyo Japan Chinese").create("no"));
```

However, you usually want to provide large datasets via file. In this case, you can use the `TextDatasetIterator` class, which reads existing files from disk. The format of the files needs to conform to the structure `text-separator-category`, for example:

```
Chinese Beijing Chinese###yes
Chinese Chinese Shanghai###yes
Chinese Macao###yes
Tokyo Japan Chinese###no
```

Alternatively, the text can also reside in separate files. In this case, a relative path to the text document must be given instead of the actual text, such as `yes/doc1.txt###yes`. Create the `TextDatasetIterator` as follows:

```java
// last parameter indicates, whether dataset.csv contains references, or full text
Iterable<Instance> docs = new TextDatasetIterator("path/to/dataset.csv", "###", true);
```

Features
--------

Features are the input for a classifier. In text classification, we have a long string as an input from which we need to derive several features during preprocessing. Palladian's text classifier works with <a href="http://en.wikipedia.org/wiki/N-gram">*n*-grams</a>. *n*-grams are sets of tokens of the length *n*, which are created by sliding a "window" over the given text. Palladian can create features using character- or word-based *n*-grams.

As an example, consider the text `"the quick brown fox"`: 

* The set of word-based 2-grams would contain the following entries: `{"the quick", "quick brown", "brown fox"}`. 
* The set of character-5-grams consists of the following entries: `{"the q", "he qu", "e qui", " quic", "quick", …}`.
* It is possible to combine *n*-grams of different lengths. For example, the set of character-4-6-grams contains the union of the sets of 4-, 5-, and 6-grams.

The configuration for the feature extraction is stored in a `FeatureSetting` instance, which is created using the `FeatureSettingBuilder`:

```java
// character-based 4-, 5-, and 6-grams
FeatureSetting charSetting = FeatureSettingBuilder.chars(4, 6).create();
// word-based 1- and 2-grams
FeatureSetting wordSetting = FeatureSettingBuilder.words(1, 2).create();
```

There are several more options available for fine-tuning the feature extraction. Please refer to `FeatureSettingBuilder`'s Javadoc for more detailed information.

Classifier
----------

The `PalladianTextClassifier` calculates the probabilities for each learned category for the input document. Palladian's text classifier was developed completely from scratch and does not rely on external libraries. In the following sections, the classifier is explained in more detail.

At the training stage, a dictionary is built by counting co-occurrences of an *n*-gram and a category. This dictionary serves as model for the classification. An example dictionary might look as shown in the following table, where each column is a category (*finance*, *travel*, and *science*) and each row is a 1-gram. In each cell, we have the co-occurrence count of a 1-gram and a category. In the example, the 1-gram *money* is more likely to be in the category *finance* than *science*, while the 1-gram *beach* is most likely to appear in the category *travel*.

| 1-gram  | *finance* | *travel* | *science* |
|---------|----------:|---------:|----------:|
| *money* |   `12`    |    `3`   |    `5`    |
| *beach* |    `2`    |   `17`   |    `1`    |
| *paper* |    `6`    |    `4`   |   `10`    |

To classify a new document, we look up the counts of each of its *n*-grams in the dictionary and calculate the probabilities for each category. We employ a <a href="http://en.wikipedia.org/wiki/Naive_Bayes_classifier">Naïve-Bayes</a>-like method, which is turbocharged using <a href="http://en.wikipedia.org/wiki/Tf–idf">TF-IDF</a> weighting and complement class scoring as described in <a href="http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf">Tackling the Poor Assumptions of Naive Bayes Text Classifiers</a> (Jason D. M. Rennie; Lawrence Shih; Jaime Teevan; David R. Karger; 2003).

Dictionaries are stored using a <a href="http://en.wikipedia.org/wiki/Trie">trie</a> data structure, which provides efficient storage as well as fast access during classification. For more information, refer to the Javadoc of `DictionaryTrieModel`.

Scorer
------

Pruning
-------

Depending on the amount of training data and the feature settings, dictionaries can get quite large in size. To reduce space consumption of such models, different pruning strategies can be used, which remove potentially less relevant *n*-grams. A simple and efficient strategy is to remove such *n*-grams which occurred rarely during training, because one can assume, that those will not occur frequently during classification, making them less useful than more frequent *n*-grams. While pruning can significantly reduce the size of a dictionary, it usually comes with a sacrifice concerning classification accuracy. Read the <a href="#optimization">next section</a>, how you can evaluate the impact off different pruning strategies.

The class `PruningStrategies` provides different ready to use methods for pruning. When instantiating a `PalladianTextClassifier`, you can explicity specify a `DictionaryBuilder` which can be setup with a pruning strategy like so:

```java
DictionaryBuilder builder = new DictionaryTrieModel.Builder();
// remove entries from final dictionary, which occurred less than five times
builder.setPruningStrategy(new PruningStrategies.TermCountPruningStrategy(5));
PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, builder);
```

You may also create your own pruning strategies by providing an implementation for `Filter<CategoryEntries>`.


Optimization
------------
<a name="optimization"></a>

In order to find out which classifier configuration works best, you can evaluate different setups. The `FeatureSettingOptimizer` provides the method `evaluateFeatureSettings` to run an extensive evaluation with different settings on given sets of training and validation data.

The evaluation method produces a CSV file which holds the results for a combination feature settings, pruning strategies and scorers. The performance is measures in <a href="http://en.wikipedia.org/wiki/Precision_and_recall">precision, recall</a>, <a href="http://en.wikipedia.org/wiki/F1_score">F1</a>, and <a href="http://en.wikipedia.org/wiki/Accuracy_and_precision">accuracy</a>, separately for each category and as an average over all categories.

Best Practices
--------------


\paragraph{Training a classifier}
Before we can use a classifier, we need to learn a model. The model is an internal representation of the learned data. After learning a model, a classifier can applied to unseen data. We now have prepared the training and testing data so we can now learn the models.
The classifier is saved as a lucene index or a database under the name of the classifier with a ``Dictionary'' suffix. The result of the learning are three files: the classifier (``CLASSIFIER.ser''), the dictionary object (``CLASSIFIERDictionary.ser''), and the actual dictionary as the index or database (e.g. ``CLASSIFIERDictionary.h2.db''). More settings can be configured in the config/classification.conf file. See \ref{sec:classification.conf} for more information.

Listing \ref{listing:trainClassifier} shows an example for how to train and save a classifier. In this case we train a classifier that can classify the language of given documents. As training data we use a list of web pages of different languages from Wikipedia.

\begin{codelisting}
\begin{lstlisting}[label=listing:trainClassifier,caption=Training a classifier.,frame=tb]
// create a classifier mananger object
ClassifierManager classifierManager = new ClassifierManager();

// specify the dataset that should be used as training data
Dataset dataset = new Dataset();

// set the path to the dataset
String dsPath = "data/datasets/classification/language/index.txt";
dataset.setPath(dsPath);

// tell the preprocessor that the first field in the file is a link to 
// the actual document
dataset.setFirstFieldLink(true);

// create a text classifier by giving a name and a path 
// where it should be saved to
String dcn = "LanguageClassifier";
String dcp = "data/models/languageClassifier/";
TextClassifier classifier = new DictionaryClassifier(dcn,dcp);

// specify the settings for the classification
ClassificationTypeSetting cts = new ClassificationTypeSetting();

// we use only a single category per document
cts.setClassificationType(ClassificationTypeSetting.SINGLE);

// we want the classifier to be serialized in the end
cts.setSerializeClassifier(true);

// specify feature settings that should be used by the classifier
FeatureSetting featureSetting = new FeatureSetting();

// we want to create character-level n-grams
featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

// the minimum length of our n-grams should be 3
featureSetting.setMinNGramLength(3);

// the maximum length of our n-grams should be 5
featureSetting.setMaxNGramLength(5);

// we assign the settings to our classifier
classifier.setClassificationTypeSetting(classificationTypeSetting);
classifier.setFeatureSetting(featureSetting);

// now we can train the classifier using the given dataset
classifierManager.trainClassifier(dataset, classifier);
\end{lstlisting}
\end{codelisting}

\paragraph{Using a classifier}
After we trained a model for a classifier we can apply it to unseen data. Let's use the model we just trained to classify the language of a new document.

Listing \ref{listing:useClassifier} shows how to use a trained classifier.

\begin{codelisting}
\begin{lstlisting}[label=listing:useClassifier,caption=Use a trained text classifier.,frame=tb]
// the path to the classifier we want to use
String path = "data/models/languageClassifier/LanguageClassifier.ser";

// load the language classifier
TextClassifier classifier = ClassifierManager.load(path);

// create a classification document that holds the result
ClassificationDocument classifiedDocument = null;

// classify the little text (if classifier works it would say Spanish)
classifiedDocument = classifier.classify("Yo solo s� que no s� nada.");

// print the classified document
System.out.println(classifiedDocument);
\end{lstlisting}
\end{codelisting}

You can also try the language classifier online at \url{http://www.webknox.com/wi#detectLanguage}.

\paragraph{Evaluating a Classifier}
To get an idea of how good a trained classifier works, we can evaluate it using test data which is structured the same way as the training data. Listing~\ref{listing:evaluateClassifier} shows how to evaluate a trained classifier, you will see that is very similar to training a classifier. Make sure that you evaluate the classifier using disjunct data, otherwise the evaluation results are invalid .

\begin{codelisting}
\begin{lstlisting}[label=listing:evaluateClassifier,caption=Evaluating a trained text classifier.,frame=tb]
// create a classifier mananger object
ClassifierManager classifierManager = new ClassifierManager();

// the path to the classifier we want to use
String path = "data/models/languageClassifier/LanguageClassifier.ser";

// specify the dataset that should be used as testing data
Dataset dataset = new Dataset();

// the path to the dataset (should NOT overlap with the training set)
dataset.setPath("data/datasets/classification/language/index.txt");

// tell the preprocessor that the first field in the file is a link
// to the actual document
dataset.setFirstFieldLink(true);

// load the language classifier
TextClassifier classifier = ClassifierManager.load(path);

// now we can test the classifier using the given dataset
ClassifierPerformance classifierPerformance = null;
classifierManager.testClassifier(dataset, classifier);
\end{lstlisting}
\end{codelisting}

\paragraph{Testing parameter combinations}
As you have seen, you can train the classifier using different parameters. So how can you be sure that you set the parameters correctly? Do they work well on different datasets? Is the chosen classifier always better than others? In order to answer these questions with hard data you can automatically run different combinations of classifiers, settings, and datasets as shown in Figure \ref{fig:bcc}. The green line shows the combination that was found to perform best. In the end you will get one evaluation csv with information about how the combination performed. You can then manually pick the best performing settings.

\begin{figure}[ht!]
\centering
\includegraphics[width=\columnwidth]{img/bcc.pdf}
\caption{Combinations for training a solid classifier.}
\label{fig:bcc}
\end{figure}

Listing \ref{listing:bcc} shows you how to do just that.

\begin{codelisting}
\begin{lstlisting}[label=listing:bcc,caption=Learning the best parameter combination for a text classifier.,frame=tb]
ClassifierManager classifierManager = new ClassifierManager();

// build a set of classification type settings to evaluate
List<ClassificationTypeSetting> ctsList;
ctsList = new ArrayList<ClassificationTypeSetting>();
ClassificationTypeSetting cts = new ClassificationTypeSetting();
cts.setClassificationType(ClassificationTypeSetting.SINGLE);
cts.setSerializeClassifier(false);
ctsList.add(cts);

// build a set of classifiers to evaluate
List<TextClassifier> classifiers = new ArrayList<TextClassifier>();
TextClassifier classifier = null;
classifier = new DictionaryClassifier();
classifiers.add(classifier);
classifier = new KNNClassifier();
classifiers.add(classifier);

// build a set of feature settings for evaluation
List<FeatureSetting> featureSettings = new ArrayList<FeatureSetting>();
FeatureSetting fs = null;
fs = new FeatureSetting();
fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
fs.setMinNGramLength(3);
fs.setMaxNGramLength(7);
featureSettings.add(fs);

fs = new FeatureSetting();
fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
fs.setMinNGramLength(2);
fs.setMaxNGramLength(5);
featureSettings.add(fs);

fs = new FeatureSetting();
fs.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
fs.setMinNGramLength(2);
fs.setMaxNGramLength(5);
featureSettings.add(fs);

// build a set of datasets that should be used for evaluation
Set<Dataset> datasets = new HashSet<Dataset>();
Dataset dataset = new Dataset();
dataset.setPath("dataset1.txt");
datasets.add(dataset);
dataset = new Dataset();
dataset.setPath("dataset2.txt");
dataset.setSeparationString("#");
datasets.add(dataset);

// set evaluation settings
EvaluationSetting evaluationSetting = new EvaluationSetting();
evaluationSetting.setTrainingPercentageMin(20);
evaluationSetting.setTrainingPercentageMax(80);
evaluationSetting.setkFolds(5);
evaluationSetting.addDataset(dataset);

// let's take the time
StopWatch stopWatch = new StopWatch();

// train and test all classifiers in all combinations
classifierManager.learnBestClassifier(ctsList, classifiers, 
                                      featureSettings,
                                      evaluationSetting);

System.out.println("finished training and testing classifier
          combinations in " + stopWatch.getElapsedTimeString());
\end{lstlisting}
\end{codelisting}