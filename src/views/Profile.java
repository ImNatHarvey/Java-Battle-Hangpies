package views;

import static main.Main.currentUser;
import static main.Main.scanner;
import static main.Main.userManager;

public class Profile
{
	public static void showProfile()
	{
		while (true)
		{
			System.out.println("\n--- My Profile ---");
			System.out.println("Username: " + currentUser.getUsername() + "(Permanent)");			
			System.out.println("First Name: " + currentUser.getFirstName());
			System.out.println("Last Name: " + currentUser.getLastName());
			System.out.println("Contact Number: " + currentUser.getContactNum());

			System.out.println("\nOptions:");
			System.out.println("1. Update Profile Information");
			System.out.println("2. Change Password");
			System.out.println("3. Go back to dashboard");

			System.out.print("Enter your choice: ");
			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doUpdateProfile();
			}

			else if (choice.equals("2"))
			{
				doChangePassword();
			}

			else if (choice.equals("3"))
			{
				break;
			}

			else
			{
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void doUpdateProfile()
	{

		System.out.println("\n--- Update User ---");
		System.out.println("(Press Enter to keep the current value)");

		System.out.print("Enter new First Name (current: " + currentUser.getFirstName() + "): ");
		String newFirstName = scanner.nextLine().trim();

		if (!newFirstName.isEmpty())
		{
			if (newFirstName.matches("^[a-zA-Z ]+$"))
			{
				currentUser.setFirstName(newFirstName);
			}

			else
			{
				System.out.println("Invalid name format. Skipping update.");
			}
		}

		System.out.print("Enter new Last Name (current: " + currentUser.getLastName() + "): ");
		String newLastName = scanner.nextLine().trim();

		if (!newLastName.isEmpty())
		{
			if (newLastName.matches("^[a-zA-Z ]+$"))
			{
				currentUser.setLastName(newLastName);
			}

			else
			{
				System.out.println("Invalid name format. Skipping update.");
			}
		}

		System.out.print("Enter new Contact Number (current: " + currentUser.getContactNum() + "): ");
		String newContact = scanner.nextLine().trim();

		if (!newContact.isEmpty())
		{
			if (newContact.length() == 11 && newContact.matches("[0-9]+"))
			{
				currentUser.setContactNum(newContact);
			}

			else
			{
				System.out.println("Invalid contact number format. Skipping update.");
			}
		}

		userManager.updateUser(currentUser);
		System.out.println("Profile updated successfully!");
	}

	private static void doChangePassword()
	{
		System.out.println("\n--- Change Password ---");
		System.out.print("Enter your CURRENT password to verify: ");
		String oldPassword = scanner.nextLine();

		if (!currentUser.getPassword().equals(oldPassword))
		{
			System.out.println("Error: Incorrect password. Password not changed.");
			return;
		}

		String newPassword;

		while (true)
		{
			System.out.print("Enter new password (must be at least 8 characters): ");
			newPassword = scanner.nextLine().trim();

			if (newPassword.isEmpty())
			{
				System.out.println("Error: Password cannot be empty.");
			}

			else if (newPassword.length() < 8)
			{
				System.out.println("Error: Password must be at least 8 characters long.");
			}

			else
			{
				break;
			}
		}

		currentUser.setPassword(newPassword);
		userManager.updateUser(currentUser);

		System.out.println("Password changed successfully!");
	}
}