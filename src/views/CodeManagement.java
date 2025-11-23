package views;

import static main.Main.codeManager;
import static main.Main.scanner;

import controllers.AlertManager;
import interfaces.Colorable;
import main.Main;
import models.RedeemCode;

public class CodeManagement implements Colorable {
	public static void showCodeManagement() {
		while (true) {
			Main.clearScreen();
			AdminMenu.displayAdminNavbar(); // Re-use the admin navbar for consistency
			System.out.println("\n--- Code Management ---");
			System.out.println("Total Active (Unused) Codes: " + codeManager.getActiveCodeCount());

			System.out.println("\nOptions:");
			System.out.println(" [1] - Generate New Codes");
			System.out.println(" [2] - View All Codes");
			System.out.println(" [3] - Delete Code");
			System.out.println(" [4] - Back to Admin Dashboard");
			System.out.print(" " + Colorable.BLUE + "[Enter your choice]: " + Colorable.RESET);

			String choice = scanner.nextLine();

			if (choice.equals("1")) {
				doGenerateCodes();
			}

			else if (choice.equals("2")) {
				doViewAllCodes();
			}

			else if (choice.equals("3")) {
				doDeleteCode();
			}

			else if (choice.equals("4")) {
				break;
			}

			else {
				AlertManager.setError("Invalid choice. Please try again.");
			}
		}
	}

	private static void doGenerateCodes() {
		System.out.println("\n--- Generate New Codes ---");

		try {
			System.out.print("Enter gold value for each code (e.g., 100): ");
			double goldValue = Double.parseDouble(scanner.nextLine().trim());

			System.out.print("Enter quantity to generate (e.g., 10): ");
			int quantity = Integer.parseInt(scanner.nextLine().trim());

			if (goldValue <= 0 || quantity <= 0) {
				AlertManager.setError("Value and quantity must be positive numbers.");
				return;
			}

			int created = codeManager.generateCodes(quantity, goldValue);

			AlertManager.setSuccess("Successfully generated " + created + " new codes.");

		} catch (NumberFormatException e) {
			AlertManager.setError("Invalid number. Please enter numeric values.");
		}
	}

	private static void doViewAllCodes() {
		Main.clearScreen();
		AdminMenu.displayAdminNavbar();
		System.out.println("\n--- All Codes ---");
		System.out.println("[VALUE]\t\t[CODE STRING]\t\t[USED STATUS]");
		System.out.println("---------------------------------------------");

		int listCount = 0;
		for (RedeemCode code : codeManager.getAllCodes()) {
			String status = code.isUsed() ? Colorable.RED + "USED" + Colorable.RESET
					: Colorable.GREEN + "ACTIVE" + Colorable.RESET;
			System.out.printf("%s%.2f G%s\t[%s]\t%s\n", Colorable.YELLOW, code.getGoldValue(), Colorable.RESET,
					code.getCodeString(), status);
			listCount++;
		}

		if (codeManager.getAllCodes().isEmpty()) {
			System.out.println("No codes found in the system.");
		}

		Main.fillUpList(20, listCount, "");

		System.out.println("\n(Press Enter to go back to the dashboard)");
		scanner.nextLine();
	}

	private static void doDeleteCode() {
		System.out.println("\n--- Delete Code ---");
		System.out.print("Enter the exact code string to delete: ");
		String codeString = scanner.nextLine().trim();

		if (codeManager.deleteCode(codeString)) {
			AlertManager.setSuccess("Code '" + codeString + "' was deleted successfully.");
		}

		else {
			AlertManager.setError("Code '" + codeString + "' not found.");
		}
	}
}