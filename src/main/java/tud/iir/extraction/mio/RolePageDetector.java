/**
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

/**
 * Detects RolePages
 * 
 * @author Martin Werner
 */
public class RolePageDetector {

    /** The concept. */
    private Concept concept;

    /** The entity. */
    // private Entity entity;

    /**
     * Instantiates a new role page detector.
     * 
     * @param entity the entity
     */
    RolePageDetector(Entity entity) {
        // this.entity = entity;
        this.concept = entity.getConcept();
    }

    /**
     * Detect role pages.
     * 
     * @param sortedMIOs the sorted MIOs
     */
    public void detectRolePages(final Set<MIO> sortedMIOs) {
        double mioAmount = sortedMIOs.size();
        // System.out.println("number Of sortedMIOs: "+ mioAmount);
        double counter = 0;
        Set<String> mioDomains = new HashSet<String>();

        // only get relevant domains, but normally less then 50percent of the results are relevant
        for (MIO mio : sortedMIOs) {
            String mioURL = mio.getDirectURL();
            String domain = Crawler.getDomain(mioURL);
            // System.out.println("Domain von " + mioURL +" " + domain);
            mioDomains.add(domain);
            counter++;
            // System.out.println("Counter: " +counter);
            if (counter >= (mioAmount / 2)) {
                break;
            }
        }
        // System.out.println("number of rolePages for Analyzing: " + mioDomains.size());
        analyzeForRolePages(mioDomains);

    }

    /**
     * Analyze a set of mioDomains for rolePages.
     * 
     * @param mioDomains the mioDomains
     * 
     */
    private void analyzeForRolePages(final Set<String> mioDomains) {

        List<RolePage> dbRolePages = loadRolePagesFromDB();
        // System.out.println(dbRolePages.size() + " RolePages aus der Datenbank geladen, fuer Concept " +
        // concept.getName());

        // initially set the first mioDomains as dbRolePages
        if (dbRolePages.isEmpty()) {
            for (String mioDomain : mioDomains) {
                RolePage rolePage = new RolePage(mioDomain, 1, concept.getID());
                dbRolePages.add(rolePage);
            }
        } else {
            for (RolePage dbRolePage : dbRolePages) {
                String dbRolePageHostname = dbRolePage.getHostname();
                // System.out.println("dbRolePageHostname: " + dbRolePageHostname);
                List<String> removingList = new ArrayList<String>();

                for (String mioDomain : mioDomains) {
                    if (mioDomain.equals(dbRolePageHostname)) {
                        dbRolePage.incrementCount();
                        removingList.add(mioDomain);
                        // mioDomains.remove(mioDomain);
                    }
                }
                for (String removalDomain : removingList) {
                    mioDomains.remove(removalDomain);
                }
            }

            // if the domain wasn't already in the dbRolePageList then add it now
            if (!mioDomains.isEmpty()) {
                for (String mioDomain : mioDomains) {
                    RolePage rolePage = new RolePage(mioDomain, concept.getID());
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

        RolePageDatabase mioDB = new RolePageDatabase();
        List<RolePage> rolePages = mioDB.loadAllRolePagesForConcept(concept);

        return rolePages;

    }

    /**
     * Save role pages to database.
     * 
     * @param rolePages the role pages
     */
    private void saveRolePagesToDB(final List<RolePage> rolePages) {
        RolePageDatabase mioDB = new RolePageDatabase();
        // ArrayList<Integer> rolePageIDs = mioDB.loadUsedRolePageIDsForEntity(entity);
        for (RolePage rolePage : rolePages) {
            if (rolePage.getID() == 0) {
                mioDB.insertRolePage(rolePage);
            } else {
                // //avoid to update a rolePage count, when this rolePage already was used for this entity
                // if(!rolePageIDs.contains(rolePage.getID())){
                mioDB.updateRolePage(rolePage);
                // }

            }
        }

    }
}
