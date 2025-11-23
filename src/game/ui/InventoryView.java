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

	// Icon Assets (NEW)
	private Image heartImg;
	private Image attackImg;

	// Scrolling & Selection
	private int scrollY = 0;
	// selectedPet is now explicitly managed by GameWindow to track the pet with the
	// green outline
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
	// UPDATED: Moved up from 180 to 160 to prevent bottom frame from touching the
	// footer
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

		// Load Icons (NEW)
		// Assuming user has locally updated to 15x15 icons
		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 15, 15);
		attackImg = AssetLoader.loadImage(GameConstants.ATTACK_IMG, 15, 15);
	}

	public Hangpie getSelectedPet() {
		return selectedPet;
	}

	/**
	 * Sets which pet should be highlighted with the green outline. This should be
	 * synchronized with GameWindow's equippedHangpie.
	 */
	public void setSelection(Hangpie pet) {
		this.selectedPet = pet;
	}

	/**
	 * Clears the temporary selection after a cancellation.
	 */
	public void clearSelectedPet() {
		this.selectedPet = null;
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
					// selectedPet is now the source of truth for the outline
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
		int nameFrameY = y - 55;
		int nameFrameH = 40;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, x + 10, nameFrameY, w - 20, nameFrameH, observer);
		}

		g.setColor(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.BOLD, 14));
		FontMetrics fm = g.getFontMetrics();
		String name = pet.getName();
		if (name.length() > 15)
			name = name.substring(0, 12) + "...";

		// Precise vertical centering for name
		int nameX = x + (w - fm.stringWidth(name)) / 2;
		int nameY = nameFrameY + ((nameFrameH - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(name, nameX, nameY);

		// --- Draw Stats Section (BELOW Card) ---
		int statsY = y + h + 10;
		int statsH = 75;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, x + 10, statsY, w - 20, statsH, observer);
		}

		// 1. Centered Level (Top Row)
		g.setColor(Color.WHITE);
		// CHANGED: Unifying font size to 16 (matching BattleView)
		Font statFont = new Font("Monospaced", Font.BOLD, 16);
		g.setFont(statFont);

		String lvlTxt = "Lvl: " + pet.getLevel();
		fm = g.getFontMetrics();
		int lvlWidth = fm.stringWidth(lvlTxt);

		// Pushed down to +28 to avoid top edge
		g.drawString(lvlTxt, x + (w - lvlWidth) / 2, statsY + 28);

		// 2. Horizontal HP and Atk (Bottom Row) - REPLACED WITH drawModernStats call
		// x+10 is the left edge of the inner frame. w-20 is the inner frame width.
		drawModernStats(g, pet, x + 10, statsY, w - 20, statsH, statFont, observer);
	}

	// Helper method to draw the specific [Heart] HP | [Sword] ATK layout (REVISED
	// FOR SPACED EDGE LAYOUT AND VERTICAL FIX)
	private void drawModernStats(Graphics2D g, Hangpie pet, int x, int y, int w, int h, Font font, ImageObserver obs) {
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		String hpTxt = pet.getMaxHealth() + "";
		String atkTxt = pet.getAttackPower() + "";
		String sep = " | ";

		int iconSize = 15; // Assuming the user set this to 15 globally
		int gap = 5; // Gap between icon and text

		// PADDING_X defines the desired space from the edge of the inner frame (the
		// beige box)
		int PADDING_X = 40;

		// Vertical alignment calculation
		// Target Y is now adjusted to visually center the text with the
		// icons/separator.
		// The middle of the 75px stats box is y + 37.5. Text ascender needs to be
		// considered.
		// Setting the baseline (targetY) to y + 45 works better for vertical centering.
		int targetY = y + 45;

		// Icon vertical adjustment: Text baseline is `targetY`. The icon top edge is
		// calculated to align the middle.
		// For a 15px icon and a font of size 16 (which has a large ascender), targetY -
		// 12 looks centered.
		int iconTopY = targetY - 12;

		// --- CALCULATE TEXT WIDTHS ---
		int hpTextWidth = fm.stringWidth(hpTxt);
		int atkTextWidth = fm.stringWidth(atkTxt);
		int separatorWidth = fm.stringWidth(sep);

		// --- DRAW HP GROUP (LEFT-SIDE ALIGNED) ---

		// Starting X position for HP icon (PADDING_X offset from left edge of inner
		// frame 'x')
		int hpIconX = x + PADDING_X;
		int currentX = hpIconX;

		// Draw Heart Icon
		if (heartImg != null) {
			g.drawImage(heartImg, currentX, iconTopY, iconSize, iconSize, obs);
		}
		currentX += iconSize + gap;

		// Draw HP Text
		g.setColor(Color.WHITE);
		g.drawString(hpTxt, currentX, targetY);
		currentX += hpTextWidth;

		// --- DRAW SEPARATOR (CENTERED) ---

		// Calculate X for the separator to be perfectly centered horizontally
		int centerPoint = x + (w / 2);
		int sepX = centerPoint - (separatorWidth / 2);

		g.setColor(Color.GRAY);
		g.drawString(sep, sepX, targetY);

		// --- DRAW ATK GROUP (RIGHT-SIDE ALIGNED) ---

		// Calculate the starting X position for the ATK icon to ensure the text ends at
		// (x + w - PADDING_X)
		int atkTextEnd = x + w - PADDING_X;
		int atkTextX = atkTextEnd - atkTextWidth;
		int atkIconX = atkTextX - gap - iconSize;

		// Draw Attack Icon
		if (attackImg != null) {
			g.drawImage(attackImg, atkIconX, iconTopY, iconSize, iconSize, obs);
		}

		// Draw Atk Text
		g.setColor(Color.WHITE);
		g.drawString(atkTxt, atkTextX, targetY);
	}

	public String handleMouseClick(int mx, int my) {
		if (backButtonBounds != null && backButtonBounds.contains(mx, my)) {
			// Do not clear the selection here; GameWindow manages state on back.
			return "BACK";
		}

		int x = START_X;
		int y = START_Y - scrollY;
		int width = GameConstants.WINDOW_WIDTH;

		for (Hangpie pet : user.getInventory()) {
			Rectangle cardBounds = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
			boolean isVisible = (y + CARD_HEIGHT > 100) && (y < GameConstants.WINDOW_HEIGHT - 100);

			if (isVisible && cardBounds.contains(mx, my)) {
				// Set selectedPet immediately so the green outline appears on click.
				// GameWindow will then check for save file and handle the final equipped state.
				this.selectedPet = pet;
				System.out.println("[Inventory] Equipped candidate: " + pet.getName());
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