/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects RolePages
 * 
 * @author Martin Werner
 */
public class RolePageDetector {

    private List<RolePage> rolePages;
    private List<RolePage> rolePageCandidates;
    private int relevanceValue = 5;

    /**
     * Instantiates a new role page detector.
     *
     * @param relevanceValue the relevance value
     */
    public RolePageDetector(int relevanceValue) {
        if (relevanceValue > 0) {
            this.relevanceValue = relevanceValue;
        }

        rolePageCandidates = new ArrayList<RolePage>();
    }

    /**
     * Analyze for role pages.
     *
     * @param MIOPages the mIO pages
     * @return the list
     */
    public List<RolePage> analyzeForRolePages(List<MIOPage> MIOPages) {

        boolean pageFound;

        // add new RolePage or recalculate Count
        for (MIOPage mioPage : MIOPages) {
            pageFound = false;
            if (!rolePageCandidates.isEmpty()) {
                for (RolePage rolePageCandidate : rolePageCandidates) {
                    if (mioPage.getHostname().equals(rolePageCandidate.getHostname())) {
                        rolePageCandidate.calcCount();
                        // System.out.println(rolePageCandidate.getHostname() + " recalculate count to: " +
                        // rolePageCandidate.getCount());
                        pageFound = true;
                        break;
                    }

                }
            }
            if (!pageFound) {
                RolePage rolePageCand = new RolePage(mioPage.getHostname(), 1);
                rolePageCandidates.add(rolePageCand);
                // System.out.println(rolePageCand.getHostname() + " added " + rolePageCand.getCount());
            }
        }

        findRelevanceRolePages(rolePageCandidates);
        return rolePages;
    }

    // only return rolePages with counts that match the minimum of the relevanceValue
    /**
     * Find relevance role pages.
     *
     * @param rolePageCandidates the role page candidates
     */
    private void findRelevanceRolePages(List<RolePage> rolePageCandidates) {
        rolePages = new ArrayList<RolePage>();
        for (RolePage rolePageCand : rolePageCandidates) {
            if (rolePageCand.getCount() >= relevanceValue) {
                rolePages.add(rolePageCand);
            }
        }
    }
}
