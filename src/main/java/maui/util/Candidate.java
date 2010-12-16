package maui.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.wikipedia.miner.model.Anchor;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Anchor.Sense;

public class Candidate {
		
		/** Normalized string or vocabulary id */
		String name;
		
		/** The original full form as it appears in the document */
		String fullForm;
		
		/** The title of the descriptor in the vocabulary */
		String title;
		
		/** Number of occurrences of the candidate in the document */
		int frequency;
		
		/**  Normalized frequenc */
		double termFrequency;
		
		/** Position of the first occurrence */
		double firstOccurrence;
		
		/** Position of the last occurrence */
		double lastOccurrence;
		
		/** Wikipedia keyphraseness */
		double wikipKeyphraseness = 0;
		
		/** Total wikipedia keyphraseness */
		double totalWikipKeyphraseness = 0;
		
		Anchor anchor = null;
		
		Vector<Anchor> anchors = null;
		
		/**
		 * HashMap to store occurrence frequencies of all full forms
		 */
		HashMap<String,Counter> fullForms;
		
		/**
		 * Constructor for the first occurrence of a candidate
		 */
		public Candidate (String name, String fullForm, int firstOccurrence) {
			this.name = name;
			this.frequency = 1;
			
			this.firstOccurrence = (double)firstOccurrence;	
			this.lastOccurrence = (double)firstOccurrence;
			this.fullForm = fullForm;
			
			fullForms = new HashMap<String,Counter>();
			fullForms.put(fullForm,new Counter());
			
		
		}
		
		public 	Candidate(String name, String fullForm, int  firstOccurrence,
				Anchor anchor, double probability) {
			
			this.name = name;
			this.frequency = 1;
			
			this.firstOccurrence = (double)firstOccurrence;	
			this.lastOccurrence = (double)firstOccurrence;
			this.fullForm = fullForm;
			
			fullForms = new HashMap<String,Counter>();
			fullForms.put(fullForm,new Counter());
			
			this.totalWikipKeyphraseness =  probability;
			this.wikipKeyphraseness =  probability;
			this.anchor = anchor;
		}

		public Candidate getCopy() {
			Candidate newCandidate = new Candidate(this.name, this.fullForm, (int) this.firstOccurrence);
			
			newCandidate.frequency = this.frequency;
			newCandidate.termFrequency = this.termFrequency;
			newCandidate.firstOccurrence = this.firstOccurrence;
			newCandidate.lastOccurrence = this.lastOccurrence;
			newCandidate.fullForms = this.fullForms;
			newCandidate.totalWikipKeyphraseness = this.totalWikipKeyphraseness;
			newCandidate.wikipKeyphraseness = this.wikipKeyphraseness;
			newCandidate.anchor = this.anchor;
			return newCandidate;
		}
		
		public void setTitle(String title) {
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
		public void setName(String name) {
			this.name = name;
		}
		
	
		public Anchor getAnchor() {
			return anchor;
		}
		
		public double getWikipKeyphraseness() {
			return wikipKeyphraseness;
		}
		
		public double getTotalWikipKeyphraseness() {
			return totalWikipKeyphraseness;
		}
		
		/** Returns all document phrases that were mapped to this candidate.
		 * 
		 * @return HashMap in which the keys are the full forms and the values are their frequencies
		 */
		public HashMap<String,Counter> getFullForms() {
			return fullForms;
		}
		
		/**
		 * Records the occurrence position and the full form of a candidate
		 * 
		 * @param fullForm
		 * @param occurrence
		 */
		public void recordOccurrence(String fullForm, int occurrence) {
			frequency++;
		
			lastOccurrence = occurrence;
			if (fullForms.containsKey(fullForm)) {
				fullForms.get(fullForm).increment();
			} else {
				fullForms.put(fullForm, new Counter());
			}
			
			if (totalWikipKeyphraseness != 0) {		
				totalWikipKeyphraseness += wikipKeyphraseness;
				
			}
		}
		
		/**
		 * In case of free indexing, e.g. tagging or keyphrase extraction,
		 * retrieves the most frequent full form
		 * for a given candidate. 
		 * @return best full form of a candidate
		 */
		public String getBestFullForm() {
			int maxFrequency = 0;
			String bestFullForm = "";
			for (String form : fullForms.keySet()) {
				int formFrequency = fullForms.get(form).value();
				if (formFrequency > maxFrequency) {
					bestFullForm = form;
					maxFrequency = formFrequency;
				} 
			}
			return bestFullForm;
		}
		
		public String getName() {
			return name;
		}
	
		public double getFrequency() {
			return frequency;
		}
		
		public double getTermFrequency() {
			return termFrequency;
		}
		
		public double getFirstOccurrence() {
			return firstOccurrence;
		}
		
		public double getLastOccurrence() {
			return lastOccurrence;
		}
		
		public double getSpread() {
			return lastOccurrence - firstOccurrence;
		}
		
		/**
		 * Normalizes all occurrence positions and frequencies by the total values in the given document
		 */
		public void normalize(int totalFrequency, int documentLength) {
			termFrequency = frequency/(double)totalFrequency;
			firstOccurrence = firstOccurrence/(double)documentLength;
			lastOccurrence = lastOccurrence/(double)documentLength;
			
		}
		
		public String toString() {
			return name + " (" + fullForm + "," + title + ")";
		}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("This is a method for creating candidate topics in a document");
	}

