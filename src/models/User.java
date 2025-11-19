package models;

import java.util.ArrayList;
import java.util.List;

public class User
{
	private String username;		// User's username
	private String password;		// User's password
	private boolean isAdmin;		// Used to define if the user is Admin

	private String firstName;
	private String lastName;
	private String contactNum;

	private double goldBalance;			// User's balance
	private List<Hangpie> inventory;		// Used to store owned Hangpies in a list
	private int worldLevel;				// Track game progress, Serves as Game Data
	private int progressLevel;

	public User(String username, String pasword, boolean isAdmin, String firstName, String lastName, String contactNum)
	{
		this.username = username;
		this.password = pasword;
		this.isAdmin = isAdmin;

		this.firstName = firstName;
		this.lastName = lastName;
		this.contactNum = contactNum;

		this.goldBalance = 0;
		this.inventory = new ArrayList<>();
		this.worldLevel = 1;
		this.progressLevel = 1;
	}

	// Used to add Gold
	public void addGold(double amount)
	{
		this.goldBalance += amount;
	}

	// Used to subtract Gold
	public boolean subtractGold(double amount)
	{
		if (goldBalance >= amount)
		{
			goldBalance -= amount;
			return true;
		}
		return false;
	}

	// Used to add Hangpie to the Inventory
	public void addToInventory(Hangpie hangpie)
	{
		inventory.add(hangpie);
	}
	
	public void removeToInventory(Hangpie hangpie)
	{
		inventory.remove(hangpie);
	}

	// Getter Methods
	public String getUsername()
	{
		return username;
	}

	public String getPassword()
	{
		return password;
	}

	public boolean isAdmin()
	{
		return isAdmin;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public String getContactNum()
	{
		return contactNum;
	}

	public double getGoldBalance()
	{
		return goldBalance;
	}

	public List<Hangpie> getInventory()
	{
		return inventory;
	}

	public int getWorldLevel()
	{
		return worldLevel;
	}

	public int getProgressLevel()
	{
		return progressLevel;
	}


	// Setter Methods
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public void setContactNum(String contactNum)
	{
		this.contactNum = contactNum;
	}

	public void setWorldLevel(int worldLevel)
	{
		this.worldLevel = worldLevel;
	}

	public void setProgressLevel(int progressLevel)
	{
		this.progressLevel = progressLevel;
	}

	public void setAdmin(boolean isAdmin)
	{
		this.isAdmin = isAdmin;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}


}
