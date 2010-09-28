/**
 * Detects RolePages
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

public class RolePageDetector {

    /** The concept. */
    private final transient Concept concept;

    /** The role page trust limit. */
    private final transient double rolePageTrustLimit;

    /**
     * Instantiates a new role page detector.
     * 
     * @param entity the entity
     */
    RolePageDetector(final Entity entity) {
        this.concept = entity.getConcept();
        rolePageTrustLimit = InCoFiConfiguration.getInstance().rolePageTrustLimit;
    }

    /**
     * Detect role pages.
     * 
     * @param sortedMIOs the sorted MIOs
     */
    public void detectRolePages(final Set<MIO> sortedMIOs) {

        final Set<String> mioDomains = new HashSet<String>();

        // only get relevant domains, but normally less then 50percent of the results are relevant
        for (MIO mio : sortedMIOs) {
            if (mio.getTrust() >= rolePageTrustLimit) {

                final String mioURL = mio.getDirectURL();
                final String domain = Crawler.getDomain(mioURL);
                mioDomains.add(domain);
            }
        }
        analyzeForRolePages(mioDomains);
    }

    /**
     * Analyze a set of mioDomains for rolePages.
     * 
     * @param mioDomains the mioDomains
     * 
     */
    private void analyzeForRolePages(final Set<String> mioDomains) {

        final List<RolePage> dbRolePages = loadRolePagesFromDB();

        // initially set the first mioDomains as dbRolePages
        if (dbRolePages.isEmpty()) {
            for (String mioDomain : mioDomains) {
                final RolePage rolePage = new RolePage(mioDomain, 1, concept.getID());
                dbRolePages.add(rolePage);
            }
        } else {
            final List<String> removingList = new ArrayList<String>();
            for (RolePage dbRolePage : dbRolePages) {
                final String dbRolePageHostname = dbRolePage.getHostname();
                removingList.clear();

                for (String mioDomain : mioDomains) {
                    if (mioDomain.equals(dbRolePageHostname)) {
                        dbRolePage.incrementCount();
                        removingList.add(mioDomain);
                    }
                }
                for (String removalDomain : removingList) {
                    mioDomains.remove(removalDomain);
                }
            }

            // if the domain wasn't already in the dbRolePageList then add it now
            if (!mioDomains.isEmpty()) {
                for (String mioDomain : mioDomains) {
                    final RolePage rolePage = new RolePage(mioDomain, concept.getID());
                    dbRolePages.add(rolePage);
                }
            }
        }
        // update Database
        saveRolePagesToDB(dbRolePages);
    }

    /**
     * Load existing rolePages from database.
     * 
     * @return the list
     */
    private List<RolePage> loadRolePagesFromDB() {

        final RolePageDatabase mioDB = new RolePageDatabase();
        final List<RolePage> rolePages = mioDB.loadAllRolePagesForConcept(concept);

        return rolePages;
    }

    /**
     * Save role pages to database.
     * 
     * @param rolePages the role pages
     */
    private void saveRolePagesToDB(final List<RolePage> rolePages) {
        final RolePageDatabase mioDB = new RolePageDatabase();
        // ArrayList<Integer> rolePageIDs = mioDB.loadUsedRolePageIDsForEntity(entity);
        for (RolePage rolePage : rolePages) {
            if (rolePage.getID() == 0) {
                mioDB.insertRolePage(rolePage);
            } else {
                // avoid to update a rolePage count, when this rolePage already was used for this entity
                mioDB.updateRolePage(rolePage);
            }
        }
    }
}
