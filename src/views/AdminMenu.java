package views;

import static main.Main.currentUser;
import static main.Main.productManager;
import static main.Main.codeManager;
import static main.Main.userManager;
import static main.Main.listingManager;
import static main.Main.scanner;

public class AdminMenu
{
	public static void showAdminDashboard()
	{
		System.out.println("\n--- ADMIN DASHBOARD ---");
		System.out.println("Welcome, Admin " + currentUser.getLastName() + "!");
		
		System.out.println("\n--- System Analytics ---");
	    try
	    {
	        System.out.println("Total Registered Users:\t\t" + userManager.getUserCount());
	        System.out.println("Products in Shop:\t\t" + productManager.getProductCount());
	        System.out.println("Listing in Marketplace:\t\t" + listingManager.getListingCount());
	        System.out.println("Total Active (Unused) Codes:\t" + codeManager.getActiveCodeCount());

	    }
	    
	    catch (Exception e)
	    {
	        System.err.println("Could not load system analytics.");
	    }

		while (true)
		{
			System.out.println("\nSelect an option:");
			System.out.println("1. Product Management");
			System.out.println("2. Code Management");
			System.out.println("3. User Management");
			System.out.println("4. Update Announcement");
			System.out.println("5. Log Out");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				ProductManagement.showProductManagement();
			}

			else if (choice.equals("2"))
			{
				CodeManagement.showCodeManagement();
			}

			else if (choice.equals("3"))
			{
				UserManagement.showUserManagement();
			}
			
			else if (choice.equals("4"))
			{
				Announcement.doUpdateAnnouncement();
			}

			else if (choice.equals("5"))
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
