package ws.palladian.external.lbj.IO;

import java.io.*;


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

public class OutFile {
	public PrintStream	 out = null;
	
	public OutFile(String filename){
		try{
			out= new PrintStream(new FileOutputStream(filename));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void println(String s){
		out.println(s);
	}
	public void print(String s){
		out.print(s);
	}
	public void close(){
		out.flush();
		out.close();
	}
}
