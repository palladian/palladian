package ws.palladian.extraction.pos;

import ws.palladian.core.Annotation;
import ws.palladian.helper.functional.Function;

final class AnnotationValueConverter implements Function<Annotation, String> {

    public static final AnnotationValueConverter INSTANCE = new AnnotationValueConverter();

    private AnnotationValueConverter() {
        // singleton
    }

    @Override
    public String compute(Annotation input) {
        return input.getValue();
    }
}