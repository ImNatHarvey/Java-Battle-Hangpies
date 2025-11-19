package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordBank {
	
	public static class WordData {
		public String word;
		public String clue;
		
		public WordData(String w, String c) {
			this.word = w;
			this.clue = c;
		}
	}
	
	private static List<WordData> easyWords = new ArrayList<>();
	private static List<WordData> hardWords = new ArrayList<>();
	
	static {
		// Easy (Level 1-3)
		easyWords.add(new WordData("JAVA", "A popular programming language"));
		easyWords.add(new WordData("CLASS", "Blueprint for objects"));
		easyWords.add(new WordData("OBJECT", "Instance of a class"));
		easyWords.add(new WordData("LOOP", "Repeats code execution"));
		easyWords.add(new WordData("ARRAY", "Fixed-size collection"));
		
		// Hard (Level 4+)
		hardWords.add(new WordData("POLYMORPHISM", "Many forms"));
		hardWords.add(new WordData("INHERITANCE", "Acquiring properties of parent"));
		hardWords.add(new WordData("ENCAPSULATION", "Hiding data"));
		hardWords.add(new WordData("INTERFACE", "Contract of methods"));
		hardWords.add(new WordData("EXCEPTION", "An error event"));
	}
	
	public static WordData getRandomWord(int level) {
		Random rand = new Random();
		List<WordData> source = (level > 3) ? hardWords : easyWords;
		return source.get(rand.nextInt(source.size()));
	}
}