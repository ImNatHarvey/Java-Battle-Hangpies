package views;

import static main.Main.scanner;
import static main.Main.announcementManager;

public class Announcement
{
	// Handles the UI for the admin to update the game announcements.
	public static void doUpdateAnnouncement()
	{
		System.out.println("\n--- Update Announcement ---");
		System.out.println("The current announcement is:");
		System.out.println("---------------------------------");
		System.out.println(announcementManager.readAnnouncement());
		System.out.println("---------------------------------");

		System.out.println("Please enter the new announcement (type 'cancel' to abort): ");
		String newAnnouncement = scanner.nextLine();

		if (newAnnouncement.equalsIgnoreCase("cancel"))
		{
			System.out.println("Update cancelled.");
			return;
		}

		announcementManager.writeAnnouncement(newAnnouncement);
		System.out.println("Announcement updated successfully!");
	}
}
