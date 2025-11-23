package views;

import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;

import java.util.Collection;

import controllers.AlertManager;
import controllers.LogManager;
import interfaces.Colorable;
import main.Main;
import models.User;

public class UserManagement implements Colorable
{
	public static void showUserManagement()
	{
		while (true)
		{
			Main.clearScreen();
			AdminMenu.displayAdminNavbar();
			
			System.out.println("\n--- User Management ---");
			System.out.println("  [Username]\t[Full Name]\t\t[Contact]\t[Is Admin]\t[Status]");
			System.out.println("  ----------------------------------------------------------------------------------");

			Collection<User> users = userManager.getAllUsers();
			int listCount = 0;

			if (users.isEmpty())
			{
				System.out.println("No users found in the system.");
			}

			else
			{
				for (User user : users)
				{
					String adminStatus = user.isAdmin() ? Colorable.GREEN + "TRUE" + Colorable.RESET : Colorable.RED + "FALSE" + Colorable.RESET;
					String bannedStatus = user.isBanned() ? Colorable.RED + "BANNED" + Colorable.RESET : Colorable.GREEN + "ACTIVE" + Colorable.RESET;
					
					System.out.printf("  %-15s\t%-20s\t%-15s\t%-5s\t\t%s\n",
							user.getUsername(),
							user.getFirstName() + " " + user.getLastName(),
							user.getContactNum(),
							adminStatus,
							bannedStatus);
					listCount++;
				}
			}
			
			Main.fillUpList(10, listCount, "  ");
			
			System.out.println("\n" + AlertManager.getAndClearAlert());

			System.out.println("\nOptions:");
			System.out.println(" [1] - Update User Details/Status");
			System.out.println(" [2] - Delete User");
			System.out.println(" [3] - Back to Admin Dashboard");
			System.out.print(" " + Colorable.BLUE + "[Enter your choice]: " + Colorable.RESET);

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doUpdateUser();
			}

			else if (choice.equals("2"))
			{
				doDeleteUser();
			}

			else if (choice.equals("3"))
			{
				break;
			}

			else
			{
				AlertManager.setError("Invalid choice. Please try again.");
			}    
		}
	}

	private static void doUpdateUser()
	{
		System.out.println("\n--- Update User ---");
		System.out.print("Enter the username of the user to update: ");
		String username = scanner.nextLine().trim();

		User user = userManager.getUserByUsername(username);

		if (user == null)
		{
			AlertManager.setError("User '" + username + "' not found.");
			return;
		}

		System.out.println("Updating user: " + user.getUsername());
		System.out.println("(Press Enter to keep the current value)");

		// Update Contact
		System.out.print("Enter new Contact Number (current: " + user.getContactNum() + "): ");
		String newContact = scanner.nextLine().trim();

		if (!newContact.isEmpty())
		{
			if (newContact.length() == 11 && newContact.matches("[0-9]+"))
			{
				user.setContactNum(newContact);
			}

			else
			{
				AlertManager.setError("Invalid contact number format. Skipping update.");
			}
		}

		// Update Admin Status
		System.out.print("Make this user an admin? (yes/no) (current: " + user.isAdmin() + "): ");
		String adminChoice = scanner.nextLine().trim();

		if (adminChoice.equalsIgnoreCase("yes"))
		{
			user.setAdmin(true);
		}

		else if (adminChoice.equalsIgnoreCase("no"))
		{

			if (user.isAdmin() && user.getUsername().equals(currentUser.getUsername()))
			{
				AlertManager.setError("Error: You cannot remove your own admin status.");
			}

			else
			{
				user.setAdmin(false);
			}
		}
		
		// Update Banned Status
		System.out.print("Ban this user? (yes/no) (current: " + user.isBanned() + "): ");
		String banChoice = scanner.nextLine().trim();
		
		if (banChoice.equalsIgnoreCase("yes"))
		{
			if (user.getUsername().equals(currentUser.getUsername()))
			{
				AlertManager.setError("Error: You cannot ban yourself.");
			}
			else
			{
				user.setStatus(true);
			}
		}
		
		else if (banChoice.equalsIgnoreCase("no"))
		{
			user.setStatus(false);
		}

		userManager.updateUser(user);
		AlertManager.setSuccess("User updated successfully!");
		
		String logMsg = "Updated user: " + user.getUsername();
		LogManager.log(currentUser.getUsername(), logMsg);
	}

	private static void doDeleteUser()
	{
		System.out.println("\n--- Delete User ---");
		System.out.print("Enter the username of the user to delete: ");
		String username = scanner.nextLine().trim();

		int result = userManager.deleteUser(username, currentUser);

		if (result == 1)
		{
			AlertManager.setSuccess("User '" + username + "' was deleted successfully.");
		}

		else if (result == 0)
		{
			AlertManager.setError("User '" + username + "' not found.");
		}

		else if (result == -1)
		{
			AlertManager.setError("You cannot delete your own admin account.");
		}
		
		String logMsg = "Attempted to delete user: " + username + ". Result: " + result;
		LogManager.log(currentUser.getUsername(), logMsg);
	}
}