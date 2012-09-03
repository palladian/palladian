package ws.palladian.external.lbj.IO;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


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

public class Keyboard {
	public static BufferedReader standard = new BufferedReader(new InputStreamReader(System.in));
	
	public static String readLine() throws IOException{
		return  standard.readLine();
	}
}
