package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Purchase
{
	private String username;
	private String productID;
	private String productName;
	private double pricePaid;
	private LocalDateTime timestamp;

	private static DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public Purchase(String username, String productID, String productName, double pricePaid)
	{
		this.username = username;
		this.productID = productID;
		this.productName = productName;
		this.pricePaid = pricePaid;
		this.timestamp = LocalDateTime.now(); 
	}

	// This constructor is for loading from the file
	public Purchase(String username, String productID, String productName, double pricePaid, String timestamp)
	{
		this.username = username;
		this.productID = productID;
		this.productName = productName;
		this.pricePaid = pricePaid;
		this.timestamp = LocalDateTime.parse(timestamp, formatter);
	}

	public String getUsername()
	{
		return username;
	}
	
	public String getProductId()
	{
		return productID;
	}

	// Converts the purchase to a simple line for the text file
	public String toFileString()
	{
		String line = String.join("|", username, productID, productName, String.valueOf(pricePaid), timestamp.format(formatter));
		
		return line;
	}

	@Override
	public String toString()
	{
		// Formatter for a nice, human-readable display
		DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm");
		
		String toString = String.format("[%s] Purchased: %s (ID: %s) for %.2fG", timestamp.format(displayFormat), productName, productID, pricePaid);
		
		return toString;
	}
}
