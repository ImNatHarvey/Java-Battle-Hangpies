package models;

import java.awt.Image;
import java.util.UUID;
import utils.AssetLoader;
import game.GameConstants;

public class Hangpie extends Character implements Comparable<Hangpie>
{
	// Animation States
	public enum AnimState {
		IDLE, ATTACK, DAMAGE, DEATH
	}
	
	private String uniqueId;		// UUID used for Marketplace
	private String productId;		// Product ID used exclusively in Shop
	private String description;		// Hangpie's description
	private double price;			// For the marketplace
	private String imageName;       // The folder name of the image (e.g., "wizard")
	
	private AnimState currentAnimState = AnimState.IDLE;

	public Hangpie(String id, String name, String description, double price, int maxHealth, int level, int attackPower, String imageName)
	{
		super(name, maxHealth, level, attackPower);
		this.productId = id;
		this.description = description;
		this.price = price;
		this.imageName = imageName;
	}

	public Hangpie (Hangpie localCopy)
	{
		super(localCopy.getName(), localCopy.getMaxHealth(), localCopy.getLevel(), localCopy.getAttackPower());
		this.uniqueId = UUID.randomUUID().toString();
		this.productId = localCopy.productId;
		this.description = localCopy.description;
		this.price = localCopy.price;
		this.imageName = localCopy.imageName;
	}
	
	// --- Animation Methods ---
	
	public void setAnimationState(AnimState state) {
		if (this.currentAnimState != state) {
			this.currentAnimState = state;
			
			// Fix: Flush the image to reset GIF animation when switching to an action
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
	
	// Helper to resolve path based on state
	private Image getImageForState(AnimState state) {
		String fileName = "idle.gif";
		switch(state) {
			case ATTACK: fileName = "attack.gif"; break;
			case DAMAGE: fileName = "damage.gif"; break;
			case DEATH: fileName = "death.gif"; break;
			default: fileName = "idle.gif"; break;
		}
		
		// Path example: images/hangpies/wizard/attack.gif
		String fullPath = GameConstants.HANGPIE_DIR + imageName + "/" + fileName;
		
		// Use AssetLoader to get the GIF (returns original size/toolkit image for animation)
		return AssetLoader.loadImage(fullPath, -1, -1); 
	}
	
	// Preloads all assets to prevent lag/skipping on first use
	public void preloadAssets() {
		for (AnimState state : AnimState.values()) {
			getImageForState(state);
		}
	}
	
	// --- Existing Methods ---

	@Override
	public String toString()
	{
		return String.format("[%s]\t%s\t\tLvl:%d\t(HP:%d, Atk:%d)\t[Img: %s]\t[Price: %.2fG]\t%s", 
				productId, name, level, maxHealth, attackPower, imageName, price, description); 
	}
	
	@Override
	public int compareTo(Hangpie other)
	{
		return this.productId.compareTo(other.getId());
	}

	public String getUniqueId() { return uniqueId; }
	public String getId() { return productId; }
	public double getPrice() { return price; }
	public String getDescription() { return description; }
	public String getImageName() { return imageName; }

	public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
	public void setPrice(double price) { this.price = price; }
	public void setDescription(String description) { this.description = description; }
	public void setImageName(String imageName) { this.imageName = imageName; }
}