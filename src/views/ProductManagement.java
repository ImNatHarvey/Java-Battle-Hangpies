package views;

import static main.Main.productManager;
import static main.Main.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.GameConstants;
import models.Hangpie;

public class ProductManagement
{
	public static void showProductManagement()
	{
		while (true)
		{
			System.out.println("\n--- Product Management ---");
			System.out.println("Current Products in Marketplace:");
			System.out.println(" [PODUCT ID]\t[NAME]\t\t\t[STATS]\t\t\t[IMG]\t\t[PRICE]\t\t\t[DESCRIPTION]");

			List<Hangpie> productList = new ArrayList<>(productManager.getAllProducts());

			Collections.sort(productList);

			for (Hangpie product : productList)
			{
				System.out.println("  - " + product.toString());
			}

			System.out.println("\nOptions:");
			System.out.println("1. Add New Product");
			System.out.println("2. Update Product");
			System.out.println("3. Delete Product");
			System.out.println("4. Back to Admin Dashboard");
			System.out.print("Enter your choice: ");

			String choice = scanner.nextLine();

			if (choice.equals("1"))
			{
				doAddProduct();
			}

			else if (choice.equals("2"))
			{
				doUpdateProduct();
			}

			else if (choice.equals("3"))
			{
				doDeleteProduct();
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

	private static void doAddProduct()
	{
		System.out.println("\n--- Add New Product ---");
		try
		{
			System.out.print("Enter Product ID (e.g., HP-004): ");
			String id = scanner.nextLine().trim();

			System.out.print("Enter Name: ");
			String name = scanner.nextLine().trim();

			System.out.print("Enter Description: ");
			String description = scanner.nextLine().trim();

			System.out.print("Enter Price (e.g., 300): ");
			double price = Double.parseDouble(scanner.nextLine().trim());

			System.out.print("Enter Max Health (e.g., 60): ");
			int maxHealth = Integer.parseInt(scanner.nextLine().trim());

			System.out.print("Enter Starting Level (e.g., 1): ");
			int level = Integer.parseInt(scanner.nextLine().trim());

			System.out.print("Enter Attack Power (e.g., 10): ");
			int attackPower = Integer.parseInt(scanner.nextLine().trim());
			
			// New Image Selection
			String imageName = selectImageFile();

			Hangpie newProduct = new Hangpie(id, name, description, price, maxHealth, level, attackPower, imageName);

			if (productManager.addProduct(newProduct))
			{
				System.out.println("Product added successfully!");
			}
			else
			{
				System.out.println("Error: A product with ID '" + id + "' already exists.");
			}
		}
		catch (NumberFormatException e)
		{
			System.err.println("Error: Invalid number. Price, Health, Level, and Attack must be numbers.");
		}
	}

	private static void doUpdateProduct()
	{
		System.out.println("\n--- Update Product ---");
		System.out.print("Enter the ID of the product to update (e.g., HP-001): ");
		String id = scanner.nextLine().trim();

		Hangpie product = productManager.getProductById(id);

		if (product == null)
		{
			System.out.println("Error: Product ID '" + id + "' not found.");
			return;
		}

		System.out.println("Updating product: " + product.getName());
		System.out.println("(Press Enter to keep the current value)");

		try
		{
			System.out.print("Enter new Name (current: " + product.getName() + "): ");
			String newName = scanner.nextLine().trim();

			if (!newName.isEmpty())
			{
				product.setName(newName);
			}

			System.out.print("Enter new Description (current: " + product.getDescription() + "): ");
			String newDesc = scanner.nextLine().trim();

			if (!newDesc.isEmpty())
			{
				product.setDescription(newDesc);
			}

			System.out.print("Enter new Price (current: " + product.getPrice() + "): ");
			String newPrice = scanner.nextLine().trim();

			if (!newPrice.isEmpty())
			{
				product.setPrice(Double.parseDouble(newPrice));
			}

			System.out.print("Enter new Max Health (current: " + product.getMaxHealth() + "): ");
			String newMaxHealth = scanner.nextLine().trim();

			if (!newMaxHealth.isEmpty())
			{
				product.setMaxHealth(Integer.parseInt(newMaxHealth));
			}

			System.out.print("Enter new Attack Power (current: " + product.getAttackPower() + "): ");
			String newAtkPower = scanner.nextLine();

			if (!newAtkPower.isEmpty())
			{
				product.setAttackPower(Integer.parseInt(newAtkPower));
			}
			
			System.out.print("Update Image? (yes/no) (current: " + product.getImageName() + "): ");
			String updateImg = scanner.nextLine().trim();
			if (updateImg.equalsIgnoreCase("yes")) {
				String newImage = selectImageFile();
				product.setImageName(newImage);
			}

			productManager.updateProduct(product);
			System.out.println("Product updated successfully!");

		}
		catch (NumberFormatException e)
		{
			System.err.println("Error: Invalid number. Price, Health, Level, and Attack must be numbers.");
		}
	}

	private static void doDeleteProduct()
	{
		System.out.println("\n--- Delete Product ---");
		System.out.print("Enter Product ID to delete: ");
		String id = scanner.nextLine().trim();

		if (productManager.deleteProduct(id))
		{
			System.out.println("Product '" + id + "' deleted successfully.");
		}
		else
		{
			System.out.println("Error: Product ID '" + id + "' not found.");
		}
	}
	
	/**
	 * Scans the images/hangpies folder and asks the user to select one.
	 * @return The filename of the selected image.
	 */
	private static String selectImageFile() {
		System.out.println("\n--- Select Hangpie Image ---");
		
		File folder = new File(GameConstants.HANGPIE_DIR);
		File[] fileList = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
		
		if (fileList == null || fileList.length == 0) {
			System.out.println("No images found in " + GameConstants.HANGPIE_DIR);
			return "default.png";
		}
		
		for (int i = 0; i < fileList.length; i++) {
			System.out.println((i + 1) + ". " + fileList[i].getName());
		}
		
		while (true) {
			System.out.print("Enter number to select image: ");
			try {
				String input = scanner.nextLine().trim();
				int choice = Integer.parseInt(input);
				
				if (choice > 0 && choice <= fileList.length) {
					return fileList[choice - 1].getName();
				} else {
					System.out.println("Invalid number.");
				}
			} catch (NumberFormatException e) {
				System.out.println("Please enter a number.");
			}
		}
	}
}