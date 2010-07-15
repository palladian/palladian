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

 

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7905678837165394359L;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIO.class);

    /** The trust. */
    private double trust = 0;
    
    /** The ml trust. */
    private double mlTrust = 0;

    /** The mio type. */
    private String mioType = "";
    
    /** The find page url. */
    private String findPageURL = "";
    
    /** The direct url. */
    private String directURL = "";
    
    /** The file name. */
    private String fileName = "";
    
    /** The file size. */
    private double fileSize = 0;
    
    /** The text content length. */
    private double textContentLength = 0;

    /** The entity. */
    private Entity entity;
    
    /** The interactivity grade. */
    private String interactivityGrade = "unclear";
    
    /** The is dedicated page. */
    private boolean isDedicatedPage = true;
    
    /** The infos. */
    private Map<String, List> infos;

    /** The Constant FLASH. */
    private static final String FLASH = "flash";

    /** The Constant APPLET. */
    private static final String APPLET = "applet";

    /** The features. */
    private Map<String, Double> features;

    /**
     * Instantiates a new mIO.
     *
     * @param mioType the mio type
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
     * Resets the MIOInfos (for saving memory).
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

    /**
     * Sets the feature.
     *
     * @param name the name
     * @param value the value
     */
    public void setFeature(String name, double value) {
        features.put(name, value);
    }

    /**
     * Gets the feature.
     *
     * @param name the name
     * @return the feature
     */
    public double getFeature(String name) {
        double result = 0;
        try {
            result = features.get(name);
        } catch (Exception e) {
            LOGGER.info("getFeature for: " + name + " failed!");
        }
        return result;
    }

    /**
     * Gets the features.
     *
     * @return the features
     */
    public Map<String, Double> getFeatures() {
        return features;
    }

    /**
     * Gets the ml trust.
     *
     * @return the ml trust
     */
    public double getMlTrust() {
        return mlTrust;
    }

    /**
     * Sets the ml trust.
     *
     * @param mlTrust the new ml trust
     */
    public void setMlTrust(double mlTrust) {
        this.mlTrust = mlTrust;
    }

    /**
     * Gets the file size.
     *
     * @return the file size
     */
    public double getFileSize() {
        return fileSize;
    }

    /**
     * Sets the file size.
     *
     * @param fileSize the new file size
     */
    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Gets the text content length.
     *
     * @return the text content length
     */
    public double getTextContentLength() {
        return textContentLength;
    }

    /**
     * Sets the text content length.
     *
     * @param textContentLength the new text content length
     */
    public void setTextContentLength(double textContentLength) {
        this.textContentLength = textContentLength;
    }
}
