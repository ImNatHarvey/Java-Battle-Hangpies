package views;

import static main.Main.codeManager;
import static main.Main.scanner;

import models.RedeemCode;

public class CodeManagement
{
	public static void showCodeManagement()
	{
		while (true)
		{
			System.out.println("\n--- Code Management ---");
			System.out.println("1. Generate New Codes");
			System.out.println("2. View All Codes");
			System.out.println("3. Delete Code");
			System.out.println("4. Back to Admin Dashboard");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doGenerateCodes();
			}

			else if (choice.equals("2"))
			{
				doViewAllCodes();
			}

			else if (choice.equals("3"))
			{
				doDeleteCode();
			}

			else if (choice.equals("4"))
			{
				break;
			}

			else
			{
				System.out.println("Invalid choice. Please try again.");
			}
		}
	}

	private static void doGenerateCodes()
	{
		System.out.println("\n--- Generate New Codes ---");

		try
		{
			System.out.print("Enter gold value for each code (e.g., 100): ");
			double goldValue = Double.parseDouble(scanner.nextLine().trim());

			System.out.print("Enter quantity to generate (e.g., 10): ");
			int quantity = Integer.parseInt(scanner.nextLine().trim());

			if (goldValue <= 0 || quantity <= 0)
			{
				System.out.println("Error: Value and quantity must be positive numbers.");
				return;
			}

			int created = codeManager.generateCodes(quantity, goldValue);

			System.out.println("Successfully generated " + created + " new codes.");

		}
		catch (NumberFormatException e)
		{
			System.err.println("Error: Invalid number. Please enter numeric values.");
		}
	}

	private static void doViewAllCodes()
	{
		System.out.println("\n--- All Codes ---");

		for (RedeemCode code : codeManager.getAllCodes())
		{
			System.out.println("  - " + code.toString());
		}

		if (codeManager.getAllCodes().isEmpty())
		{
			System.out.println("No codes found in the system.");
		}
	}

	private static void doDeleteCode()
	{
		System.out.println("\n--- Delete Code ---");
		System.out.print("Enter the exact code string to delete: ");
		String codeString = scanner.nextLine().trim();

		if (codeManager.deleteCode(codeString))
		{
			System.out.println("Code '" + codeString + "' was deleted successfully.");
		}

		else
		{
			System.out.println("Error: Code '" + codeString + "' not found.");
		}
	}
}
