package views;

import static main.Main.currentUser;
import static main.Main.productManager;
import static main.Main.purchaseManager;
import static main.Main.scanner;
import static main.Main.userManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Hangpie;
import models.Purchase;
import controllers.LogManager;

public class Shop {
	public static void showShop() {
		while (true) {
			System.out.println("\n--- Shop ---");
			System.out.println(
					"Welcome! Browse our available Hangpies. Your balance: " + currentUser.getGoldBalance() + "G");

			// Updated Header to match the new toString format
			System.out.println("NO.\t[PRODUCT ID]\t[NAME]\t\t\t[STATS]\t\t\t[PRICE]\t\t\t[DESCRIPTION]");

			// 1. Get the list of B2C products
			List<Hangpie> productList = new ArrayList<>(productManager.getAllProducts());

			// Sort them by ID
			Collections.sort(productList);

			if (productList.isEmpty()) {
				System.out.println("The shop is currently empty. Please check back later.");
			}

			else {
				// 2. Display them as a numbered list
				int count = 1;

				for (Hangpie product : productList) {
					System.out.println(count++ + ".\t" + product.toString());
				}

			}

			System.out.println("\nOptions:");
			System.out.println("1. Buy a Hangpie");
			System.out.println("2. Back to Dashboard");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1")) {
				// 3. Pass the list to the "buy" method
				doBuyHangpie(productList);
			}

			else if (choice.equals("2")) {
				break;
			}

			else {
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void doBuyHangpie(List<Hangpie> productList) {
		// 4. Ask for the NUMBER, not the ID
		System.out.print("Enter the number of the Hangpie you wish to buy: ");
		String inputNumber = scanner.nextLine();

		int productNumber = Integer.parseInt(inputNumber);

		try {
			if (productNumber < 1 || productNumber > productList.size()) {
				System.out.println("Invalid number.");
				return;
			}
		}

		catch (NumberFormatException e) {
			System.out.println("Invalid input. Please enter a number.");
			return;
		}

		// 5. Get the selected product from the list
		Hangpie product = productList.get(productNumber - 1);

		if (currentUser.getGoldBalance() < product.getPrice()) {
			System.out.println("Error: Not enough gold. You need " + product.getPrice() + "G.");
			return;
		}

		if (currentUser.subtractGold(product.getPrice())) {
			// Create the local copy
			Hangpie ownedPet = new Hangpie(product);

			// Add to inventory
			currentUser.addToInventory(ownedPet);

			// Update the user data (which saves their inventory)
			userManager.updateUser(currentUser);

			// Create purchase record
			Purchase newPurchase = new Purchase(currentUser.getUsername(), ownedPet.getId(), ownedPet.getName(),
					ownedPet.getPrice());
			purchaseManager.addPurchase(newPurchase);

			// Log the activity
			String logMsg = "Bought " + ownedPet.getName() + " from the Shop for " + ownedPet.getPrice() + "G.";
			LogManager.log(currentUser.getUsername(), logMsg);

			System.out.println("Congratulations! You have purchased: " + ownedPet.getName());
			System.out.println("Your new balance: " + currentUser.getGoldBalance() + "G");
		}

		else {
			System.out.println("An unknown error occurred during purchase.");
		}
	}
}