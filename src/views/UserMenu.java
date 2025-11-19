package views;

import static main.Main.currentUser;
import static main.Main.scanner;

import java.util.List;

import models.Hangpie;

import static main.Main.announcementManager;
import static main.Main.purchaseManager;
import static main.Main.productManager;

import game.GameLauncher; // Import the new Launcher

public class UserMenu
{
	public static void showUserDashboard()
	{
		// This all prints *once* when the dashboard loads
		System.out.println("\n--- USER DASHBOARD ---");
		System.out.println("Welcome, " + currentUser.getFirstName() + "!");

		// --- Game Description Requirement ---
		System.out.println("\n--- About Battle Hangpies ---");
		System.out.println("A word-guessing game where your pet (a Hangpie) battles opponents!");
		System.out.println("Guess letters correctly to damage the enemy. Guess wrong, and your Hangpie takes damage!");

		// --- Game Announcement Requirement ---
		System.out.println("\n--- Announcements ---");
		System.out.println(announcementManager.readAnnouncement());

		// --- Featured Most Bought Requirement ---
		System.out.println("\n--- Featured Hangpies ---");

		try
		{
			List<String> featuredIds = purchaseManager.getTopMostBought();

			if (featuredIds.isEmpty())
			{
				System.out.println("No Hangpies have been purchased yet. Be the first!");
			}
			else
			{
				int count = 1;
				for (String productId : featuredIds)
				{
					Hangpie products = productManager.getProductById(productId);
					if (products != null)
					{
						System.out.printf("#%d Top Seller: [%s]\n", count++, products.getName());
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Could not load featured items.");
		}

		while (true)
		{
			System.out.println("\nSelect an option:");
			System.out.println("1. Shop");
			System.out.println("2. Marketplace");
			System.out.println("3. My Inventory");
			System.out.println("4. My Wallet (Balance: " + currentUser.getGoldBalance() + "G)");
			System.out.println("5. My Profile");
			System.out.println("6. Purchase History");
			System.out.println("7. Activity Log");
			System.out.println("8. Play Game");
			System.out.println("9. Log Out");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				Shop.showShop();
			}
			else if (choice.equals("2"))
			{
				Marketplace.showMarketplace();
			}
			else if (choice.equals("3"))
			{
				Inventory.showInventory();
			}
			else if (choice.equals("4"))
			{
				Wallet.showWalletMenu();
			}
			else if (choice.equals("5"))
			{
				Profile.showProfile();
			}
			else if (choice.equals("6"))
			{
				PurchaseHistory.showPurchaseHistory();
			}
			else if (choice.equals("7"))
			{
				ActivityLog.showActivityLog();
			} 
			else if (choice.equals("8"))
			{
				// INTEGRATION: Launch the AWT Game Window
				GameLauncher.launchGame(currentUser);
			} 
			else if (choice.equals("9"))
			{
				System.out.println("Logging out...");
				break;
			}
			else
			{
				System.out.println("Invalid choice. Please try again.");
			}
		}
	}
}