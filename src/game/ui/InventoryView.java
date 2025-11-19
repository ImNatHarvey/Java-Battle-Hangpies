package game.ui;

import java.awt.Color;
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
    private Rectangle backButtonBounds;
    
    public InventoryView(User user) {
        this.user = user;
        loadAssets();
    }
    
    private void loadAssets() {
        String path = GameConstants.BG_DIR + GameConstants.INVENTORY_BG;
        background = AssetLoader.loadImage(path, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
    }
    
    public void render(Graphics2D g, int width, int height) {
        // 1. Draw Background
        if (background != null) {
            g.drawImage(background, 0, 0, width, height, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);
        }
        
        // 2. Draw Overlay for readability
        g.setColor(new Color(0, 0, 0, 150)); // Semi-transparent black
        g.fillRect(100, 50, width - 200, height - 100);
        
        g.setColor(Color.WHITE);
        g.drawRect(100, 50, width - 200, height - 100); // Border
        
        // 3. Draw Header
        g.setFont(GameConstants.HEADER_FONT);
        String title = "MY INVENTORY";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 120);
        
        // 4. Draw List
        g.setFont(GameConstants.LIST_FONT);
        List<Hangpie> inventory = user.getInventory();
        
        int startX = 150;
        int startY = 180;
        int lineHeight = 40;
        
        if (inventory.isEmpty()) {
            g.drawString("Your inventory is empty.", startX, startY);
        } else {
            g.drawString(String.format("%-5s %-20s %-10s %-10s", "No.", "Name", "Level", "Health"), startX, startY);
            g.drawLine(startX, startY + 10, width - 150, startY + 10);
            
            startY += lineHeight;
            
            int count = 1;
            for (Hangpie pet : inventory) {
                String entry = String.format("%-5d %-20s Lvl:%-5d HP:%d/%d", 
                        count++, 
                        pet.getName(), 
                        pet.getLevel(),
                        pet.getCurrentHealth(),
                        pet.getMaxHealth());
                
                g.drawString(entry, startX, startY);
                startY += lineHeight;
            }
        }
        
        // 5. Draw Back Button
        String backText = "[ BACK ]";
        g.setFont(GameConstants.BUTTON_FONT);
        fm = g.getFontMetrics();
        int backW = fm.stringWidth(backText);
        int backH = fm.getHeight();
        int backX = width - 150 - backW;
        int backY = height - 80;
        
        // Store bounds for click detection
        backButtonBounds = new Rectangle(backX, backY - fm.getAscent(), backW, backH);
        
        // Hover effect handled by GameWindow logic or simple static draw here
        g.setColor(GameConstants.ACCENT_COLOR);
        g.drawString(backText, backX, backY);
    }
    
    /**
     * Handles mouse click events within the inventory view.
     * @param x Mouse X
     * @param y Mouse Y
     * @return true if the "Back" button was clicked.
     */
    public boolean handleMouseClick(int x, int y) {
        if (backButtonBounds != null && backButtonBounds.contains(x, y)) {
            return true; // Back button clicked
        }
        return false;
    }
}