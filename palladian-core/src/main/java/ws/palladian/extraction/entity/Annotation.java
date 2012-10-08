package ws.palladian.extraction.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.extraction.entity.evaluation.EvaluationAnnotation;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.nlp.StringHelper;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class Annotation extends UniversalInstance {

    public static final int WINDOW_SIZE = 40;

    private static final long serialVersionUID = 6235371698078169268L;

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The annotated entity. */
    private String entity;

    /** The left context of the annotation */
    private String leftContext = "";

    /** The right context of the annotation */
    private String rightContext = "";

    private List<String> subTypes = null;

    public Annotation(Annotation annotation) {
        super(null);
        offset = annotation.getOffset();
        length = annotation.getLength();
        entity = annotation.getEntity();
        setInstanceCategory(annotation.getInstanceCategory());
        assignedCategoryEntries = annotation.getTags();
    }

    public Annotation(Annotation annotation, List<UniversalInstance> instances) {
        super(instances);
        offset = annotation.getOffset();
        length = annotation.getLength();
        entity = annotation.getEntity();
        assignedCategoryEntries = annotation.getTags();
    }

    public Annotation(int offset, String entityName, CategoryEntries tags) {
        super(null);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        this.assignedCategoryEntries = tags;
    }

    public Annotation(int offset, String entityName, CategoryEntries tags, List<UniversalInstance> instances) {
        super(instances);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        this.assignedCategoryEntries = tags;
    }

    public Annotation(int offset, String entityName, String tagName) {
        super(null);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        assignedCategoryEntries.add(new CategoryEntry(assignedCategoryEntries, new Category(tagName), 1));
    }

    public Annotation(int offset, String entityName, String tagName, Annotations annotations) {
        super(annotations);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        assignedCategoryEntries.add(new CategoryEntry(assignedCategoryEntries, new Category(tagName), 1));
    }

    public Annotation(int offset, String entityName, String tagName, List<UniversalInstance> instances) {
        super(instances);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        assignedCategoryEntries.add(new CategoryEntry(assignedCategoryEntries, new Category(tagName), 1));
    }

    private int containsDateFragment(String text) {
        text = text.toLowerCase();
        String[] regExps = RegExp.DATE_FRAGMENTS;

        int fragments = 0;
        for (String regExp : regExps) {
            if (text.matches(regExp.toLowerCase())) {
                fragments++;
            }

        }

        return fragments;
    }

    /**
     * Get the following features for each annotation:<br>
     * Numeric features<br>
     * <ol>
     * <li>#Words: The number of words of the annotation.</li>
     * <li>#Chars: The number of characters of the annotation.</li>
     * <li>#Digits: The number of digits of the annotation.</li>
     * <li>#UpperCaseChars: The number of upper case chars of the annotation.</li>
     * <li>(#SpecialChars: The number of special chars of the annotation such as ,"':-=?!.#.)</li>
     * <li>#DateFragements: The number of date fragments such as "July" or "Jul" in the annotation.</li>
     * </ol>
     * 
     * Nominal features<br>
     * <ol>
     * <li>Whether the annotation is at the start of a sentence (yes/no)</li>
     * <li>Whether the annotation is in quotes (yes/no)</li>
     * <li>Whether the annotation is all uppercase (yes/no)</li>
     * <li>Whether the annotation is in brackets ()[]{}(yes/no)</li>
     * <li>Whether the annotation ends with apostrophe ' (yes/no)</li>
     * </ol>
     */
    public void createFeatures() {

        String entity = getEntity();
        String leftContext = getLeftContext().trim();
        String rightContext = getRightContext().trim();

        // // get the numeric features
        List<Double> numericFeatures = new ArrayList<Double>();

        // get the number of words
        double numberOfWords = entity.split(" ").length;
        numericFeatures.add(numberOfWords);

        // get the number of chars
        double numberOfChars = entity.length();
        numericFeatures.add(numberOfChars);

        // get the number of digits
        double numberOfDigits = StringHelper.countDigits(entity);
        numericFeatures.add(numberOfDigits);

        // get the number of uppercase chars
        double numberOfUppercaseChars = StringHelper.countUppercaseLetters(entity);
        numericFeatures.add(numberOfUppercaseChars);

        // get the number of special chars (no positive effect)
        // double numberOfSpecialChars = StringHelper.countOccurences(entity, "[\"':;-=?!.#()/&%$§°\\[\\]]", false);
        // numericFeatures.add(numberOfSpecialChars);

        // get the number of date fragments
        double numberOfDateFragments = containsDateFragment(entity);
        numericFeatures.add(numberOfDateFragments);

        // get the informativeness score
        // double informativeness = InformativenessAssigner.getInstance().getInformativeness(entity);
        // numericFeatures.add(informativeness);

        // // get the nominal features
        List<String> nominalFeatures = new ArrayList<String>();

        // is the entity at the start of a sentence? check if there is a period in the immediate left context
        boolean startOfSentence = leftContext.endsWith(".") || leftContext.endsWith("?") || leftContext.endsWith("!")
                || leftContext.endsWith("-DOCSTART-");
        nominalFeatures.add(String.valueOf(startOfSentence));

        // is the entity in quotes? ",',´
        boolean inQuotes = false;
        if (leftContext.endsWith("\"") && rightContext.startsWith("\"") || leftContext.endsWith("'")
                && rightContext.startsWith("'") || leftContext.endsWith("´") && rightContext.startsWith("´")) {
            inQuotes = true;
        }
        nominalFeatures.add(String.valueOf(inQuotes));

        // whether the entity is uppercase only
        boolean completelyUppercase = StringHelper.isCompletelyUppercase(entity);
        nominalFeatures.add(String.valueOf(completelyUppercase));

        // is the entity in brackets? ()[]{}
        boolean inBrackets = false;
        if (leftContext.endsWith("(") && rightContext.startsWith(")") || leftContext.endsWith("[")
                && rightContext.startsWith("]") || leftContext.endsWith("{") && rightContext.startsWith("}")) {
            inBrackets = true;
        }
        nominalFeatures.add(String.valueOf(inBrackets));

        // does the entity end with apostrophe? '
        boolean endsWithApostrophe = false;
        if (rightContext.startsWith("'")) {
            endsWithApostrophe = true;
        }
        nominalFeatures.add(String.valueOf(endsWithApostrophe));

        nominalFeatures.add(String.valueOf(numberOfChars));
        // nominalFeatures.add(String.valueOf(numberOfWords));
        // nominalFeatures.add(String.valueOf(numberOfUppercaseChars));
        // nominalFeatures.add(String.valueOf(containsDateFragment(entity)));

        // POS signature
        // String posSignature = "";
        // LingPipePOSTagger lpt = new LingPipePOSTagger();
        // Object model = DataHolder.getInstance().getDataObject("models.lingpipe.en.postag");
        // if (model == null) {
        // DataHolder.getInstance().putDataObject("models.lingpipe.en.postag", lpt.loadModel().getModel());
        // model = DataHolder.getInstance().getDataObject("models.lingpipe.en.postag");
        // }
        // lpt.setModel(model);
        // for (TagAnnotation annotation : lpt.loadModel().tag(entity).getTagAnnotations()) {
        // posSignature += annotation.getTag();
        // }
        // nominalFeatures.add(posSignature);

        // starts uppercase
        boolean startsUppercase = StringHelper.startsUppercase(entity);
        nominalFeatures.add(String.valueOf(startsUppercase));

        // case signature
        // String caseSignature = StringHelper.getCaseSignature(entity);
        // nominalFeatures.add(caseSignature);

        // left token starts uppercase
        String[] leftContextTokens = leftContext.split(" ");
        boolean leftStartsUppercase = false;

        if (leftContextTokens.length > 0) {
            leftStartsUppercase = StringHelper.startsUppercase(leftContextTokens[leftContextTokens.length - 1]);
        }
        nominalFeatures.add(String.valueOf(leftStartsUppercase));

        // right token starts uppercase
        boolean rightStartsUppercase = StringHelper.startsUppercase(rightContext);
        nominalFeatures.add(String.valueOf(rightStartsUppercase));

        setTextFeature(entity);
        setNumericFeatures(numericFeatures);
        setNominalFeatures(nominalFeatures);
    }

    public int getEndIndex() {
        return getOffset() + getLength();
    }

    public String getEntity() {
        return entity;
    }

    public String getLeftContext() {
        return leftContext;
    }

    public String[] getLeftContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String leftContext = getLeftContext();
        String[] words = leftContext.split(" ");
        int wordNumber = 1;
        for (int i = words.length - 1; i >= 0; i--) {

            String token = words[i];
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                 token = "NUM";
             }

             if (wordNumber == 1) {
                 contexts[0] = token;
                 contexts[1] = token;
                 contexts[2] = token;
             }

             if (wordNumber == 2) {
                 contexts[1] = token + " " + contexts[1];
                 contexts[2] = token + " " + contexts[2];
             }

             if (wordNumber == 3) {
                 contexts[2] = token + " " + contexts[2];
                 break;
             }

             wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

    // yuk. looks like this shouldn't be here.
    //    public String[] getLeftContextsPOS() {
    //
    //        Object o = Cache.getInstance().getDataObject("lpt");
    //        BasePosTagger lpt;
    //
    //        if (o != null) {
    //            lpt = (BasePosTagger) o;
    //        } else {
    //            lpt = new LingPipePosTagger();
    //            Cache.getInstance().putDataObject("lpt", lpt);
    //        }
    //
    //        String[] contexts = new String[3];
    //        contexts[0] = "";
    //        contexts[1] = "";
    //        contexts[2] = "";
    //
    //        String leftContext = getLeftContext();
    //
    //        String posLeftContext = "";
    //        TagAnnotations tas = lpt.tag(leftContext);
    //        for (TagAnnotation ta : tas) {
    //            posLeftContext += ta.getTag() + " ";
    //        }
    //        posLeftContext = posLeftContext.trim();
    //
    //        String[] words = posLeftContext.split(" ");
    //        int wordNumber = 1;
    //        for (int i = words.length - 1; i >= 0; i--) {
    //
    //            String token = words[i];
    //
    //            if (wordNumber == 1) {
    //                contexts[0] = token;
    //                contexts[1] = token;
    //                contexts[2] = token;
    //            }
    //
    //            if (wordNumber == 2) {
    //                contexts[1] = token + " " + contexts[1];
    //                contexts[2] = token + " " + contexts[2];
    //            }
    //
    //            if (wordNumber == 3) {
    //                contexts[2] = token + " " + contexts[2];
    //                break;
    //            }
    //
    //            wordNumber++;
    //        }
    //
    //        if (words.length < 3) {
    //            contexts[2] = "";
    //        }
    //        if (words.length < 2) {
    //            contexts[1] = "";
    //        }
    //
    //        return contexts;
    //    }

    public String[] getRightContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String rightContext = getRightContext();
        String[] words = rightContext.split(" ");
        int wordNumber = 1;
        for (String word : words) {

            String token = word;
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                 token = "NUM";
             }

             if (wordNumber == 1) {
                 contexts[0] = token;
                 contexts[1] = token;
                 contexts[2] = token;
             }

             if (wordNumber == 2) {
                 contexts[1] = contexts[1] + " " + token;
                 contexts[2] = contexts[2] + " " + token;
             }

             if (wordNumber == 3) {
                 contexts[2] = contexts[2] + " " + token;
                 break;
             }

             wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

    //    public String[] getRightContextsPOS() {
    //
    //        Object o = Cache.getInstance().getDataObject("lpt");
    //        BasePosTagger lpt;
    //
    //        if (o != null) {
    //            lpt = (BasePosTagger) o;
    //        } else {
    //            lpt = new LingPipePosTagger();
    //            Cache.getInstance().putDataObject("lpt", lpt);
    //        }
    //
    //        String[] contexts = new String[3];
    //        contexts[0] = "";
    //        contexts[1] = "";
    //        contexts[2] = "";
    //
    //        String rightContext = getRightContext();
    //
    //        String posRightContext = "";
    //        TagAnnotations tas = lpt.tag(rightContext);
    //        for (TagAnnotation ta : tas) {
    //            posRightContext += ta.getTag() + " ";
    //        }
    //        posRightContext = posRightContext.trim();
    //
    //        String[] words = posRightContext.split(" ");
    //        int wordNumber = 1;
    //        for (String token : words) {
    //
    //            if (wordNumber == 1) {
    //                contexts[0] = token;
    //                contexts[1] = token;
    //                contexts[2] = token;
    //            }
    //
    //            if (wordNumber == 2) {
    //                contexts[1] = contexts[1] + " " + token;
    //                contexts[2] = contexts[2] + " " + token;
    //            }
    //
    //            if (wordNumber == 3) {
    //                contexts[2] = contexts[2] + " " + token;
    //                break;
    //            }
    //
    //            wordNumber++;
    //        }
    //
    //        if (words.length < 3) {
    //            contexts[2] = "";
    //        }
    //        if (words.length < 2) {
    //            contexts[1] = "";
    //        }
    //
    //        return contexts;
    //    }

    public int getLength() {
        return length;
    }

    public CategoryEntry getMostLikelyTag() {
        return getTags().getMostLikelyCategoryEntry();
    }

    public String getMostLikelyTagName() {
        return getTags().getMostLikelyCategoryEntry().getCategory().getName();
    }

    public int getOffset() {
        return offset;
    }

    public String getRightContext() {
        return rightContext;
    }

    public CategoryEntries getTags() {
        return getAssignedCategoryEntries();
    }

    public void addSubTypes(List<String> subTypes){

        if(this.subTypes == null){

            this.subTypes = subTypes;

        }else{

            this.subTypes.addAll(subTypes);

        }

    }

    public List<String> getSubTypes(){

        return subTypes;

    }

    public boolean matches(Annotation annotation) {
        if (getOffset() == annotation.getOffset() && getLength() == annotation.getLength()) {
            return true;
        }
        return false;
    }

    public boolean overlaps(Annotation annotation) {
        if (getOffset() <= annotation.getOffset() && getEndIndex() >= annotation.getOffset()
                || getOffset() <= annotation.getEndIndex() && getEndIndex() >= annotation.getOffset()) {
            return true;
        }
        return false;
    }

    public boolean sameTag(Annotation annotation) {
        if (getMostLikelyTag().getCategory().getName()
                .equalsIgnoreCase(annotation.getMostLikelyTag().getCategory().getName())) {
            return true;
        }
        return false;
    }

    /**
     * Compare this annotation with an annotation where the correct tag is known (gold standard / evaluation
     * annotation).
     * 
     * @param goldStandardAnnotation The gold standard annotation.
     * @return
     */
    public boolean sameTag(EvaluationAnnotation goldStandardAnnotation) {
        if (getMostLikelyTag().getCategory().getName()
                .equalsIgnoreCase(goldStandardAnnotation.getInstanceCategoryName())) {
            return true;
        }
        return false;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setLeftContext(String leftContext) {
        this.leftContext = leftContext;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setRightContext(String rightContext) {
        this.rightContext = rightContext;
    }

    public void setTags(CategoryEntries tags) {
        this.assignedCategoryEntries = tags;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [offset=");
        builder.append(offset);
        builder.append(", length=");
        builder.append(length);
        builder.append(", entity=");
        builder.append(entity);
        builder.append(", tag=");
        builder.append(getMostLikelyTagName());
        builder.append("]");
        return builder.toString();
    }

    /**
     * Try to find which of the given annotation are part of this entity. For example: "New York City and Dresden"
     * contains two entities that might be in the given annotation set. If so, we return the found annotations.
     * 
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    public Annotations unwrapAnnotations(Annotations annotations, Dictionary entityDictionary) {
        Annotations unwrappedAnnotations = new Annotations();

        boolean isAllUppercase = StringHelper.isCompletelyUppercase(getEntity());

        if (!isAllUppercase) {
            return unwrappedAnnotations;
        }

        String entityName = getEntity().toLowerCase();
        int length = entityName.length();

        // annotations.sortByLength();

        for (Annotation annotation : annotations) {
            if (annotation.getLength() < length) {
                int index = entityName.indexOf(" " + annotation.getEntity().toLowerCase() + " ");
                if (index > -1 && annotation.getEntity().length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, annotation.getEntity(),
                            annotation.getMostLikelyTagName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(annotation.getEntity().toLowerCase() + " ");
                if (index == 0 && annotation.getEntity().length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index, annotation.getEntity(),
                            annotation.getMostLikelyTagName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(" " + annotation.getEntity().toLowerCase());
                if (index == entityName.length() - annotation.getEntity().length() - 1
                        && annotation.getEntity().length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, annotation.getEntity(),
                            annotation.getMostLikelyTagName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        // go through the entity dictionary
        for (Map.Entry<String, CategoryEntries> termEntry : entityDictionary.getCategoryEntries().entrySet()) {
            String word = termEntry.getKey();
            if (word.length() < length) {
                int index = entityName.indexOf(" " + word.toLowerCase() + " ");
                if (index > -1 && word.length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, word, termEntry.getValue()
                            .getMostLikelyCategoryEntry().getCategory().getName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(word.toLowerCase() + " ");
                if (index == 0 && word.length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index, word, termEntry.getValue()
                            .getMostLikelyCategoryEntry().getCategory().getName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(" " + word.toLowerCase());
                if (index == entityName.length() - word.length() - 1 && word.length() > 2) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, word, termEntry.getValue()
                            .getMostLikelyCategoryEntry().getCategory().getName(), annotations);
                    wrappedAnnotation.createFeatures();
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        return unwrappedAnnotations;
    }

    public Annotations unwrapAnnotations(PalladianTextClassifier classifier, Preprocessor preprocessor) {
        Annotations unwrappedAnnotations = new Annotations();

        if (getEntity().indexOf(" ") == -1) {
            return unwrappedAnnotations;
        }

        String[] words = getEntity().split(" ");
        String[] tags = new String[words.length];

        // classify each word
        for (int i = 0; i < words.length; i++) {

            tags[i] = classifier.classify(words[i]).getMostLikelyCategoryEntry().getCategory().getName();
            // TextInstance document = preprocessor.preProcessDocument(words[i]);
            // tags[i] = document.getMainCategoryEntry().getCategory().getName();

        }

        // create annotations
        Annotation lastAnnotation = new Annotation(0, "", "");
        for (int i = 0; i < words.length; i++) {
            String tag = tags[i];

            if (!tag.equalsIgnoreCase(lastAnnotation.getMostLikelyTagName())) {
                List<Integer> indexList = StringHelper.getOccurrenceIndices(getEntity(), " ");
                int offsetPlus = 0;
                if (i > 0) {
                    offsetPlus = indexList.get(i - 1) + 1;
                }
                lastAnnotation = new Annotation(getOffset() + offsetPlus, words[i], tags[i]);
                unwrappedAnnotations.add(lastAnnotation);
            } else {
                // update last annotation
                lastAnnotation.setEntity(lastAnnotation.getEntity() + " " + words[i]);
                lastAnnotation.setLength(lastAnnotation.getEntity().length());
            }
        }

        return unwrappedAnnotations;
    }

}
