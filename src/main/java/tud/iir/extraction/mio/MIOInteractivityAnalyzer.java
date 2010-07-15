package tud.iir.extraction.mio;

public class MIOInteractivityAnalyzer {

    /**
     * Sets the interactivity grade.
     *
     * @param mio the mio
     * @param mioPage the mio page
     */
    public void setInteractivityGrade(final MIO mio, final MIOPage mioPage) {
        if (mio.getTextContentLength() > 0) {
            mio.setInteractivityGrade("strong");
        } else {
            mio.setInteractivityGrade(calcInteractivityGrade(mio, mioPage));
        }

    }

    /**
     * Calculates interactivity grade.
     *
     * @param mio the mio
     * @param mioPage the mio page
     * @return the string
     */
    private String calcInteractivityGrade(final MIO mio, final MIOPage mioPage) {
        String returnValue="unclear";

        final double fileNameiGrade = calcSingleValue(mio.getFileName());
        final double pageTitleiGrade = calcSingleValue(mioPage.getTitle());
        double headlineiGrade = (double) 0;
        if (mio.getInfos().containsKey("previousHeadlines")) {
           final String headline = (String) mio.getInfos().get("previousHeadlines").get(0);
            if (headline.length() > 1) {
                headlineiGrade = calcSingleValue(headline);
            }

        }
        double surroundingTextiGrade = (double) 0;
        if (mio.getInfos().containsKey("surroundingText")) {
            final String surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
            if (surroundingText.length() > 1) {
                surroundingTextiGrade = calcSingleValue(surroundingText);
            }
        }
        final double result = fileNameiGrade + pageTitleiGrade + headlineiGrade + surroundingTextiGrade;
        if (result > 0) {
            returnValue= "strong";
        }
        if (result < 0) {
            returnValue= "weak";
        }

        return returnValue;
    }

    // 0 means unclear, 1 is strong, -1 is weak
    /**
     * Calculates single value.
     *
     * @param checkString the check string
     * @return the double
     */
    private double calcSingleValue(final String checkString) {
        double returnValue=0;

        if (containsStrongInteractivityIndicator(checkString)) {
            if (!containsWeakInteractivityIndicator(checkString)) {
                returnValue= 1;
            }
        } else {
            if (containsWeakInteractivityIndicator(checkString)) {
                returnValue= -1;
            }
        }
        return returnValue;
    }

    /**
     * Contains strong interactivity indicator.
     *
     * @param checkString the check string
     * @return true, if successful
     */
    private boolean containsStrongInteractivityIndicator(final String checkString) {
        return containsInteractivityIndicator(checkString, true);
    }

    /**
     * Contains weak interactivity indicator.
     *
     * @param checkString the check string
     * @return true, if successful
     */
    private boolean containsWeakInteractivityIndicator(final String checkString) {
        return containsInteractivityIndicator(checkString, false);
    }

    /**
     * Contains interactivity indicator.
     *
     * @param checkString the check string
     * @param checkStrong the check strong
     * @return true, if successful
     */
    private boolean containsInteractivityIndicator(final String checkString, final boolean checkStrong) {
        boolean returnValue= false;
        String[] iIndicator = { "unboxing", "video tour", "preview", "youtube" };
        if (checkStrong) {
            final String[] isStrongIndicator = { "click", "try out", "360 view" };
            iIndicator = isStrongIndicator;
        }

        for (String indicator : iIndicator) {
            if (checkString.contains(indicator)) {
                returnValue= true;
            }
        }
        return returnValue;

    }

}
