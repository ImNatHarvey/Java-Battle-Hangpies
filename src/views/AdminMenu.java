package views;

import static main.Main.currentUser;
import static main.Main.productManager;
import static main.Main.purchaseManager;
import static main.Main.codeManager;
import static main.Main.userManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import controllers.AlertManager;
import interfaces.Colorable;
import main.Main;
import models.Hangpie;

import static main.Main.listingManager;
import static main.Main.scanner;

public class AdminMenu {
	public static void showAdminDashboard() {

		while (true) {
			Main.clearScreen();
			displayAdminNavbar();

			// PRODUCT MANAGEMENT
			//

			System.out.println("   ______________________________ \n"
					+ "  |                              |\t[DASHBOARD OVERVIEW]: \n"
					+ "  |  🏠︎ ADMIN PORTAL\t         |\n"
					+ "  |                              |\tTotal Registered Users:\t\t" + userManager.getUserCount()
					+ "\n" + "  |  [1] - 📦 PRODUCT MANAGEMENT\t |\n"
					+ "  |                              |\tProducts in Shop:\t\t" + productManager.getProductCount()
					+ "\n" + "  |  [2] - 👥 USER MANAGEMENT\t |\n"
					+ "  |                              |\tListing in Marketplace:\t\t"
					+ listingManager.getListingCount() + "\n" + "  |  [3] - <> CODE MANAGEMENT\t |\n"
					+ "  |                              |\tTotal Active (Unused) Codes:\t"
					+ codeManager.getActiveCodeCount() + "\n"
					+ "  |  [4] - 📁 ACTIVITY LOG\t |\t_____________________________________________________________________________________________________________________________\n"
					+ "  |                              |\n"
					+ "  |                              |\t[FEATURED MOST BOUGHT HANGPIES IN SHOP]:\n"
					+ "  |                              |");

			// --- Featured Most Bought Requirement ---
			try {
				// 1. Get the simple list of top 3 IDs
				List<String> featuredIds = purchaseManager.getTopMostBought();

				if (featuredIds.isEmpty()) {
					System.out.println(
							"  |                              |\tNo Hangpies have been purchased yet. Be the first!");
				}

				else {
					// 2. Display top 3 products
					int count = 1;

					for (String productId : featuredIds) {
						Hangpie products = productManager.getProductById(productId);

						if (products != null) {
							System.out.printf(
									Colorable.GREEN + "  |                              |\t#%d Top Seller:"
											+ Colorable.RESET + " [%s] - %s\n",
									count++, products.getName(), products.getDescription());
						}
					}
				}

				Main.fillUpList(3, featuredIds.size(), "  |                              |\t");

			}

			catch (Exception e) {
				System.err.println("Could not load featured items.");
			}

			System.out.println(
					"  |                              |\t_____________________________________________________________________________________________________________________________\n"
							+ "  |                              |\n"
							+ "  |                              |\t[RECENT ACTIVITY]:\n"
							+ "  |                              |");

			showRecentAdminActivity();

			System.out.println("  |﻿  [5] ➜] LOG OUT\t         |\n" + "  |______________________________|");

			System.out.println("\n  " + AlertManager.getAndClearAlert());

			System.out.print("\n  [Enter your choice]: ");

			String choice = scanner.nextLine();

			if (choice.equals("1")) {
				ProductManagement.showProductManagement();
			}

			else if (choice.equals("2")) {
				UserManagement.showUserManagement();
			}

			else if (choice.equals("3")) {
				CodeManagement.showCodeManagement();
			}

			else if (choice.equals("4")) {
				ActivityLog.showActivityLog();
			}

			else if (choice.equals("5")) {
				AlertManager.setSuccess("You have been Logged out.");
				break;
			}

			else {
				AlertManager.setError("Invalid choice. Please try again.");
			}
		}
	}

	public static void displayAdminNavbar() {
		System.out.println(Colorable.YELLOW + "  █▀▄ █▀█ ▀█▀ ▀█▀ █   █▀▀   █ █ █▀█ █▀█ █▀▀ █▀█ ▀█▀ █▀▀ █▀▀\n"
				+ "  █▀▄ █▀█  █   █  █   █▀▀   █▀█ █▀█ █ █ █ █ █▀▀  █  █▀▀ ▀▀█\t\t\t\t\t\t\t\t\t\t\t(👤) "
				+ currentUser.getUsername() + "\n" + "  ▀▀  ▀ ▀  ▀   ▀  ▀▀▀ ▀▀▀   ▀ ▀ ▀ ▀ ▀ ▀ ▀▀▀ ▀   ▀▀▀ ▀▀▀ ▀▀▀"
				+ Colorable.RESET);
		System.out.println(
				"_______________________________________________________________________________________________________________________________________________________________________");
	}

	private static void showRecentAdminActivity() {
		List<String> adminLogs = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader("activity_log.txt"))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// We check if the line contains the user's name
				if (line.contains("| " + currentUser.getUsername() + " |")) {
					adminLogs.add(line);
				}
			}
		}

		catch (IOException e) {
			System.err.println("  |                              |\tError reading activity log: " + e.getMessage());
		}

		if (adminLogs.isEmpty()) {
			System.out.println("  |                              |\tYou have no activity logged yet.");
		}

		else {
			// Show the newest logs first
			for (int i = adminLogs.size() - 1; i >= 0; i--) {
				if (i == adminLogs.size() - 6) {
					break;
				}

				System.out.println("  |                              |\t◉ " + adminLogs.get(i));
			}
		}
	}
}