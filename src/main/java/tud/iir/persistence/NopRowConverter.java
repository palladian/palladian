package tud.iir.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NopRowConverter implements RowConverter<ResultSet> {

    @Override
    public ResultSet convert(ResultSet resultSet) throws SQLException {
        return resultSet;
    }

}
