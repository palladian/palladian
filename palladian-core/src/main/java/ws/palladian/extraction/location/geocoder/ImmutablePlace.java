package ws.palladian.extraction.location.geocoder;

import java.util.ArrayList;

import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.geo.GeoCoordinate;

public final class ImmutablePlace implements Place {
    public static final class Builder implements Factory<ImmutablePlace> {
        private String houseNumber;
        private String street;
        private String postalcode;
        private String country;
        private String region;
        private String county;
        private String locality;
        private String neighbourhood;
        private String label;
        private String state;
        private String city;
        private String cityDistrict;
        private String continent;
        private String citySubdistrict;
        private String municipality;
        private String politicalUnion;
        private String stateDistrict;
        private String province;
        private GeoCoordinate coordinate;

        public Builder setHouseNumber(String houseNumber) {
            this.houseNumber = houseNumber;
            return this;
        }

        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        public Builder setPostalcode(String postalcode) {
            this.postalcode = postalcode;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setCounty(String county) {
            this.county = county;
            return this;
        }

        @Deprecated
        public Builder setLocality(String locality) {
            this.locality = locality;
            return this;
        }

        public Builder setNeighbourhood(String neighbourhood) {
            this.neighbourhood = neighbourhood;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Builder setCityDistrict(String cityDistrict) {
            this.cityDistrict = cityDistrict;
            return this;
        }

        public Builder setContinent(String continent) {
            this.continent = continent;
            return this;
        }

        public Builder setCitySubdistrict(String citySubdistrict) {
            this.citySubdistrict = citySubdistrict;
            return this;
        }

        public Builder setMunicipality(String municipality) {
            this.municipality = municipality;
            return this;
        }

        public Builder setPoliticalUnion(String politicalUnion) {
            this.politicalUnion = politicalUnion;
            return this;
        }

        public Builder setStateDistrict(String stateDistrict) {
            this.stateDistrict = stateDistrict;
            return this;
        }

        public Builder setProvince(String province) {
            this.province = province;
            return this;
        }

        public Builder setCooordinate(GeoCoordinate coordinate) {
            this.coordinate = coordinate;
            return this;
        }

        @Override
        public ImmutablePlace create() {
            return new ImmutablePlace(this);
        }

    }

    private final String houseNumber;
    private final String street;
    private final String postalcode;
    private final String country;
    private final String region;
    private final String county;
    private final String locality;
    private final String neighbourhood;
    private final String label;
    private final String state;
    private final String city;
    private final String cityDistrict;
    private final String continent;
    private final String citySubdistrict;
    private final String municipality;
    private final String politicalUnion;
    private final String stateDistrict;
    private final String province;
    private final GeoCoordinate coordinate;

    private ImmutablePlace(Builder builder) {
        houseNumber = builder.houseNumber;
        street = builder.street;
        postalcode = builder.postalcode;
        country = builder.country;
        region = builder.region;
        county = builder.county;
        locality = builder.locality;
        neighbourhood = builder.neighbourhood;
        label = builder.label;
        state = builder.state;
        city = builder.city;
        cityDistrict = builder.cityDistrict;
        continent = builder.continent;
        citySubdistrict = builder.citySubdistrict;
        municipality = builder.municipality;
        politicalUnion = builder.politicalUnion;
        stateDistrict = builder.stateDistrict;
        province = builder.province;
        coordinate = builder.coordinate;
    }

    @Override
    public String getHouseNumber() {
        return houseNumber;
    }

    @Override
    public String getStreet() {
        return street;
    }

    @Override
    public String getPostalcode() {
        return postalcode;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getCounty() {
        return county;
    }

    @Override
    public String getLocality() {
        return locality;
    }

    @Override
    public String getNeighbourhood() {
        return neighbourhood;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getCityDistrict() {
        return cityDistrict;
    }

    @Override
    public String getContinent() {
        return continent;
    }

    @Override
    public String getCitySubdistrict() {
        return citySubdistrict;
    }

    @Override
    public String getMunicipality() {
        return municipality;
    }

    @Override
    public String getPoliticalUnion() {
        return politicalUnion;
    }

    @Override
    public String getStateDistrict() {
        return stateDistrict;
    }

    @Override
    public String getProvince() {
        return province;
    }

    @Override
    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        var stringParts = new ArrayList<String>();
        if (houseNumber != null) {
            stringParts.add(String.format("houseNumber=%s", houseNumber));
        }
        if (street != null) {
            stringParts.add(String.format("street=%s", street));
        }
        if (postalcode != null) {
            stringParts.add(String.format("postalcode=%s", postalcode));
        }
        if (country != null) {
            stringParts.add(String.format("country=%s", country));
        }
        if (region != null) {
            stringParts.add(String.format("region=%s", region));
        }
        if (county != null) {
            stringParts.add(String.format("county=%s", county));
        }
        if (locality != null) {
            stringParts.add(String.format("locality=%s", locality));
        }
        if (neighbourhood != null) {
            stringParts.add(String.format("neighbourhood=%s", neighbourhood));
        }
        if (label != null) {
            stringParts.add(String.format("label=%s", label));
        }
        if (state != null) {
            stringParts.add(String.format("state=%s", state));
        }
        if (city != null) {
            stringParts.add(String.format("city=%s", city));
        }
        if (cityDistrict != null) {
            stringParts.add(String.format("cityDistrict=%s", cityDistrict));
        }
        if (continent != null) {
            stringParts.add(String.format("continent=%s", continent));
        }
        if (citySubdistrict != null) {
            stringParts.add(String.format("citySubdistrict=%s", citySubdistrict));
        }
        if (municipality != null) {
            stringParts.add(String.format("municipality=%s", municipality));
        }
        if (politicalUnion != null) {
            stringParts.add(String.format("politicalUnion=%s", politicalUnion));
        }
        if (stateDistrict != null) {
            stringParts.add(String.format("stateDistrict=%s", stateDistrict));
        }
        if (province != null) {
            stringParts.add(String.format("province=%s", province));
        }
        if (coordinate != null) {
            stringParts.add(String.format("coordinate=%s", coordinate));
        }
        return String.format("Place [%s]", String.join(", ", stringParts));
    }

}
