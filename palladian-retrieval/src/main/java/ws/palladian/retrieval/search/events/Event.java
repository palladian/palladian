package ws.palladian.retrieval.search.events;

import java.util.Date;


/**
 * <p>
 * {@link Event}s represent search results from event searches on web search engines or APIs.
 * </p>
 * 
 * @author David Urbansky
 */
public class Event {

    private String title = "";
    private String description = "";
    private Date startDate;
    private Date endDate;
    private String recurringString = "";
    private String url;
    private String venueName;
    private String venueAddress;
    private String venueZipCode;
    private String venueCity;
    private String venueRegion;
    private String venueCountry;
    private Double venueLatitude;
    private Double venueLongitude;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * <p>
     * Get the duration of the event in milliseconds.
     * </p>
     * 
     * @return The duration of the event in milliseconds or null if unknown.
     */
    public Long getDuration() {
        if (getEndDate() == null) {
            return null;
        }
        return getEndDate().getTime() - getStartDate().getTime();
    }

    public String getRecurringString() {
        return recurringString;
    }

    public void setRecurringString(String recurringString) {
        this.recurringString = recurringString;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public String getVenueZipCode() {
        return venueZipCode;
    }

    public void setVenueZipCode(String venueZipCode) {
        this.venueZipCode = venueZipCode;
    }

    public String getVenueCity() {
        return venueCity;
    }

    public void setVenueCity(String venueCity) {
        this.venueCity = venueCity;
    }

    public String getVenueRegion() {
        return venueRegion;
    }

    public void setVenueRegion(String venueRegion) {
        this.venueRegion = venueRegion;
    }

    public String getVenueCountry() {
        return venueCountry;
    }

    public void setVenueCountry(String venueCountry) {
        this.venueCountry = venueCountry;
    }

    public Double getVenueLatitude() {
        return venueLatitude;
    }

    public void setVenueLatitude(Double venueLatitude) {
        this.venueLatitude = venueLatitude;
    }

    public Double getVenueLongitude() {
        return venueLongitude;
    }

    public void setVenueLongitude(Double venueLongitude) {
        this.venueLongitude = venueLongitude;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Event [");
        if (title != null) {
            builder.append("title=");
            builder.append(title);
            builder.append(", ");
        }
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (startDate != null) {
            builder.append("startDate=");
            builder.append(startDate);
            builder.append(", ");
        }
        if (endDate != null) {
            builder.append("endDate=");
            builder.append(endDate);
            builder.append(", ");
        }
        if (recurringString != null) {
            builder.append("recurringString=");
            builder.append(recurringString);
            builder.append(", ");
        }
        if (url != null) {
            builder.append("url=");
            builder.append(url);
            builder.append(", ");
        }
        if (venueName != null) {
            builder.append("venueName=");
            builder.append(venueName);
            builder.append(", ");
        }
        if (venueAddress != null) {
            builder.append("venueAddress=");
            builder.append(venueAddress);
            builder.append(", ");
        }
        if (venueZipCode != null) {
            builder.append("venueZipCode=");
            builder.append(venueZipCode);
            builder.append(", ");
        }
        if (venueCity != null) {
            builder.append("venueCity=");
            builder.append(venueCity);
            builder.append(", ");
        }
        if (venueRegion != null) {
            builder.append("venueRegion=");
            builder.append(venueRegion);
            builder.append(", ");
        }
        if (venueCountry != null) {
            builder.append("venueCountry=");
            builder.append(venueCountry);
            builder.append(", ");
        }
        if (venueLatitude != null) {
            builder.append("venueLatitude=");
            builder.append(venueLatitude);
            builder.append(", ");
        }
        if (venueLongitude != null) {
            builder.append("venueLongitude=");
            builder.append(venueLongitude);
        }
        builder.append("]");
        return builder.toString();
    }

}
