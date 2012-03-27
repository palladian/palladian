package ws.palladian.external.lbj.Tagger;

import java.util.Hashtable;
import java.util.StringTokenizer;

import ws.palladian.external.lbj.IO.InFile;




/**
  * This project was started by Nicholas Rizzolo (rizzolo@uiuc.edu) . 
  * Most of design, development, modeling and
  * coding was done by Lev Ratinov (ratinov2@uiuc.edu).
  * For modeling details and citations, please refer
  * to the paper: 
  * External Knowledge and Non-local Features in Named Entity Recognition
  * by Lev Ratinov and Dan Roth 
  * submitted/to appear/published at NAACL 09.
  * 
 **/

public class Parameters extends ParametersForLbjCode{
	
	public static void readConfigAndLoadExternalData(String configFile){
		InFile in=new InFile(configFile);
		taggingScheme=in.readLine();
		pathToModelFile=in.readLine();
		tokenizationScheme=in.readLine();
		String line=in.readLine();
		StringTokenizer st=new StringTokenizer(line,"\t ");
		st.nextToken();
		trainingRounds=Integer.parseInt(st.nextToken());
		featuresToUse=new Hashtable<String,Boolean>();
		line=in.readLine();
		while(line!=null){
			st=new StringTokenizer(line,"\t");
			String feature=st.nextToken();
			if(st.nextToken().equals("1")){
				System.out.println("Adding feature: "+feature);
				featuresToUse.put(feature,true);
			}
			line=in.readLine();
		}
		in.close();
		in.close();
		
		
		BrownClusters.init();
		if(Parameters.featuresToUse.containsKey("GazetteersFeatures")) {
            Gazzetteers.init("data/models/illinoisner/data/knownLists");
        }
		if(Parameters.featuresToUse.containsKey("NEShapeTaggerFeatures")){
			System.out.println("loading contextless shape classifier");
			ShapeClassifierManager.load();
			System.out.println("Done- loading contextless shape classifier");
		}
	}	
}
