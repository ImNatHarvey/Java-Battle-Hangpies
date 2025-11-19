package game.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

import game.GameConstants;
import models.User;
import utils.AssetLoader;

public class GameWindow extends Frame implements Runnable {
    private User currentUser;
    private boolean isRunning;
    private Canvas gameCanvas;
    private Thread gameThread;

    // Assets
    private Image background;
    private Image titleImage;

    // Menu State
    private String[] options = {"Play Game", "Inventory"};
    private volatile int selectedOption = -1; // -1 means no selection
    private Rectangle[] menuBounds; // Store hitboxes for the buttons

    public GameWindow(User user) {
        this.currentUser = user;
        this.menuBounds = new Rectangle[options.length];
        
        setupWindow();
        loadAssets();
        
        setVisible(true);
        
        // Start the Game Loop
        start();
    }

    private void setupWindow() {
        setTitle(GameConstants.GAME_TITLE);
        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        setResizable(false);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setBackground(Color.BLACK);

        // Use a Canvas for active rendering
        gameCanvas = new Canvas();
        gameCanvas.setPreferredSize(new Dimension(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT));
        gameCanvas.setFocusable(true);
        gameCanvas.setBackground(Color.BLACK);
        
        // IMPORTANT: Ignore OS repaints to prevent flickering
        gameCanvas.setIgnoreRepaint(true);
        
        // --- MOUSE INPUT HANDLING ---
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkMouseHover(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (selectedOption != -1) {
                    handleMenuClick(selectedOption);
                }
            }
        };
        
        gameCanvas.addMouseListener(mouseHandler);
        gameCanvas.addMouseMotionListener(mouseHandler);
        
        add(gameCanvas, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
    }
    
    private void checkMouseHover(int mx, int my) {
        boolean found = false;
        for (int i = 0; i < options.length; i++) {
            // Check collision with the button bounds
            if (menuBounds[i] != null && menuBounds[i].contains(mx, my)) {
                selectedOption = i;
                found = true;
                break;
            }
        }
        // If mouse isn't over any button, reset selection
        if (!found) {
            selectedOption = -1;
        }
    }
    
    private void handleMenuClick(int option) {
        if (option == 0) {
            System.out.println("[Game] Action: Play Game clicked!");
            // TODO: Transition to Battle Scene
        } else if (option == 1) {
            System.out.println("[Game] Action: Inventory clicked!");
            // TODO: Transition to Inventory Overlay
        }
    }

    private void loadAssets() {
        String bgPath = GameConstants.BG_DIR + GameConstants.MAIN_BG;
        background = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        int titleWidth = 600;
        int titleHeight = 200;
        String titlePath = GameConstants.BG_DIR + GameConstants.TITLE_IMG;
        titleImage = AssetLoader.loadImage(titlePath, titleWidth, titleHeight);
    }

    private synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("\n[Game] Returning to Console Dashboard...");
        dispose();
    }

    @Override
    public void run() {
        // Create BufferStrategy for Triple Buffering
        gameCanvas.createBufferStrategy(3);
        BufferStrategy bs = gameCanvas.getBufferStrategy();

        // Game Loop Constants
        final double FPS = 60.0;
        final double TIME_PER_TICK = 1000000000 / FPS;
        long lastTime = System.nanoTime();
        double delta = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / TIME_PER_TICK;
            lastTime = now;

            if (delta >= 1) {
                update(); 
                delta--;
            }

            render(bs);
        }
    }

    private void update() {
        // Logic updates can go here
    }

    private void render(BufferStrategy bs) {
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();

        // Clear screen 
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // 1. Draw Background
        if (background != null) {
            g.drawImage(background, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight(), gameCanvas);
        }

        // 2. Draw Title
        if (titleImage != null) {
            int x = (gameCanvas.getWidth() - 600) / 2;
            int y = 50;
            g.drawImage(titleImage, x, y, 600, 200, null);
        }

        // 3. Draw Menu
        drawMenu(g);

        // Clean up and flip the buffer
        g.dispose();
        bs.show();
        
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawMenu(Graphics g) {
        g.setFont(GameConstants.UI_FONT);
        FontMetrics fm = g.getFontMetrics();

        int startY = 400;
        int spacing = 50;

        for (int i = 0; i < options.length; i++) {
            String text = options[i];
            boolean isSelected = (i == selectedOption);
            
            // Hover Effect: Change Color and Add Arrows
            if (isSelected) {
                g.setColor(GameConstants.SELECTION_COLOR);
                text = "> " + text + " <";
            } else {
                g.setColor(GameConstants.TEXT_COLOR);
            }

            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            // Use gameCanvas dimensions for accurate centering
            int x = (gameCanvas.getWidth() - textWidth) / 2;
            int y = startY + (i * spacing);

            // Update Hitbox for Mouse Detection
            // Note: drawString y is the baseline, so we subtract ascent to get top-left y
            menuBounds[i] = new Rectangle(x, y - fm.getAscent(), textWidth, textHeight);

            g.drawString(text, x, y);
        }
    }
}