package views;

import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;

import java.util.Collection;

import models.User;

public class UserManagement
{
	public static void showUserManagement()
	{
		while (true)
		{
			System.out.println("\n--- User Management ---");
			System.out.println("1. View All Users");
			System.out.println("2. Delete User");
			System.out.println("3. Update User");
			System.out.println("4. Back to Admin Dashboard");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doViewAllUsers();
			}

			else if (choice.equals("2"))
			{
				doDeleteUser();
			}

			else if (choice.equals("3"))
			{
				doUpdateUser();
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

	private static void doViewAllUsers()
	{
		System.out.println("\n--- All Registered Users ---");
		System.out.println("[Username]\t[Full Name]\t\t[Contact]\t[Is Admin]");
		System.out.println("-----------------------------------------------------------------");

		Collection<User> users = userManager.getAllUsers();

		if (users.isEmpty())
		{
			System.out.println("No users found in the system.");
		}

		else
		{
			for (User user : users)
			{
				System.out.printf("%-15s\t%-20s\t%-15s\t%-5s\n",
						user.getUsername(),
						user.getFirstName() + " " + user.getLastName(),
						user.getContactNum(),
						user.isAdmin());
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
			System.out.println("Error: User '" + username + "' not found.");
			return;
		}

		System.out.println("Updating user: " + user.getUsername());
		System.out.println("(Press Enter to keep the current value)");

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
				System.out.println("Invalid contact number format. Skipping update.");
			}
		}

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
				System.out.println("Error: You cannot remove your own admin status.");
			}

			else
			{
				user.setAdmin(false);
			}
		}

		userManager.updateUser(user);
		System.out.println("User updated successfully!");
	}

	private static void doDeleteUser()
	{
		System.out.println("\n--- Delete User ---");
		System.out.print("Enter the username of the user to delete: ");
		String username = scanner.nextLine().trim();

		int result = userManager.deleteUser(username, currentUser);

		if (result == 1)
		{
			System.out.println("User '" + username + "' was deleted successfully.");
		}

		else if (result == 0)
		{
			System.out.println("Error: User '" + username + "' not found.");
		}

		else if (result == -1)
		{
			System.out.println("Error: You cannot delete your own admin account.");
		}
	}
}
