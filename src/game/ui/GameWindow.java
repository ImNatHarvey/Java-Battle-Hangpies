package game.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.Random;

import game.GameConstants;
import models.Hangpie;
import models.User;
import utils.AssetLoader;

public class GameWindow extends Frame implements Runnable {
	private User currentUser;
	private boolean isRunning;
	private Canvas gameCanvas;
	private Thread gameThread;

	// State Management
	public enum GameState {
		MENU, INVENTORY, PLAYING
	}

	private GameState currentState;

	// Views
	private InventoryView inventoryView;
	private BattleView battleView;

	// Game Data
	private Hangpie equippedHangpie = null;

	// Assets
	private Image background;
	private Image titleImage;
	private Image titleCoverImage;

	// UI Assets
	private Image modalImg;
	private Image frameImg;
	private Image nameFrameImg;

	// Dimensions
	private final int COVER_WIDTH = 980;
	private final int COVER_HEIGHT = 140;
	private final int TITLE_WIDTH = 900;
	private final int TITLE_HEIGHT = 100;

	private final int MENU_UI_BUTTON_SIZE = 50;
	private final int MODAL_WIDTH = 400;
	private final int MODAL_HEIGHT = 500;

	// Menu State
	private String[] options = { "Play Game", "Inventory", "Exit Game" };
	private volatile int selectedOption = -1;
	private Rectangle[] menuBounds;

	// Menu Controls & State
	private Rectangle instructionBtnBounds;
	private boolean isInstructionOpen = false;

	// Equipped Error State
	private long errorFlashStartTime = 0;
	private final int ERROR_DURATION = 1500;
	private final int SHAKE_MAGNITUDE = 5;
	private int shakeX = 0;
	private int shakeY = 0;
	private long lastShakeTime = 0;
	private Random random = new Random();

	public GameWindow(User user) {
		this.currentUser = user;
		this.menuBounds = new Rectangle[options.length];
		this.currentState = GameState.MENU;

		this.inventoryView = new InventoryView(user);

		setupWindow();
		loadAssets();

		// Setup Menu UI Bounds
		instructionBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, 20, MENU_UI_BUTTON_SIZE,
				MENU_UI_BUTTON_SIZE);

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

		gameCanvas = new Canvas() {
			private static final long serialVersionUID = 1L;

			@Override
			public void update(Graphics g) {
			}

			@Override
			public void paint(Graphics g) {
			}
		};

		gameCanvas.setPreferredSize(new Dimension(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT));
		gameCanvas.setFocusable(true);
		gameCanvas.setBackground(Color.BLACK);
		gameCanvas.setIgnoreRepaint(true);

