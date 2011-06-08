package ws.palladian.daterecognition.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import weka.core.Instances;
import ws.palladian.daterecognition.evaluation.weka.WekaTraineeSetHelper;

public class WekaTraineeSetHelperTest {
	@Test
	public void testGetXFoldSets() {
		WekaTraineeSetHelper wtsh = new WekaTraineeSetHelper();
		Instances[] instances;
		String path = "/wekaClassifier/allDates.arff";
		instances = wtsh.getXFoldSets(path, 5);
		wtsh.createTraineeAndTestSets(instances, 3);

		int cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}

		File file = new File(wtsh.getClass().getResource(path).getFile());
		FileReader in;
		BufferedReader bufferedReader;
		int cntOrgInstances = 0;
		try {
			in = new FileReader(file);
			bufferedReader = new BufferedReader(in);
			String line;
			boolean preData = true;
			while((line = bufferedReader.readLine()) != null){
				if(preData){
					if(line.toLowerCase().indexOf("@data") != -1){
						preData = false;
					}
				}else{
					cntOrgInstances++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertThat(cntOrgInstances, is(cntNewInstances));
	}
	
	@Test
	public void createTraineeAndTestSets() {
		WekaTraineeSetHelper wtsh = new WekaTraineeSetHelper();
		Instances[] instances;
		Instances[] instancesXFold;
		String path = "/wekaClassifier/allDates.arff";
		instancesXFold = wtsh.getXFoldSets(path, 5);
		wtsh.createTraineeAndTestSets(instancesXFold, 3);
		int cntAllInstances = 0;
		for (int i = 0; i < instancesXFold.length; i++) {
			cntAllInstances += instancesXFold[i].numInstances();
		}

		instances = wtsh.createTraineeAndTestSets(instancesXFold, 0);
		int cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}
		assertThat(cntNewInstances, is(cntAllInstances));
		
		instances = wtsh.createTraineeAndTestSets(instancesXFold, 1);
		cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}
		assertThat(cntNewInstances, is(cntAllInstances));
		instances = wtsh.createTraineeAndTestSets(instancesXFold, 2);
		cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}
		assertThat(cntNewInstances, is(cntAllInstances));
		
		instances = wtsh.createTraineeAndTestSets(instancesXFold, 3);
		cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}
		assertThat(cntNewInstances, is(cntAllInstances));
		
		instances = wtsh.createTraineeAndTestSets(instancesXFold, 4);
		cntNewInstances = 0;
		for (int i = 0; i < instances.length; i++) {
			cntNewInstances += instances[i].numInstances();
		}
		assertThat(cntNewInstances, is(cntAllInstances));
		
	}
}
