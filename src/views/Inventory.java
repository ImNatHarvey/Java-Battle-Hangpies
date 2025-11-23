package views;

import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;
import static main.Main.listingManager;

import java.util.List;
import models.Hangpie;
import controllers.AlertManager;
import controllers.LogManager;
import interfaces.Colorable;
import main.Main;

public class Inventory
{
	public static void showInventory()
	{
		while (true)
		{
			Main.clearScreen();

			System.out.println("   _________________________________________________________________________________________________________________________________________________________________ \n"
							 + "  |                                                                                                                                                                 |");
			System.out.println("  |  " + Colorable.YELLOW + "░█▄█░█░█░░░▀█▀░█▀█░█░█░█▀▀░█▀█░▀█▀░█▀█░█▀▄░█░█" + Colorable.RESET + "                                                                                                                 |\n"
							 + "  |  " + Colorable.YELLOW + "░█░█░░█░░░░░█░░█░█░▀▄▀░█▀▀░█░█░░█░░█░█░█▀▄░░█░" + Colorable.RESET + "                                                                                                                 |\n"
							 + "  |  " + Colorable.YELLOW + "░▀░▀░░▀░░░░▀▀▀░▀░▀░░▀░░▀▀▀░▀░▀░░▀░░▀▀▀░▀░▀░░▀░" + Colorable.RESET + "                                                                                                                 |\n"
							 + "  |_________________________________________________________________________________________________________________________________________________________________|\n");

			List<Hangpie> inventory = currentUser.getInventory();
			
			int totalCount = 0;
			
			if (inventory.isEmpty())
			{
				System.out.println("    Your inventory is empty. Visit the Marketplace or Shop to buy a Hangpie!");
			}

			else
			{
				int count = 1;
				
				for (Hangpie pet : inventory)
				{
					// We'll use a different toString() for the inventory
					System.out.printf("    %d. [%s] [%s] Lvl:%d (HP:%d, Atk:%d)\n",
							count++,
							pet.getUniqueId().substring(0, 8).toUpperCase(),
							pet.getName(), // The custom name
							pet.getLevel(),
							pet.getMaxHealth(),
							pet.getAttackPower());
					totalCount++;
					
					try
					{
						Thread.sleep(16); // Short pause for effect on list view
					}
					
					catch (Exception e)
					{
						
					}
				}
				
			}
			
			Main.fillUpList(20, totalCount, "");
			
			System.out.println("\n   " + AlertManager.getAndClearAlert());
			
			System.out.println("\n   Options:");
			System.out.println("   [1] - Rename a Hangpie");
			System.out.println("   [2] - Sell to Shop");
			System.out.println("   [3] - List to Marketplace");
			System.out.println("   [4] - Back to Dashboard");
			System.out.print("   " + Colorable.BLUE + "[Enter your choice]: " + Colorable.RESET);

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doRenameHangpie();
			}

			else if (choice.equals("2"))
			{
				doSellToShop();
			}

			else if (choice.equals("3"))
			{
				doListToMarketplace();
			}

			else if (choice.equals("4"))
			{
				break;
			}

			else
			{
				AlertManager.setError("Invalid choice.");
			}
		}
	}

	private static void doRenameHangpie()
	{
		System.out.println("\n--- Rename Hangpie ---");
		List<Hangpie> inventory = currentUser.getInventory();

		if (inventory.isEmpty())
		{
			AlertManager.setError("You have no Hangpies to rename.");
			return;
		}

		System.out.print("   Enter the number of the Hangpie to rename (e.g., 1): ");
		String input = scanner.nextLine().trim();

		int petNumber;

		try
		{
			petNumber = Integer.parseInt(input);

			if (petNumber < 1 || petNumber > inventory.size())
			{
				System.out.println("Invalid number.");
				return;
			}
		}

		catch (NumberFormatException e)
		{
			System.out.println("Invalid input. Please enter a number.");
			return;
		}

		Hangpie petToRename = inventory.get(petNumber - 1);

		System.out.print("   Enter new name for " + petToRename.getName() + ": ");
		String newName = scanner.nextLine().trim();

		if (newName.isEmpty())
		{
			System.out.println("Name cannot be empty. Rename cancelled.");
			return;
		}

		petToRename.setName(newName);	

		userManager.updateUser(currentUser);

		AlertManager.setSuccess("Your Hangpie has been renamed to " + newName + "!");
	}

	private static void doSellToShop()
	{
		System.out.println("\n--- Sell Hangpie ---");
		List<Hangpie> inventory = currentUser.getInventory();

		if (inventory.isEmpty())
		{
			AlertManager.setError("You have no Hangpies to sell.");
			return;
		}

		System.out.print("Enter the number of the Hangpie to sell (e.g., 1): ");
		String input = scanner.nextLine().trim();

		int petNumber;

		try
		{
			petNumber = Integer.parseInt(input);

			if (petNumber < 1 || petNumber > inventory.size())
			{
				System.out.println("Invalid number.");
				return;
			}
		}

		catch (NumberFormatException e)
		{
			System.out.println("Invalid input. Please enter a number.");
			return;
		}

		Hangpie petToSell = inventory.get(petNumber - 1);

		// 50 Gold per level plus 10% of its original price
		double sellPrice = petToSell.getLevel() * 50 + (petToSell.getPrice() / 10);

		System.out.print("   " + Colorable.YELLOW + "[Are you sure you want to sell " + petToSell.getName() + " for " + sellPrice + "G? (yes/no)]: " + Colorable.RESET);
		String choice = scanner.nextLine().trim();

		if (choice.equalsIgnoreCase("yes"))
		{
			// 1. Add gold to user
			currentUser.addGold(sellPrice);

			// 2. Remove pet from inventory
			currentUser.removeToInventory(petToSell);

			// 3. Save all changes
			userManager.updateUser(currentUser);

			AlertManager.setSuccess(petToSell.getName() + " has been sold. Your new balance: " + currentUser.getGoldBalance() + "G");

			String logMsg = "Sold " + petToSell.getName() + " to the Shop for " + sellPrice + "G.";
			LogManager.log(currentUser.getUsername(), logMsg);
		}

		else if (choice.equalsIgnoreCase("no"))
		{
			AlertManager.setError("Sale cancelled.");
		}

		else
		{
			AlertManager.setError("Invalid Input. Sale cancelled.");
		}

	}

	private static void doListToMarketplace()
	{
		System.out.println("\n--- List Hangpie for Sale (Marketplace) ---");
		List<Hangpie> inventory = currentUser.getInventory();

		if (inventory.isEmpty())
		{
			AlertManager.setError("You have no Hangpies to list.");
			return;
		}

		// 1. Get the pet to sell
		System.out.print("   Enter the number of the Hangpie to list (e.g., 1): ");
		String inputPetNumber = scanner.nextLine();

		int petNumber;

		try
		{
			petNumber = Integer.parseInt(inputPetNumber);
			if (petNumber < 1 || petNumber > inventory.size())
			{
				AlertManager.setError("Invalid number.");
				return;
			}
		}

		catch (NumberFormatException e)
		{
			AlertManager.setError("Invalid input. Please enter a number.");
			return;
		}


		Hangpie petToList = inventory.get(petNumber - 1);

		// 2. Set the desired price
		double sellingPrice;

		try
		{
			System.out.print("   Enter selling price for " + petToList.getName() + ": ");
			String inputSellPrice = scanner.nextLine();

			sellingPrice = Double.parseDouble(inputSellPrice);

			if (sellingPrice < 0)
			{
				AlertManager.setError("Price must be positive. Listing cancelled.");
				return;
			}
		}

		catch (NumberFormatException e)
		{
			AlertManager.setError("Invalid input. Please enter a number.");
			return;
		}
		
		// Confirmation
		System.out.print(Colorable.YELLOW + "   [Are you sure you want to sell " + petToList.getName() + " for " + sellingPrice + " G? (yes/no)]: " + Colorable.RESET);
		String confirmation = scanner.nextLine().trim();
		
		if (confirmation.equalsIgnoreCase("YES"))
		{
			// 3. Remove pet from user's inventory
			currentUser.removeToInventory(petToList);

			// 4. Create the new listing in the ListingManager
			listingManager.createListing(currentUser, petToList, sellingPrice);

			// 5. Save the user's updated inventory
			userManager.updateUser(currentUser);

			// 6. Add new log to the Activity Log
			String logMsg = "Listed " + petToList.getName() + " on the P2P Marketplace for " + sellingPrice + "G.";
			LogManager.log(currentUser.getUsername(), logMsg);
			
			AlertManager.setSuccess(petToList.getName() + " is now listed on the Marketplace for " + sellingPrice + "G!");
		}
		
		else if (confirmation.equalsIgnoreCase("NO"))
		{
			AlertManager.setError("Listing to Marketplace Cancelled.");
		}
		
		else
		{
			AlertManager.setError("Invalid Input. Cancelling.");
		}
	}
}