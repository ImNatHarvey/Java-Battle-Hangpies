package views;

import static main.Main.codeManager;
import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import controllers.AlertManager;
import controllers.LogManager;
import interfaces.Colorable;
import main.Main;

public class Wallet
{
	public static void showWalletMenu()
	{
		while (true)
		{
			Main.clearScreen();
			
			System.out.println("   _________________________________________________________________________________________________________________________________________________________________ \n"
							 + "  |                                                                                                                                                                 |");
			System.out.println("  |  " + Colorable.YELLOW + "░█▄█░█░█░░░█░█░█▀█░█░░░█░░░█▀▀░▀█▀" + Colorable.RESET + "                                                                                                                             |\n"
							 + "  |  " + Colorable.YELLOW + "░█░█░░█░░░░█▄█░█▀█░█░░░█░░░█▀▀░░█░" + Colorable.RESET + "                                                                                                                             |\n"
							 + "  |  " + Colorable.YELLOW + "░▀░▀░░▀░░░░▀░▀░▀░▀░▀▀▀░▀▀▀░▀▀▀░░▀░" + Colorable.RESET + "                                                                                                                             |\n"
							 + "  |_________________________________________________________________________________________________________________________________________________________________|\n");
			
			showRedeemHistory();
			System.out.println("\n  " + AlertManager.getAndClearAlert());
			System.out.println(Colorable.YELLOW + "\n  Current Gold Balance: " + currentUser.getGoldBalance() + "G\n" + Colorable.RESET);
			System.out.println("  [Options]:");
			System.out.println("  [1] - Redeem a Code");
			System.out.println("  [2] - Back to Dashboard");
			System.out.print("\n  " + Colorable.BLUE + "[Enter your choice]: " + Colorable.RESET);

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doRedeemCode();
			}

			else if (choice.equals("2"))
			{
				break;
			}

			else
			{
				AlertManager.setError("Invalid choice.");
			}
		}
	}

	private static void doRedeemCode()
	{
		System.out.print(Colorable.YELLOW + "\n  [Enter your 12-character code (e.g., AAAA-BBBB-CCCC)]: " + Colorable.RESET);
		String codeString = scanner.nextLine().trim();

		// This is used to check the code's status andd get the code's goldValue 
		double goldValue = codeManager.redeemCode(codeString);

		// IF the code does not found or if the code is used, it will return -1 to goldValue

		// IF goldValue is greater than 0
		if (goldValue > 0)
		{
			currentUser.addGold(goldValue);
			userManager.updateUser(currentUser);
			
			AlertManager.setSuccess("Success! " + goldValue + "G has been added to your wallet.");

			// Log the user's activity
			String logMsg = "Redeemed code " + codeString + " for " + goldValue + "G.";
			LogManager.log(currentUser.getUsername(), logMsg);
		}

		// This means goldValue has -1 value
		else
		{
			// Set error message 
			AlertManager.setError("That code is invalid or has already been used.");
		}
	}
	
	private static void showRedeemHistory()
	{
		List<String> redeemHistory = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader("activity_log.txt")))
		{
			String line;
			
			while ((line = reader.readLine()) != null)
			{
				if (line.contains("| " + currentUser.getUsername() + " |") && line.contains("Redeemed code"))
				{
					redeemHistory.add(line);
				}
			}
		}
		
		catch (IOException e)
		{
			AlertManager.setError("Error reading activity log: " + e.getMessage());
		}
		
		if (redeemHistory.isEmpty())
		{
			System.out.println("   You have no redemed code yet.");
		}
		
		else
		{
			// Show the newest logs first
			System.out.println(Colorable.YELLOW + "   [REDEEMED CODE HISTORY]\n" + Colorable.RESET);
			
			for (int i = redeemHistory.size() - 1; i >= 0; i--)
			{
				System.out.println("   " + redeemHistory.get(i));
			}
		}
		
		Main.fillUpList(16, redeemHistory.size(), "");
	}
}