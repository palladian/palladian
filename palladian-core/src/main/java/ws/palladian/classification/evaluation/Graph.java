package ws.palladian.classification.evaluation;

import java.io.File;
import java.io.IOException;

public interface Graph {
	void show();
	void save(File file) throws IOException;
}
