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
		// --- EASY (Single Words: Animals, Food, Nature) ---
		easyWords.add(new WordData("BANANA", "A long curved yellow fruit"));
		easyWords.add(new WordData("GIRAFFE", "The tallest land animal"));
		easyWords.add(new WordData("PIZZA", "Italian dish with cheese and tomato"));
		easyWords.add(new WordData("GUITAR", "A musical instrument with strings"));
		easyWords.add(new WordData("OCEAN", "A large body of salt water"));
		easyWords.add(new WordData("PENGUIN", "A flightless bird living in ice"));
		easyWords.add(new WordData("DOCTOR", "Someone who treats sick people"));
		easyWords.add(new WordData("SOCCER", "Most popular sport in the world"));
		easyWords.add(new WordData("SUMMER", "The hottest season of the year"));
		easyWords.add(new WordData("LIBRARY", "A place full of books"));
		easyWords.add(new WordData("PLANET", "Earth is one of these"));
		easyWords.add(new WordData("BUTTERFLY", "Insect with colorful wings"));
		easyWords.add(new WordData("RAINBOW", "Colorful arc in the sky"));
		
		// --- HARD (Phrases, Idioms, Pop Culture) ---
		hardWords.add(new WordData("PIECE OF CAKE", "Idiom: Something very easy"));
		hardWords.add(new WordData("BREAK A LEG", "Idiom: Good luck performance"));
		hardWords.add(new WordData("HARRY POTTER", "The Boy Who Lived"));
		hardWords.add(new WordData("STAR WARS", "May the Force be with you"));
		hardWords.add(new WordData("THE LION KING", "Disney movie with Simba"));
		hardWords.add(new WordData("UP IN THE AIR", "Idiom: Uncertain or unresolved"));
		hardWords.add(new WordData("JURASSIC PARK", "Movie with dinosaurs"));
		hardWords.add(new WordData("NEW YORK CITY", "The Big Apple"));
		hardWords.add(new WordData("ICE CREAM CAKE", "A frozen birthday treat"));
		hardWords.add(new WordData("UNDER THE WEATHER", "Idiom: Feeling sick"));
		hardWords.add(new WordData("SPIDER MAN", "Hero bitten by an arachnid"));
	}
	
	public static WordData getRandomWord(int level) {
		Random rand = new Random();
		// Use Hard words (Phrases) for levels > 3, otherwise Easy words
		List<WordData> source = (level > 3) ? hardWords : easyWords;
		return source.get(rand.nextInt(source.size()));
	}
}