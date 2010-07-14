/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Extractable;

/**
 * An interactive multimedia object.
 * 
 * @author Martin Werner
 */
public class MIO extends Extractable {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIO.class);

    private double trust = 0;
    private double mlTrust = 0;

    private String mioType = "";
    private String findPageURL = "";
    private String directURL = "";
    private String fileName = "";
    private double fileSize = 0;
    private double textContentLength = 0;

    private Entity entity;
    private String interactivityGrade = "unclear";
    private boolean isDedicatedPage = true;
    private Map<String, List> infos;

    /** The Constant FLASH. */
    private static final String FLASH = "flash";

    /** The Constant APPLET. */
    private static final String APPLET = "applet";

    private Map<String, Double> features;

    /**
     * Instantiates a new mIO.
     * 
     * @param type the type
     * @param directURL the direct url
     * @param findPageURL the find page url
     * @param entity the entity
     */
    public MIO(String mioType, String directURL, String findPageURL, Entity entity) {

        this.features = new HashMap<String, Double>();
        this.mioType = mioType;
        this.findPageURL = findPageURL;
        this.entity = entity;
        this.directURL = directURL;
        this.fileName = extractFileName(directURL, mioType);

        setExtractedAt(new Date(System.currentTimeMillis()));

        infos = new HashMap<String, List>();
    }

    /**
     * Extract file name.
     * 
     * @param directURL the direct url
     * @param type the type
     * @return the string
     */
    private String extractFileName(String directURL, String type) {

        String fileEnding = "";

        if (mioType.equals(FLASH)) {
            fileEnding = "swf";
        } else {
            if (mioType.equals(APPLET)) {
                fileEnding = "class";
            } else {
                if ("silverlight".equals(mioType)) {
                    fileEnding = "xap";
                } else {
                    if ("quicktime".equals(mioType)) {
                        fileEnding = "mov";
                    }
                }
            }
        }

        // if (type.equals("swf")) {
        String regExp = "[/=]?.[^/=]*\\." + fileEnding;
        Pattern p = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(directURL);
        while (m.find()) {
            return m.group(0);
        }
        // }
        return directURL;
    }

    /**
     * Resets the MIOInfos (for saving memory)
     * 
     */
    public void resetMIOInfos() {
        infos = new HashMap<String, List>();
    }

    /**
     * Gets the trust.
     * 
     * @return the trust
     */
    public double getTrust() {
        return trust;
    }

    /**
     * Sets the trust.
     * 
     * @param trust the new trust
     */
    public void setTrust(double trust) {
        this.trust = trust;
    }

    /**
     * Gets the find page url.
     * 
     * @return the find page url
     */
    public String getFindPageURL() {
        return findPageURL;
    }

    /**
     * Sets the find page url.
     * 
     * @param findPageURL the new find page url
     */
    public void setFindPageURL(String findPageURL) {
        this.findPageURL = findPageURL;
    }

    /**
     * Gets the direct url.
     * 
     * @return the direct url
     */
    public String getDirectURL() {
        return directURL;
    }

    /**
     * Sets the direct url.
     * 
     * @param directURL the new direct url
     */
    public void setDirectURL(String directURL) {
        this.directURL = directURL;
    }

    /**
     * Gets the entity.
     * 
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Sets the entity.
     * 
     * @param entity the new entity
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    /**
     * Gets the interactivity grade.
     * 
     * @return the interactivity grade
     */
    public String getInteractivityGrade() {
        return interactivityGrade;
    }

    /**
     * Sets the interactivity grade.
     * 
     * @param interactivityGrade the new interactivity grade
     */
    public void setInteractivityGrade(String interactivityGrade) {
        this.interactivityGrade = interactivityGrade;
    }

    /**
     * Checks if is dedicated page.
     * 
     * @return true, if is dedicated page
     */
    public boolean isDedicatedPage() {
        return isDedicatedPage;
    }

    /**
     * Sets the dedicated page.
     * 
     * @param isDedicatedPage the new dedicated page
     */
    public void setDedicatedPage(boolean isDedicatedPage) {
        this.isDedicatedPage = isDedicatedPage;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public String getMIOType() {
        return mioType;
    }

    /**
     * Sets the type.
     * 
     * @param type the new type
     */
    public void setMIOType(String type) {
        this.mioType = type;
    }

    /**
     * Gets the infos.
     * 
     * @return the infos
     */
    public Map<String, List> getInfos() {
        return infos;
    }

    /**
     * Sets the infos.
     * 
     * @param infos the infos
     */
    public void setInfos(Map<String, List> infos) {
        this.infos = infos;
    }

    /**
     * Adds the infos.
     * 
     * @param infoName the info name
     * @param infoList the info list
     */
    public void addInfos(String infoName, List infoList) {
        infos.put(infoName, infoList);
    }

    /**
     * Gets the file name.
     * 
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name.
     * 
     * @param fileName the new file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFeature(String name, double value) {
        features.put(name, value);
    }

    public double getFeature(String name) {
        double result = 0;
        try {
            result = features.get(name);
        } catch (Exception e) {
            LOGGER.info("getFeature for: " + name + " failed!");
        }
        return result;
    }

    public Map<String, Double> getFeatures() {
        return features;
    }

    public double getMlTrust() {
        return mlTrust;
    }

    public void setMlTrust(double mlTrust) {
        this.mlTrust = mlTrust;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }

    public double getTextContentLength() {
        return textContentLength;
    }

    public void setTextContentLength(double textContentLength) {
        this.textContentLength = textContentLength;
    }
}
