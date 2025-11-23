package controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AnnouncementManager {
	private String databaseFile = "announcements.txt";

	// Reads the current announcement from the file.
	// Acts as the Getter
	public String readAnnouncement() {
		try (BufferedReader reader = new BufferedReader(new FileReader(databaseFile))) {
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}

			return content.toString();
		} catch (IOException e) {
			// Critical error that prevents the reading of announcements
			AlertManager.setError("Failed to load announcements: " + e.getMessage());
			return "No announcements at this time.";
		}
	}

	// Overwrites the announcement file with new content.
	// Acts as the Setter
	public void writeAnnouncement(String newContent) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseFile))) {
			writer.write(newContent);
		}

		catch (IOException e) {
			// Critical error that prevents saving of new announcement
			AlertManager.setError("Could not save announcement: " + e.getMessage());
		}
	}
}