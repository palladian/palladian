package ws.palladian.retrieval.search.videos;

import ws.palladian.retrieval.search.Facet;

/**
 * Channel fact
 *
 * @author David Urbansky
 */
public class ChannelFacet implements Facet {
    public static String CHANNEL_FACET = "channelId";

    private String channelId;

    public ChannelFacet(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public String getIdentifier() {
        return CHANNEL_FACET;
    }

    @Override
    public String getValue() {
        return channelId;
    }
}
