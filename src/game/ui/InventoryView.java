package game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.List;

import game.GameConstants;
import models.Hangpie;
import models.User;
import utils.AssetLoader;

public class InventoryView {
    private User user;
    private Image background;
    private Image cardBackground; // Using titlecover.png as the placeholder/card bg
    private Rectangle backButtonBounds;
    
    public InventoryView(User user) {
        this.user = user;
        loadAssets();
    }
    
    private void loadAssets() {
        String path = GameConstants.BG_DIR + GameConstants.INVENTORY_BG;
        background = AssetLoader.loadImage(path, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        
        String cardPath = GameConstants.BG_DIR + GameConstants.TITLE_COVER_IMG;
        // Load it smaller to serve as a card background
        cardBackground = AssetLoader.loadImage(cardPath, 300, 400); 
    }
    
    public void render(Graphics2D g, int width, int height) {
        // 1. Draw Background
        if (background != null) {
            g.drawImage(background, 0, 0, width, height, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);
        }
        
        // 2. Draw Header
        g.setFont(GameConstants.HEADER_FONT);
        g.setColor(Color.WHITE);
        String title = "MY INVENTORY";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 80);
        
        // 3. Draw Inventory Grid
        List<Hangpie> inventory = user.getInventory();
        
        if (inventory.isEmpty()) {
        	g.setFont(GameConstants.UI_FONT);
            g.drawString("Your inventory is empty.", (width/2) - 100, height/2);
        } else {
            int startX = 100;
            int startY = 150;
            int cardWidth = 220;
            int cardHeight = 300;
            int gap = 40;
            
            int x = startX;
            int y = startY;
            
            for (Hangpie pet : inventory) {
            	drawPetCard(g, pet, x, y, cardWidth, cardHeight);
            	
            	x += cardWidth + gap;
            	
            	// Wrap to next line if we run out of width
            	if (x + cardWidth > width - 50) {
            		x = startX;
            		y += cardHeight + gap;
            	}
            }
        }
        
        // 4. Draw Back Button
        String backText = "[ BACK ]";
        g.setFont(GameConstants.BUTTON_FONT);
        fm = g.getFontMetrics();
        int backW = fm.stringWidth(backText);
        int backH = fm.getHeight();
        int backX = width - 150 - backW;
        int backY = height - 50;
        
        // Store bounds for click detection
        backButtonBounds = new Rectangle(backX, backY - fm.getAscent(), backW, backH);
        
        g.setColor(GameConstants.ACCENT_COLOR);
        g.drawString(backText, backX, backY);
    }
    
    private void drawPetCard(Graphics2D g, Hangpie pet, int x, int y, int w, int h) {
    	// A. Draw Card Background (using titlecover.png)
    	if (cardBackground != null) {
    		g.drawImage(cardBackground, x, y, w, h, null);
    	} else {
    		g.setColor(new Color(0, 0, 0, 150));
    		g.fillRect(x, y, w, h);
    	}
    	
    	// Border
    	g.setColor(Color.WHITE);
    	g.drawRect(x, y, w, h);
    	
    	// B. Draw Hangpie Image (Centered in top half)
    	String imgPath = GameConstants.HANGPIE_DIR + pet.getImageName();
    	Image petImg = AssetLoader.loadImage(imgPath, 120, 120);
    	
    	if (petImg != null) {
    		int imgX = x + (w - 120) / 2;
    		int imgY = y + 30;
    		g.drawImage(petImg, imgX, imgY, 120, 120, null);
    	}
    	
    	// C. Draw Stats Text
    	g.setColor(Color.YELLOW);
    	g.setFont(new Font("Monospaced", Font.BOLD, 16));
    	FontMetrics fm = g.getFontMetrics();
    	
    	String name = pet.getName();
    	// Truncate name if too long
    	if (name.length() > 15) name = name.substring(0, 12) + "...";
    	
    	int textX = x + (w - fm.stringWidth(name)) / 2;
    	g.drawString(name, textX, y + 180);
    	
    	g.setColor(Color.WHITE);
    	g.setFont(new Font("Monospaced", Font.PLAIN, 14));
    	g.drawString("Level: " + pet.getLevel(), x + 20, y + 210);
    	g.drawString("HP: " + pet.getCurrentHealth() + "/" + pet.getMaxHealth(), x + 20, y + 230);
    	g.drawString("Attack: " + pet.getAttackPower(), x + 20, y + 250);
    }
    
    public boolean handleMouseClick(int x, int y) {
        if (backButtonBounds != null && backButtonBounds.contains(x, y)) {
            return true; // Back button clicked
        }
        return false;
    }
}