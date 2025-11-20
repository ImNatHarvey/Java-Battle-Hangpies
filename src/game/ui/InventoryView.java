package game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.util.List;

import game.GameConstants;
import models.Hangpie;
import models.User;
import utils.AssetLoader;

public class InventoryView {
	private User user;
	private Image background;
	private Rectangle backButtonBounds;

	// New UI Assets
	private Image nameFrameImg;
	private Image frameImg;

	// Scrolling & Selection
	private int scrollY = 0;
	private Hangpie selectedPet = null;
	private boolean isBackHovered = false;

	// Layout Constants
	private final int CARD_WIDTH = 220;
	private final int CARD_HEIGHT = 320;
	
	// Split Gap into X and Y to fit 4 items per row
	private final int GAP_X = 70; 
	private final int GAP_Y = 160; 
	
	// Adjusted Start X to center the grid of 4 items
	private final int START_X = 85;
	// UPDATED: Moved up from 180 to 160 to prevent bottom frame from touching the footer
	private final int START_Y = 160; 

	public InventoryView(User user) {
		this.user = user;
		loadAssets();
	}

	private void loadAssets() {
		String path = GameConstants.BG_DIR + GameConstants.INVENTORY_BG;
		background = AssetLoader.loadImage(path, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
		
		// Load UI Elements
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 200, 50);
		frameImg = AssetLoader.loadImage(GameConstants.FRAME_IMG, 220, 60);
	}
	
	public Hangpie getSelectedPet() {
		return selectedPet;
	}

