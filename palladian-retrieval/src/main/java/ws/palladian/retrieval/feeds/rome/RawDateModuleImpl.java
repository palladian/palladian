package ws.palladian.retrieval.feeds.rome;

import com.rometools.rome.feed.CopyFrom;
import com.rometools.rome.feed.module.ModuleImpl;

public class RawDateModuleImpl extends ModuleImpl implements RawDateModule {

    private static final long serialVersionUID = 1L;

    public RawDateModuleImpl() {
        super(RawDateModule.class, RawDateModule.URI);
    }

    private String rawDate;

    @Override
    public Class<? extends CopyFrom> getInterface() {
        return RawDateModule.class;
    }

    @Override
    public void copyFrom(CopyFrom obj) {
        RawDateModule module = (RawDateModule) obj;
        setRawDate(module.getRawDate());
    }

    @Override
    public String getRawDate() {
        return rawDate;
    }

    @Override
    public void setRawDate(String rawDate) {
        this.rawDate = rawDate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RawDateModuleImpl [rawDate=");
        builder.append(rawDate);
        builder.append("]");
        return builder.toString();
    }

}
