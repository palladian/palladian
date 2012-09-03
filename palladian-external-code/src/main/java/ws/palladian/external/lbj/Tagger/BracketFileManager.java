package ws.palladian.external.lbj.Tagger;


import java.util.StringTokenizer;
import java.util.Vector;

import ws.palladian.external.lbj.IO.InFile;
import ws.palladian.external.lbj.IO.OutFile;
import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.parse.LinkedVector;



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

public class BracketFileManager{
	
	  public static Vector<LinkedVector> readAndAnnotate(String fileName){
		  System.out.println("Reading and annotating the file: "+fileName);
		  Vector<String> tokens=new Vector<String>();
		  Vector<String> tags=new Vector<String>();
		  parseBracketedFile(fileName, tags, tokens);
		  StringBuffer allText=new StringBuffer(tokens.size()*20);
		  for(int i=0;i<tokens.size();i++) {
            allText.append(tokens.elementAt(i)+" ");
        }
		  String[] text={normalizeStr(allText.toString())};
		  SentenceSplitter parser=new SentenceSplitter(text);
		  Vector<LinkedVector> res=new Vector<LinkedVector>();
		  Sentence s=(Sentence)parser.next();
		  int i=0;
		  while(s!=null){
			  StringTokenizer st=new StringTokenizer(s.text," \n\t");
			  LinkedVector sentence=new LinkedVector();
			  while(st.hasMoreTokens()){
				  String token=st.nextToken();
				  if(!token.equals(tokens.elementAt(i))){
					  System.out.println("Error in alligning tokens in bracketed file reader");
					  System.exit(0);
				  }
				  NEWord word=new NEWord(new Word(token),null,tags.elementAt(i));
				  Vector<NEWord> v=Reuters2003Parser.splitWord(word);
				  if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme)){
					  sentence.add(word);
					  word.parts=new String[v.size()];
					  for(int j=0;j<v.size();j++) {
                        word.parts[j]=v.elementAt(j).form;
                    }
				  }
				  else{
					  if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.LbjTokenizationScheme)){
						  for(int j=0;j<v.size();j++) {
                            sentence.add(v.elementAt(j));
                        }
					  }
					  else{
						System.out.println("Fatal error in BracketFileManager.readAndAnnotate - unrecognized tokenization scheme: "+Parameters.tokenizationScheme);
						System.exit(0);
					  }					  
				  }
				  i++;
			  }
			  res.addElement(sentence);
			  s=(Sentence)parser.next();
		  }
		  Reuters2003Parser.annotate(res);
		  System.out.println("Done reading and annotating");
		  return res;
	  }
	
	  public static Vector<LinkedVector> parsePlainText(String file){
		  InFile in=new InFile(file);
		  String line=in.readLine();
		  StringBuffer buf=new StringBuffer(100000);
		  while(line!=null){
			  buf.append(line+ " \n");
			  line=in.readLine();
		  }
		  buf.append(" ");
		  in.close();
		  return parseText(buf.toString());
	  }

	  public static Vector<LinkedVector> parseText(String text){
		  Vector<LinkedVector> res=new Vector<LinkedVector>(); 
		  if(ParametersForLbjCode.forceNewSentenceOnLineBreaks){
			  //System.out.print("Forcing line breaks\t\t ");
			  StringTokenizer st=new StringTokenizer(text,"\n");
			  //int count=0;
			  while(st.hasMoreTokens()){
				  String s=st.nextToken();
				 // System.out.print("Sentence " + (count++)+ " : "+ s+"\t\t");
				 Vector<LinkedVector> parsedLine=parseTextLineNoAnnotate(s);
				 for(int i=0;i<parsedLine.size();i++) {
                    res.addElement(parsedLine.elementAt(i));
                }
			  }
		  } else {
            res= parseTextLineNoAnnotate(text);
        }
		  Reuters2003Parser.annotate(res);
		  return res;
	  }

	  public static Vector<LinkedVector> parseTextLineNoAnnotate(String text){
		  String[] input={normalizeStr(text)};
		  SentenceSplitter parser=new SentenceSplitter(input);
		  Sentence s=(Sentence)parser.next();
		  int i=0;
		  Vector<LinkedVector> res=new Vector<LinkedVector>();
		  while(s!=null){
			  String sentenceText=s.text;
			  if(sentenceText.charAt(sentenceText.length()-1)=='.') {
                sentenceText=sentenceText.substring(0,sentenceText.length()-1)+" . ";
            }
			  StringTokenizer st=new StringTokenizer(sentenceText," \n\t");
			  LinkedVector sentence=new LinkedVector();
			  while(st.hasMoreTokens()){
				  String token=st.nextToken();
				  NEWord word=new NEWord(new Word(token),null,"unlabeled");
				  Vector<NEWord> v=Reuters2003Parser.splitWord(word);
				  word.parts=new String[v.size()];
				  for(int j=0;j<v.size();j++) {
                    word.parts[j]=v.elementAt(j).form;
                }
				  sentence.add(word);
				  i++;
			  }
			  res.addElement(sentence);
			  s=(Sentence)parser.next();
		  }
		  return res;		  
	  }
	
	
	public static void parseBracketedFile(String filename,Vector<String> tags,Vector<String> words){
		System.out.println("Parsing a bracketed FILE: "+filename);
		InFile in=new InFile(filename);
		String line=in.readLine();
		StringBuffer text=new StringBuffer();
		while(line!=null){
			text.append(line+"\n");
			line=in.readLine();
		}
		parseBracketed(normalizeStr(text.toString()),tags,words);
		in.close();
		System.out.println("DONE: parsing a bracketed FILE");
	}
	
	public static String normalizeStr(String line){
		StringBuffer buf=new StringBuffer((int)(line.length()*1.2));
		for(int i=0;i<line.length();i++){
			if(line.charAt(i)==',')
			{	
				if(i>0&&i<line.length()-1&&Character.isDigit(line.charAt(i-1))&&Character.isDigit(line.charAt(i+1))) {
                    buf.append(",");
                } else {
                    buf.append(" , ");
                }
			}
			else{
				buf.append(line.charAt(i));
			}
		}
		line=replaceSubstring(buf.toString(),"\""," \" ");
		line=replaceSubstring(line,"'nt"," 'nt ");
		line=replaceSubstring(line,"'s"," 's ");
		line=replaceSubstring(line,"'d"," 'd ");
		line=replaceSubstring(line,"'m"," 'm ");
		line=replaceSubstring(line,"'ve"," 've ");
		line=replaceSubstring(line,"``"," \" ");
		line=replaceSubstring(line,"''"," \" ");
		line=replaceSubstring(line,";"," ; ");
      		line=replaceSubstring(line,"]"," ] ");
		line=replaceSubstring(line,"[PER", "_START_PER_");
                line=replaceSubstring(line,"[ORG", "_START_ORG_");
                line=replaceSubstring(line,"[LOC", "_START_LOC_");
                line=replaceSubstring(line,"[MISC", "_START_MISC_");
       		line=replaceSubstring(line,"["," [ ");
                line=replaceSubstring(line,"_START_PER_", " [PER ");
                line=replaceSubstring(line,"_START_ORG_", " [ORG ");
                line=replaceSubstring(line,"_START_LOC_", " [LOC ");
                line=replaceSubstring(line,"_START_MISC_", " [MISC ");
		line=replaceSubstring(line,")"," ) ");
		line=replaceSubstring(line,"("," ( ");
		line=replaceSubstring(line,"{"," { ");
		line=replaceSubstring(line,"}"," } ");
		line=replaceSubstring(line,"?"," ? ");
		line=replaceSubstring(line,"!"," ! ");
		return line;
	}
	
	public static void parseBracketed(String bracketsLine,Vector<String> tags,Vector<String> words){
		Vector<String> tokens=new Vector<String>();
		bracketsLine=replaceSubstring(bracketsLine,"["," [");
		bracketsLine=replaceSubstring(bracketsLine,"]"," ] ");
		bracketsLine=normalizeStr(bracketsLine);
		StringTokenizer st=new StringTokenizer(bracketsLine," \n\t");
		while(st.hasMoreTokens()) {
            tokens.addElement(st.nextToken());
        }
		for(int i=0;i<tokens.size();i++){
			boolean added=false;
			if(tokens.elementAt(i).equals("[PER")){
				i++;
				boolean first=true;
				while(!tokens.elementAt(i).equals("]")){
					words.addElement(tokens.elementAt(i));
					if(first){
						tags.addElement("B-PER");
						first=false;
					}
					else{
						tags.addElement("I-PER");						
					}
					i++;
				}
				added=true;
			}
			if(tokens.elementAt(i).equals("[ORG")){
				i++;
				boolean first=true;
				while(!tokens.elementAt(i).equals("]")){
					words.addElement(tokens.elementAt(i));
					if(first){
						tags.addElement("B-ORG");
						first=false;
					}
					else{
						tags.addElement("I-ORG");						
					}
					i++;
				}
				added=true;
			}
			if(tokens.elementAt(i).equals("[LOC")){
				i++;
				boolean first=true;
				while(!tokens.elementAt(i).equals("]")){
					words.addElement(tokens.elementAt(i));
					if(first){
						tags.addElement("B-LOC");
						first=false;
					}
					else{
						tags.addElement("I-LOC");
					}
					i++;
				}
				added=true;
			}
			if(tokens.elementAt(i).equals("[CONTACT")){
				i++;
				boolean first=true;
				while(!tokens.elementAt(i).equals("]")){
					words.addElement(tokens.elementAt(i));
					if(first){
						tags.addElement("B-CONTACT");
						first=false;
					}
					else{
						tags.addElement("I-CONTACT");
					}
					i++;
				}
				added=true;
			}
			if(tokens.elementAt(i).equals("[MISC"))
			{
				i++;
				boolean first=true;
				while(!tokens.elementAt(i).equals("]")){
					words.addElement(tokens.elementAt(i));
					if(first){
						tags.addElement("B-MISC");
						first=false;
					}
					else{
						tags.addElement("I-MISC");
					}
					i++;
				}	
				added=true;
			}
			if(!added){
				words.addElement(tokens.elementAt(i));
				tags.addElement("O");
			}
		}
	}
	
	
	public static void removeNestedTags(Vector<String> tags,Vector<String> words){
		for(int i=0;i<words.size();)
		{
			if(words.elementAt(i).equalsIgnoreCase("[PER")||
					words.elementAt(i).equalsIgnoreCase("[LOC")||
					words.elementAt(i).equalsIgnoreCase("[ORG")||
					words.elementAt(i).equalsIgnoreCase("[MISC")){
				System.out.println("nested tags discovered!!!, removing nested tags. Printing the nested tag sequence:");
				System.out.print(words.elementAt(i)+" ");
				words.removeElementAt(i);
				tags.removeElementAt(i);
				while(!words.elementAt(i).equals("]")){
					System.out.print(words.elementAt(i)+" ");
					i++;
				}
				words.removeElementAt(i);
				tags.removeElementAt(i);				
				System.out.println("");
			} else {
                i++;
            }
		}
	}
	
	
	public static void saveTaggedDataNoBioInfo(Vector<String> tags,Vector<String> words,String outfile){
		String prevTagType="O";
		OutFile out=new OutFile(outfile);
		for(int i=0;i<words.size();i++){
			String tag=tags.elementAt(i);
			String word=words.elementAt(i);
			if(tag.equals("PER")){
				if(!prevTagType.equals("PER")){
					if(!prevTagType.equals("O")) {
                        out.print(" ] ");
                    }
					out.print(" [PER ");
				}
				prevTagType="PER";
			}
			if(tag.equals("LOC"))
			{
				if(!prevTagType.equals("LOC")){
					if(!prevTagType.equals("O")) {
                        out.print(" ] ");
                    }
					out.print(" [LOC ");
				}
				prevTagType="LOC";
			}
			if(tag.equals("MISC"))
			{
				if(!prevTagType.equals("MISC")){
					if(!prevTagType.equals("O")) {
                        out.print(" ] ");
                    }
					out.print(" [MISC ");
				}
				prevTagType="MISC";
			}
			if(tag.equals("ORG"))
			{
				if(!prevTagType.equals("ORG")){
					if(!prevTagType.equals("O")) {
                        out.print(" ] ");
                    }
					out.print(" [ORG ");
				}
				prevTagType="ORG";
			}
			if(tag.equals("O")){
				if(!prevTagType.equals("O")) {
                    out.print(" ] ");
                }
				prevTagType="O";
			}
			if(tag==null){
				System.out.println("Null tag for word "+word);
				System.exit(0);
			}
			out.print(word+" ");
			if(word.indexOf(".")>-1) {
                out.print("\n");
            }
		}
		out.close();
	}

	
	/*
	 * replace the source with the target within the line
	 */
	public static String replaceSubstring(String line,String source,String target){
		int start=0;
		StringBuffer res=new StringBuffer(line.length()*2);
		while(line.indexOf(source,start)>-1){
			int next=line.indexOf(source,start);
			res.append(line.substring(start,next)+target);
			start=next+source.length();
		}
		res.append(line.substring(start,line.length()));
		return res.toString();
	}

	public static void main(String[] args){
		System.out.println(normalizeStr("Friday,"));
	}
}
