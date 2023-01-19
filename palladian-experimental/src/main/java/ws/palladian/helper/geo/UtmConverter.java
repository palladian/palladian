package ws.palladian.helper.geo;

import org.apache.commons.lang3.Validate;

import static java.lang.Math.*;

/**
 * <p>
 * Converter between latitude/longitude coordinates and UTM (Universal Transverse Mercator) coordinates and back. The
 * code of this class was mainly ported from Chuck Taylor's JavaScript-based Geographic/UTM Coordinate Converter (see
 * link below).
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://home.hiwaay.net/~taylorc/toolbox/geography/geoutm.html">Geographic/UTM Coordinate Converter</a>
 * @see <a
 * href="http://stackoverflow.com/questions/9186496/determining-utm-zone-to-convert-from-longitude-latitude">Stack
 * Overflow: Determining UTM zone (to convert) from longitude/latitude</a>
 * @see <a
 * href="http://gis.stackexchange.com/questions/13291/is-there-a-simple-way-to-compute-the-utm-zone-from-a-lat-long-point">Stack
 * Exchange: Is there a simple way to compute the UTM Zone from a lat/long point?</a>
 */
public final class UtmConverter {

    /* Ellipsoid model constants (actual values here are for WGS84) */
    private static final double sm_a = 6378137.0;
    private static final double sm_b = 6356752.314;

    private static final double UTMScaleFactor = 0.9996;

    static final String UTM_BAND_CHARS = "CDEFGHJKLMNPQRSTUVWXX";

    private UtmConverter() {
        // helper class; no instantiation
    }

    /**
     * <p>
     * Computes the ellipsoidal distance from the equator to a point at a given latitude. Reference: Hoffmann-Wellenhof,
     * B., Lichtenegger, H., and Collins, J., GPS: Theory and Practice, 3rd ed. New York: Springer-Verlag Wien, 1994.
     * </p>
     *
     * @param phi Latitude of the point, in radians.
     * @return The ellipsoidal distance of the point from the equator, in meters.
     */
    private static double arcLengthOfMeridian(double phi) {
        /* Precalculate n */
        double n = (sm_a - sm_b) / (sm_a + sm_b);
        /* Precalculate alpha */
        double alpha = ((sm_a + sm_b) / 2.0) * (1.0 + (pow(n, 2.0) / 4.0) + (pow(n, 4.0) / 64.0));
        /* Precalculate beta */
        double beta = (-3.0 * n / 2.0) + (9.0 * pow(n, 3.0) / 16.0) + (-3.0 * pow(n, 5.0) / 32.0);
        /* Precalculate gamma */
        double gamma = (15.0 * pow(n, 2.0) / 16.0) + (-15.0 * pow(n, 4.0) / 32.0);
        /* Precalculate delta */
        double delta = (-35.0 * pow(n, 3.0) / 48.0) + (105.0 * pow(n, 5.0) / 256.0);
        /* Precalculate epsilon */
        double epsilon = (315.0 * pow(n, 4.0) / 512.0);
        /* Now calculate the sum of the series and return */
        return alpha * (phi + (beta * sin(2.0 * phi)) + (gamma * sin(4.0 * phi)) + (delta * sin(6.0 * phi)) + (epsilon * sin(8.0 * phi)));
    }

    /**
     * <p>
     * Determines the central meridian for the given UTM zone.
     * </p>
     *
     * @param zone An integer value designating the UTM zone, range [1,60].
     * @return The central meridian for the given UTM zone, in radians, or zero if the UTM zone parameter is outside the
     * range [1,60]. Range of the central meridian is the radian equivalent of [-177,+177].
     */
    private static double utmCentralMeridian(int zone) {
        return toRadians(-183.0 + (zone * 6.0));
    }

