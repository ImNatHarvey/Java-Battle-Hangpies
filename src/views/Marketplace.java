package views;

import static main.Main.currentUser;
import static main.Main.listingManager;
import static main.Main.productManager;
import static main.Main.scanner;
import static main.Main.userManager;

import java.util.ArrayList;
import java.util.List;
import models.Hangpie;
import models.Listing;
import models.User;
import controllers.LogManager;

public class Marketplace
{
	public static void showMarketplace()
	{
		while (true)
		{
			System.out.println("\n--- Player Marketplace (P2P) ---");
			System.out.println("Here you can buy unique Hangpies from other players.");
			
			// 1. Get the list of listings
			List<Listing> listings = new ArrayList<>(listingManager.getAllListings());

			if (listings.isEmpty())
			{
				System.out.println("The marketplace is currently empty. Check back later!");
			}
			
			else
			{
				// 2. Display them as a numbered list
				int count = 1;
				
				for (Listing listing : listings)
				{
					System.out.println(count++ + ". " + listing.toString());
				}
				
			}

			System.out.println("\nOptions:");
			System.out.println("1. Buy a Hangpie from a Player");
			System.out.println("2. Back to Dashboard");

			System.out.print("Enter your choice: ");
			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				if (listings.isEmpty())
				{
					System.out.println("There are no items to buy.");
				}

				else
				{
					// 3. Pass the list to the "buy" method
					doBuyFromMarketplace(listings);
				}
			}

			else if (choice.equals("2"))
			{
				break;
			}

			else
			{
				System.out.println("Invalid choice.");
			}

		}
	}

	private static void doBuyFromMarketplace(List<Listing> listings)
	{
		System.out.println("\n--- Buy from Marketplace ---");
		System.out.print("Enter the number of the pet you wish to buy: ");	
		String inputSellNumber = scanner.nextLine().trim();
		
		int sellNumber;
		
		try
		{
			sellNumber = Integer.parseInt(inputSellNumber);
			
			if (sellNumber < 1 || sellNumber > listings.size())
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
		
		Listing listing = listings.get(sellNumber - 1);

		if (listing.getSellerUsername().equals(currentUser.getUsername()))
		{
			System.out.println("Error: You cannot buy your own item.");
			return;
		}

		if (currentUser.getGoldBalance() < listing.getPrice())
		{
			System.out.println("Error: Not enough gold. You need " + listing.getPrice() + "G.");
			return;
		}

		User seller = userManager.getUserByUsername(listing.getSellerUsername());

		
		if (seller == null)
		{
			System.out.println("CRITICAL ERROR: The seller no longer exists. Cancelling sale.");
			listingManager.removeListing(listing.getUniqueId());
			return;
		}

		// 1. Transfer Gold
		currentUser.subtractGold(listing.getPrice());
		seller.addGold(listing.getPrice());

		// 2. "Rebuild" the Hangpie
		Hangpie blueprint = productManager.getProductById(listing.getProductId());

		if (blueprint == null)
		{
			System.out.println("CRITICAL ERROR: The original product blueprint no longer exists. Cancelling.");
			return;
		}

		// Create a new copy
		Hangpie purchasedPet = new Hangpie(blueprint);

		purchasedPet.setUniqueId(listing.getUniqueId());
		purchasedPet.setName(listing.getPetName());
		purchasedPet.setLevel(listing.getPetLevel());
		purchasedPet.setMaxHealth(listing.getPetHealth());
		purchasedPet.setAttackPower(listing.getPetAttack());

		// 3. Add pet to buyer's inventory
		currentUser.addToInventory(purchasedPet);

		// 4. Remove listing from marketplace
		listingManager.removeListing(listing.getUniqueId());

		// 5. Save both users and the listings
		userManager.updateUser(currentUser);
		userManager.updateUser(seller);
		
		// Update Buyer's Activity Log
		String logMsg = "Bought " + purchasedPet.getName() + " from " + seller.getUsername() + " for " + listing.getPrice() + "G.";
	    LogManager.log(currentUser.getUsername(), logMsg);
	    
	    // Update Seller's Activity Log
	    String sellerLogMsg = "Sold " + purchasedPet.getName() + " to " + currentUser.getUsername() + " for " + listing.getPrice() + "G.";
	    LogManager.log(seller.getUsername(), sellerLogMsg);

		System.out.println("Congratulations! You have purchased " + purchasedPet.getName() + " from " + seller.getUsername() + "!");
	}
}