		// --- Input Listeners ---

		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (currentState == GameState.MENU) {
					checkMenuHover(e.getX(), e.getY());
				} else if (currentState == GameState.INVENTORY) {
					inventoryView.handleMouseMove(e.getX(), e.getY());
				} else if (currentState == GameState.PLAYING && battleView != null) {
					// Update: handle hover effects in battle view
					battleView.handleMouseMove(e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (currentState == GameState.MENU) {
					if (isInstructionOpen) {
						isInstructionOpen = false; // Close instruction modal on click
						return;
					}

					if (instructionBtnBounds != null && instructionBtnBounds.contains(e.getX(), e.getY())) {
						isInstructionOpen = true;
						return;
					}

					if (selectedOption != -1) {
						handleMenuClick(selectedOption);
					}
				} else if (currentState == GameState.INVENTORY) {
					String action = inventoryView.handleMouseClick(e.getX(), e.getY());
					if (action.equals("BACK")) {
						currentState = GameState.MENU;
					} else if (action.equals("SELECT")) {
						equippedHangpie = inventoryView.getSelectedPet();
					}
				} else if (currentState == GameState.PLAYING && battleView != null) {
					// Pass click to BattleView to handle Settings/Modal interaction
					String action = battleView.handleMouseClick(e.getX(), e.getY());
					if (action.equals("EXIT")) {
						stop(); // Close game entirely
					} else if (action.equals("MENU")) {
						currentState = GameState.MENU;
						battleView = null;
					}
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (currentState == GameState.INVENTORY) {
					inventoryView.handleMouseScroll(e.getWheelRotation());
				}
			}
		};

		gameCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (currentState == GameState.MENU && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					isInstructionOpen = false; // Close instruction modal on ESC
				} else if (currentState == GameState.PLAYING && battleView != null) {
					// Pass Key Press to Battle View
					battleView.handleKeyPress(e.getKeyCode(), e.getKeyChar());

					if (battleView.isExitRequested()) {
						currentState = GameState.MENU;
						battleView = null;
					}
				}
			}
		});

		gameCanvas.addMouseListener(mouseHandler);
		gameCanvas.addMouseMotionListener(mouseHandler);
		gameCanvas.addMouseWheelListener(mouseHandler);

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
			// PLAY GAME
			if (equippedHangpie == null) {
				System.out.println("[Game] Cannot start: No Hangpie equipped.");
				// NEW: Set error state and shake
				errorFlashStartTime = System.currentTimeMillis();
			} else {
				startBattle();
			}

		} else if (option == 1) {
			// INVENTORY
			currentState = GameState.INVENTORY;

		} else if (option == 2) {
			// EXIT
			stop();
		}
	}

	private void startBattle() {
		battleView = new BattleView(currentUser, equippedHangpie);
		currentState = GameState.PLAYING;
	}

	private void loadAssets() {
		String bgPath = GameConstants.BG_DIR + GameConstants.MAIN_BG;
		background = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

		String titlePath = GameConstants.BG_DIR + GameConstants.TITLE_IMG;
		titleImage = AssetLoader.loadImage(titlePath, TITLE_WIDTH, TITLE_HEIGHT);

		String titleCoverPath = GameConstants.BG_DIR + GameConstants.TITLE_COVER_IMG;
		titleCoverImage = AssetLoader.loadImage(titleCoverPath, COVER_WIDTH, COVER_HEIGHT);

		// Load UI
		modalImg = AssetLoader.loadImage(GameConstants.MODAL_IMG, MODAL_WIDTH, MODAL_HEIGHT);
		frameImg = AssetLoader.loadImage(GameConstants.FRAME_IMG, 600, 60);
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 250, 50);
	}

	private synchronized void start() {
		if (isRunning)
			return;
		isRunning = true;
		gameThread = new Thread(this);
		gameThread.start();
	}

	private synchronized void stop() {
		if (!isRunning)
			return;
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
				if (currentState == GameState.PLAYING && battleView != null) {
					battleView.update();
				}
				// NEW: Update shake effect if error is active
				updateMenuShake();
				delta--;
			}
			render(bs);
		}
	}

	private void updateMenuShake() {
		if (errorFlashStartTime > 0) {
			long timeElapsed = System.currentTimeMillis() - errorFlashStartTime;
			if (timeElapsed < ERROR_DURATION) {
				if (System.currentTimeMillis() - lastShakeTime > 50) {
					shakeX = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					shakeY = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					lastShakeTime = System.currentTimeMillis();
				}
			} else {
				errorFlashStartTime = 0;
				shakeX = 0;
				shakeY = 0;
			}
		}
	}

	private void render(BufferStrategy bs) {
		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

		switch (currentState) {
		case MENU:
			renderMenu(g);
			if (isInstructionOpen) {
				drawInstructionModal(g, gameCanvas.getWidth(), gameCanvas.getHeight());
			}
			break;
		case INVENTORY:
			inventoryView.render(g, gameCanvas.getWidth(), gameCanvas.getHeight(), gameCanvas);
			break;
		case PLAYING:
			if (battleView != null) {
				battleView.render(g, gameCanvas.getWidth(), gameCanvas.getHeight(), gameCanvas);
			}
			break;
		}

		g.dispose();
		bs.show();
		Toolkit.getDefaultToolkit().sync();
	}

	private void renderMenu(Graphics2D g) {
		// Base Shake Offset only applies to the Equipped Status box
		int equippedOffsetX = (equippedHangpie == null && errorFlashStartTime > 0) ? shakeX : 0;
		int equippedOffsetY = (equippedHangpie == null && errorFlashStartTime > 0) ? shakeY : 0;

		// Menu Options offset is always 0, 0 unless a generic global shake is
		// implemented (not requested here)
		int menuOptionsOffsetX = 0;
		int menuOptionsOffsetY = 0;

		// 1. Draw Background
		if (background != null) {
			g.drawImage(background, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight(), gameCanvas);
		}

		// NEW: Draw Instruction Button in the upper right
		if (instructionBtnBounds != null && nameFrameImg != null) {
			int btnX = instructionBtnBounds.x;
			int btnY = instructionBtnBounds.y;
			int btnW = instructionBtnBounds.width;
			int btnH = instructionBtnBounds.height;

			g.drawImage(nameFrameImg, btnX, btnY, btnW, btnH, null);

			// Draw "?" text - Adjusted for centering
			g.setFont(GameConstants.HEADER_FONT);
			g.setColor(Color.BLACK);
			FontMetrics fm = g.getFontMetrics();
			String qText = "?";
			int textX = btnX + (btnW - fm.stringWidth(qText)) / 2;
			// Adjusted textY calculation to vertically center the large HEADER_FONT (40pt)
			// in the 50px nameframe
			// Reduced baseline adjustment from -6 to -10 to push the text up slightly
			int textY = btnY + (btnH - fm.getAscent()) / 2 + fm.getAscent() - 10;
			g.drawString(qText, textX, textY);
		}

		// 2. Draw Title Banner
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

		// 3. Draw World Level/Stage Level with Frame
		int levelFrameCenterY = 260;
		int levelFrameW = 350;
		int levelFrameH = 50;
		int levelFrameX = (gameCanvas.getWidth() - levelFrameW) / 2;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, levelFrameX, levelFrameCenterY, levelFrameW, levelFrameH, null);

			// Draw World Level Text Centered in Frame
			g.setFont(GameConstants.SUBTITLE_FONT);
			g.setColor(GameConstants.TEXT_COLOR);
			FontMetrics fm = g.getFontMetrics();

			String levelText = String.format("WORLD: %d - STAGE: %d", currentUser.getWorldLevel(),
					currentUser.getProgressLevel());
			int levelWidth = fm.stringWidth(levelText);
			int levelTextX = (gameCanvas.getWidth() - levelWidth) / 2;
			int levelTextY = levelFrameCenterY + ((levelFrameH - fm.getHeight()) / 2) + fm.getAscent();
			g.drawString(levelText, levelTextX, levelTextY);
		}

		// 4. Draw Equipped Status (Name Frame)
		int equipY = levelFrameCenterY + 70;
		int nfW = 350;
		int nfH = 50;
		int nfX = (gameCanvas.getWidth() - nfW) / 2;
		int nfY = equipY;

		// The shake is applied to the DRAWN coordinates of the frame and the text
		int drawNFX = nfX + equippedOffsetX;
		int drawNFY = nfY + equippedOffsetY;

		// NEW: Red flash for error state
		if (equippedHangpie == null && errorFlashStartTime > 0) {
			long timeElapsed = System.currentTimeMillis() - errorFlashStartTime;
			int flicker = (int) (timeElapsed / 100) % 2;
			if (flicker == 0) {
				g.setColor(new Color(200, 0, 0, 150));
				g.fillRect(drawNFX, drawNFY, nfW, nfH);
			}
		}

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, drawNFX, drawNFY, nfW, nfH, null);
		}

		// Text: Use SMALLER_BUTTON_FONT for equipped status
		g.setFont(GameConstants.SMALLER_BUTTON_FONT);
		FontMetrics fmEquip = g.getFontMetrics();
		String nameText = (equippedHangpie != null) ? equippedHangpie.getName() : "No Hangpie Equipped";

		if (equippedHangpie == null) {
			g.setColor(Color.RED);
			if (errorFlashStartTime > 0) {
				g.setColor(Color.WHITE);
			}
		} else {
			g.setColor(Color.WHITE);
		}

		int nameW = fmEquip.stringWidth(nameText);
		int nameX = (gameCanvas.getWidth() - nameW) / 2 + equippedOffsetX;
		int nameY = drawNFY + ((nfH - fmEquip.getHeight()) / 2) + fmEquip.getAscent();
		g.drawString(nameText, nameX, nameY);

		// 5. Draw Menu Options (Original position)
		drawMenuOptions(g, menuOptionsOffsetX, menuOptionsOffsetY);
	}

	private void drawInstructionModal(Graphics2D g, int width, int height) {
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, width, height);

		int mW = MODAL_WIDTH;
		int mH = MODAL_HEIGHT;
		int mX = (width - mW) / 2;
		int mY = (height - mH) / 2;

		if (modalImg != null) {
			g.drawImage(modalImg, mX, mY, mW, mH, null);
		} else {
			g.setColor(Color.GRAY);
			g.fillRect(mX, mY, mW, mH);
		}

		// Title
		g.setColor(Color.BLACK);
		g.setFont(GameConstants.HEADER_FONT);
		String title = GameConstants.INSTRUCTION_TITLE;
		FontMetrics fm = g.getFontMetrics();
		g.drawString(title, mX + (mW - fm.stringWidth(title)) / 2, mY + 50);

		// Instructions Content: Using SUBTITLE_FONT (18pt) for better fit
		g.setFont(GameConstants.SUBTITLE_FONT);
		g.setColor(Color.BLACK);

		String[] instructions = GameConstants.INSTRUCTION_TEXT.split("\n");
		// Start Y adjusted to account for HEADER_FONT (40pt) height
		int startY = mY + 95;
		FontMetrics fmSmall = g.getFontMetrics();
		int lineHeight = fmSmall.getHeight();
		int indent = 30;

		for (String line : instructions) {

			String trimmedLine = line.trim();
			int lineW = fmSmall.stringWidth(line);

			// Calculate text baseline Y position
			int textY = startY + fmSmall.getAscent();

			if (trimmedLine.isEmpty()) {
				startY += lineHeight / 3; // Smaller gap for empty line
				continue;
			}

			if (trimmedLine.endsWith(":")) {
				// Header lines (e.g., "Correct Guess:") - aligned slightly to the left
				g.drawString(line, mX + indent, textY);
			} else if (trimmedLine.startsWith("Type")) {
				// Main section headers (Type the letters...) - centered
				g.drawString(line, mX + (mW - lineW) / 2, textY);
			} else if (trimmedLine.startsWith("Your Hangpie") || trimmedLine.startsWith("Your Hangpie takes")) {
				// Details / bullet points - indented further
				g.drawString(line, mX + indent + 20, textY);
			} else if (trimmedLine.startsWith("Objectives")) {
				// Objectives header
				g.drawString(line, mX + indent, textY);
			} else if (trimmedLine.startsWith("Defeat enemy") || trimmedLine.startsWith("Win the World")) {
				// Objectives list items
				g.drawString(line, mX + indent + 20, textY);
			}

			startY += lineHeight;
		}

		// Close Instruction
		g.setFont(new Font("Monospaced", Font.BOLD, 16));
		g.setColor(Color.DARK_GRAY);
		String closeText = "[Press ESC or Click to Close]";
		g.drawString(closeText, mX + (mW - g.getFontMetrics().stringWidth(closeText)) / 2, mY + mH - 30);
	}

	private void drawMenuOptions(Graphics g, int offsetX, int offsetY) {
		g.setFont(GameConstants.UI_FONT);
		FontMetrics fm = g.getFontMetrics();

		int startY = 530 + offsetY;
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
			int x = (gameCanvas.getWidth() - textWidth) / 2 + offsetX;
			int y = startY + (i * spacing);

			menuBounds[i] = new Rectangle(x, y - fm.getAscent(), textWidth, textHeight);

			g.drawString(text, x, y);
		}
	}
}