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
import main.Main;
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

	// NEW: Inventory Confirmation State
	private boolean isInventoryConfirmationOpen = false;
	private Hangpie confirmPetCandidate = null;
	private Rectangle modalConfirmYesBounds;
	private Rectangle modalConfirmNoBounds;
	private int selectedConfirmOption = -1; // 0 for NO, 1 for YES

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

		// If the user has pets, try to equip the first one by default if none is set
		// (initial launch logic)
		if (user.getInventory() != null && !user.getInventory().isEmpty()) {
			this.equippedHangpie = user.getInventory().get(0);
			this.equippedHangpie.setCurrentHealth(this.equippedHangpie.getMaxHealth());
			this.inventoryView.setSelection(this.equippedHangpie);
		}

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
					if (isInventoryConfirmationOpen) {
						checkInventoryConfirmationHover(e.getX(), e.getY());
						return;
					}
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

					if (isInventoryConfirmationOpen) { // Handle modal click first
						handleInventoryConfirmationClick(e.getX(), e.getY());
						return;
					}

					String action = inventoryView.handleMouseClick(e.getX(), e.getY());
					if (action.equals("BACK")) {
						currentState = GameState.MENU;
						// On back, clear selection only if it was a candidate pet and not the equipped
						// one.
						// However, since handleMenuClick sets the correct equipped pet on re-entry, we
						// don't need to manually clear it here.
					} else if (action.equals("SELECT")) {
						Hangpie selectedPet = inventoryView.getSelectedPet();

						// If the pet selected is already equipped, don't show modal, just close
						// inventory
						if (selectedPet == equippedHangpie) {
							// inventoryView.selectedPet is already set by the click, so we can exit.
							currentState = GameState.MENU;
							return;
						}

						// Check if a save exists for the current user (using Main.saveManager)
						if (Main.saveManager.hasSave(currentUser.getUsername())) {
							confirmPetCandidate = selectedPet;
							isInventoryConfirmationOpen = true;
							selectedConfirmOption = -1; // Reset hover state
						} else {
							// No save, just equip and set max HP (HP RESET FIX)
							equippedHangpie = selectedPet;
							equippedHangpie.setCurrentHealth(equippedHangpie.getMaxHealth()); // HP RESET FIX
							// NEW: Sync InventoryView for drawing after immediate equip
							inventoryView.setSelection(equippedHangpie);
							currentState = GameState.MENU; // Added to fully complete the equip action
						}
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
				if (currentState == GameState.INVENTORY && !isInventoryConfirmationOpen) {
					inventoryView.handleMouseScroll(e.getWheelRotation());
				}
			}
		};

		gameCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (currentState == GameState.MENU && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					isInstructionOpen = false; // Close instruction modal on ESC
				} else if (currentState == GameState.INVENTORY && isInventoryConfirmationOpen
						&& e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					isInventoryConfirmationOpen = false;
					confirmPetCandidate = null;
					inventoryView.setSelection(equippedHangpie); // Restore selection to equipped pet
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

	// Logic for handling the confirmation modal on equip
	private void checkInventoryConfirmationHover(int mx, int my) {
		selectedConfirmOption = -1;

		// Note: The order here must match the drawing order (YES then NO)
		if (modalConfirmYesBounds != null && modalConfirmYesBounds.contains(mx, my)) {
			selectedConfirmOption = 1; // YES
		} else if (modalConfirmNoBounds != null && modalConfirmNoBounds.contains(mx, my)) {
			selectedConfirmOption = 0; // NO
		}
	}

	private void handleInventoryConfirmationClick(int mx, int my) {
		// Note: The order here must match the drawing order (YES then NO)
		if (modalConfirmYesBounds != null && modalConfirmYesBounds.contains(mx, my)) {
			// YES: Equip the new pet, delete the save, and close the modal
			equippedHangpie = confirmPetCandidate;
			equippedHangpie.setCurrentHealth(equippedHangpie.getMaxHealth()); // HP RESET FIX
			Main.saveManager.deleteSave(currentUser.getUsername());
			inventoryView.setSelection(equippedHangpie); // ** NEW: Sync InventoryView for drawing **
			isInventoryConfirmationOpen = false;
			confirmPetCandidate = null;
			currentState = GameState.MENU; // Added to fully complete the equip action
			System.out.println("[Game] Equipped new Hangpie and deleted active save.");
		} else if (modalConfirmNoBounds != null && modalConfirmNoBounds.contains(mx, my)) {
			// NO: Cancel and close the modal
			isInventoryConfirmationOpen = false;
			confirmPetCandidate = null;
			// ** NEW: If cancelled, reset the temporary visual selection back to the actual
			// equipped pet **
			inventoryView.setSelection(equippedHangpie);
		}
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
			// ** NEW: Ensure the green outline starts on the CURRENTLY equipped pet **
			inventoryView.setSelection(equippedHangpie);
			isInventoryConfirmationOpen = false; // Reset just in case
			confirmPetCandidate = null;
			selectedConfirmOption = -1;

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
			if (isInventoryConfirmationOpen) { // Render confirmation modal
				renderInventoryConfirmation(g, gameCanvas.getWidth(), gameCanvas.getHeight());
			}
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

	// Method to draw the inventory confirmation modal with new styling
	private void renderInventoryConfirmation(Graphics2D g, int width, int height) {
		if (!isInventoryConfirmationOpen)
			return;

		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, height);

		int mW = 600;
		int mH = 250;
		int mX = (width - mW) / 2;
		int mY = (height - mH) / 2;

		// Draw Modal Background
		if (modalImg != null) {
			g.drawImage(modalImg, mX, mY, mW, mH, null);
		} else {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(mX, mY, mW, mH);
		}

		// --- TITLE (REDUCED FONT SIZE) ---
		g.setColor(Color.BLACK);
		Font titleFont = new Font("Monospaced", Font.BOLD, 30); // Reduced from 40pt (HEADER_FONT) to 30pt
		g.setFont(titleFont);
		String title = "WARNING: Active Save Detected";
		FontMetrics fmTitle = g.getFontMetrics();
		g.drawString(title, mX + (mW - fmTitle.stringWidth(title)) / 2, mY + 50);

		// --- MESSAGES (REDUCED TO 16PT) ---
		g.setFont(GameConstants.INSTRUCTION_FONT); // 16pt font
		FontMetrics fmMessage = g.getFontMetrics();

		// Message 1 (Red)
		g.setColor(Color.RED);
		String msg = "Switching pets will DELETE your current battle save.";
		g.drawString(msg, mX + (mW - fmMessage.stringWidth(msg)) / 2, mY + 100);

		// Message 2 (Black)
		g.setColor(Color.BLACK);
		String petName = (confirmPetCandidate != null) ? confirmPetCandidate.getName() : "this pet";
		String question = "Are you sure you want to equip " + petName + " and proceed?";
		g.drawString(question, mX + (mW - fmMessage.stringWidth(question)) / 2, mY + 125); // Adjusted Y up

		// --- BUTTONS (YES | NO, Centered & Stable) ---
		g.setFont(GameConstants.BUTTON_FONT);
		FontMetrics fmButton = g.getFontMetrics();
		int btnH = 40;

		// --- PARAMETERS FOR BUTTON STYLING AND POSITIONING ---
		// Adjust these parameters if the buttons look off-center or too close/far apart
		int buttonSpacing = 30;
		int btnAreaY = mY + 175; // Baseline Y for button text

		String yesTxtContent = "YES";
		String noTxtContent = "NO";

		String yesTxtHover = "> " + yesTxtContent + " <";
		String noTxtHover = "> " + noTxtContent + " <";

		// Calculate Max Widths for Stability
		int yesTxtWMax = fmButton.stringWidth(yesTxtHover);
		int noTxtWMax = fmButton.stringWidth(noTxtHover);

		// Calculate Horizontal Position
		int totalBtnWidth = yesTxtWMax + noTxtWMax + buttonSpacing;
		int startX = mX + (mW - totalBtnWidth) / 2; // Starting X to center the block

		int currentX = startX;

		// 1. YES Button (Draws first)
		String yesTxt = (selectedConfirmOption == 1) ? yesTxtHover : yesTxtContent;
		int yesTxtWidthNormal = fmButton.stringWidth(yesTxtContent); // Width without arrows

		// Calculate necessary offset to center the normal text within the hover text
		// width
		int yesDrawOffsetX = (yesTxtWMax - yesTxtWidthNormal) / 2;

		// Bounds for click detection (using max width)
		modalConfirmYesBounds = new Rectangle(currentX, btnAreaY, yesTxtWMax, btnH);

		g.setColor((selectedConfirmOption == 1) ? GameConstants.SELECTION_COLOR : Color.BLACK);

		if (selectedConfirmOption == 1) {
			// Draw hovered text (full width, starts at currentX)
			g.drawString(yesTxt, currentX, btnAreaY + fmButton.getAscent());
		} else {
			// Draw normal text (starts at currentX + offset for visual stability)
			g.drawString(yesTxt, currentX + yesDrawOffsetX, btnAreaY + fmButton.getAscent());
		}

		currentX += yesTxtWMax + buttonSpacing; // Advance X using the max width

		// 2. NO Button (Draws second)
		String noTxt = (selectedConfirmOption == 0) ? noTxtHover : noTxtContent;
		int noTxtWidthNormal = fmButton.stringWidth(noTxtContent); // Width without arrows

		// Calculate necessary offset to center the normal text within the hover text
		// width
		int noDrawOffsetX = (noTxtWMax - noTxtWidthNormal) / 2;

		// Bounds for click detection (using max width)
		modalConfirmNoBounds = new Rectangle(currentX, btnAreaY, noTxtWMax, btnH);

		g.setColor((selectedConfirmOption == 0) ? GameConstants.SELECTION_COLOR : Color.BLACK);

		if (selectedConfirmOption == 0) {
			// Draw hovered text (full width, starts at currentX)
			g.drawString(noTxt, currentX, btnAreaY + fmButton.getAscent());
		} else {
			// Draw normal text (starts at currentX + offset for visual stability)
			g.drawString(noTxt, currentX + noDrawOffsetX, btnAreaY + fmButton.getAscent());
		}
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

		// Instructions Content: Using the new, smaller INSTRUCTION_FONT (16pt)
		g.setFont(GameConstants.INSTRUCTION_FONT);
		g.setColor(Color.BLACK);

		String content = GameConstants.INSTRUCTION_TEXT;
		String[] lines = content.split("\n");

		// Word Wrapping parameters
		int wrapWidth = mW - 60; // Modal width (400) - 30px left padding - 30px right padding
		int startX = mX + 30; // 30px from left edge of modal
		int startY = mY + 95; // Y position after the title
		FontMetrics fmSmall = g.getFontMetrics();
		int lineHeight = fmSmall.getHeight();
		int indent = 20;

		for (String line : lines) {
			String trimmedLine = line.trim();

			if (trimmedLine.isEmpty()) {
				startY += lineHeight / 3;
				continue;
			}

			// Special Handling for Headers/Non-wrapping lines
			if (trimmedLine.endsWith(":") || trimmedLine.startsWith("Objectives")) {
				// Print as-is, centered or left-aligned
				int lineW = fmSmall.stringWidth(line);
				int textX = startX;
				int textY = startY + fmSmall.getAscent();
				g.drawString(line, textX, textY);
				startY += lineHeight;
			}
			// UPDATED: Added logic to treat the "Type the letters..." line as a normal
			// wrapping line
			else if (trimmedLine.startsWith("Type")) {
				// Handle "Type the letters on your keyboard to guess the word."
				String[] words = trimmedLine.split(" ");
				StringBuilder currentLine = new StringBuilder();
				int currentX = mX + (mW - wrapWidth) / 2; // Center alignment for the introductory line

				for (String word : words) {
					// Check if adding the next word (plus a space) exceeds the wrap width
					if (fmSmall.stringWidth(currentLine.toString() + " " + word) > wrapWidth) {
						// Draw the current line
						int textY = startY + fmSmall.getAscent();
						g.drawString(currentLine.toString().trim(), currentX, textY);

						// Start a new line
						startY += lineHeight;
						currentLine = new StringBuilder();
					}

					// Append the word (with a space if not the very first word on a line)
					if (currentLine.length() > 0) {
						currentLine.append(" ");
					}
					currentLine.append(word);
				}

				// Draw the remaining part of the line
				if (currentLine.length() > 0) {
					int textY = startY + fmSmall.getAscent();
					g.drawString(currentLine.toString().trim(), currentX, textY);
					startY += lineHeight;
				}
			}
			// Handle all other wrapping/bullet lines
			else {
				// Apply word wrap to instruction/list items
				String[] words = trimmedLine.split(" ");
				StringBuilder currentLine = new StringBuilder();
				int currentX = startX + (trimmedLine.startsWith("Your") || trimmedLine.startsWith("Defeat")
						|| trimmedLine.startsWith("Win") ? indent : 0);

				for (String word : words) {
					// Check if adding the next word (plus a space) exceeds the wrap width
					if (fmSmall.stringWidth(currentLine.toString() + " " + word) > wrapWidth) {
						// Draw the current line
						int textY = startY + fmSmall.getAscent();
						g.drawString(currentLine.toString().trim(), currentX, textY);

						// Start a new line
						startY += lineHeight;
						currentLine = new StringBuilder();
						currentX = startX + (trimmedLine.startsWith("Your") || trimmedLine.startsWith("Defeat")
								|| trimmedLine.startsWith("Win") ? indent * 2 : indent); // Increase indent for wrapped
																							// lines to preserve
																							// bullet-style
					}

					// Append the word (with a space if not the very first word on a line)
					if (currentLine.length() > 0) {
						currentLine.append(" ");
					}
					currentLine.append(word);
				}

				// Draw the remaining part of the line
				if (currentLine.length() > 0) {
					int textY = startY + fmSmall.getAscent();
					g.drawString(currentLine.toString().trim(), currentX, textY);
					startY += lineHeight;
				}
			}
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