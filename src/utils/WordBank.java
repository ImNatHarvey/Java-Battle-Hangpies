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
	
	// Separate word lists by difficulty/length
	// World 1 (Short, Easy, 5-7 letters) - Stages 1-4
	private static List<WordData> world1Words = new ArrayList<>();
	// World 2 (Medium, 8-10 letters) - Stages 1-4
	private static List<WordData> world2Words = new ArrayList<>();
	// World 3+ (Long, 10+ letters) - Stages 1-4
	private static List<WordData> world3Words = new ArrayList<>();
	// Boss Words (Phrases/Sentences) - Stage 5 of any World
	private static List<WordData> bossWords = new ArrayList<>();
	
	static {
		// --- World 1 Words (Short, Easy: 5-7 letters) ---
		world1Words.add(new WordData("BANANA", "A long curved yellow fruit"));
		world1Words.add(new WordData("OCEAN", "A large body of salt water"));
		world1Words.add(new WordData("SOCCER", "Most popular sport in the world"));
		world1Words.add(new WordData("SUMMER", "The hottest season of the year"));
		world1Words.add(new WordData("PLANET", "Earth is one of these"));
		world1Words.add(new WordData("DOCTOR", "Someone who treats sick people"));
		world1Words.add(new WordData("EAGLE", "A large bird of prey"));
		world1Words.add(new WordData("FROZEN", "Turned into ice"));
		world1Words.add(new WordData("GLOW", "To shine brightly"));
		world1Words.add(new WordData("TIGER", "Large striped cat"));
		world1Words.add(new WordData("HEAVEN", "The abode of God and angels"));
		world1Words.add(new WordData("LIGHT", "The opposite of darkness"));
		world1Words.add(new WordData("MUSIC", "Rhythm, harmony, and melody"));
		world1Words.add(new WordData("SMART", "Intelligent or clever"));
		world1Words.add(new WordData("PHONE", "Used to make calls"));
		world1Words.add(new WordData("CHAIR", "Furniture to sit on"));
		world1Words.add(new WordData("TABLE", "Furniture to eat on"));
		
		// --- World 2 Words (Medium, 8-10 letters) ---
		world2Words.add(new WordData("GIRAFFE", "The tallest land animal"));
		world2Words.add(new WordData("GUITAR", "A musical instrument with strings"));
		world2Words.add(new WordData("PENGUIN", "A flightless bird living in ice"));
		world2Words.add(new WordData("LIBRARY", "A place full of books"));
		world2Words.add(new WordData("MOUNTAIN", "A large natural elevation of the earth's surface"));
		world2Words.add(new WordData("ELEPHANT", "The world's largest land animal"));
		world2Words.add(new WordData("KEYBOARD", "Input device for a computer"));
		world2Words.add(new WordData("DIAMOND", "A precious stone, pure carbon"));
		world2Words.add(new WordData("ABSTRACT", "Existing in thought or as an idea"));
		world2Words.add(new WordData("ADVENTURE", "An exciting or unusual experience"));
		world2Words.add(new WordData("FLAMINGO", "A pink wading bird"));
		world2Words.add(new WordData("ASTRONAUT", "A person trained to travel in space"));
		world2Words.add(new WordData("CALENDAR", "System for fixing the beginning, length, and divisions of the year"));
		world2Words.add(new WordData("TREASURE", "A quantity of valuable things"));
		world2Words.add(new WordData("DISCOVER", "To find or find out something"));

		// --- World 3+ Words (Long, 10+ letters) ---
		world3Words.add(new WordData("BUTTERFLY", "Insect with colorful wings"));
		world3Words.add(new WordData("RAINBOW", "Colorful arc in the sky"));
		world3Words.add(new WordData("COMMUNITY", "A group of people living together in one place"));
		world3Words.add(new WordData("TEMPERATURE", "Degree or intensity of heat present in a substance or object"));
		world3Words.add(new WordData("TECHNOLOGY", "The application of scientific knowledge for practical purposes"));
		world3Words.add(new WordData("UNIVERSAL", "Relating to all people or things in the world"));
		world3Words.add(new WordData("PHOTOGRAPH", "A picture made using a camera"));
		world3Words.add(new WordData("GOVERNMENT", "The political body that governs a country"));
		world3Words.add(new WordData("CHAMPIONSHIP", "A competition for a title or trophy"));
		world3Words.add(new WordData("ACCOMPLISH", "Achieve or complete successfully"));
		world3Words.add(new WordData("EXPERIENCE", "Practical contact with and observation of facts or events"));
		world3Words.add(new WordData("INTERNATIONAL", "Existing or occurring between two or more nations"));
		
		// --- Boss Words (Phrases/Sentences) ---
		bossWords.add(new WordData("PIECE OF CAKE", "Idiom: Something very easy"));
		bossWords.add(new WordData("BREAK A LEG", "Idiom: Good luck performance"));
		bossWords.add(new WordData("HARRY POTTER", "The Boy Who Lived"));
		bossWords.add(new WordData("STAR WARS", "May the Force be with you"));
		bossWords.add(new WordData("THE LION KING", "Disney movie with Simba"));
		bossWords.add(new WordData("UP IN THE AIR", "Idiom: Uncertain or unresolved"));
		bossWords.add(new WordData("JURASSIC PARK", "Movie with dinosaurs"));
		bossWords.add(new WordData("NEW YORK CITY", "The Big Apple"));
		bossWords.add(new WordData("ICE CREAM CAKE", "A frozen birthday treat"));
		bossWords.add(new WordData("UNDER THE WEATHER", "Idiom: Feeling sick"));
		bossWords.add(new WordData("SPIDER MAN", "Hero bitten by an arachnid"));
		bossWords.add(new WordData("THE MATRIX", "Blue pill or red pill?"));
		bossWords.add(new WordData("FORBIDDEN FOREST", "A dark and mysterious woods"));
		bossWords.add(new WordData("GLOBAL WARMING", "The rise in Earth's average temperature"));
		bossWords.add(new WordData("THE SILENT HILL", "Horror game with fog"));
	}
	
	public static WordData getRandomWord(int worldLevel, int progressLevel) {
		Random rand = new Random();
		List<WordData> source;
		
		if (progressLevel == 5) {
			// Boss stages always use the hard, phrase-based word list
			source = bossWords;
		} else if (worldLevel == 1) {
			source = world1Words;
		} else if (worldLevel == 2) {
			source = world2Words;
		} else {
			// World 3 and above use the long word list
			source = world3Words;
		}
		
		// Check if the source list is empty (shouldn't happen with the current setup, but safe check)
		if (source.isEmpty()) {
			// Fallback to the shortest list if the target list is empty
			System.err.println("Warning: Word list for World Level " + worldLevel + " is empty. Falling back to World 1 list.");
			source = world1Words.isEmpty() ? bossWords : world1Words;
		}

		return source.get(rand.nextInt(source.size()));
	}
}