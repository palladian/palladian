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
    final String VERSION = "${project.version}";

    /** Palladian build. */
    final String BUILD = "${timestamp}";

    /** Palladian copyright. */
    final String COPYRIGHT = "Copyright 2009-2014 by David Urbansky, Philipp Katz, Klemens Muthmann";

    /** Palladian info. */
    final String INFO = "Palladian version " + VERSION + " (build " + BUILD + ")\n" + COPYRIGHT + "\n";

}
