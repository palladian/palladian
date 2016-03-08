package ws.palladian.classification.utils;

import ws.palladian.core.value.Value;

public interface CsvValueParser {

	Value parse(String name, String input);

}
