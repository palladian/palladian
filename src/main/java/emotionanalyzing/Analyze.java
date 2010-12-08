package emotionanalyzing;

import java.io.FileWriter;
import java.io.IOException;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import emotionanalyzing.WordEntryList;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;




public class Analyze {
   
	public static void main(String[] args) {
      		 HashMap<String, ArrayList<WordEntry>> words = new HashMap<String,ArrayList<WordEntry>>();
      			
	         FileReader myFile= null;
	         BufferedReader buff= null;
	         String [] values;
	         
	        try {
	                myFile =new FileReader("src/main/java/emotionanalyzing/wort2.csv");
	                buff =new BufferedReader(myFile);
	                while (true) {              
	                    String line = buff.readLine();
	                    if (line == null)
	                        break;            
	                   
	                   values = line.split(";");                    
	                   
	                	if(words.containsKey(values[1])== false){
	                	   words.put(values[1], new WordEntryList());
	                	   
	                	   words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
	                	}else{
	                	   words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
	                	}
	                }
	        }              
	        catch (IOException e) {
	                System.err.println("Error2 :"+e);
	        } finally {
	                try{
	                    buff.close();
	                    myFile.close();
	                }catch (IOException e) {
	                	System.err.println("Error2 :"+e);
	                }
	        } 
	       // words.get("Vertrauen").get(1).increment();       
	       // System.out.println(words.get("Vertrauen"));
	        System.out.println(words);
	        
	        SourceRetriever s = new SourceRetriever();
	        String searchQuery = null;
	        
	       
	        BufferedReader bin = new BufferedReader(
	                               new InputStreamReader(System.in));
	      
	          System.out.println("Bitte geben Sie das Produkt ein: ");
	         
			try {
				searchQuery = bin.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	          System.out.println("Meinungen zu folgendem Produkt werden gesucht: " + searchQuery);
	        
	          s.setResultCount(20);
	  		
	  		s.setLanguage(SourceRetriever.LANGUAGE_GERMAN);
	  		
	  		s.setSource(SourceRetrieverManager.GOOGLE);
	  		
	  		
	  		ArrayList<WebResult> webURLs = s.getWebResults(searchQuery, 2, false);
	  		
	  		Integer f = webURLs.size();
	  		String url = null;
	  		ArrayList<String> urls = new ArrayList<String>();
			for (int i = 0; i<f; i++){
	  			 url = webURLs.get(i).getUrl();
				 System.out.println(url);
				 urls.add(url);
			}
			Integer v = urls.size();
			
			//HashMap<String, String> sites = new HashMap<String, String>();
			
			String content = null;
			PageContentExtractor p = new PageContentExtractor();
			List<SentenceEntry> listSentence = new ArrayList<SentenceEntry>();
			for (int l = 0; l<v; l++){
				String t = urls.get(l);
				content = p.getResultText(t);
				
				List<String> sentenceUrl = new ArrayList<String>();
				
				sentenceUrl = Tokenizer.getSentences(content);
				
				for (int i = 0; i<sentenceUrl.size(); i++){
			    SentenceEntry se = new SentenceEntry(sentenceUrl.get(i), t);
				listSentence.add(se);	
				}
				//System.out.println(sentenceUrl);
				Integer u = sentenceUrl.size();
				
				/*for (int j = 0; j<u; j++) {
					String h = sentenceUrl.get(j);
					sites.put(h, t);
				}*/
				//sites.put(content, t);
			}
			//System.out.println(sites);
			
			//Set<String> sentence = sites.keySet();
			String vorW = null;
			String vorWW = null;
			String w = null;
			
			//System.out.println(sentence);
			for (int h = 0; h<listSentence.size(); h++){
				SentenceEntry en = listSentence.get(h);
				String n = en.getSentence();
				List<String> tokens = new ArrayList<String>();
				n = n.replaceAll("�", "ue");
				n = n.replaceAll("�", "Ue");
				n = n.replaceAll("�", "oe");
				n = n.replaceAll("�", "Oe");
				n = n.replaceAll("�", "ae");
				n = n.replaceAll("�", "Ae");
				n = n.replaceAll("�", "ss");
				tokens = Tokenizer.tokenize(n);
				Integer k = tokens.size();
				
				for (int i = 0; i<k; i++){
					w = tokens.get(i);
					System.out.print(vorW + " ");
					System.out.print(w + " ");
					for (String e : words.keySet()){
						List<WordEntry> emoW = new ArrayList<WordEntry>();
						emoW = words.get(e);
						Integer d = emoW.size();
						// for (WordEntry w: words.get(e))
						for (int j = 0; j<d; j++){
							String vergleich = emoW.get(j).getWord();
							vergleich = vergleich.toLowerCase();
							w = w.toLowerCase();							
							if ( "nicht".equals(vorW) == false && vergleich.equals(w) && "nicht".equals(vorWW) == false && "ohne".endsWith(vorWW)== false && "keine".equals(vorW) == false){
									
									emoW.get(j).increment();
									en.addWordEntry(emoW.get(j));
									String r = en.getUrl();
									String b = en.getSentence();
									emoW.get(j).saveSentenceUrl(b, r);
									System.out.print(vorW);
							}
								//System.out.println("Wort gefunden");
								//System.out.println(sites);
								//System.out.println(n);
								//System.out.println(emoW.get(j).getWord());
								//System.out.println(r);
								//System.out.println(emoW.get(j).getCounter());
								//System.out.println(emoW.get(j).getSentenceUrlList());
							}											}/*
								else if (w.equals(vergleich) == true && vorWW == "nicht") {
									System.out.print(w);
									System.out.print("Eine Negation wurde gefunden!");
								}
							}*/
							 
						
					 
					vorWW = vorW;
					vorW = w;
					
				}
				//System.out.println(tokens);
			}
				
			for (String j : words.keySet()){
				List<WordEntry> emoW = words.get(j);
				Integer c = 0; 
				for (int i = 0; i < emoW.size(); i++){
					Integer x = emoW.get(i).getCounter();
					c = c + x;
				}
				System.out.println("Die Emotion " + j + " kam " + c + " mal vor!");
			
			}
			
				try
				{
				    FileWriter writer = new FileWriter("e:\\test.csv");
			 
				    for (String n : words.keySet()){
				    	List<WordEntry> emoWords = words.get(n);
				    	writer.write(n);
				    	writer.append('\n');
				    	writer.append('\n');
				    	for (int i = 0; i<emoWords.size(); i++){
				    		if (emoWords.get(i).counter != 0){
				    		writer.write(emoWords.get(i).getWord());
				    		writer.append('\n');
				    		writer.write(Integer.toString(emoWords.get(i).counter));
				    		writer.append('\n');
				    		for (String m : emoWords.get(i).getSentenceUrlList().keySet()){
				    			writer.write(m);
				    			writer.append('\n');
				    			writer.write(emoWords.get(i).getSentenceUrlList().get(m));
				    			writer.append('\n');
				    			writer.append('\n');
				    		}
				    		}
				    		
				    	}
				    }
				    //writer.write(words.get("Vertrauen").get(2).getWord());
			 
				    //generate whatever data you want
			 
				    writer.flush();
				    writer.close();
				}
				catch(IOException e)
				{
				     e.printStackTrace();
				} 
			    
			 
			 
			
			/*Integer w = sites.size();
			PageContentExtractor p = new PageContentExtractor();
			for (int i = 0; i<w; i++){
				x = sites.get(i);
				String g = p.getResultText(documentLocation)
			}*/
	  		//CollectionHelper.print(resultURLs);
	   
	  		/*Crawler c = new Crawler();
	  		String url = resultURLs.get(14);
	  		Document d = c.getWebDocument(url);
	  		String g = c.extractBodyContent(d);
	  		System.out.println(g);*/
	  		/*String url = resultURLs.get(3);
	  		PageContentExtractor p = new PageContentExtractor();
	  		String ergebnis = p.getResultText(url);
	  		System.out.println(ergebnis);*/
	  		
	  		
	  		//List g = new List(t.tokenize(ergebnis));
	  		/*List<String> c = new ArrayList<String>();
	  		c = Tokenizer.getSentences(ergebnis);
	  		System.out.println(c);*/
		}	
}