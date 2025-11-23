package views;

import static main.Main.currentUser;
import static main.Main.purchaseManager;
import static main.Main.scanner;

import java.util.List;

import interfaces.Colorable;
import main.Main;
import models.Purchase;

public class PurchaseHistory
{
	public static void showPurchaseHistory()
	{
		System.out.println("\n\n   _________________________________________________________________________________________________________________________________________________________________ \n"
						 + "  |                                                                                                                                                                 |");
		System.out.println("  |  " + Colorable.YELLOW + "░█▄█░█░█░░░█▀█░█░█░█▀▄░█▀▀░█░█░█▀█░█▀▀░█▀▀░░░█░█░▀█▀░█▀▀░▀█▀░█▀█░█▀▄░█░█" + Colorable.RESET + "                                                                                       |\n"
						 + "  |  " + Colorable.YELLOW + "░█░█░░█░░░░█▀▀░█░█░█▀▄░█░░░█▀█░█▀█░▀▀█░█▀▀░░░█▀█░░█░░▀▀█░░█░░█░█░█▀▄░░█░" + Colorable.RESET + "                                                                                       |\n"
						 + "  |  " + Colorable.YELLOW + "░▀░▀░░▀░░░░▀░░░▀▀▀░▀░▀░▀▀▀░▀░▀░▀░▀░▀▀▀░▀▀▀░░░▀░▀░▀▀▀░▀▀▀░░▀░░▀▀▀░▀░▀░░▀░" + Colorable.RESET + "                                                                                       |\n"
						 + "  |_________________________________________________________________________________________________________________________________________________________________|\n");

		List<Purchase> history = purchaseManager.getPurchasesForUser(currentUser.getUsername());

		if (history.isEmpty())
		{
			System.out.println("You have not purchased any items yet.");
		}
		else
		{
			// Loop in reverse to show newest first
			for (int i = history.size() - 1; i >= 0; i--)
			{
				System.out.println("    ◉ " + history.get(i).toString());
			}
		}
		
		Main.fillUpList(28, history.size(), "");

		System.out.print("\n    (Press Enter to go back to the dashboard)");
		scanner.nextLine(); // Wait for user to press Enter
	}
}