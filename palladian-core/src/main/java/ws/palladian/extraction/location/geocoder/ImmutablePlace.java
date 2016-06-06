package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.functional.Factory;

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
	public String toString() {
		StringBuilder toStringBuilder = new StringBuilder();
		toStringBuilder.append("ImmutablePlace [houseNumber=");
		toStringBuilder.append(houseNumber);
		toStringBuilder.append(", street=");
		toStringBuilder.append(street);
		toStringBuilder.append(", postalcode=");
		toStringBuilder.append(postalcode);
		toStringBuilder.append(", country=");
		toStringBuilder.append(country);
		toStringBuilder.append(", region=");
		toStringBuilder.append(region);
		toStringBuilder.append(", county=");
		toStringBuilder.append(county);
		toStringBuilder.append(", locality=");
		toStringBuilder.append(locality);
		toStringBuilder.append(", neighbourhood=");
		toStringBuilder.append(neighbourhood);
		toStringBuilder.append(", label=");
		toStringBuilder.append(label);
		toStringBuilder.append("]");
		return toStringBuilder.toString();
	}

}
