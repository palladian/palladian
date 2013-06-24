/**
 * Created on: 17.06.2013 14:32:29
 */
package ws.palladian.processing.features.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Filters a {@link FeatureVector} by removing some of the features in that FeatureVector. Removing can happen based on
 * a white list or a black list. If a white list is used no black list is possible and vice versa. A white list passes
 * only the features on the list to the filtered {@link FeatureVector}. A black list removes only the features on the
 * list from the {@link FeatureVector}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.1
 * 
 */
public class FeatureVectorFilter extends AbstractPipelineProcessor {

    /**
     * <p>
     * The white list containing the {@link FeatureDescriptor}s to pass to keep.
     * </p>
     */
    private List<FeatureDescriptor> whiteList;
    /**
     * <p>
     * The black list containing the {@link FeatureDescriptor}s to remove.
     * </p>
     */
    private List<FeatureDescriptor> blackList;

    /**
     * <p>
     * Creates a new {@link FeatureVectorFilter} with empty black and white list. You need to set at least one entry for
     * either the black or the white list to call this {@link PipelineProcessor}. Use either
     * {@link #setBlackListFeatures(List)}, {@link #setWhiteListFeatures(List)},
     * {@link #addBlackListFeature(FeatureDescriptor)} or {@link #addWhiteListFeature(FeatureDescriptor)} for this
     * purpose.
     * </p>
     */
    public FeatureVectorFilter() {
        whiteList = new ArrayList<FeatureDescriptor>();
        blackList = new ArrayList<FeatureDescriptor>();
    }

    /**
     * <p>
     * Sets the white list clearing the black list in the process.
     * </p>
     * 
     * @param whiteList The new white list to use.
     */
    public void setWhiteListFeatures(final List<FeatureDescriptor> whiteList) {
        Validate.notEmpty(whiteList);

        this.blackList.clear();
        this.whiteList.clear();
        this.whiteList.addAll(whiteList);
    }

    /**
     * <p>
     * Sets the black list clearing the white list in the process.
     * </p>
     * 
     * @param blackList The new black list to use.
     */
    public void setBlackListFeatures(final List<FeatureDescriptor> blackList) {
        Validate.notEmpty(blackList);

        this.blackList.clear();
        this.whiteList.clear();
        this.blackList.addAll(blackList);
    }

    /**
     * <p>
     * Adds a {@link FeatureDescriptor} to the white list of this filter. If the black list was used previously it is
     * cleared and the descriptor is the new only entry of the white list.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} to add to the white list.
     */
    public void addWhiteListFeature(final FeatureDescriptor descriptor) {
        blackList.clear();
        whiteList.add(descriptor);
    }

    /**
     * <p>
     * Adds a {@link FeatureDescriptor} to the black list of this filter. If the white list was used previously it is
     * cleared and the descriptor is the new only entry of the black list.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} to add to the black list.
     */
    public void addBlackListFeature(final FeatureDescriptor descriptor) {
        whiteList.clear();
        blackList.add(descriptor);
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        Validate.isTrue(
                !whiteList.isEmpty() || !blackList.isEmpty(),
                "Trying to call non initialized FeatureVectorFilter. Please either initilize to white list or the black list to use this PipelineProcessor. View the Javadoc for further information.");

        PipelineDocument document = getInput();
        if (!whiteList.isEmpty()) {
            applyWhiteList(document.getFeatureVector());
        } else if (!blackList.isEmpty()) {
            applyBlackList(document.getFeatureVector());
        }

        setOutput(document);
    }

    /**
     * <p>
     * Applies the entries from the black list to the provided {@link FeatureVector}.
     * </p>
     * 
     * @param featureVector The {@link FeatureVector} to filter.
     */
    private void applyBlackList(final FeatureVector featureVector) {
        List<Feature> featuresToRemove = new ArrayList<Feature>();
        for (FeatureDescriptor descriptor : blackList) {
            if (descriptor.getName().equals(descriptor.getIdentifier())) {
                for (Feature feature : featureVector) {
                    if (feature.getName().equals(descriptor.getName())) {
                        featuresToRemove.add(feature);
                    }
                }
            } else {
                for (Feature feature : featureVector) {
                    if (feature instanceof SparseFeature && feature.getName().equals(descriptor.getName())
                            && ((SparseFeature)feature).getIdentifier().equals(descriptor.getIdentifier())) {
                        featuresToRemove.add(feature);
                    }
                }
            }
        }

        for (Feature feature : featuresToRemove) {
            featureVector.remove(feature);
        }
    }

    /**
     * <p>
     * Applies the entries from the white list to the provided {@link FeatureVector}.
     * </p>
     * 
     * @param featureVector The {@link FeatureVector} to filter.
     */
    private void applyWhiteList(final FeatureVector featureVector) {
        Map<Feature, Boolean> featuresToRemove = new HashMap<Feature, Boolean>();
        for (FeatureDescriptor descriptor : whiteList) {
            if (descriptor.getName().equals(descriptor.getIdentifier())) {
                for (Feature feature : featureVector) {

                    Boolean removeIt = featuresToRemove.get(feature);
                    if (removeIt == null) {
                        removeIt = true;
                    }
                    if (feature.getName().equals(descriptor.getName())) {
                        removeIt = false;
                    }
                    featuresToRemove.put(feature, removeIt);
                }
            } else {
                for (Feature feature : featureVector) {

                    Boolean removeIt = featuresToRemove.get(feature);
                    if (removeIt == null) {
                        removeIt = true;
                    }
                    if ((feature instanceof SparseFeature && feature.getName().equals(descriptor.getName()) && ((SparseFeature)feature)
                            .getIdentifier().equals(descriptor.getIdentifier()))
                            || feature.getName().equals(descriptor.getName())
                            && feature.getValue().equals(descriptor.getIdentifier())) {
                        removeIt = false;
                    }
                    featuresToRemove.put(feature, removeIt);
                }
            }
        }

        for (Entry<Feature, Boolean> feature : featuresToRemove.entrySet()) {
            if (feature.getValue()) {
                featureVector.remove(feature.getKey());
            }
        }
    }

    public void removeWhiteListFeature(FeatureDescriptor descriptor) {
        whiteList.remove(descriptor);
    }
}
