/**
 * 
 */
package ws.palladian.daterecognition.evaluation.weka;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.experiment.InstanceQuery;
import weka.filters.Filter;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.Random;

/**
 * Provides date Instances for weka learner classifier.
 * 
 * @author Martin Gregor
 * 
 */
public class WekaTraineeSetHelper {

	/**
	 * Every instance has an ID for identifying.
	 */
	private HashMap<Integer, Instance> instances = new HashMap<Integer, Instance>();

	/**
	 * Attributes used to create instances.
	 */
	private HashMap<String, Attribute> attributes;

	private HashMap<String, FastVector> vectorMap = new HashMap<String, FastVector>();

	private HashSet<String> filterColumns;
	private ResultSet rs;
	private HashMap<String, Integer> columns = new HashMap<String, Integer>();
	private Attribute classAttribute;
	private Instances featurSet;

	/**
	 * Returns a traineeset out of instances. <br>
	 * <br>
	 * Use
	 * {@link WekaTraineeSetHelper#createInstances(String, String, PageDateType)}
	 * before.
	 * 
	 * @param size
	 *            If size between 0 and 1 (inclusive) it is interpreted as
	 *            percent otherwise as absolute number <br>
	 *            If size is less then 0, the positive value will be used. <br>
	 * <br>
	 * @param classIndex
	 *            publish or modified Date.
	 */
	public Instances getTraineeSet(double size, PageDateType classIndex) {
		return getTraineeSet(size, classIndex, null);
	}