	public void render(Graphics2D g, int width, int height, ImageObserver observer) {
		if (background != null) {
			g.drawImage(background, 0, 0, width, height, observer);
		} else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, width, height);
		}

		Shape originalClip = g.getClip();
		// UPDATED: Adjusted clip to start at 90 (was 100) to fit the higher cards
		// Height adjusted to (height - 90 - 100) = height - 190
		g.setClip(0, 90, width, height - 190);

		List<Hangpie> inventory = user.getInventory();

		if (inventory.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			g.setColor(Color.WHITE);
			g.drawString("Your inventory is empty.", (width / 2) - 150, height / 2);
		} else {

			int x = START_X;
			int y = START_Y - scrollY;

			for (Hangpie pet : inventory) {
				// Draw if within visible bounds (using GAP_Y for vertical spacing check)
				if (y + CARD_HEIGHT + GAP_Y > 0 && y - 100 < height) {
					drawPetCard(g, pet, x, y, CARD_WIDTH, CARD_HEIGHT, observer);
				}

				x += CARD_WIDTH + GAP_X;
				
				// Check if we need to wrap to the next row
				if (x + CARD_WIDTH > width - 20) {
					x = START_X;
					y += CARD_HEIGHT + GAP_Y;
				}
			}
		}

		g.setClip(originalClip);

		// Header
		g.setColor(new Color(0, 0, 0, 150));
		// UPDATED: Reduced header height to 90 to match clip and not overlap cards
		g.fillRect(0, 0, width, 90);

		g.setFont(GameConstants.HEADER_FONT);
		g.setColor(Color.WHITE);
		String title = "MY INVENTORY - Select to Equip";
		FontMetrics fm = g.getFontMetrics();
		int titleX = (width - fm.stringWidth(title)) / 2;
		g.drawString(title, titleX, 70);

		// Footer
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, height - 100, width, 100);

		String backText = isBackHovered ? "> Back <" : "Back";
		g.setFont(GameConstants.BUTTON_FONT);
		fm = g.getFontMetrics();

		int baseWidth = fm.stringWidth("Back");
		int buttonCenterX = (width - 150) - (baseWidth / 2);

		int backW = fm.stringWidth(backText);
		int backH = fm.getHeight();

		int backX = buttonCenterX - (backW / 2);
		int backY = height - 50;

		backButtonBounds = new Rectangle(backX - 10, backY - fm.getAscent() - 10, backW + 20, backH + 20);

		if (isBackHovered) {
			g.setColor(GameConstants.SELECTION_COLOR);
		} else {
			g.setColor(Color.WHITE);
		}

		g.drawString(backText, backX, backY);
	}

	private void drawPetCard(Graphics2D g, Hangpie pet, int x, int y, int w, int h, ImageObserver observer) {
		// --- Draw Background (Specific Tarot Card) ---
		String tarotPath = GameConstants.HANGPIE_DIR + pet.getImageName() + "/tarot.png";
		Image tarotCard = AssetLoader.loadImage(tarotPath, w, h);
		
		if (tarotCard != null) {
			g.drawImage(tarotCard, x, y, w, h, null);
		} else {
			g.setColor(new Color(0, 0, 0, 150));
			g.fillRect(x, y, w, h);
			g.setColor(Color.RED);
			g.drawString("No Tarot", x + 80, y + 150);
		}

		// Draw Selection Border
		if (pet == selectedPet) {
			g.setColor(Color.GREEN); 
			g.setStroke(new BasicStroke(4));
			g.drawRect(x, y, w, h);
			g.setStroke(new BasicStroke(1));
			
			g.setColor(Color.GREEN);
			g.setFont(new Font("Monospaced", Font.BOLD, 14));
			g.drawString("EQUIPPED", x + 10, y + 30);
		} else {
			g.setColor(new Color(255, 255, 255, 50)); // Subtle border
			g.drawRect(x, y, w, h);
		}
		
		// --- Draw Name Frame (ABOVE Card) ---
		// Adjusted in previous step: Moved up to y - 65
		int nameFrameY = y - 55;
		int nameFrameH = 40;
		
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, x + 10, nameFrameY, w - 20, nameFrameH, observer);
		}
		
		g.setColor(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.BOLD, 14));
		FontMetrics fm = g.getFontMetrics();
		String name = pet.getName();
		if (name.length() > 15) name = name.substring(0, 12) + "...";
		int nameX = x + (w - fm.stringWidth(name)) / 2;
		g.drawString(name, nameX, nameFrameY + 25);

		// --- Draw Stats Section (BELOW Card) ---
		int statsY = y + h + 10; 
		// Adjusted in previous step: Increased height to 75
		int statsH = 75;
		
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, x + 10, statsY, w - 20, statsH, observer);
		}
		
		// 1. Centered Level (Top Row)
		g.setColor(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.BOLD, 16));
		String lvlTxt = "Lvl: " + pet.getLevel();
		int lvlWidth = g.getFontMetrics().stringWidth(lvlTxt);
		g.drawString(lvlTxt, x + (w - lvlWidth) / 2, statsY + 25);

		// 2. Horizontal HP and Atk (Bottom Row)
		g.setFont(new Font("Monospaced", Font.BOLD, 14));
		
		// Left Aligned HP
		g.setColor(Color.GREEN);
		String hpTxt = "HP: " + pet.getMaxHealth();
		g.drawString(hpTxt, x + 30, statsY + 52);
		
		// Right Aligned Atk
		g.setColor(Color.RED);
		String atkTxt = "Atk: " + pet.getAttackPower();
		int atkWidth = g.getFontMetrics().stringWidth(atkTxt);
		g.drawString(atkTxt, x + w - 30 - atkWidth, statsY + 52);
	}

	public String handleMouseClick(int mx, int my) {
		if (backButtonBounds != null && backButtonBounds.contains(mx, my)) {
			return "BACK";
		}

		int x = START_X;
		int y = START_Y - scrollY;
		int width = GameConstants.WINDOW_WIDTH;

		for (Hangpie pet : user.getInventory()) {
			Rectangle cardBounds = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
			boolean isVisible = (y + CARD_HEIGHT > 100) && (y < GameConstants.WINDOW_HEIGHT - 100);

			if (isVisible && cardBounds.contains(mx, my)) {
				this.selectedPet = pet;
				System.out.println("[Inventory] Equipped: " + pet.getName());
				return "SELECT";
			}

			x += CARD_WIDTH + GAP_X;
			if (x + CARD_WIDTH > width - 20) {
				x = START_X;
				y += CARD_HEIGHT + GAP_Y;
			}
		}

		return "NONE";
	}

	public void handleMouseMove(int mx, int my) {
		if (backButtonBounds != null) {
			isBackHovered = backButtonBounds.contains(mx, my);
		}
	}

	public void handleMouseScroll(int units) {
		int scrollSpeed = 30;
		scrollY += units * scrollSpeed;

		int totalRows = (int) Math.ceil((double) user.getInventory().size() / 4.0);
		int totalContentHeight = (totalRows * (CARD_HEIGHT + GAP_Y));
		int maxScroll = Math.max(0, totalContentHeight - (GameConstants.WINDOW_HEIGHT - 200));

		if (scrollY < 0)
			scrollY = 0;
		if (scrollY > maxScroll + 100)
			scrollY = maxScroll + 100;
	}
}