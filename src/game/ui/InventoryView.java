package game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
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
    
    // Scrolling & Selection
    private int scrollY = 0;
    private Hangpie selectedPet = null;
    private boolean isBackHovered = false; // Track hover state
    
    // Layout Constants
    private final int CARD_WIDTH = 220;
    private final int CARD_HEIGHT = 320;
    private final int GAP = 40;
    private final int START_X = 100;
    private final int START_Y = 150; // Start drawing items below header
    
    // Cache for pet images
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
    }
    
    // Reverted: Removed ImageObserver to fix flickering
    public void render(Graphics2D g, int width, int height) {
        // 1. Draw Background (Static)
        if (background != null) {
            g.drawImage(background, 0, 0, width, height, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);
        }
        
        // 2. Draw Inventory Grid (With Clipping)
        // We set a clip so items don't draw over the Header or the Footer/Back Button area
        Shape originalClip = g.getClip();
        
        // Clip area: x=0, y=100 (below header), width=full, height=until bottom area
        g.setClip(0, 100, width, height - 200); 
        
        List<Hangpie> inventory = user.getInventory();
        
        if (inventory.isEmpty()) {
        	g.setFont(GameConstants.UI_FONT);
        	g.setColor(Color.WHITE);
            g.drawString("Your inventory is empty.", (width/2) - 150, height/2);
        } else {
            
            int x = START_X;
            int y = START_Y - scrollY; // Apply Scroll Offset
            
            for (Hangpie pet : inventory) {
            	// Optimization: Only draw if visible within the clip area
            	if (y + CARD_HEIGHT > 0 && y < height) {
            		drawPetCard(g, pet, x, y, CARD_WIDTH, CARD_HEIGHT);
            	}
            	
            	x += CARD_WIDTH + GAP;
            	
            	// Wrap to next line if we run out of width
            	if (x + CARD_WIDTH > width - 50) {
            		x = START_X;
            		y += CARD_HEIGHT + GAP;
            	}
            }
        }
        
        // Restore full screen drawing for Header and Buttons
        g.setClip(originalClip);
        
        // 3. Draw Header (Drawn AFTER grid to stay on top)
        // Optional: Semi-transparent background for header to make text pop
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, 100); 
        
        g.setFont(GameConstants.HEADER_FONT);
        g.setColor(Color.WHITE);
        String title = "MY INVENTORY";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 70);
        
        // 4. Draw Back Button (Footer Area)
        // Draw background for footer
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, height - 100, width, 100);
        
        // Logic: Plain "Back" vs "> Back <"
        String backText = isBackHovered ? "> Back <" : "Back";
        
        g.setFont(GameConstants.BUTTON_FONT);
        fm = g.getFontMetrics();
        
        // Calculate a fixed center point based on the unhovered "Back" text width.
        // This anchors the button so "Back" stays in the same visual position.
        int baseWidth = fm.stringWidth("Back");
        int buttonCenterX = (width - 150) - (baseWidth / 2); 
        
        int backW = fm.stringWidth(backText);
        int backH = fm.getHeight();
        
        // Calculate X to center the current text (hovered or not) around that fixed point
        int backX = buttonCenterX - (backW / 2);
        int backY = height - 50;
        
        // Store bounds for click/hover detection
        backButtonBounds = new Rectangle(backX - 10, backY - fm.getAscent() - 10, backW + 20, backH + 20);
        
        if (isBackHovered) {
            g.setColor(GameConstants.SELECTION_COLOR); // Yellow
        } else {
            g.setColor(Color.WHITE); // White
        }
        
        g.drawString(backText, backX, backY);
    }
    
    private void drawPetCard(Graphics2D g, Hangpie pet, int x, int y, int w, int h) {
    	// A. Draw Card Background
    	if (cardBackground != null) {
    		g.drawImage(cardBackground, x, y, w, h, null);
    	} else {
    		g.setColor(new Color(0, 0, 0, 150));
    		g.fillRect(x, y, w, h);
    	}
    	
    	// B. Selection Highlight Border
    	if (pet == selectedPet) {
    		g.setColor(Color.CYAN);
    		g.setStroke(new BasicStroke(3)); // Thicker border for selection
    		g.drawRect(x, y, w, h);
    		g.setStroke(new BasicStroke(1)); // Reset
    	} else {
    		g.setColor(Color.WHITE);
    		g.drawRect(x, y, w, h);
    	}
    	
    	// C. Draw Hangpie Image
    	String imageName = pet.getImageName();
    	String imgPath;
    	
    	// SCALED LARGER
    	int imgW = 170; 
    	int imgH = 170; 
    	
    	// Path resolution
    	if (imageName != null && (imageName.contains(".png") || imageName.contains(".gif") || imageName.contains(".jpg"))) {
    		imgPath = GameConstants.HANGPIE_DIR + imageName;
    	} else {
    		imgPath = GameConstants.HANGPIE_DIR + imageName + "/idle.gif";
    	}
    	
    	// Cache & Draw
    	Image petImg = petImageCache.get(imgPath);
    	if (petImg == null) {
    		petImg = AssetLoader.loadImage(imgPath, imgW, imgH);
    		if (petImg != null) petImageCache.put(imgPath, petImg);
    	}
    	
    	if (petImg != null) {
    		int imgX = x + (w - imgW) / 2;
    		
    		// Center vertically in the top portion of the card
            // ADJUST THIS VALUE TO MOVE IMAGE UP/DOWN (Lower value = higher up)
            // Original was 100. Changed to 85 to move image up away from text.
    		int centerY = y + 50; 
    		int imgY = centerY - (imgH / 2);
    		
    		// Ensure it doesn't go too high if the image is very tall
    		if (imgY < y + 5) imgY = y + 5;
    		
    		// FLICKER FIX: Use Nearest Neighbor, but DO NOT pass observer (pass null)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            
    		g.drawImage(petImg, imgX, imgY, imgW, imgH, null);
    	}
    	
    	// D. Draw Stats Text
    	int textStartY = y + 190;
    	
    	g.setColor(Color.YELLOW);
    	g.setFont(new Font("Monospaced", Font.BOLD, 16));
    	FontMetrics fm = g.getFontMetrics();
    	
    	String name = pet.getName();
    	if (name.length() > 15) name = name.substring(0, 12) + "...";
    	int textX = x + (w - fm.stringWidth(name)) / 2;
    	g.drawString(name, textX, textStartY);
    	
    	g.setColor(Color.WHITE);
    	g.setFont(new Font("Monospaced", Font.PLAIN, 14));
    	g.drawString("Level: " + pet.getLevel(), x + 15, textStartY + 25);
    	g.drawString("HP: " + pet.getCurrentHealth() + "/" + pet.getMaxHealth(), x + 15, textStartY + 45);
    	g.drawString("Attack: " + pet.getAttackPower(), x + 15, textStartY + 65);
    	
    	// E. Draw Description
    	g.setColor(Color.LIGHT_GRAY);
    	g.setFont(new Font("Monospaced", Font.ITALIC, 12));
    	String desc = pet.getDescription();
    	if (desc != null && desc.length() > 28) desc = desc.substring(0, 25) + "..."; 
    	if (desc != null) g.drawString(desc, x + 15, textStartY + 95);
    }
    
    public boolean handleMouseClick(int mx, int my) {
    	// 1. Check Back Button
        if (backButtonBounds != null && backButtonBounds.contains(mx, my)) {
            return true; // Go back
        }
        
        // 2. Check Pet Selection
        int x = START_X;
        int y = START_Y - scrollY;
        int width = GameConstants.WINDOW_WIDTH; 
        
        // We need to apply the same logic as render to find where the cards are
        for (Hangpie pet : user.getInventory()) {
        	Rectangle cardBounds = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
        	
        	// Only allow selection if visible (simple check based on clip area)
        	// Top clip is 100, bottom is Height - 100.
        	boolean isVisible = (y + CARD_HEIGHT > 100) && (y < GameConstants.WINDOW_HEIGHT - 100);
        	
        	if (isVisible && cardBounds.contains(mx, my)) {
        		this.selectedPet = pet;
        		System.out.println("[Inventory] Selected: " + pet.getName());
        		return false; 
        	}
        	
        	x += CARD_WIDTH + GAP;
        	if (x + CARD_WIDTH > width - 50) {
        		x = START_X;
        		y += CARD_HEIGHT + GAP;
        	}
        }
        
        return false;
    }
    
    public void handleMouseMove(int mx, int my) {
        if (backButtonBounds != null) {
            isBackHovered = backButtonBounds.contains(mx, my);
        }
    }
    
    public void handleMouseScroll(int units) {
    	int scrollSpeed = 30;
    	scrollY += units * scrollSpeed;
    	
    	// Clamp Scrolling logic
    	int totalRows = (int) Math.ceil((double) user.getInventory().size() / 4.0); 
    	int totalContentHeight = (totalRows * (CARD_HEIGHT + GAP));
    	// Subtract viewable height to find max scroll
    	int maxScroll = Math.max(0, totalContentHeight - (GameConstants.WINDOW_HEIGHT - 200)); // 200 accounts for header/footer
    	
    	if (scrollY < 0) scrollY = 0;
    	if (scrollY > maxScroll + 100) scrollY = maxScroll + 100; // Allow a little extra overscroll padding
    }
}