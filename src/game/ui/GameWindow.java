package game.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import game.GameConstants;
import models.User;
import utils.AssetLoader;

public class GameWindow extends Frame {
    private User currentUser;
    private boolean isRunning;

    public GameWindow(User user) {
        this.currentUser = user;
        this.isRunning = true;
        
        setupWindow();
        setupComponents();
        
        setVisible(true);
    }

    private void setupWindow() {
        setTitle(GameConstants.GAME_TITLE);
        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        setResizable(false);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Center on screen
        setBackground(GameConstants.BACKGROUND_COLOR);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeGame();
            }
        });
    }

    private void setupComponents() {
        // We load the image using the CONSTANTS (1280x720) instead of getWidth()
        // because getWidth() is 0 until the window pops up.
        String bgPath = GameConstants.BG_DIR + GameConstants.MAIN_BG;
        Image background = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        Canvas gameCanvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                
                // Draw the background (GIF or Image)
                if (background != null) {
                    // 'this' acts as the ImageObserver, required for GIFs to animate
                    g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
                }
                
                // Optional: Draw a "Press Space to Start" text or similar overlay later
            }
        };
        
        gameCanvas.setBackground(Color.BLACK);
        add(gameCanvas, BorderLayout.CENTER);
    }

    private void closeGame() {
        isRunning = false;
        System.out.println("\n[Game] Returning to Console Dashboard...");
        dispose();
    }
}