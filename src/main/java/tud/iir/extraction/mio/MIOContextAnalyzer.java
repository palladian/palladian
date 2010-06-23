/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import tud.iir.knowledge.Entity;

public class MIOContextAnalyzer {

    private Entity entity;
    private MIOPage mioPage;
    private double pageContextTrust = 0;
    private String[] badWords = { "banner", "tower", "titlefont", "newsletter", "cloud" };

    /**
     * Instantiates a new mIO context analyzer.
     * 
     * @param entity the entity
     * @param mioPage the mio page
     */
    public MIOContextAnalyzer(Entity entity, MIOPage mioPage) {
        this.entity = entity;
        this.mioPage = mioPage;
        pageContextTrust = calcPageContextTrust();
        System.out.println("PageContextTrust: " + pageContextTrust + " for: " + mioPage.getUrl());
    }

    /**
     * Calculate trust.
     * 
     * @param mio the mio
     */
    public void calculateTrust(MIO mio) {
        this.entity = mio.getEntity();
        double fileNameRelevance = (calcStringRelevance(mio.getFileName(), entity)) * 4;
        // System.out.println("FileNameRelevance: " + fileNameRelevance);
        double filePathRelevance = calcStringRelevance(mio.getDirectURL(), entity) * 2;
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

        double MIOTrust = pageContextTrust + fileNameRelevance + filePathRelevance + altTextRelevance;
        // System.out.println("pageContextTrust: " + pageContextTrust);

        mio.setTrust(MIOTrust);
        mio.setTrust(checkForBadWords(mio));
    }

    /**
     * Initially calculate the trust for a mioPage which influences all its containing MIOs.
     * 
     * @return the double
     */
    private double calcPageContextTrust() {

        double titleRelevance = calcStringRelevance(mioPage.getTitle(), entity);

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

        double urlRelevance = calcStringRelevance(mioPage.getUrl(), entity);

        double dpTrust = mioPage.getDedicatedPageTrust() * 2;

        double pcTrust = titleRelevance + linkNameRelevance + linkTitleRelevance + iframeParentTitleRelevance
                + urlRelevance + (dpTrust * 2);
        return pcTrust;

    }

    /**
     * Calculate the relevance of a string by checking how many terms or morphs of the entityName are included in the
     * string. A special role play words like
     * d500 or x500i. Returns: a value from 0 to 1
     * 
     * @param string the string
     * @param entity the entity
     * @return the double
     */
    private double calcStringRelevance(String string, Entity entity) {
        SearchWordMatcher swm = new SearchWordMatcher(entity.getName());
        String Elements[] = entity.getName().split("\\s");
        // calculate the number of searchWord-Matches
        double NumOfMatches = (double) swm.getNumberOfSearchWordMatches(string);
        // calculate the number of searchWord-Matches with ignoring specialWords
        // like D500x
        double NumOfMatchesWithoutSW = (double) swm.getNumberOfSearchWordMatches(string, true, entity.getName()
                .toLowerCase());
        double Diff = (double) NumOfMatches - NumOfMatchesWithoutSW;

        double result = (double) ((NumOfMatchesWithoutSW * 2) + (Diff * 3)) / (double) (Elements.length * 3);
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
        if (result > 1) {
            System.out.println("result groesser 1!");
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
            if (mio.getDirectURL().contains(badWord)) {
                double result = mio.getTrust();
                return (result / 2);
            }
        }
        return mio.getTrust();
    }

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
    }

}
