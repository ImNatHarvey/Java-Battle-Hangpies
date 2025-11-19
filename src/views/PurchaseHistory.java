package views;

import static main.Main.currentUser;
import static main.Main.purchaseManager;
import static main.Main.scanner;

import java.util.List;

import models.Purchase;

public class PurchaseHistory
{
	public static void showPurchaseHistory()
	{
		System.out.println("\n--- My Purchase History ---");
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
	            System.out.println(" - " + history.get(i).toString());
	        }
		}
		
		System.out.println("\n(Press Enter to go back to the dashboard)");
	    scanner.nextLine(); // Wait for user to press Enter
	}
}
