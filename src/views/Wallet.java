package views;

import static main.Main.codeManager;
import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;

import controllers.LogManager;

public class Wallet
{
	public static void showWalletMenu()
	{
		while (true)
		{
			System.out.println("\n--- My Wallet ---");
			System.out.println("Current Gold Balance: " + currentUser.getGoldBalance() + "G");
			System.out.println("\nOptions:");
			System.out.println("1. Redeem a Code");
			System.out.println("2. Back to Dashboard");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doRedeemCode();
			}

			else if (choice.equals("2"))
			{
				break;
			}

			else {
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void doRedeemCode()
	{
		System.out.println("\n--- Redeem Code ---");
		System.out.print("Enter your 12-character code (e.g., AAAA-BBBB-CCCC): ");
		String codeString = scanner.nextLine().trim();

		double goldValue = codeManager.redeemCode(codeString);

		if (goldValue > 0)
		{
			currentUser.addGold(goldValue);
			userManager.updateUser(currentUser);

			System.out.println("Success! " + goldValue + "G has been added to your wallet.");
			System.out.println("New Balance: " + currentUser.getGoldBalance() + "G");
			
			String logMsg = "Redeemed code " + codeString + " for " + goldValue + "G.";
			LogManager.log(currentUser.getUsername(), logMsg);
		}

		else
		{
			System.out.println("Error: That code is invalid or has already been used.");
		}
	}
}
