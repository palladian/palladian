package ws.palladian.extraction.entity;

import java.util.List;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.evaluation.EvaluationAnnotation;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Classified;
import ws.palladian.processing.features.Annotated;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class Annotation implements Annotated, Classified {

    public static final int WINDOW_SIZE = 40;
    
    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

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

    private String targetClass;

    public Annotation(Annotation annotation) {
        // super(annotation.getTargetClass());
        this.targetClass = annotation.getTargetClass();
        offset = annotation.getStartPosition();
        length = annotation.getLength();
        entity = annotation.getValue();
        tags = new CategoryEntriesMap(annotation.getTags());
    }

    public Annotation(int offset, String entityName, String tagName) {
        // super(tagName);
        this.targetClass = tagName;
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        tags.set(tagName, 1);
    }

//    public Annotation(int offset, String entityName, String tagName, Annotations annotations) {
//        super(tagName);
//        this.offset = offset;
//        this.length = entityName.length();
//        entity = entityName;
//        tags.set(tagName, 1);
//    }

//    private int containsDateFragment(String text) {
//        text = text.toLowerCase();
//        String[] regExps = RegExp.DATE_FRAGMENTS;
//
//        int fragments = 0;
//        for (String regExp : regExps) {
//            if (text.matches(regExp.toLowerCase())) {
//                fragments++;
//            }
//        }
//
//        return fragments;
//    }

//    /**
//     * Get the following features for each annotation:<br>
//     * Numeric features<br>
//     * <ol>
//     * <li>#Words: The number of words of the annotation.</li>
//     * <li>#Chars: The number of characters of the annotation.</li>
//     * <li>#Digits: The number of digits of the annotation.</li>
//     * <li>#UpperCaseChars: The number of upper case chars of the annotation.</li>
//     * <li>(#SpecialChars: The number of special chars of the annotation such as ,"':-=?!.#.)</li>
//     * <li>#DateFragements: The number of date fragments such as "July" or "Jul" in the annotation.</li>
//     * </ol>
//     * 
//     * Nominal features<br>
//     * <ol>
//     * <li>Whether the annotation is at the start of a sentence (yes/no)</li>
//     * <li>Whether the annotation is in quotes (yes/no)</li>
//     * <li>Whether the annotation is all uppercase (yes/no)</li>
//     * <li>Whether the annotation is in brackets ()[]{}(yes/no)</li>
//     * <li>Whether the annotation ends with apostrophe ' (yes/no)</li>
//     * </ol>
//     */
//    public void createFeatures() {
//
//        String entity = getEntity();
//        String leftContext = getLeftContext().trim();
//        String rightContext = getRightContext().trim();
//
//        // // get the numeric features
//        List<Double> numericFeatures = new ArrayList<Double>();
//
//        // get the number of words
//        double numberOfWords = entity.split(" ").length;
//        numericFeatures.add(numberOfWords);
//
//        // get the number of chars
//        double numberOfChars = entity.length();
//        numericFeatures.add(numberOfChars);
//
//        // get the number of digits
//        double numberOfDigits = StringHelper.countDigits(entity);
//        numericFeatures.add(numberOfDigits);
//
//        // get the number of uppercase chars
//        double numberOfUppercaseChars = StringHelper.countUppercaseLetters(entity);
//        numericFeatures.add(numberOfUppercaseChars);
//
//        // get the number of special chars (no positive effect)
//        // double numberOfSpecialChars = StringHelper.countOccurences(entity, "[\"':;-=?!.#()/&%$§°\\[\\]]", false);
//        // numericFeatures.add(numberOfSpecialChars);
//
//        // get the number of date fragments
//        double numberOfDateFragments = containsDateFragment(entity);
//        numericFeatures.add(numberOfDateFragments);
//
//        // get the informativeness score
//        // double informativeness = InformativenessAssigner.getInstance().getInformativeness(entity);
//        // numericFeatures.add(informativeness);
//
//        // // get the nominal features
//        List<String> nominalFeatures = new ArrayList<String>();
//
//        // is the entity at the start of a sentence? check if there is a period in the immediate left context
//        boolean startOfSentence = leftContext.endsWith(".") || leftContext.endsWith("?") || leftContext.endsWith("!")
//                || leftContext.endsWith("-DOCSTART-");
//        nominalFeatures.add(String.valueOf(startOfSentence));
//
//        // is the entity in quotes? ",',´
//        boolean inQuotes = false;
//        if (leftContext.endsWith("\"") && rightContext.startsWith("\"") || leftContext.endsWith("'")
//                && rightContext.startsWith("'") || leftContext.endsWith("´") && rightContext.startsWith("´")) {
//            inQuotes = true;
//        }
//        nominalFeatures.add(String.valueOf(inQuotes));
//
//        // whether the entity is uppercase only
//        boolean completelyUppercase = StringHelper.isCompletelyUppercase(entity);
//        nominalFeatures.add(String.valueOf(completelyUppercase));
//
//        // is the entity in brackets? ()[]{}
//        boolean inBrackets = false;
//        if (leftContext.endsWith("(") && rightContext.startsWith(")") || leftContext.endsWith("[")
//                && rightContext.startsWith("]") || leftContext.endsWith("{") && rightContext.startsWith("}")) {
//            inBrackets = true;
//        }
//        nominalFeatures.add(String.valueOf(inBrackets));
//
//        // does the entity end with apostrophe? '
//        boolean endsWithApostrophe = false;
//        if (rightContext.startsWith("'")) {
//            endsWithApostrophe = true;
//        }
//        nominalFeatures.add(String.valueOf(endsWithApostrophe));
//
//        nominalFeatures.add(String.valueOf(numberOfChars));
//        // nominalFeatures.add(String.valueOf(numberOfWords));
//        // nominalFeatures.add(String.valueOf(numberOfUppercaseChars));
//        // nominalFeatures.add(String.valueOf(containsDateFragment(entity)));
//
//        // POS signature
//        // String posSignature = "";
//        // LingPipePOSTagger lpt = new LingPipePOSTagger();
//        // Object model = DataHolder.getInstance().getDataObject("models.lingpipe.en.postag");
//        // if (model == null) {
//        // DataHolder.getInstance().putDataObject("models.lingpipe.en.postag", lpt.loadModel().getModel());
//        // model = DataHolder.getInstance().getDataObject("models.lingpipe.en.postag");
//        // }
//        // lpt.setModel(model);
//        // for (TagAnnotation annotation : lpt.loadModel().tag(entity).getTagAnnotations()) {
//        // posSignature += annotation.getTag();
//        // }
//        // nominalFeatures.add(posSignature);
//
//        // starts uppercase
//        boolean startsUppercase = StringHelper.startsUppercase(entity);
//        nominalFeatures.add(String.valueOf(startsUppercase));
//
//        // case signature
//        // String caseSignature = StringHelper.getCaseSignature(entity);
//        // nominalFeatures.add(caseSignature);
//
//        // left token starts uppercase
//        String[] leftContextTokens = leftContext.split(" ");
//        boolean leftStartsUppercase = false;
//
//        if (leftContextTokens.length > 0) {
//            leftStartsUppercase = StringHelper.startsUppercase(leftContextTokens[leftContextTokens.length - 1]);
//        }
//        nominalFeatures.add(String.valueOf(leftStartsUppercase));
//
//        // right token starts uppercase
//        boolean rightStartsUppercase = StringHelper.startsUppercase(rightContext);
//        nominalFeatures.add(String.valueOf(rightStartsUppercase));
//
//        setTextFeature(entity);
//        setNumericFeatures(numericFeatures);
//        setNominalFeatures(nominalFeatures);
//    }

//    public int getEndIndex() {
//        return getOffset() + getLength();
//    }

//    public String getEntity() {
//        return entity;
//    }

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

//    public CategoryEntry getMostLikelyTag() {
//        return getTags().getMostLikelyCategoryEntry();
//    }

//    public String getMostLikelyTagName() {
//        return getTags().getMostLikelyCategory();
//    }

//    public int getOffset() {
//        return offset;
//    }

    public String getRightContext() {
        return rightContext;
    }

    public CategoryEntries getTags() {
        return tags;
    }

    public void addSubTypes(List<String> subTypes) {
        if (this.subTypes == null) {
            this.subTypes = subTypes;
        } else {
            this.subTypes.addAll(subTypes);
        }
    }

    public List<String> getSubTypes(){
        return subTypes;
    }

    public boolean matches(Annotation annotation) {
        if (getStartPosition() == annotation.getStartPosition() && getLength() == annotation.getLength()) {
            return true;
        }
        return false;
    }

    public boolean overlaps(Annotation annotation) {
        if (getStartPosition() <= annotation.getStartPosition() && getEndPosition() >= annotation.getStartPosition()
                || getStartPosition() <= annotation.getEndPosition() && getEndPosition() >= annotation.getStartPosition()) {
            return true;
        }
        return false;
    }

    public boolean sameTag(Annotation annotation) {
        if (getTag()
                .equalsIgnoreCase(annotation.getTag())) {
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
        if (getTag()
                .equalsIgnoreCase(goldStandardAnnotation.getTargetClass())) {
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
        this.tags = new CategoryEntriesMap(tags);
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
        builder.append(getTag());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int getStartPosition() {
        return offset;
    }

    @Override
    public int getEndPosition() {
        return offset + length;
    }

    @Override
    public int getIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getValue() {
        return entity;
    }

    @Override
    public String getTag() {
        return getTags().getMostLikelyCategory();
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

}
