package ws.palladian;

/**
 * <p>
 * This class provides constants with version information and copyright about the Palladian library.
 * </p>
 *
 * @author Philipp Katz
 */
public interface PalladianVersion {

    /** Palladian version number. */
    String VERSION = "${project.version}";

    /** Palladian build. */
    String BUILD = "${timestamp}";

    /** Palladian copyright. */
    String COPYRIGHT = "Copyright 2009-2024 by David Urbansky, Philipp Katz, Klemens Muthmann";

    /** Palladian info. */
    String INFO = "Palladian version " + VERSION + " (build " + BUILD + ")\n" + COPYRIGHT + "\n";

    /** Git Branch of this build. */
    String GIT_BRANCH = "${gitBranch}";

    /** Git Commit SHA of this build. */
    String GIT_COMMIT_SHA = "${gitCommitSha}";

}
