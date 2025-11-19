package main;

import java.util.Scanner;

import controllers.AnnouncementManager;
import controllers.CodeManager;
import controllers.ListingManager;
import controllers.ProductManager;
import controllers.PurchaseManager;
import controllers.UserManager;
import models.User;
import views.AdminMenu;
import views.Login;
import views.SignUp;
import views.UserMenu;

public class Main
{
	public static UserManager userManager;
	public static ProductManager productManager;
	public static CodeManager codeManager;
	public static PurchaseManager purchaseManager;
	public static ListingManager listingManager;
	public static AnnouncementManager announcementManager;

	public static Scanner scanner;
	public static User currentUser;

	public static void main(String[] args)
	{
		productManager = new ProductManager();
		codeManager = new CodeManager();
		userManager = new UserManager(productManager);
		purchaseManager = new PurchaseManager();
		listingManager = new ListingManager();
		announcementManager = new AnnouncementManager();

		try (Scanner mainScanner = new Scanner(System.in))
		{
			scanner = mainScanner;

			while(true)
			{
				currentUser = null;

				showLoginMenu();

				String choice = scanner.nextLine();

				if(choice.equals("1"))
				{
					Login.doLogin();
				}
				else if (choice.equals("2"))
				{
					SignUp.doSignUp();
				}
				else if (choice.equals("3"))
				{
					System.out.println("Thank you for playing Battle Hangpies!");
					break;
				}
				else
				{
					System.out.println("Invalid choice. Please enter 1, 2, or 3.");
				}

				if (currentUser != null)
				{
					if (currentUser.isAdmin())
					{
						AdminMenu.showAdminDashboard();
					}
					else
					{
						UserMenu.showUserDashboard();
					}
				}

			}
		}
	}

	private static void showLoginMenu()
	{
		System.out.println("\n--- BATTLE HANGPIES ---");
		System.out.println("Welcome! Please log in or create an account.");
		System.out.println("1. Log In");
		System.out.println("2. Sign Up");
		System.out.println("3. Exit");
		System.out.print("Enter your choice: ");
	}
}
