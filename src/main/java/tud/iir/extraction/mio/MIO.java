package tud.iir.extraction.mio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.knowledge.Entity;

/**
 * An interactive multimedia object.
 * 
 * @author Martin Werner
 */
public class MIO {

    private double trust = 0;
    private String type = "";
    private String findPageURL = "";
    private String directURL = "";
    private String fileName = "";

    private Entity entity;
    private String interactivityGrade = "";
    private boolean isDedicatedPage = true;
    private Map<String, List> infos;

    public MIO(String type, String directURL, String findPageURL, Entity entity) {

        this.type = type;
        this.findPageURL = findPageURL;
        this.entity = entity;
        this.directURL = directURL;
        this.fileName = extractFileName(directURL, type);

        infos = new HashMap<String, List>();
    }

    private String extractFileName(String directURL, String type) {

        if (type.equals("swf")) {
            String regExp = "[/=]?.[^/=]*\\." + type;
            Pattern p = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(directURL);
            while (m.find()) {
                return m.group(0);
            }
        }
        return directURL;
    }

    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public String getFindPageURL() {
        return findPageURL;
    }

    public void setFindPageURL(String findPageURL) {
        this.findPageURL = findPageURL;
    }

    public String getDirectURL() {
        return directURL;
    }

    public void setDirectURL(String directURL) {
        this.directURL = directURL;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String getInteractivityGrade() {
        return interactivityGrade;
    }

    public void setInteractivityGrade(String interactivityGrade) {
        this.interactivityGrade = interactivityGrade;
    }

    public boolean isDedicatedPage() {
        return isDedicatedPage;
    }

    public void setDedicatedPage(boolean isDedicatedPage) {
        this.isDedicatedPage = isDedicatedPage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List> getInfos() {
        return infos;
    }

    public void setInfos(Map<String, List> infos) {
        this.infos = infos;
    }

    public void addInfos(String infoName, List infoList) {
        infos.put(infoName, infoList);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
