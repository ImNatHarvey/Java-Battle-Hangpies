package models;

import java.awt.Image;
import utils.AssetLoader;

public class Enemy extends Character {
	
	public enum AnimState {
		IDLE, ATTACK, DAMAGE, DEATH
	}
	
	private String assetFolder; // e.g., "enemies/skeleton"
	private AnimState currentState;
	
	public Enemy(String name, int maxHealth, int level, int attackPower, String folderName) {
		super(name, maxHealth, level, attackPower);
		this.assetFolder = "images/" + folderName; // e.g., "images/enemies/skeleton"
		this.currentState = AnimState.IDLE;
	}
	
	public void setAnimationState(AnimState state) {
		this.currentState = state;
	}
	
	public Image getCurrentImage() {
		String fileName = "idle.gif";
		switch(currentState) {
			case ATTACK: fileName = "attack.gif"; break;
			case DAMAGE: fileName = "damage.gif"; break;
			case DEATH: fileName = "death.gif"; break;
			default: fileName = "idle.gif"; break;
		}
		
		String fullPath = assetFolder + "/" + fileName;
		// Don't scale GIFs here to preserve animation quality, 
		// BattleView handles drawing size. AssetLoader handles toolkit logic.
		return AssetLoader.loadImage(fullPath, -1, -1); 
	}
}