    /**
     * <p>
     * Computes the footpoint latitude for use in converting transverse Mercator coordinates to ellipsoidal coordinates.
     * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J., GPS: Theory and Practice, 3rd ed. New York:
     * Springer-Verlag Wien, 1994.
     * </p>
     *
     * @param y The UTM northing coordinate, in meters.
     * @return The footpoint latitude, in radians.
     */
    private static double footpointLatitude(double y) {
        /* Precalculate n (Eq. 10.18) */
        double n = (sm_a - sm_b) / (sm_a + sm_b);
        /* Precalculate alpha_ (Eq. 10.22) */
        /* (Same as alpha in Eq. 10.17) */
        double alpha_ = ((sm_a + sm_b) / 2.0) * (1 + (pow(n, 2.0) / 4) + (pow(n, 4.0) / 64));
        /* Precalculate y_ (Eq. 10.23) */
        double y_ = y / alpha_;
        /* Precalculate beta_ (Eq. 10.22) */
        double beta_ = (3.0 * n / 2.0) + (-27.0 * pow(n, 3.0) / 32.0) + (269.0 * pow(n, 5.0) / 512.0);
        /* Precalculate gamma_ (Eq. 10.22) */
        double gamma_ = (21.0 * pow(n, 2.0) / 16.0) + (-55.0 * pow(n, 4.0) / 32.0);
        /* Precalculate delta_ (Eq. 10.22) */
        double delta_ = (151.0 * pow(n, 3.0) / 96.0) + (-417.0 * pow(n, 5.0) / 128.0);
        /* Precalculate epsilon_ (Eq. 10.22) */
        double epsilon_ = (1097.0 * pow(n, 4.0) / 512.0);
        /* Now calculate the sum of the series (Eq. 10.21) */
        return y_ + (beta_ * sin(2.0 * y_)) + (gamma_ * sin(4.0 * y_)) + (delta_ * sin(6.0 * y_)) + (epsilon_ * sin(8.0 * y_));
    }

    /**
     * <p>
     * Converts a latitude/longitude pair to x and y coordinates in the Transverse Mercator projection. Note that
     * Transverse Mercator is not the same as UTM; a scale factor is required to convert between them. Reference:
     * Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J., GPS: Theory and Practice, 3rd ed. New York:
     * Springer-Verlag Wien, 1994.
     * </p>
     *
     * @param coordinate The GeoCoordinate of the point to convert.
     * @param lambda0    Longitude of the central meridian to be used, in radians.
     * @return A 2-element array containing the x and y coordinates of the computed point.
     */
    private static double[] mapLatLonToXY(GeoCoordinate coordinate, double lambda0) {
        double phi = toRadians(coordinate.getLatitude());
        double lambda = toRadians(coordinate.getLongitude());
        /* Precalculate ep2 */
        double ep2 = (pow(sm_a, 2.0) - pow(sm_b, 2.0)) / pow(sm_b, 2.0);
        /* Precalculate nu2 */
        double nu2 = ep2 * pow(cos(phi), 2.0);
        /* Precalculate N */
        double N = pow(sm_a, 2.0) / (sm_b * sqrt(1 + nu2));
        /* Precalculate t */
        double t = tan(phi);
        double t2 = t * t;
        /* Precalculate l */
        double l = lambda - lambda0;
        /*
         * Precalculate coefficients for l**n in the equations below
         * so a normal human being can read the expressions for easting
         * and northing
         * -- l**1 and l**2 have coefficients of 1.0
         */
        double l3coef = 1.0 - t2 + nu2;
        double l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);
        double l5coef = 5.0 - 18.0 * t2 + (t2 * t2) + 14.0 * nu2 - 58.0 * t2 * nu2;
        double l6coef = 61.0 - 58.0 * t2 + (t2 * t2) + 270.0 * nu2 - 330.0 * t2 * nu2;
        double l7coef = 61.0 - 479.0 * t2 + 179.0 * (t2 * t2) - (t2 * t2 * t2);
        double l8coef = 1385.0 - 3111.0 * t2 + 543.0 * (t2 * t2) - (t2 * t2 * t2);

