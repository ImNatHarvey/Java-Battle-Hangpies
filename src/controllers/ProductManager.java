package controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import models.Hangpie;

public class ProductManager
{
	private Map<String, Hangpie> productMap;
	private String databaseFile = "products.txt";

	public ProductManager()
	{
		this.productMap = new HashMap<>();
		loadProducts();
	}

	private void loadProducts()
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(databaseFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("//") || line.trim().isEmpty())
				{
					continue;
				}

				String[] parts = line.split("\\|");

				if (parts.length < 6)
				{
					continue;
				}

				try
				{
					String id = parts[0];
					String name = parts[1];
					String description = parts[2];
					double price = Double.parseDouble(parts[3]);
					int maxHealth = Integer.parseInt(parts[4]);
					int level = Integer.parseInt(parts[5]);
					int attackPower = Integer.parseInt(parts[6]);					

					Hangpie product = new Hangpie(id, name, description, price, maxHealth, level, attackPower);
					productMap.put(id, product);

				}
				catch (NumberFormatException e)
				{
					System.err.println("[Warning]: Bad data in products.txt for line: " + line);
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Error loading product database: " + e.getMessage());
		}
	}

	private void saveProducts()
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseFile)))
		{
			writer.write("// FORMAT: hangpieID|name|description|price|maxHealth|level|attackPower");
			writer.newLine();

			for (Hangpie product : productMap.values())
			{
				String line = String.join("|",
						product.getId(),
						product.getName(),
						product.getDescription(),
						String.valueOf(product.getPrice()),
						String.valueOf(product.getMaxHealth()),
						String.valueOf(product.getLevel()),
						String.valueOf(product.getAttackPower()));
				
				writer.write(line);
				writer.newLine();
			}
		}

		catch (IOException e)
		{
			System.err.println("CRITICAL ERROR: Could not save product database: " + e.getMessage());
		}
	}
	
	public Collection<Hangpie> getAllProducts()
	{
		return productMap.values();
	}
	
	public boolean addProduct(Hangpie product)
	{
		if (productMap.containsKey(product.getId()))
		{
			return false;
		}
		
		productMap.put(product.getId(), product);
		saveProducts();
		return true;
	}
	
	public void updateProduct(Hangpie product)
	{
		productMap.put(product.getId(), product);
		saveProducts();
	}
	
	public boolean deleteProduct(String id)
	{
		Hangpie removed = productMap.remove(id);
		if (removed != null)
		{
			saveProducts();
			return true;
		}
		return false;
	}
	
	public Hangpie getProductById(String id)
	{
		return productMap.get(id);
	}
	
	public int getProductCount()
	{
        return productMap.size();
    }
}
