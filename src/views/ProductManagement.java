package views;

import static main.Main.productManager;
import static main.Main.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import controllers.AlertManager;
import interfaces.Colorable;
import main.Main;
import models.Hangpie;

public class ProductManagement implements Colorable {
	public static void showProductManagement() {
		while (true) {
			Main.clearScreen();
			AdminMenu.displayAdminNavbar(); // Re-use the admin navbar for consistency
			System.out.println("\n--- Product Management ---");
			System.out.println("Current Products in Shop:");
			System.out.println(" [PODUCT ID]\t[NAME]\t\t\t[STATS]\t\t\t[PRICE]\t\t\t[DESCRIPTION]");

			List<Hangpie> productList = new ArrayList<>(productManager.getAllProducts());

			Collections.sort(productList);
			int listCount = 0;

			for (Hangpie product : productList) {
				System.out.println("  - " + product.toString());
				listCount++;
			}

			Main.fillUpList(10, listCount, "");

			System.out.println("\n" + AlertManager.getAndClearAlert());

			System.out.println("\nOptions:");
			System.out.println(" [1] - Add New Product");
			System.out.println(" [2] - Update Product");
			System.out.println(" [3] - Delete Product");
			System.out.println(" [4] - Back to Admin Dashboard");
			System.out.print(" " + Colorable.BLUE + "[Enter your choice]: " + Colorable.RESET);

			String choice = scanner.nextLine();

			if (choice.equals("1")) {
				doAddProduct();
			}

			else if (choice.equals("2")) {
				doUpdateProduct();
			}

			else if (choice.equals("3")) {
				doDeleteProduct();
			}

			else if (choice.equals("4")) {
				break;
			}

			else {
				AlertManager.setError("Invalid choice. Please try again.");
			}
		}
	}

	private static void doAddProduct() {
		System.out.println("\n--- Add New Product ---");
		try {
			System.out.print("Enter Product ID (e.g., HP-004): ");
			String id = scanner.nextLine().trim();

			System.out.print("Enter Name: ");
			String name = scanner.nextLine().trim();

			System.out.print("Enter Description: ");
			String description = scanner.nextLine().trim();

			System.out.print("Enter Image Folder Name (e.g., evil_wizard): ");
			String imageName = scanner.nextLine().trim();

			System.out.print("Enter Price (e.g., 300): ");
			double price = Double.parseDouble(scanner.nextLine().trim());

			System.out.print("Enter Max Health (e.g., 60): ");
			int maxHealth = Integer.parseInt(scanner.nextLine().trim());

			System.out.print("Enter Starting Level (e.g., 1): ");
			int level = Integer.parseInt(scanner.nextLine().trim());

			System.out.print("Enter Attack Power (e.g., 10): ");
			int attackPower = Integer.parseInt(scanner.nextLine().trim());

			Hangpie newProduct = new Hangpie(id, name, description, price, maxHealth, level, attackPower, imageName);

			if (productManager.addProduct(newProduct)) {
				AlertManager.setSuccess("Product added successfully!");
			} else {
				AlertManager.setError("A product with ID '" + id + "' already exists.");
			}
		} catch (NumberFormatException e) {
			AlertManager.setError("Invalid number. Price, Health, Level, and Attack must be numbers.");
		}
	}

	private static void doUpdateProduct() {
		System.out.println("\n--- Update Product ---");
		System.out.print("Enter the ID of the product to update (e.g., HP-001): ");
		String id = scanner.nextLine().trim();

		Hangpie product = productManager.getProductById(id);

		if (product == null) {
			AlertManager.setError("Product ID '" + id + "' not found.");
			return;
		}

		System.out.println("Updating product: " + product.getName());
		System.out.println("(Press Enter to keep the current value)");

		try {
			System.out.print("Enter new Name (current: " + product.getName() + "): ");
			String newName = scanner.nextLine().trim();

			if (!newName.isEmpty()) {
				product.setName(newName);
			}

			System.out.print("Enter new Description (current: " + product.getDescription() + "): ");
			String newDesc = scanner.nextLine().trim();

			if (!newDesc.isEmpty()) {
				product.setDescription(newDesc);
			}

			System.out.print("Enter new Image Folder Name (current: " + product.getImageName() + "): ");
			String newImageName = scanner.nextLine().trim();

			if (!newImageName.isEmpty()) {
				product.setImageName(newImageName);
			}

			System.out.print("Enter new Price (current: " + product.getPrice() + "): ");
			String newPrice = scanner.nextLine().trim();

			if (!newPrice.isEmpty()) {
				product.setPrice(Double.parseDouble(newPrice));
			}

			System.out.print("Enter new Max Health (current: " + product.getMaxHealth() + "): ");
			String newMaxHealth = scanner.nextLine().trim();

			if (!newMaxHealth.isEmpty()) {
				product.setMaxHealth(Integer.parseInt(newMaxHealth));
			}

			System.out.print("Enter new Attack Power (current: " + product.getAttackPower() + "): ");
			String newAtkPower = scanner.nextLine();

			if (!newAtkPower.isEmpty()) {
				product.setAttackPower(Integer.parseInt(newAtkPower));
			}

			productManager.updateProduct(product);
			AlertManager.setSuccess("Product updated successfully!");

		} catch (NumberFormatException e) {
			AlertManager.setError("Invalid number. Price, Health, Level, and Attack must be numbers.");
		}

	}

	private static void doDeleteProduct() {
		System.out.println("\n--- Delete Product ---");
		System.out.print("Enter Product ID to delete: ");
		String id = scanner.nextLine().trim();

		if (productManager.deleteProduct(id)) {
			AlertManager.setSuccess("Product '" + id + "' deleted successfully.");
		} else {
			AlertManager.setError("Product ID '" + id + "' not found.");
		}
	}
}