package models;

// This abstract class is used to define the common attributes and behaviors between various Characters used in the game.
public abstract class Character
{
	// Attributes
	protected String name;				// Character's name
	protected int maxHealth;			// Character's MAX HP
	protected int currentHealth;		// Character's current HP
	protected int level;				// Character's Level
	protected int attackPower;			// Character's Attack Power

	public Character(String name, int maxHealth, int level, int attackPower)
	{
		this.name = name;
		this.maxHealth = maxHealth;
		this.currentHealth = maxHealth;	// At the start of the battle, Characters start at full health
		this.level = level;
		this.attackPower  = attackPower;
	}

	public void takeDamage(int damage)
	{
		this.currentHealth -= damage;	// For every Character that took damage, the currentHealth will be reduced by the damage

		if (this.currentHealth < 0)		// The value of currentHealth will always be 0 after having negative value subtracted by the damage
		{
			this.currentHealth = 0;
		}
	}

	public boolean isAlive()			// If the Character's Health reaches 0, isAlive() return false
	{
		return this.currentHealth > 0;
	}


	// Getter Methods
	public String getName()
	{
		return name;
	}

	public int getMaxHealth()
	{
		return maxHealth;
	}

	public int getCurrentHealth()
	{
		return currentHealth;
	}

	public int getLevel()
	{
		return level;
	}
	
	public int getAttackPower()
	{
		return attackPower;
	}

	// Setter Method
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setLevel(int level)
	{
		this.level = level;
	}
	
	public void setMaxHealth(int maxHealth)
	{
		this.maxHealth = maxHealth;
	}
	
	// --- NEWLY ADDED METHOD TO FIX ERROR ---
	public void setCurrentHealth(int health) 
	{
		this.currentHealth = health;
		if (this.currentHealth > this.maxHealth) {
			this.currentHealth = this.maxHealth;
		}
	}
	
	public void setAttackPower(int attackPower)
	{
		this.attackPower = attackPower;
	}

}