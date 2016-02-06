package ws.palladian.retrieval.ip;

import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.geo.GeoCoordinate;

public final class IpLookupResult {
	public static final class Builder implements Factory<IpLookupResult> {

		private String ip;
		private String countryCode;
		private String countryName;
		private String regionCode;
		private String regionName;
		private String city;
		private String zipCode;
		private String timeZone;
		private GeoCoordinate coordinate;
		private String metroCode;

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setCountryCode(String countryCode) {
			this.countryCode = countryCode;
			return this;
		}

		public Builder setCountryName(String countryName) {
			this.countryName = countryName;
			return this;
		}

		public Builder setRegionCode(String regionCode) {
			this.regionCode = regionCode;
			return this;
		}

		public Builder setRegionName(String regionName) {
			this.regionName = regionName;
			return this;
		}

		public Builder setCity(String city) {
			this.city = city;
			return this;
		}

		public Builder setZipCode(String zipCode) {
			this.zipCode = zipCode;
			return this;
		}

		public Builder setTimeZone(String timeZone) {
			this.timeZone = timeZone;
			return this;
		}

		public Builder setCoordinate(GeoCoordinate coordinate) {
			this.coordinate = coordinate;
			return this;
		}

		public Builder setMetroCode(String metroCode) {
			this.metroCode = metroCode;
			return this;
		}

		@Override
		public IpLookupResult create() {
			return new IpLookupResult(this);
		}
	}

	private final String ip;
	private final String countryCode;
	private final String countryName;
	private final String regionCode;
	private final String regionName;
	private final String city;
	private final String zipCode;
	private final String timeZone;
	private final GeoCoordinate coordinate;
	private final String metroCode;

	public IpLookupResult(Builder builder) {
		this.ip = builder.ip;
		this.countryCode = builder.countryCode;
		this.countryName = builder.countryName;
		this.regionCode = builder.regionCode;
		this.regionName = builder.regionName;
		this.city = builder.city;
		this.zipCode = builder.zipCode;
		this.timeZone = builder.timeZone;
		this.coordinate = builder.coordinate;
		this.metroCode = builder.metroCode;
	}

	public String getIp() {
		return ip;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getCountryName() {
		return countryName;
	}

	public String getRegionCode() {
		return regionCode;
	}

	public String getRegionName() {
		return regionName;
	}

	public String getCity() {
		return city;
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public GeoCoordinate getCoordinate() {
		return coordinate;
	}

	public String getMetroCode() {
		return metroCode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IpLookupResult [ip=");
		builder.append(ip);
		builder.append(", countryCode=");
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
		builder.append(", timeZone=");
		builder.append(timeZone);
		builder.append(", coordinate=");
		builder.append(coordinate);
		builder.append(", metroCode=");
		builder.append(metroCode);
		builder.append("]");
		return builder.toString();
	}
	
	

}
