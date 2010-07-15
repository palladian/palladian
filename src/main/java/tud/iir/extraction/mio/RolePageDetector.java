/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tud.iir.web.Crawler;

/**
 * Detects RolePages
 * 
 * @author Martin Werner
 */
public class RolePageDetector {

    /**
     * Detect role pages.
     *
     * @param sortedMIOs the sorted MIOs
     */
    public void detectRolePages(final Set<MIO> sortedMIOs) {
        int mioAmount = sortedMIOs.size();
        int counter = 0;
        Set<String> mioDomains = new HashSet<String>();

        // only get relevant domains, but normally less then 50percent of the results are relevant
        for (MIO mio : sortedMIOs) {
            String mioURL = mio.getDirectURL();
            String domain = Crawler.getDomain(mioURL);
            mioDomains.add(domain);
            counter++;
            if (counter >= mioAmount / 2) {
                break;
            }
        }

        analyzeForRolePages(mioDomains);

    }

    /**
     * Analyze for role pages.
     *
     * @param mioDomains the mio domains
     * @return the list
     */
    private void analyzeForRolePages(final Set<String> mioDomains) {

        List<RolePage> dbRolePages = loadRolePagesFromDB();

        // initially set the first mioDomains as dbRolePages
        if (dbRolePages.isEmpty()) {
            for (String mioDomain : mioDomains) {
                RolePage rolePage = new RolePage(mioDomain, 1);
                dbRolePages.add(rolePage);
            }
        } else {
            for (RolePage dbRolePage : dbRolePages) {
                String dbRolePageHostname = dbRolePage.getHostname();
                for (String mioDomain : mioDomains) {
                    if (mioDomain.equals(dbRolePageHostname)) {
                        dbRolePage.incrementCount();
                        mioDomains.remove(mioDomain);

                    }
                }
            }

            // if the domain wasn't already in the dbRolePageList then add it now
            if (!mioDomains.isEmpty()) {
                for (String mioDomain : mioDomains) {
                    RolePage rolePage = new RolePage(mioDomain);
                    dbRolePages.add(rolePage);
                }

            }

        }

        // update Database
        saveRolePagesToDB(dbRolePages);

    }

    /**
     * Load role pages from db.
     *
     * @return the list
     */
    private List<RolePage> loadRolePagesFromDB() {
        // List<RolePage> rolePages = new ArrayList<RolePage>();
        RolePageDatabase mioDB = new RolePageDatabase();
        List<RolePage> rolePages = mioDB.loadRolePages();

        return rolePages;

    }

    /**
     * Save role pages to db.
     *
     * @param rolePages the role pages
     */
    private void saveRolePagesToDB(final List<RolePage> rolePages) {
        RolePageDatabase mioDB = new RolePageDatabase();
        for (RolePage rolePage : rolePages) {
            if (rolePage.getId() == 0) {
                mioDB.insertRolePage(rolePage);
            } else {
                mioDB.addRolePage(rolePage);
            }
        }

    }
}
