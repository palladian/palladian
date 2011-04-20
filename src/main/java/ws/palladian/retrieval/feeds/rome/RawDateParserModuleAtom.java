package ws.palladian.retrieval.feeds.rome;

public class RawDateParserModuleAtom extends RawDateParserModule {
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    @Override
    public String getNamespaceUri() {
        return ATOM_NS;
    }
}
