package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.helper.SqlHelper;

final class TestClazzRowConverter implements RowConverter<TestClazz> {

    @Override
    public TestClazz convert(ResultSet resultSet) throws SQLException {
        TestClazz testClazz = new TestClazz();
        testClazz.setId(SqlHelper.getInteger(resultSet, "id"));
        testClazz.setName(resultSet.getString("name"));
        testClazz.setAge(resultSet.getInt("age"));
        return testClazz;
    }

}
