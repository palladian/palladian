package tud.iir.persistence;

public class Location {

    private String countryCode;
    private String countryName;
    private int regionCode;
    private String regionName;
    private String city;
    private int zipCode;
    private double latitude;
    private double longitude;
    private int metrocode;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public int getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(int regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getZipCode() {
        return zipCode;
    }

    public void setZipCode(int zipCode) {
        this.zipCode = zipCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getMetrocode() {
        return metrocode;
    }

    public void setMetrocode(int metrocode) {
        this.metrocode = metrocode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Location [countryCode=");
        builder.append(countryCode);
        builder.append(", countryName=");
        builder.append(countryName);
        builder.append(", regionCode=");
        builder.append(regionCode);
        builder.append(", regionName=");
        builder.append(regionName);
        builder.append(", city=");
        builder.append(city);
        builder.append(", zipCode=");
        builder.append(zipCode);
        builder.append(", latitude=");
        builder.append(latitude);
        builder.append(", longitude=");
        builder.append(longitude);
        builder.append(", metrocode=");
        builder.append(metrocode);
        builder.append("]");
        return builder.toString();
    }
}
