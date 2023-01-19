package ws.palladian.persistence;

import ws.palladian.persistence.helper.SqlHelper;

import java.sql.ResultSet;
import java.sql.SQLException;

final class SampleClazzRowConverter implements RowConverter<SampleClazz> {

    @Override
    public SampleClazz convert(ResultSet resultSet) throws SQLException {
        SampleClazz testClazz = new SampleClazz();
        testClazz.setId(SqlHelper.getInteger(resultSet, "id"));
        testClazz.setName(resultSet.getString("name"));
        testClazz.setAge(resultSet.getInt("age"));
        return testClazz;
    }

}
