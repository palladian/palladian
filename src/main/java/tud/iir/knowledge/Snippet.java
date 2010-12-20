package tud.iir.knowledge;

import java.util.Date;

import tud.iir.web.WebResult;

/**
 * The knowledge unit snippet contains the snippet text, a reference to the entity it belongs to, a reference to the
 * web result it was extracted from.
 * 
 * @author Christopher Friedrich
 * @author David Urbansky
 */
public class Snippet extends Extractable {

    private static final long serialVersionUID = 4331103475156237956L;

    private Entity entity;
    private WebResult webresult;
    private String text;

    public Snippet(Entity entity, WebResult webresult, String text) {
        this.entity = entity;
        this.webresult = webresult;
        this.text = text;
        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    public Entity getEntity() {
        return entity;
    }

    public WebResult getWebResult() {
        return webresult;
    }

    public String getText() {
        return text;
    }

    /**
     * Whether the snippet starts with a mentioning of the related entity.
     * 
     * @return True, if it starts with an entity and False otherwise.
     */
    public boolean startsWithEntity() {

        if (getText().toLowerCase().startsWith(getEntity().getName().toLowerCase())) {
            return true;
        }

        return false;
    }

    @Override
    public final String toString() {
        return "Snippet [entity=" + entity + ", text=" + text + "]";
    }
}