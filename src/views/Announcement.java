package views;

import static main.Main.scanner;
import static main.Main.announcementManager;

import controllers.AlertManager;
import interfaces.Colorable;
import main.Main;

public class Announcement implements Colorable
{
	// Handles the UI for the admin to update the game announcements.
	public static void doUpdateAnnouncement()
	{
		Main.clearScreen();
		AdminMenu.displayAdminNavbar();
		
		System.out.println("\n--- Update Announcement ---");
		System.out.println(Colorable.YELLOW + "The current announcement is:" + Colorable.RESET);
		System.out.println("---------------------------------");
		System.out.println(announcementManager.readAnnouncement());
		System.out.println("---------------------------------");

		System.out.println("\n" + AlertManager.getAndClearAlert());
		
		System.out.print(Colorable.YELLOW + "Please enter the new announcement (type 'cancel' to abort): " + Colorable.RESET);
		String newAnnouncement = scanner.nextLine();

		if (newAnnouncement.equalsIgnoreCase("cancel"))
		{
			AlertManager.setError("Update cancelled.");
			return;
		}

		announcementManager.writeAnnouncement(newAnnouncement);
		AlertManager.setSuccess("Announcement updated successfully!");
	}
}