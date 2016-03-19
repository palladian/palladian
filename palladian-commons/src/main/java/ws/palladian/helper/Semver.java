package ws.palladian.helper;

/**
 * Simple semantic versioning class, @see http://semver.org for more information.
 *
 * @author David Urbansky
 */
public class Semver {

    private Integer major = 0;
    private Integer minor = 0;
    private Integer patch = 0;

    public Semver(String versionString) {
        String[] split = versionString.trim().split("\\.");
        if (split.length > 0) {
            this.major = Integer.valueOf(split[0]);
        }
        if (split.length > 1) {
            this.minor = Integer.valueOf(split[1]);
        }
        if (split.length > 2) {
            this.patch = Integer.valueOf(split[2]);
        }

    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getPatch() {
        return patch;
    }

    public void setPatch(Integer patch) {
        this.patch = patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}