        double[] result = new double[2];
        /* Calculate easting (x) */
        result[0] = N * cos(phi) * l + (N / 6.0 * pow(cos(phi), 3.0) * l3coef * pow(l, 3.0)) + (N / 120.0 * pow(cos(phi), 5.0) * l5coef * pow(l, 5.0)) + (N / 5040.0 * pow(cos(phi),
                7.0) * l7coef * pow(l, 7.0));
        /* Calculate northing (y) */
        result[1] = arcLengthOfMeridian(phi) + (t / 2.0 * N * pow(cos(phi), 2.0) * pow(l, 2.0)) + (t / 24.0 * N * pow(cos(phi), 4.0) * l4coef * pow(l, 4.0)) + (t / 720.0 * N * pow(
                cos(phi), 6.0) * l6coef * pow(l, 6.0)) + (t / 40320.0 * N * pow(cos(phi), 8.0) * l8coef * pow(l, 8.0));
        return result;
    }

    /**
     * <p>
     * Converts x and y coordinates in the Transverse Mercator projection to a latitude/longitude pair. Note that
     * Transverse Mercator is not the same as UTM; a scale factor is required to convert between them. Reference:
     * Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J., GPS: Theory and Practice, 3rd ed. New York:
     * Springer-Verlag Wien, 1994.
     * </p>
     *
     * @param x       The easting of the point, in meters.
     * @param y       The northing of the point, in meters.
     * @param lambda0 Longitude of the central meridian to be used, in radians.
     * @return A 2-element array containing the latitude and longitude in radians.
     */
    private static double[] mapXYToLatLon(double x, double y, double lambda0) {
        // Remarks:
        // 1) The local variables Nf, nuf2, tf, and tf2 serve the same purpose as N, nu2, t, and t2 in MapLatLonToXY,
        // but they are computed with respect to the footpoint latitude phif.
        // 2) x1frac, x2frac, x2poly, x3poly, etc. are to enhance readability and to optimize computations.

        /* Get the value of phif, the footpoint latitude. */
        double phif = footpointLatitude(y);
        /* Precalculate ep2 */
        double ep2 = (pow(sm_a, 2.0) - pow(sm_b, 2.0)) / pow(sm_b, 2.0);
        /* Precalculate cos (phif) */
        double cf = cos(phif);
        /* Precalculate nuf2 */
        double nuf2 = ep2 * pow(cf, 2.0);
        /* Precalculate Nf and initialize Nfpow */
        double Nf = pow(sm_a, 2.0) / (sm_b * sqrt(1 + nuf2));
        double Nfpow = Nf;
        /* Precalculate tf */
        double tf = tan(phif);
        double tf2 = tf * tf;
        double tf4 = tf2 * tf2;

        /*
         * Precalculate fractional coefficients for x**n in the equations below to simplify the expressions for latitude
         * and longitude.
         */
        double x1frac = 1.0 / (Nfpow * cf);

        Nfpow *= Nf; /* now equals Nf**2) */
        double x2frac = tf / (2.0 * Nfpow);

        Nfpow *= Nf; /* now equals Nf**3) */
        double x3frac = 1.0 / (6.0 * Nfpow * cf);

        Nfpow *= Nf; /* now equals Nf**4) */
        double x4frac = tf / (24.0 * Nfpow);

        Nfpow *= Nf; /* now equals Nf**5) */
        double x5frac = 1.0 / (120.0 * Nfpow * cf);

        Nfpow *= Nf; /* now equals Nf**6) */
        double x6frac = tf / (720.0 * Nfpow);

        Nfpow *= Nf; /* now equals Nf**7) */
        double x7frac = 1.0 / (5040.0 * Nfpow * cf);

        Nfpow *= Nf; /* now equals Nf**8) */
        double x8frac = tf / (40320.0 * Nfpow);

        /*
         * Precalculate polynomial coefficients for x**n.
         * -- x**1 does not have a polynomial coefficient.
         */
        double x2poly = -1.0 - nuf2;
        double x3poly = -1.0 - 2 * tf2 - nuf2;
        double x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * (nuf2 * nuf2) - 9.0 * tf2 * (nuf2 * nuf2);
        double x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
        double x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2 + 162.0 * tf2 * nuf2;
        double x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
        double x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);

        double[] phiLambda = new double[2];

        /* Calculate latitude */
        phiLambda[0] = phif + x2frac * x2poly * (x * x) + x4frac * x4poly * pow(x, 4.0) + x6frac * x6poly * pow(x, 6.0) + x8frac * x8poly * pow(x, 8.0);

        /* Calculate longitude */
        phiLambda[1] = lambda0 + x1frac * x + x3frac * x3poly * pow(x, 3.0) + x5frac * x5poly * pow(x, 5.0) + x7frac * x7poly * pow(x, 7.0);

        return phiLambda;
    }

    /**
     * <p>
     * Converts a latitude/longitude pair to x and y coordinates in the Universal Transverse Mercator projection.
     * </p>
     *
     * @param coordinate The coordinate of the point to convert, not <code>null</code>.
     * @return A UTM coordinate of the given point.
     */
    public static UtmCoordinate toUtm(GeoCoordinate coordinate) {
        int zone = utmZone(coordinate);
        char band = utmBand(coordinate.getLatitude());
        double[] xy = mapLatLonToXY(coordinate, utmCentralMeridian(zone));
        /* Adjust easting and northing for UTM system. */
        xy[0] = xy[0] * UTMScaleFactor + 500000.0;
        xy[1] *= UTMScaleFactor;
        if (xy[1] < 0.0) {
            xy[1] += 10000000.0;
        }
        return new UtmCoordinate(xy[0], xy[1], zone, band);
    }

    /**
     * <p>
     * Converts x and y coordinates in the Universal Transverse Mercator projection to a latitude/longitude pair.
     * </p>
     *
     * @param easting   The easting of the point, in meters.
     * @param northing  The northing of the point, in meters.
     * @param zone      The UTM zone in which the point lies.
     * @param southHemi <code>true</code> if the point is in the southern hemisphere; <code>false</code> otherwise.
     * @return A latitude/longitude coordinate for the given point.
     */
    public static GeoCoordinate toLatLon(double easting, double northing, int zone, boolean southHemi) {
        easting -= 500000.0;
        easting /= UTMScaleFactor;
        /* If in southern hemisphere, adjust y accordingly. */
        if (southHemi) {
            northing -= 10000000.0;
        }
        northing /= UTMScaleFactor;
        double cmeridian = utmCentralMeridian(zone);
        double[] latLng = mapXYToLatLon(easting, northing, cmeridian);
        double lat = toDegrees(latLng[0]);
        double lng = toDegrees(latLng[1]);
        return GeoCoordinate.from(lat, lng);
    }

    /**
     * <p>
     * Get the UTM zone for the given latitude/longitude coordinate. This method handles the <a
     * href="http://www.dmap.co.uk/utmworld.htm">exceptions</a> for Norway and Svalbard correctly.
     * </p>
     *
     * @param coordinate The coordinate for which to get the UTM zone, not <code>null</code>.
     * @return The UTM zone [1,60]
     */
    public static int utmZone(GeoCoordinate coordinate) {
        // code taken from: http://www.igorexchange.com/node/927
        Validate.notNull(coordinate, "coordinate must not be null");
        double lat = coordinate.getLatitude();
        double lng = coordinate.getLongitude();
        int zone = (int) floor((lng + 180.0) / 6) + 1;

        // Norway
        if (lat >= 56.0 && lat < 64.0 && lng >= 3.0 && lng < 12.0) {
            zone = 32;
        }
        // Special zones for Svalbard
        if (lat >= 72.0 && lat < 84.0) {
            if (lng >= 0.0 && lng < 9.0) {
                zone = 31;
            } else if (lng >= 9.0 && lng < 21.0) {
                zone = 33;
            } else if (lng >= 21.0 && lng < 33.0) {
                zone = 35;
            } else if (lng >= 33.0 && lng < 42.0) {
                zone = 37;
            }
        }
        return zone;
    }

    /**
     * <p>
     * Get the UTM band for the given latitude.
     * </p>
     *
     * @param lat The latitude for which to get the UTM letter.
     * @return The UTM latitude band as char [CDEFGHJKLMNPQRSTUVWXX], or Z in case the latitude was not valid.
     */
    public static char utmBand(double lat) {
        // code taken from: http://www.igorexchange.com/node/927
        // XXX I guess, there are exceptions for artic/antarctic region?
        return (-80 <= lat && lat <= 84) ? UTM_BAND_CHARS.charAt((int) (lat + 80) / 8) : 'Z';
    }

    /**
     * <p>
     * Converts a given grid zone, such as <code>10S</code> to an approximate latitude/longitude {@link GeoCoordinate}.
     * We take the center of the grid zone as resulting coordinate.
     * </p>
     *
     * @param gridZone The grid zone to convert, in a format like <code>10S</code> (zone followed directly by band). Not
     *                 <code>null</code> or empty.
     * @return A {@link GeoCoordinate} in the center of the given grid zone.
     * @throws IllegalArgumentException In case the given value was in an invalid format or could not be parsed.
     */
    public static GeoCoordinate gridZoneToLatLon(String gridZone) {
        Validate.notEmpty(gridZone, "gridZone must not be empty");

        // asked about this here:
        // http://gis.stackexchange.com/questions/84218/get-approximate-lat-long-coordinate-for-utm-grid-zone

        try {
            int zone = Integer.parseInt(gridZone.replaceAll("[A-Z]+", ""));
            if (zone < 1 || zone > 60) {
                throw new IllegalArgumentException("Invalid UTM zone: " + zone + ".");
            }
            char band = gridZone.replaceAll("[0-9]+", "").charAt(0);
            int bandIdx = UtmConverter.UTM_BAND_CHARS.indexOf(band);
            if (bandIdx == -1) {
                throw new IllegalArgumentException("Unknown UTM band: '" + band + "'.");
            }
            double lat = bandIdx * 8 - 76;
            double lng = ((zone - 1) * 6) - 177;

            // for Norway, we need to modify the longitude according to the special rules
            if (band == 'V') {
                if (zone == 31) {
                    lng = 1.5;
                } else if (zone == 32) {
                    lng = 7.5;
                }
            }
            return GeoCoordinate.from(lat, lng);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + gridZone + "' cannot be parsed.");
        }
    }

}
