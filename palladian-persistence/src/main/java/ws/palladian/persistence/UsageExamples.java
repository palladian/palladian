package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.collection.CollectionHelper;

public class UsageExamples {
    public static void main(String[] args) {
        CatDatabase catDatabase = DatabaseManagerFactory.create(CatDatabase.class, getConfig());

        catDatabase.createTable();

        catDatabase.saveCat(new Cat("Snowball 1", "black"));
        catDatabase.saveCat(new Cat("Snowball 2", "grey"));
        catDatabase.saveCat(new Cat("Snowball 3", "dark grey"));

        CollectionHelper.print(catDatabase.getCats());

        System.out.println(catDatabase.getCat("Snowball 1"));
    }

    private static PropertiesConfiguration getConfig() {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.addProperty("db.driver", "org.h2.Driver");
        config.addProperty("db.jdbcUrl", "jdbc:h2:mem:test");
        config.addProperty("db.username", "sa");
        config.addProperty("db.password", "");
        return config;
    }

}

class CatDatabase extends DatabaseManager {

    private static final String ADD_CAT = "INSERT INTO cats SET name = ?, color = ?";
    private static final String GET_CATS = "SELECT * FROM cats";
    private static final String GET_CAT_BY_NAME = "SELECT * FROM cats WHERE name = ?";

    protected CatDatabase(DataSource dataSource) {
        super(dataSource);
    }

    public void createTable() {
        runUpdate("CREATE TABLE cats (name VARCHAR(255), color VARCHAR(255))");
    }

    public boolean saveCat(Cat cat) {
        return runInsertReturnId(ADD_CAT, cat.getName(), cat.getColor()) > 0;
    }

    public List<Cat> getCats() {
        return runQuery(new CatRowConverter(), GET_CATS);
    }

    public Cat getCat(String name) {
        return runSingleQuery(new CatRowConverter(), GET_CAT_BY_NAME, name);
    }
}

class CatRowConverter implements RowConverter<Cat> {

    @Override
    public Cat convert(ResultSet resultSet) throws SQLException {
        String name = resultSet.getString("name");
        String color = resultSet.getString("color");
        return new Cat(name, color);
    }

}

class Cat {
    private String name;
    private String color;

    public Cat(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Cat [name=" + name + ", color=" + color + "]";
    }
}