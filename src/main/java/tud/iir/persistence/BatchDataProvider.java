package tud.iir.persistence;

import java.util.List;

public interface BatchDataProvider {

    List<Object> getData(int number);

    int getCount();

}
