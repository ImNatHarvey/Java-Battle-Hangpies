package views;

import static main.Main.scanner;
import static main.Main.userManager;

import models.User;

public class SignUp
{
	public static void doSignUp()
	{
		System.out.println("\n--- Create Account ---");

		// Add restrictions here
		// This fields must be filled by the users
		System.out.print("Enter First Name: ");
		String firstName = scanner.nextLine();

		System.out.print("Enter Last Name: ");
		String lastName = scanner.nextLine();

		String contactNum;

		while (true)
		{
			System.out.print("Enter Contact Number (must be 11 digits): ");
			contactNum = scanner.nextLine().trim();

			if (contactNum.isEmpty())
			{
				System.out.println("Error: Contact number cannot be empty.");
			}

			else if (!contactNum.matches("[0-9]+"))
			{
				System.out.println("Error: Contact number must contain only numbers.");
			}

			else if (contactNum.length() != 11)
			{
				System.out.println("Error: Contact number must be exactly 11 digits.");
			}

			else
			{
				break;
			}
		}

		String username;

		while(true)
		{
			System.out.print("Enter new username (must be at least 8 characters): ");
			username = scanner.nextLine().trim();

			if (username.isEmpty())
			{
				System.out.println("Error: Username cannot be empty.");
			}

			else if (username.contains("|"))	// To be edit, need to add more restrictions
			{
				System.out.println("Error: Username cannot contain the '|' character.");
			}

			else if (username.length() < 8)
			{
				System.out.println("Error: Username must be at least 8 characters long.");
			}

			else
			{
				break;
			}
		}

		String password;

		while (true)
		{
			System.out.print("Enter new password (must be at least 8 characters): ");
			password = scanner.nextLine().trim();

			if (password.isEmpty())
			{
				System.out.println("Error: Password cannot be empty.");
			}

			else if (password.length() < 8)
			{
				System.out.println("Error: Password must be at least 8 characters long.");
			}

			else
			{
				break;
			}
		}

		User newUser = new User(username, password, false, firstName, lastName, contactNum);

		boolean isCreated = userManager.createAccount(newUser);

		if (isCreated)
		{
			System.out.println("Account created successfully!");
			System.out.println("You may now log in.");
		}
		else
		{
			System.out.println("Error: That username is already taken. Please try again.");
		}
	}
}
