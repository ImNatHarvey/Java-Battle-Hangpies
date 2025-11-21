package models;

import java.awt.Image;
import java.util.UUID;
import utils.AssetLoader;
import game.GameConstants;

public class Hangpie extends Character implements Comparable<Hangpie> {
	// Animation States
	public enum AnimState {
		IDLE, ATTACK, DAMAGE, DEATH
	}

	private String uniqueId; // UUID used for Marketplace
	private String productId; // Product ID used exclusively in Shop
	private String description; // Hangpie's description
	private double price; // For the marketplace
	private String imageName; // The folder name of the image (e.g., "wizard")

	private int currentExp; // Current EXP

	private AnimState currentAnimState = AnimState.IDLE;

	public Hangpie(String id, String name, String description, double price, int maxHealth, int level, int attackPower,
			String imageName) {
		super(name, maxHealth, level, attackPower);
		this.productId = id;
		this.description = description;
		this.price = price;
		this.imageName = imageName;
		this.currentExp = 0;
	}

	public Hangpie(Hangpie localCopy) {
		super(localCopy.getName(), localCopy.getMaxHealth(), localCopy.getLevel(), localCopy.getAttackPower());
		this.uniqueId = UUID.randomUUID().toString();
		this.productId = localCopy.productId;
		this.description = localCopy.description;
		this.price = localCopy.price;
		this.imageName = localCopy.imageName;
		this.currentExp = localCopy.currentExp;
	}

	// --- Progression Methods ---

	public int getMaxExpForCurrentLevel() {
		// Level 1: 10, Level 2: 20, Level 3: 30...
		return this.level * 10;
	}

	/**
	 * Adds EXP to the Hangpie. Handles level ups and caps based on World Level.
	 * 
	 * @param amount          The amount of EXP to gain.
	 * @param worldLevelLimit The player's current World Level (Hard Cap for Pet
	 *                        Level).
	 * @return true if the pet leveled up, false otherwise.
	 */
	public boolean gainExp(int amount, int worldLevelLimit) {
		// Progression Gate: If already at level cap and max exp, do nothing
		if (this.level >= worldLevelLimit && this.currentExp >= getMaxExpForCurrentLevel()) {
			this.currentExp = getMaxExpForCurrentLevel(); // Ensure clamped
			return false;
		}

		this.currentExp += amount;
		boolean leveledUp = false;

		int requiredExp = getMaxExpForCurrentLevel();

		// Level Up Loop
		while (this.currentExp >= requiredExp && this.level < worldLevelLimit) {
			this.currentExp -= requiredExp;
			levelUp();
			leveledUp = true;
			requiredExp = getMaxExpForCurrentLevel(); // Update for next level
		}

		// Hard Cap Enforcement: If we hit the world limit, cap EXP at max
		if (this.level >= worldLevelLimit) {
			if (this.currentExp > requiredExp) {
				this.currentExp = requiredExp;
			}
		}

		return leveledUp;
	}

	private void levelUp() {
		this.level++;
		// Stats Growth: +1 HP / +1 ATK per level
		this.maxHealth += 1;
		this.attackPower += 1;
		// Heal on level up
		this.currentHealth = this.maxHealth;
	}

	// --- Animation Methods ---

	public void setAnimationState(AnimState state) {
		if (this.currentAnimState != state) {
			this.currentAnimState = state;

			if (state == AnimState.ATTACK || state == AnimState.DAMAGE || state == AnimState.DEATH) {
				Image img = getCurrentImage();
				if (img != null) {
					img.flush();
				}
			}
		}
	}

	public Image getCurrentImage() {
		return getImageForState(this.currentAnimState);
	}

	private Image getImageForState(AnimState state) {
		String fileName = "idle.gif";
		switch (state) {
		case ATTACK:
			fileName = "attack.gif";
			break;
		case DAMAGE:
			fileName = "damage.gif";
			break;
		case DEATH:
			fileName = "death.gif";
			break;
		default:
			fileName = "idle.gif";
			break;
		}

		String fullPath = GameConstants.HANGPIE_DIR + imageName + "/" + fileName;
		return AssetLoader.loadImage(fullPath, -1, -1);
	}

	public void preloadAssets() {
		for (AnimState state : AnimState.values()) {
			getImageForState(state);
		}
	}

	// --- Getters & Setters ---

	@Override
	public String toString() {
		return String.format("[%s]\t%s\t\tLvl:%d (EXP:%d/%d)\t[Price: %.2fG]\t%s", productId, name, level, currentExp,
				getMaxExpForCurrentLevel(), price, description);
	}

	@Override
	public int compareTo(Hangpie other) {
		return this.productId.compareTo(other.getId());
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public String getId() {
		return productId;
	}

	public double getPrice() {
		return price;
	}

	public String getDescription() {
		return description;
	}

	public String getImageName() {
		return imageName;
	}

	public int getCurrentExp() {
		return currentExp;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public void setCurrentExp(int currentExp) {
		this.currentExp = currentExp;
	}
}
