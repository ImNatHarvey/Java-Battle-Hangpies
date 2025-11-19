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

    // State Management
    private enum GameState {
        MENU,
        INVENTORY,
        PLAYING
    }
    private GameState currentState;

    // Views
    private InventoryView inventoryView;

    // Assets
    private Image background;
    private Image titleImage;
    private Image titleCoverImage;

    // Dimensions
    private final int COVER_WIDTH = 980;
    private final int COVER_HEIGHT = 140;
    private final int TITLE_WIDTH = 900;
    private final int TITLE_HEIGHT = 100;

    // Menu State
    private String[] options = { "Play Game", "Inventory", "Exit Game" };
    private volatile int selectedOption = -1;
    private Rectangle[] menuBounds;

    public GameWindow(User user) {
        this.currentUser = user;
        this.menuBounds = new Rectangle[options.length];
        this.currentState = GameState.MENU; // Start at Menu
        
        // Initialize Views
        this.inventoryView = new InventoryView(user);

        setupWindow();
        loadAssets();

        setVisible(true);
        start();
    }

    private void setupWindow() {
        setTitle(GameConstants.GAME_TITLE);
        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        setResizable(false);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setBackground(Color.BLACK);

        gameCanvas = new Canvas();
        gameCanvas.setPreferredSize(new Dimension(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT));
        gameCanvas.setFocusable(true);
        gameCanvas.setBackground(Color.BLACK);
        gameCanvas.setIgnoreRepaint(true);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (currentState == GameState.MENU) {
                    checkMenuHover(e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState == GameState.MENU) {
                    if (selectedOption != -1) {
                        handleMenuClick(selectedOption);
                    }
                } else if (currentState == GameState.INVENTORY) {
                    if (inventoryView.handleMouseClick(e.getX(), e.getY())) {
                        currentState = GameState.MENU; // Go back to menu
                    }
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

    private void checkMenuHover(int mx, int my) {
        boolean found = false;
        for (int i = 0; i < options.length; i++) {
            if (menuBounds[i] != null && menuBounds[i].contains(mx, my)) {
                selectedOption = i;
                found = true;
                break;
            }
        }
        if (!found) {
            selectedOption = -1;
        }
    }

    private void handleMenuClick(int option) {
        if (option == 0) {
            System.out.println("[Game] Action: Play Game clicked!");
            // TODO: Implement Play State later
        } else if (option == 1) {
            System.out.println("[Game] Switching to Inventory View...");
            currentState = GameState.INVENTORY;
        } else if (option == 2) {
            stop();
        }
    }

    private void loadAssets() {
        String bgPath = GameConstants.BG_DIR + GameConstants.MAIN_BG;
        background = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        String titlePath = GameConstants.BG_DIR + GameConstants.TITLE_IMG;
        titleImage = AssetLoader.loadImage(titlePath, TITLE_WIDTH, TITLE_HEIGHT);

        String titleCoverPath = GameConstants.BG_DIR + GameConstants.TITLE_COVER_IMG;
        titleCoverImage = AssetLoader.loadImage(titleCoverPath, COVER_WIDTH, COVER_HEIGHT);
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
        dispose();
    }

    @Override
    public void run() {
        gameCanvas.createBufferStrategy(3);
        BufferStrategy bs = gameCanvas.getBufferStrategy();

        final double FPS = 60.0;
        final double TIME_PER_TICK = 1000000000 / FPS;
        long lastTime = System.nanoTime();
        double delta = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / TIME_PER_TICK;
            lastTime = now;

            if (delta >= 1) {
                // update(); // Logic updates can go here
                delta--;
            }

            render(bs);
        }
    }

    private void render(BufferStrategy bs) {
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Render based on State
        switch (currentState) {
            case MENU:
                renderMenu(g);
                break;
            case INVENTORY:
                inventoryView.render(g, gameCanvas.getWidth(), gameCanvas.getHeight());
                break;
            case PLAYING:
                // renderGameplay(g);
                break;
        }

        g.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    private void renderMenu(Graphics2D g) {
        // 1. Background
        if (background != null) {
            g.drawImage(background, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight(), gameCanvas);
        }

        // 2. Title Section
        int bannerY = 100;
        if (titleCoverImage != null) {
            int coverX = (gameCanvas.getWidth() - COVER_WIDTH) / 2;
            g.drawImage(titleCoverImage, coverX, bannerY, COVER_WIDTH, COVER_HEIGHT, null);
        }

        if (titleImage != null) {
            int textX = (gameCanvas.getWidth() - TITLE_WIDTH) / 2;
            int textY = bannerY + (COVER_HEIGHT - TITLE_HEIGHT) / 2;
            g.drawImage(titleImage, textX, textY, TITLE_WIDTH, TITLE_HEIGHT, null);
        }

        // Draw Subtitle
        g.setFont(GameConstants.SUBTITLE_FONT);
        g.setColor(GameConstants.TEXT_COLOR);
        FontMetrics fm = g.getFontMetrics();
        String subtitle = GameConstants.SUBTITLE_TEXT;
        int subWidth = fm.stringWidth(subtitle);
        int subX = (gameCanvas.getWidth() - subWidth) / 2;
        int subY = bannerY + COVER_HEIGHT + 25;
        g.drawString(subtitle, subX, subY);

        // 3. Draw Menu Options
        drawMenuOptions(g);
    }

    private void drawMenuOptions(Graphics g) {
        g.setFont(GameConstants.UI_FONT);
        FontMetrics fm = g.getFontMetrics();

        int startY = 530;
        int spacing = 50;

        for (int i = 0; i < options.length; i++) {
            String text = options[i];
            boolean isSelected = (i == selectedOption);

            if (isSelected) {
                g.setColor(GameConstants.SELECTION_COLOR);
                text = "> " + text + " <";
            } else {
                g.setColor(GameConstants.TEXT_COLOR);
            }

            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            int x = (gameCanvas.getWidth() - textWidth) / 2;
            int y = startY + (i * spacing);

            menuBounds[i] = new Rectangle(x, y - fm.getAscent(), textWidth, textHeight);

            g.drawString(text, x, y);
        }
    }
}