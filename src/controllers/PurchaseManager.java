package controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Purchase;

public class PurchaseManager
{
	private String databaseFile = "purchases.txt";

	public void addPurchase(Purchase purchase)
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseFile, true)))
		{	
			writer.write(purchase.toFileString());
			writer.newLine();
		}
		catch (IOException e)
		{
			System.err.println("CRITICAL ERROR: Could not save purchase history: " + e.getMessage());
		}
	}

	public List<Purchase> getPurchasesForUser(String username)
	{
		List<Purchase> userHistory = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(databaseFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{

				if (line.startsWith(username + "|"))
				{
					String[] parts = line.split("\\|");
					if (parts.length < 5)
					{
						continue;
					}

					String name = parts[0];
					String productId =  parts[1];
					String productName = parts[2];
					double pricePaid = Double.parseDouble(parts[3]);
					String timestampStr = parts[4];

					Purchase p = new Purchase(name, productId, productName, pricePaid, timestampStr);

					userHistory.add(p);
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Error loading purchase history: " + e.getMessage());
		}

		return userHistory;
	}

	// Reads ALL purchases from the file.
	private List<Purchase> getAllPurchases()
	{
		List<Purchase> allHistory = new ArrayList<>();

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

				if (parts.length < 5)
				{
					continue;
				}

				String name = parts[0];
				String productId =  parts[1];
				String productName = parts[2];
				double pricePaid = Double.parseDouble(parts[3]);
				String timestampStr = parts[4];

				Purchase p = new Purchase(name, productId, productName, pricePaid, timestampStr);

				allHistory.add(p);
			}
		}

		catch (IOException e)
		{
			System.err.println("Error loading full purchase history: " + e.getMessage());
		}

		return allHistory;
	}

	public List<String> getTopMostBought()
	{
		// 1. Get all purchases
		List<Purchase> allPurchases = getAllPurchases();

		// 2. DSA: Use a HashMap to count
		Map<String, Integer> purchaseCounts = new HashMap<>();

		for (Purchase p : allPurchases)
		{
			String productId = p.getProductId();

			// 'getOrDefault' adds 1 to the count, or starts at 0+1=1 if new
			purchaseCounts.put(productId, purchaseCounts.getOrDefault(productId, 0) + 1);			
		}

		List<String> top3List = new ArrayList<>();

		// 3. Find the Top 3 manually
		String top1_Id = null;
		String top2_Id = null;
		String top3_Id = null;

		// Purchase default count??
		int top1_Count = -1;
		int top2_Count = -1;
		int top3_Count = -1;

		// Loop through all our counts
		for (Map.Entry<String, Integer> entry : purchaseCounts.entrySet())
		{
			String currentId = entry.getKey();
			int currentCount = entry.getValue();

			if (currentCount > top1_Count)
			{
				// This new item is #1. Shift everyone else down.
				top3_Id = top2_Id;
				top3_Count = top2_Count;

				top2_Id = top1_Id;
				top2_Count = top1_Count;

				top1_Id = currentId;
				top1_Count = currentCount;
			}

			else if (currentCount > top2_Count)
			{
				// This new item is #2. Shift #3 down.
                top3_Id = top2_Id;
                top3_Count = top2_Count;

                top2_Id = currentId;
                top2_Count = currentCount;
			}

			else if (currentCount > top3_Count)
			{
				// This new item is #3
                top3_Id = currentId;
                top3_Count = currentCount;
			}
		}
		
		// 4. Add the winners to our final list (if they exist)
        if (top1_Id != null) top3List.add(top1_Id);
        if (top2_Id != null) top3List.add(top2_Id);
        if (top3_Id != null) top3List.add(top3_Id);
        
        return top3List;
	}
}