	/**
	 * Returns a traineeset out of instances. <br>
	 * <br>
	 * Use
	 * {@link WekaTraineeSetHelper#createInstances(String, String, PageDateType)}
	 * before.
	 * 
	 * @param size
	 *            If size between 0 and 1 (inclusive) it is interpreted as
	 *            percent otherwise as absolute number <br>
	 *            If size is less then 0, the positive value will be used. <br>
	 * <br>
	 * @param classIndex
	 *            publish or modified Date.
	 * @param filter
	 *            Weka-Filter for use on {@link Instance}
	 */
	public Instances getTraineeSet(double size, PageDateType classIndex,
			Filter filter) {
		Instances instances = null;
		int traineeSize;
		double posSize = Math.abs(size);
		int instancesSize = this.instances.size();
		int tempSize;
		Random random = new Random();
		HashSet<Integer> ranSet;
		Integer index;
		
		if (posSize > 1) {
			tempSize = (int) posSize;
		} else {
			tempSize = (int) (posSize * instancesSize);
		}
		traineeSize = (tempSize > instancesSize) ? instancesSize : tempSize;
		instances = new Instances("TraineeSet",
				getAttributeVector(classIndex), traineeSize);
		instances.setClass(classAttribute);
		ranSet = random.nextIntSet(traineeSize, instancesSize);
		if (filter != null) {
			try {
				filter.setInputFormat(instances);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		index = 0;
		for (Entry<Integer, Instance> instance : this.instances.entrySet()) {
			if (ranSet.contains(index)) {
				Instance realInstance = instance.getValue();
				if (filter != null) {
					try {
						filter.input(realInstance);

					} catch (Exception e) {
						e.printStackTrace();
					}
					realInstance = filter.output();
				}
				instances.add(realInstance);
			}
			index++;
		}
		return instances;

	}

	/**
	 * Creates instances out of a database table, where table columns are
	 * templates for attributes. <br>
	 * To ignore columns use
	 * {@link WekaTraineeSetHelper#setFilterColumns(String[])} <br>
	 * Do not filter Class values like "pub" and "mod".
	 * 
	 * 
	 * @param dataBase
	 * @param table
	 * @param classIndex
	 */
	public void createInstances(String dataBase, String table,
			PageDateType classIndex) {
		this.columns = new HashMap<String, Integer>();
		setFilteredResultset(dataBase, table, classIndex);
	}

	private void setFilteredResultset(String dataBase, String table,
			PageDateType classIndex) {
		DataSetHandler.setDB(dataBase);
		DataSetHandler.openConnection();

		String sqlQuery = "Select * From " + table;

		try {
			this.rs = DataSetHandler.st.executeQuery(sqlQuery);
			ResultSetMetaData rsMetaData = rs.getMetaData();
			int numOfColumns = rsMetaData.getColumnCount();
			for (int i = 1; i <= numOfColumns; i++) {
				String columnName = rsMetaData.getColumnName(i);
				if (!isInFilter(columnName)) {
					columns.put(columnName, rsMetaData.getColumnType(i));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createVector();
		createAttributes();
		creatFeaturSet(classIndex);
		createInstances(classIndex);
		DataSetHandler.closeConnection(false);
	}

	private void createAttributes() {
		attributes = new HashMap<String, Attribute>();
		int type;
		for (Entry<String, Integer> column : this.columns.entrySet()) {
			type = column.getValue();
			switch (type) {
			case Types.INTEGER:
			case Types.DOUBLE:
				attributes.put(column.getKey(), new Attribute(column.getKey()));
				break;
			case Types.VARCHAR:
				attributes.put(column.getKey(), new Attribute(column.getKey(),
						vectorMap.get(column.getKey())));
				break;
			}
		}
	}

	private void createVector() {
		try {
			rs.beforeFirst();
			while (rs.next()) {
				for (Entry<String, Integer> column : this.columns.entrySet()) {

					if (column.getValue() == Types.VARCHAR) {
						String columnName = column.getKey();
						String value = rs.getString(columnName);
						FastVector vector = this.vectorMap.get(columnName);
						if (vector == null) {
							vector = new FastVector();
						}
						if (!isInVector(vector, value) && value != null) {
							vector.addElement(value);
						}
						this.vectorMap.put(columnName, vector);
					}

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public FastVector getStringVector() {
		FastVector attributes = new FastVector();
		int type;
		for (Entry<String, Integer> column : this.columns.entrySet()) {
			type = column.getValue();
			if (type == Types.VARCHAR) {
				attributes.addElement(new Attribute(column.getKey(), 
						vectorMap.get(column.getKey())));
			}
		}
		return attributes;
	}

	private boolean isInVector(FastVector vector, String value) {
		boolean inVector = false;
		for (int i = 0; i < vector.capacity(); i++) {
			String vectorValue = (String) vector.elementAt(i);
			if (vectorValue != null && vectorValue.equals(value)) {
				inVector = true;
				break;
			}
		}

		return inVector;
	}

	private void creatFeaturSet(PageDateType classIndex) {

		String classString;
		if (classIndex.equals(PageDateType.publish)) {
			classString = "pub";
		} else {
			classString = "mod";
		}

		FastVector attrVector = new FastVector();
		Attribute classAttr = null;
		for (Entry<String, Attribute> attribute : this.attributes.entrySet()) {
			if (attribute.getKey().equals(classString)) {
				classAttr = attribute.getValue();
			}
			attrVector.addElement(attribute.getValue());
		}
		this.featurSet = new Instances("Featurset", attrVector, attrVector.capacity());
		this.featurSet.setClass(classAttr);

	}

	private void createInstances(PageDateType classIndex) {
		String notClassValue;
		String classValue;
		if (classIndex.equals(PageDateType.publish)) {
			notClassValue = "mod";
			classValue = "pub";
		} else {
			notClassValue = "pub";
			classValue = "mod";
		}
		try {
			rs.beforeFirst();
			while (rs.next()) {
				Instance instance = new Instance(columns.size());
				instance.setDataset(this.featurSet);
				int id = rs.getInt("id");
				String indexClass = rs.getString(classValue);
				for (Entry<String, Integer> column : this.columns.entrySet()) {
					String columnName = column.getKey();
					if (!columnName.equals(notClassValue)
							&& !columnName.equals(classValue)) {
						Attribute attr = attributes.get(columnName);
						switch (column.getValue()) {
						case Types.INTEGER:
							instance.setValue(attr, rs.getInt(columnName));
							break;
						case Types.DOUBLE:
							instance.setValue(attr, rs.getDouble(columnName));
							break;
						case Types.VARCHAR:
							instance.setValue(attr, rs.getString(columnName));
							break;
						}
					}
					instance.setClassValue(indexClass);
				}
				this.instances.put(id, instance);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean isInFilter(String arg) {
		return this.filterColumns.contains(arg);
	}

	private FastVector getAttributeVector(PageDateType classIndex) {
		FastVector fastVector = new FastVector();
		String className;
		String notClassName;
		if (classIndex.equals(PageDateType.publish)) {
			className = "pub";
			notClassName = "mod";
		} else {
			className = "mod";
			notClassName = "pub";
		}
		for (Entry<String, Attribute> attribute : this.attributes.entrySet()) {
			if (!attribute.getKey().equals(notClassName)) {
				if (attribute.getKey().equals(className)) {
					this.classAttribute = attribute.getValue();
				}
				fastVector.addElement(attribute.getValue());
			}
		}

		return fastVector;
	}

	public void setInstances(HashMap<Integer, Instance> instances) {
		this.instances = instances;
	}

	public HashMap<Integer, Instance> getInstances() {
		return this.instances;
	}

	public void setFilterColumns(String[] filterColumns) {
		this.filterColumns = new HashSet<String>();
		for (int i = 0; i < filterColumns.length; i++) {
			this.filterColumns.add(filterColumns[i]);
		}
	}

	public void getDataset() {
		try {
			InstanceQuery iq = new InstanceQuery();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
