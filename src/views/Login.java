package views;

import static main.Main.scanner;
import static main.Main.userManager;
import static main.Main.currentUser;

import models.User;
import controllers.LogManager;

public class Login
{
	public static void doLogin()
	{
		System.out.println("\n--- Log In ---");
		System.out.print("Enter username: ");
		String username = scanner.nextLine();

		System.out.print("Enter password: ");
		String password = scanner.nextLine();

		User user = userManager.login(username, password);

		if (user != null)
		{
			System.out.println("Login successful! Welcome, " + user.getFirstName());
			currentUser = user;
			
			// Added to the Activity Log
			LogManager.log(currentUser.getUsername(), "Logged in successfully.");
		}
		else
		{
			System.out.println("Login failed. Invalid username or password.");
		}
	}
}
