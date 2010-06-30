/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import tud.iir.knowledge.Entity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

/**
 * The Class MIOContextAnalyzer.
 */
public class MIOContextAnalyzer {

    /** The entity. */
    private Entity entity;

    /** The mioPage. */
    private MIOPage mioPage;

    /** The page-context-trust. */
    private double pageContextTrust = 0;

    /** A List of bad words. */
    private String[] badWords = { "banner", "tower", "titlefont", "newsletter", "cloud", "footer", "ticker", "ads",
            "expressinstall" };

    /**
     * Instantiates a new mIO context analyzer.
     * 
     * @param entity the entity
     * @param mioPage the mio page
     */
    public MIOContextAnalyzer(final Entity entity, final MIOPage mioPage) {
        this.entity = entity;
        this.mioPage = mioPage;
        pageContextTrust = calcPageContextTrust();
        // System.out.println("PageContextTrust: " + pageContextTrust + " for: " + mioPage.getUrl());
    }

    /**
     * Instantiates a new mIO context analyzer.
     */
    public MIOContextAnalyzer() {

    }

    /**
     * Calculate trust.
     * 
     * @param mio the mio
     */
    public void calculateTrust(final MIO mio) {
        this.entity = mio.getEntity();
        final double fileNameRelevance = (calcStringRelevance(mio.getFileName(), entity)) * 4;
        // System.out.println("FileNameRelevance: " + fileNameRelevance);
        final double filePathRelevance = calcStringRelevance(mio.getDirectURL(), entity) * 2;
        // System.out.println("FilePathRelevance: "+filePathRelevance);
        // double pcTrust = calcPageContextTrust(mioPage);

        double altTextRelevance = 0;
        String altText = "";
        if (mio.getInfos().containsKey("altText")) {
            try {
                altText = (String) mio.getInfos().get("altText").get(0);
            } catch (Exception e) {
                // do nothing
            }

            if (altText.length() > 1) {
                altTextRelevance = calcStringRelevance(altText, entity);
            }
        }

        double headlineRelevance = 0;
        String headlines = "";
        if (mio.getInfos().containsKey("previousHeadlines")) {
            try {
                // get the nearest previousHeadline
                headlines = (String) mio.getInfos().get("previousHeadlines").get(0);
            } catch (Exception e) {
                // do nothing
            }

            if (headlines.length() > 1) {
                headlineRelevance = calcStringRelevance(headlines, entity);
            }
        }

        double surroundingTextRelevance = 0;
        String surroundingText = "";
        if (mio.getInfos().containsKey("surroundingText")) {
            try {
                surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
            } catch (Exception e) {
                // do nothing
            }

            if (surroundingText.length() > 1) {
                surroundingTextRelevance = calcStringRelevance(surroundingText, entity);
            }
        }

        final double mioTrust = pageContextTrust + fileNameRelevance + filePathRelevance + altTextRelevance
                + headlineRelevance + surroundingTextRelevance;
        // System.out.println("pageContextTrust: " + pageContextTrust);

        mio.setTrust(mioTrust);
        mio.setTrust(checkForBadWords(mio));
    }

    /**
     * Initially calculate the trust for a mioPage which influences all its containing MIOs.
     * 
     * @return the double
     */
    private double calcPageContextTrust() {

        final double titleRelevance = calcStringRelevance(mioPage.getTitle(), entity);

        double linkNameRelevance = 0;
        double linkTitleRelevance = 0;
        if (mioPage.isLinkedPage()) {
            linkNameRelevance = calcStringRelevance(mioPage.getLinkName(), entity);
            linkTitleRelevance = calcStringRelevance(mioPage.getLinkTitle(), entity);
        }

        double iframeParentTitleRelevance = 0;
        if (mioPage.isIFrameSource()) {
            iframeParentTitleRelevance = calcStringRelevance(mioPage.getIframeParentPageTitle(), entity);
        }

        final double urlRelevance = calcStringRelevance(mioPage.getUrl(), entity);

        final double dpTrust = mioPage.getDedicatedPageTrust() * 2;

        final double pcTrust = titleRelevance + linkNameRelevance + linkTitleRelevance + iframeParentTitleRelevance
                + urlRelevance + (dpTrust * 2);
        return pcTrust;

    }

    /**
     * Calc string relevance.
     * 
     * @param inputString the input string
     * @param entity the entity
     * @return the double
     */
    private double calcStringRelevance(final String inputString, final Entity entity) {
        return calcStringRelevance(inputString, entity.getName());
    }

