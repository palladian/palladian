//package ws.palladian.processing;
//
//import org.apache.commons.lang3.Validate;
//
//import ws.palladian.processing.features.FeatureVector;
//
///**
// * <p>
// * Decorator for {@link Classifiable}, allowing to set a target class and thus making it {@link Trainable}.
// * </p>
// * 
// * @author Philipp Katz
// */
//public final class TrainableWrap implements Trainable {
//
//    // XXX could be extended with generics to allow retrieving the concrete subtype of Classifiable.
//
//    private final Classifiable classifiable;
//    private final String targetClass;
//
//    public TrainableWrap(Classifiable classifiable, String targetClass) {
//        Validate.notNull(classifiable, "classifiable must not be null");
//        Validate.notNull(targetClass, "targetClass must not be null");
//        this.classifiable = classifiable;
//        this.targetClass = targetClass;
//    }
//
//    @Override
//    public FeatureVector getFeatureVector() {
//        return classifiable.getFeatureVector();
//    }
//
//    @Override
//    public String getTargetClass() {
//        return targetClass;
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((classifiable == null) ? 0 : classifiable.hashCode());
//        result = prime * result + ((targetClass == null) ? 0 : targetClass.hashCode());
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        TrainableWrap other = (TrainableWrap)obj;
//        if (classifiable == null) {
//            if (other.classifiable != null)
//                return false;
//        } else if (!classifiable.equals(other.classifiable))
//            return false;
//        if (targetClass == null) {
//            if (other.targetClass != null)
//                return false;
//        } else if (!targetClass.equals(other.targetClass))
//            return false;
//        return true;
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder builder = new StringBuilder();
//        builder.append("TrainableWrap [classifiable=");
//        builder.append(classifiable);
//        builder.append(", targetClass=");
//        builder.append(targetClass);
//        builder.append("]");
//        return builder.toString();
//    }
//
//}