	public String getIdAndTitle() {
		
		return name + ": " + title;
	}

	/**
	 * If two candidates were disambiguated to the same topic, their values are merged.
	 * @param previousCandidate
	 */
	public void mergeWith(Candidate previousCandidate) {

		// name stays the same
		// full form stays the same
		// title stays the same
		
		// frequency increments
		this.frequency += previousCandidate.frequency;
		
		// term frequency increments
		this.termFrequency += previousCandidate.termFrequency;
		
		// update first occurrence to the earliest one
		double previous = previousCandidate.firstOccurrence;
		if (previous < this.firstOccurrence) {
			this.firstOccurrence = previous;
		}
		
		// and the opposite with the last occurrence
		previous = previousCandidate.lastOccurrence;
		if (previous > this.lastOccurrence) {
			this.lastOccurrence = previous;
		}
		
		// increment wikip keyphr
		this.totalWikipKeyphraseness += previousCandidate.totalWikipKeyphraseness;
		this.wikipKeyphraseness += previousCandidate.wikipKeyphraseness;
		
		// anchor should be added to the list of anchors
		if (anchors == null) {
			anchors = new Vector<Anchor>();
			anchors.add(this.anchor);
		}
		anchors.add(previousCandidate.anchor);
		
		// full forms should be added to the hash of full forms
		if (fullForms == null) {
			System.err.println("Is it ever empty??? ");
			fullForms = previousCandidate.fullForms;
		}
		HashMap<String,Counter> prevFullForms = previousCandidate.fullForms;
		for(String prevForm : prevFullForms.keySet()) {
			int count = prevFullForms.get(prevForm).value();
			if (fullForms.containsKey(prevForm)) {
				fullForms.get(prevForm).increment(count);
			} else {
				fullForms.put(prevForm, new Counter(count));
			}
		}
		
	}

	/**
	 * Retrieves all recorded info about a candidate
	 * @return info about a candidate formatted as a string  
	 */
	public String getInfo() {
		
		String result = "";
		
		String allFullForms = "";
		for (String form : fullForms.keySet()) {
			allFullForms +=  form + " (" + fullForms.get(form) + "), ";
		}
		
		String allAnchors = "";
		if (anchors != null) {
			for (Anchor anch : anchors) {
				allAnchors +=  anch + ", ";
			}
		}
		
		result += "\tName: " + this.name + "\n";
		result += "\tFullForm: " + this.fullForm + "\n";
		result += "\tArticle: " + this.article + "\n";
		result += "\tAllFullForms: " + allFullForms + "\n";
		result += "\tTitle: " + this.title + "\n";
		result += "\tFreq " + this.frequency + "\n";
		result += "\tTermFreq: " + this.termFrequency + "\n";
		result += "\tFirstOcc: " + this.firstOccurrence + "\n";
		result += "\tLastOcc: " + this.lastOccurrence + "\n";
		result += "\tWikipKeyphr: " + this.wikipKeyphraseness + "\n";
		result += "\tTotalWikipKeyphr: " + this.totalWikipKeyphraseness + "\n";
		result += "\tAnchor: " + this.anchor + "\n";
		result += "\tAnchors: " + allAnchors + "\n";

		return result;
	}

	public Vector<Anchor> getAnchors() {
		return this.anchors;
	}

	private Article article;
	public void setArticle(Article article) {
		this.article = article;
	}
	
	public Article getArticle() {
		return this.article;
	}

}