    /**
     * Calculate the relevance of a string by checking how many terms or morphs of the entityName are included in the
     * string. A special role play words like
     * d500 or x500i. Returns: a value from 0 to 1
     * 
     * @param inputString the string
     * @param entityName the entity name
     * @return the double
     */
    private double calcStringRelevance(final String inputString, final String entityName) {
        // SearchWordMatcher swm = new SearchWordMatcher(entityName);

        // String Elements[] = entityName.split("\\s");
        // String input[]= inputString.split("\\s");
        // // calculate the number of searchWord-Matches
        // double NumOfMatches = (double) swm.getNumberOfSearchWordMatches(inputString);
        // // System.out.println("number of swm: " + NumOfMatches);
        // // calculate the number of searchWord-Matches with ignoring specialWords
        // // like D500x
        // double NumOfMatchesWithoutSW = (double) swm.getNumberOfSearchWordMatches(inputString, true,
        // entityName.toLowerCase());
        // // System.out.println("number of swm without SW: " + NumOfMatchesWithoutSW);
        // double Diff = (double) NumOfMatches - NumOfMatchesWithoutSW;
        //
        // double result = (double) ((NumOfMatchesWithoutSW * 2) + (Diff * 3)) / (double) (Elements.length * 2);

        // if (Diff>0){
        //
        // //result = (double)(NumOfMatches-1)/Elements.length+
        // (Diff/Elements.length) + (Diff/(2*Elements.length));
        // //
        // result=(double)NumOfMatches/Elements.length+(Diff/(2*Elements.length));
        // result =
        // (double)((NumOfMatchesWithoutSW*2)+(Diff*3))/(double)(Elements.length*2);
        // }else{
        // result=(double)NumOfMatches/Elements.length;
        // }

        final JaroWinkler js = new JaroWinkler();
        double result = (double) js.getSimilarity(entityName, inputString);

        if (result > 1) {
            // System.out.println("result groesser 1!");
            result = 1;
        }
        return result;
    }

    /**
     * Check for bad words.
     * 
     * @param mio the mio
     * @return the double
     */
    private double checkForBadWords(MIO mio) {

        for (String badWord : badWords) {
            if (!mio.getEntity().getName().contains(badWord) && mio.getDirectURL().contains(badWord)) {
                double result = mio.getTrust();
                return (result / 2);
            }
        }
        return mio.getTrust();
    }

    // private double calcHeadlineRelevance(mio){
    //
    // }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {
        // MIOContextAnalyzer mCA = new MIOContextAnalyzer();
        // System.out
        // .println(mCA
        // .extractALTTextFromTag("<OBJECT classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" ID=flaMovie WIDTH=250 HEIGHT=250>  <EMBED src=\"flaMovie.swf\"    FlashVars=\"userName=permadi&score=80\"     bgcolor=#99CC33 WIDTH=250 HEIGHT=250    TYPE=\"application/x-shockwave-flash\">  </EMBED> Hier steht der alternative Content!</OBJECT>"));
        // Concept exampleConcept = new Concept("mobilePhone");
        // Entity entity = new Entity("samsung wave s8500", exampleConcept);
        // System.out.println("Relevance:"
        // + mCA.calcStringRelevance("wave s8500", entity));

        // http://www.dcs.shef.ac.uk/~sam/stringmetrics.html#overlap
        // OverlapCoefficient oc = new OverlapCoefficient();
        // QGramsDistance js = new QGramsDistance();
        // CosineSimilarity js = new CosineSimilarity();
        // BlockDistance bd = new BlockDistance();;
        // DiceSimilarity ds = new DiceSimilarity();
        // EuclideanDistance js = new EuclideanDistance();
        // JaccardSimilarity js = new JaccardSimilarity();
        // JaroWinkler js = new JaroWinkler();
        // MIOContextAnalyzer mioCon = new MIOContextAnalyzer();
        //
        // System.out.println(js.getSimilarity("samsung s8500 wave",
        // "das samsung s8500 wave ist besser als das samsung s9500!"));
        // System.out.println("own: "
        // + mioCon.calcStringRelevance("das samsung s8500 wave ist besser als das samsung s9500!",
        // "samsung s8500 wave"));
        //
        // System.out.println(js.getSimilarity("samsung s8500 wave", "samsung s8500 wave"));
        // System.out.println(js.getSimilarity("samsung s8500 wave", "wave s8500 samsung"));
        // System.out.println("own: " + mioCon.calcStringRelevance("samsung s8500 wave", "samsung s8500 wave"));
        //
        // System.out.println(js.getSimilarity("samsung", "das neue samsung ist toll"));
        // System.out.println("own: " + mioCon.calcStringRelevance("das neue samsung ist toll", "samsung"));
        //
        // System.out.println(js.getSimilarity("s8500", "s_8500"));
        // System.out.println("own: " + mioCon.calcStringRelevance("s_8500", "s8500"));
        //
        // System.out.println(js.getSimilarity("samsung s8500 wave",
        // "das neue samsung ist super aber auch das s_8500 wave"));
        // System.out.println("own: "
        // + mioCon.calcStringRelevance("das neue samsung ist super aber auch das s_8500 wave",
        // "samsung s8500 wave"));
        //
        // System.out.println(js.getSimilarity("samsung s8500 wave",
        // "das neue samsung ist super aber auch das s_8500 von wave"));
        // System.out.println("own: "
        // + mioCon.calcStringRelevance("das neue samsung ist super aber auch das s_8500 von wave",
        // "samsung s8500 wave"));
        //
        // System.exit(0);
    }

}
