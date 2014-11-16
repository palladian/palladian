Text Classification
===================

Text classification is the process of assigning one or more categories to a given text document. A typical use case is spam filtering, where e-mails are categorized as being *spam* or *no-spam*. Another example is language detection, where the natural language of a supplied text is predicted, e.g. whether the text is likely in *English*, *German*, *French* or *Italian*.

In the following we will explain which features can be used for the classification, the basic theory of the classifier, and how the performance of a classifier can be evaluated. In each section, we will describe the underlying theory and how the classification components can be used programmatically.

This classifier won the first Research Garden competition where the goal was to classify product descriptions into eight different categories. See <a href="https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137">press release</a> (via archive.org).

Datasets
--------

This section describes how to prepare text input data to learn, evaluate, and use a text classifier. The classifier requires an `Iterable` of `Instance` for learning. Each `Instance` is a training document which provides text and a category. Instances for the text classifier can be created programmatically using the `InstanceBuilder` class:

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

Features are the input for a classifier. In text classification, we have a long string as an input from which we need to derive features during preprocessing. Palladian's text classifier works with <a href="http://en.wikipedia.org/wiki/N-gram">*n*-grams</a>. *n*-grams are sets of tokens of the length *n*, which are created by sliding a "window" over the given text. Palladian can create features using character- or word-based *n*-grams.

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

There are several more options available for fine-tuning. Please refer to `FeatureSettingBuilder`'s Javadoc for more detailed information. The optimal feature setting depends heavily on data you want to classify. In the section <a href="#Optimization">Optimization</a>, we will explain how you can deterine the perfect settings for your data conveniently.

Classifier
----------

The `PalladianTextClassifier` calculates the probabilities for each learned category for the input document. Before we can use a classifier, we need to learn a model. At the training stage, a dictionary is built by counting co-occurrences of an *n*-gram and a category. This dictionary serves as model for the classification of unseen data. An example dictionary might look as shown in the following table, where each column is a category (*finance*, *travel*, and *science*) and each row is a 1-gram. In each cell, we have the co-occurrence count of a 1-gram and a category. In the example, the 1-gram *money* is more likely to be in the category *finance* than *science*, while the 1-gram *beach* is most likely to appear in the category *travel*.

| 1-gram  | *finance* | *travel* | *science* |
|---------|----------:|---------:|----------:|
| *money* |   `12`    |    `3`   |    `5`    |
| *beach* |    `2`    |   `17`   |    `1`    |
| *paper* |    `6`    |    `4`   |   `10`    |

To classify a new document, the classifier looks up the counts of each of its *n*-grams in the dictionary and calculates probabilities for each category. The used scoring mechanism is described in the section <a href="#Scoring">Scoring</a>.

Dictionaries are stored using a <a href="http://en.wikipedia.org/wiki/Trie">trie</a> data structure, which provides efficient storage as well as fast access during classification. For more information, refer to the Javadoc of `DictionaryTrieModel`.

Scoring
-------

For calculating category probabilities a `Scorer` is used. We employ a <a href="http://en.wikipedia.org/wiki/Naive_Bayes_classifier">Naïve-Bayes</a>-like method, which is turbocharged using complement class scoring as described in <a href="http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf">Tackling the Poor Assumptions of Naive Bayes Text Classifiers</a> (Jason D. M. Rennie; Lawrence Shih; Jaime Teevan; David R. Karger; 2003).

The following equations show how the scoring works: 

<!--
	\operatorname{p}(\mathit{\neg\,\mathit{category}} \, \vert \, \mathit{D}) = 
	  \sum_{t\, \in \, \operatorname{preprocess}(\mathit{D})} 
	  % \operatorname{tfidf}(t)
	  \log
	  \frac
	    {\operatorname{count}(t, \neg\,\mathit{category}) + 1}
	    {\operatorname{termCount}(\neg\,\mathit{category}) + \lvert\,\mathit{Terms}\,\rvert}
-->

<!-- ![](../resources/images/equation-complement-naive-bayes.png) -->

<img src="images/equation-complement-naive-bayes.png" />

<!--
  \operatorname{classify}(D) = 
  \operatorname*{arg\,max}_{\mathit{category}\,\in\,\mathit{Categories}} 
  \big(
        \log \frac
          {\operatorname{docCount}(\mathit{category})}
          {\lvert\,\mathit{Documents}\,\rvert}
        - \operatorname{p}(\neg\, \mathit{category} \, \vert \, D)
  \big)
-->

<!-- ![](../resources/equation-complement-naive-bayes-2.png) -->
<img src="images/equation-complement-naive-bayes-2.png" />

The described scorer implementation is available as class `BayesScorer`. If you want to customize the scoring method, you may roll out your own implementation. See `Scorer` Javadoc for more details.

Pruning
-------

Depending on the amount of training data and the feature settings, dictionaries can get quite large in size. To reduce space consumption of such models, different pruning strategies can be used, which remove potentially less relevant *n*-grams. A simple and efficient strategy is to remove such *n*-grams which occurred rarely during training, because one can assume, that those will not occur frequently during classification, making them less useful than more frequent *n*-grams. While pruning can significantly reduce the size of a dictionary, it usually comes with a sacrifice concerning classification accuracy. Read the <a href="#Optimization">next section</a>, how you can evaluate the impact of different pruning strategies.

The class `PruningStrategies` provides different ready to use methods for pruning. When instantiating a `PalladianTextClassifier`, you can explicity specify a `DictionaryBuilder` which can be setup with a pruning strategy like so:

```java
DictionaryBuilder builder = new DictionaryTrieModel.Builder();
// remove entries from final dictionary, which occurred less than five times
builder.setPruningStrategy(PruningStrategies.termCount(5));
PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, builder);
```

You may also create your own pruning strategies by providing an implementation for `Filter<CategoryEntries>`.


Optimization
------------

In order to find out which classifier configuration works best, you can evaluate different setups. The `PalladianTextClassifierOptimizer` allows to run an extensive evaluation with different settings on given sets of training and validation data. Make sure to use disjunct datasets for training and validation, otherwise the results will not be meaningful.

The evaluation produces a CSV file which holds the results for a combination of <a href="#Features">feature settings</a>, <a href="#pruning">pruning strategies</a> and <a href="#Scoring">scorers</a>. The performance is measured in <a href="http://en.wikipedia.org/wiki/Precision_and_recall">precision, recall</a>, <a href="http://en.wikipedia.org/wiki/F1_score">F1</a>, and <a href="http://en.wikipedia.org/wiki/Accuracy_and_precision">accuracy</a>, separately for each category and as an average over all categories.

```java
PalladianTextClassifierOptimizer optimizer = new PalladianTextClassifierOptimizer();

// create combinations of settings for char-based 5- to 8- and word-based 1- to 2-grams;
// this will also create settings e.g. for character-6-7-grams, if you do not want that,
// add the #noCombinations method
optimizer.setFeatureSettings(new FeatureSettingGenerator().chars(5, 8).words(1, 3).create());

// compare no pruning with minimum term count 2
optimizer.setPruningStrategies(PruningStrategies.none(), PruningStrategies.termCount(2));

// evaluate bayes scorer
optimizer.setScorers(new BayesScorer(LAPLACE, COMPLEMENT));

// run optimization with all (feature setting, pruning, scorer) combinations using the 
// given training and validation data, and write results to optimizationResult.csv file
optimizer.runOptimization(train, validate, "optimizationResult.csv");
```

Full Example
------------

