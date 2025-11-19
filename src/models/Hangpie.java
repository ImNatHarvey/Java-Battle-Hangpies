package models;

import java.util.UUID;

public class Hangpie extends Character implements Comparable<Hangpie>
{
	private String uniqueId;		// UUID used for Marketplace
	private String productId;		// Product ID used exclusively in Shop
	private String description;		// Hangpie's description
	private double price;			// For the marketplace
	private String imageName;       // The filename of the image (e.g., dragon.png)

	public Hangpie(String id, String name, String description, double price, int maxHealth, int level, int attackPower, String imageName)
	{
		super(name, maxHealth, level, attackPower);	// Calls the parent (Character) constructor using "super" keyword
		this.productId = id;
		this.description = description;
		this.price = price;
		this.imageName = imageName;
	}

	/* This is a Copy constructor.
	 * It serves as the local copy of products of the user.
	 * Its purpose is to store owned products to the Inventory.
	 * Having a local copy of products can allow the user to modify their Hangpie's data without affecting the Marketplace Product data. 
	 * This is used to prevent data manipulation to the product list.
	 */
	public Hangpie (Hangpie localCopy)
	{
		// super keyword for the Character constructor
		super(localCopy.getName(), localCopy.getMaxHealth(), localCopy.getLevel(), localCopy.getAttackPower());
		
		// Generate random UUID. Used UUID Class
		this.uniqueId = UUID.randomUUID().toString();
		
		this.productId = localCopy.productId;
		this.description = localCopy.description;
		this.price = localCopy.price;
		this.imageName = localCopy.imageName;
	}
	
	@Override
	// This method is used to display this object to the marketplace easily
	public String toString()
	{
		return String.format("[%s]\t%s\t\tLvl:%d\t(HP:%d, Atk:%d)\t[Img: %s]\t[Price: %.2fG]\t%s", 
				productId, name, level, maxHealth, attackPower, imageName, price, description); 
	}
	
	@Override
	// This is a sorting method used to sort out Products by its Product ID
	// It can be used to sort the Product list to ascending or descending
	public int compareTo(Hangpie other)
	{
		return this.productId.compareTo(other.getId());
	}

	// Getter Methods
	public String getUniqueId()
	{
		return uniqueId;
	}
	
	public String getId()
	{
		return productId;
	}

	public double getPrice()
	{
		return price;
	}

	public String getDescription()
	{
		return description;
	}
	
	public String getImageName()
	{
		return imageName;
	}


	// Setter Method
	public void setUniqueId(String uniqueId)
	{
		this.uniqueId = uniqueId;
	}
	
	public void setPrice(double price)
	{
		this.price = price;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public void setImageName(String imageName)
	{
		this.imageName = imageName;
	}
}