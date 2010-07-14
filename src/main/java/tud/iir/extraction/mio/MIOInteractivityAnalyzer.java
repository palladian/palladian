package tud.iir.extraction.mio;

public class MIOInteractivityAnalyzer {

    public void setInteractivityGrade(MIO mio, MIOPage mioPage) {
        if (mio.getTextContentLength() > 0) {
            mio.setInteractivityGrade("strong");
        } else {
            mio.setInteractivityGrade(calcInteractivityGrade(mio, mioPage));
        }

    }

    private String calcInteractivityGrade(final MIO mio, final MIOPage mioPage) {

        double fileNameiGrade = calcSingleValue(mio.getFileName());
        double pageTitleiGrade = calcSingleValue(mioPage.getTitle());
        double headlineiGrade = (double) 0;
        if (mio.getInfos().containsKey("previousHeadlines")) {
            String headline = (String) mio.getInfos().get("previousHeadlines").get(0);
            if (headline.length() > 1) {
                headlineiGrade = calcSingleValue(headline);
            }

        }
        double surroundingTextiGrade = (double) 0;
        if (mio.getInfos().containsKey("surroundingText")) {
            String surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
            if (surroundingText.length() > 1) {
                surroundingTextiGrade = calcSingleValue(surroundingText);
            }
        }
        double result = fileNameiGrade + pageTitleiGrade + headlineiGrade + surroundingTextiGrade;
        if (result > 0) {
            return "strong";
        }
        if (result < 0) {
            return "weak";
        }

        return "unclear";
    }

    // 0 means unclear, 1 is strong, -1 is weak
    private double calcSingleValue(String checkString) {

        if (containsStrongInteractivityIndicator(checkString)) {
            if (containsWeakInteractivityIndicator(checkString)) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (containsWeakInteractivityIndicator(checkString)) {
                return -1;
            }
        }
        return 0;
    }

    private boolean containsStrongInteractivityIndicator(String checkString) {
        return containsInteractivityIndicator(checkString, true);
    }

    private boolean containsWeakInteractivityIndicator(String checkString) {
        return containsInteractivityIndicator(checkString, false);
    }

    private boolean containsInteractivityIndicator(String checkString, boolean checkStrong) {
        String[] iIndicator = { "unboxing", "video tour", "preview", "youtube" };
        if (checkStrong) {
            String[] isStrongIndicator = { "click", "try out", "360 view" };
            iIndicator = isStrongIndicator;
        }

        for (String indicator : iIndicator) {
            if (checkString.contains(indicator)) {
                return true;
            }
        }
        return false;

    }

}
