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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.GameConstants;
import models.Hangpie;
import models.User;
import utils.AssetLoader;

public class InventoryView {
	private User user;
	private Image background;
	private Image cardBackground;
	private Rectangle backButtonBounds;

	// New UI Assets
	private Image nameFrameImg;
	private Image frameImg;
	private Image heartImg;
	private Image emptyHeartImg;

	// Scrolling & Selection
	private int scrollY = 0;
	private Hangpie selectedPet = null;
	private boolean isBackHovered = false;

	// Layout Constants
	private final int CARD_WIDTH = 220;
	private final int CARD_HEIGHT = 320;
	private final int GAP = 40;
	private final int START_X = 100;
	private final int START_Y = 150;

	private Map<String, Image> petImageCache;

	public InventoryView(User user) {
		this.user = user;
		this.petImageCache = new HashMap<>();
		loadAssets();
	}

	private void loadAssets() {
		String path = GameConstants.BG_DIR + GameConstants.INVENTORY_BG;
		background = AssetLoader.loadImage(path, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

		String cardPath = GameConstants.BG_DIR + GameConstants.TITLE_COVER_IMG;
		cardBackground = AssetLoader.loadImage(cardPath, 300, 400);
		
		// Load UI Elements
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 200, 50);
		frameImg = AssetLoader.loadImage(GameConstants.FRAME_IMG, 220, 60);
		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 20, 20);
		emptyHeartImg = AssetLoader.loadImage(GameConstants.EMPTY_HEART_IMG, 20, 20);
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
		g.setClip(0, 100, width, height - 200);

		List<Hangpie> inventory = user.getInventory();

		if (inventory.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			g.setColor(Color.WHITE);
			g.drawString("Your inventory is empty.", (width / 2) - 150, height / 2);
		} else {

			int x = START_X;
			int y = START_Y - scrollY;

			for (Hangpie pet : inventory) {
				if (y + CARD_HEIGHT > 0 && y < height) {
					drawPetCard(g, pet, x, y, CARD_WIDTH, CARD_HEIGHT, observer);
				}

				x += CARD_WIDTH + GAP;
				if (x + CARD_WIDTH > width - 50) {
					x = START_X;
					y += CARD_HEIGHT + GAP;
				}
			}
		}

		g.setClip(originalClip);

		// Header
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, width, 100);

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
		// Draw Background
		if (cardBackground != null) {
			g.drawImage(cardBackground, x, y, w, h, null);
		} else {
			g.setColor(new Color(0, 0, 0, 150));
			g.fillRect(x, y, w, h);
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
			g.setColor(Color.WHITE);
			g.drawRect(x, y, w, h);
		}
		
		// --- Draw Name Frame (Top) ---
		int nameFrameH = 40;
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, x + 10, y + 40, w - 20, nameFrameH, observer);
		}
		
		g.setColor(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.BOLD, 14));
		FontMetrics fm = g.getFontMetrics();
		String name = pet.getName();
		if (name.length() > 15) name = name.substring(0, 12) + "...";
		int nameX = x + (w - fm.stringWidth(name)) / 2;
		g.drawString(name, nameX, y + 40 + 25);

		// --- Draw Character Image ---
		String imageName = pet.getImageName();
		String imgPath;
		int imgW = 100;
		int imgH = 100;

		if (imageName != null && (imageName.contains(".png") || imageName.contains(".gif") || imageName.contains(".jpg"))) {
			imgPath = GameConstants.HANGPIE_DIR + imageName;
		} else {
			imgPath = GameConstants.HANGPIE_DIR + imageName + "/idle.gif";
		}

		Image petImg = petImageCache.get(imgPath);
		if (petImg == null) {
			petImg = AssetLoader.loadImage(imgPath, imgW, imgH);
			if (petImg != null)
				petImageCache.put(imgPath, petImg);
		}

		if (petImg != null) {
			int imgX = x + (w - imgW) / 2;
			int imgY = y + 90;
			g.drawImage(petImg, imgX, imgY, imgW, imgH, observer);
		}

		// --- Draw Stats Section (Frame + Hearts) ---
		int statsY = y + 200;
		
		// Draw Frame for Hearts
		if (frameImg != null) {
			// x, y, width, height
			g.drawImage(frameImg, x + 10, statsY, w - 20, 50, observer);
		}
		
		// Draw Hearts logic (Assume 1 HP = 1 Heart)
		int hearts = Math.min(pet.getMaxHealth(), 8); // Cap at 8 visually for inventory card width
		int heartStartX = x + 25;
		int heartY = statsY + 15;
		
		for (int i = 0; i < hearts; i++) {
			if (heartImg != null) {
				g.drawImage(heartImg, heartStartX + (i * 22), heartY, 20, 20, observer);
			}
		}
		
		// Draw Text details
		g.setColor(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.PLAIN, 12));
		String lvlTxt = "Level: " + pet.getLevel();
		String atkTxt = "Atk Scale: " + (1 + (pet.getLevel()/2)); // Approximation of battle logic
		
		g.drawString(lvlTxt, x + 20, statsY + 70);
		g.drawString(atkTxt, x + 20, statsY + 90);
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

			x += CARD_WIDTH + GAP;
			if (x + CARD_WIDTH > width - 50) {
				x = START_X;
				y += CARD_HEIGHT + GAP;
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
		int totalContentHeight = (totalRows * (CARD_HEIGHT + GAP));
		int maxScroll = Math.max(0, totalContentHeight - (GameConstants.WINDOW_HEIGHT - 200));

		if (scrollY < 0)
			scrollY = 0;
		if (scrollY > maxScroll + 100)
			scrollY = maxScroll + 100;
	}
}