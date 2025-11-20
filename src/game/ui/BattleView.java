package game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import game.GameConstants;
import main.Main;
import models.Character;
import models.Enemy;
import models.Hangpie;
import models.User;
import utils.AssetLoader;
import utils.WordBank;

public class BattleView {

	private User playerUser;
	private Hangpie playerPet;
	private Enemy currentEnemy;

	// Battle State
	private String secretWord;
	private String clue;
	private Set<java.lang.Character> guessedLetters;
	private boolean battleOver = false;
	private boolean playerWon = false;
	private boolean exitRequested = false;

	// Rewards
	private int goldReward = 0;
	private boolean rewardsClaimed = false;

	private String message = "";

	// Animation Timers
	private long actionStartTime = 0;
	private boolean isAnimatingAction = false;
	private final int ANIMATION_DURATION = 1000;

	// Assets
	private Image bgImage;
	private Image nameFrameImg;
	private Image settingsImg;
	private Image modalImg;

	// Heart Assets
	private Image heartImg;
	private Image halfHeartImg;
	private Image emptyHeartImg;

	private Random random = new Random();

	// Settings Modal State
	private boolean isSettingsOpen = false;
	private boolean isExitConfirmation = false;

	// UI Buttons (Clickable Areas)
	private Rectangle settingsBtnBounds;
	private Rectangle modalSaveBounds;
	private Rectangle modalMenuBounds;
	private Rectangle modalExitBounds;

	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();

		// Reset animations
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		loadAssets();
		initBattle();
	}

	private void loadAssets() {
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 200, 50);
		settingsImg = AssetLoader.loadImage(GameConstants.SETTINGS_BTN_IMG, 50, 50);
		modalImg = AssetLoader.loadImage(GameConstants.MODAL_IMG, 400, 300);

		// Load Hearts
		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 20, 20);
		halfHeartImg = AssetLoader.loadImage(GameConstants.HALF_HEART_IMG, 20, 20);
		emptyHeartImg = AssetLoader.loadImage(GameConstants.EMPTY_HEART_IMG, 20, 20);
	}

	private void initBattle() {
		// Reset Round State
		this.guessedLetters.clear();
		this.battleOver = false;
		this.playerWon = false;
		this.rewardsClaimed = false;
		this.message = "";
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());

		// 1. Pick Word
		WordBank.WordData data = WordBank.getRandomWord(playerUser.getWorldLevel());
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;

		// 2. Pick Random Background (Updated for range 1-22, skipping missing 19)
		// Available BGs: 1-18, 20-22. (19 is missing from assets)
		int[] validBgIndices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21, 22 };

		int bgIndex = random.nextInt(validBgIndices.length);
		int bgNum = validBgIndices[bgIndex];

		String bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
		this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

		// 3. Spawn Random Enemy
		int enemyHp = 3 + playerUser.getWorldLevel();
		int enemyAtk = 2 * (1 + (playerUser.getWorldLevel() / 2));

		// Define the available enemy types and their folder paths
		String[][] availableEnemies = { { "Lava Worm", "enemies/enemies/worm" },
				{ "Evil Eye", "enemies/enemies/evil_eye" }, { "Goblin", "enemies/enemies/goblin" },
				{ "Mushroom", "enemies/enemies/mushroom" }, { "Skeleton", "enemies/enemies/skeleton" } };

		// Select a random enemy from the list
		int idx = random.nextInt(availableEnemies.length);
		String eName = availableEnemies[idx][0];
		String ePath = availableEnemies[idx][1];

		this.currentEnemy = new Enemy(eName, enemyHp, playerUser.getWorldLevel(), enemyAtk, ePath);

		// 4. Preload Assets
		System.out.println("[Battle] Preloading assets...");
		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		// Initialize button bounds
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, 20, 50, 50);
	}

	public void update() {
		if (isSettingsOpen)
			return;

		if (isAnimatingAction) {
			if (System.currentTimeMillis() - actionStartTime > ANIMATION_DURATION) {
				isAnimatingAction = false;

				// Reset idle states if still alive
				if (playerPet.isAlive())
					playerPet.setAnimationState(Hangpie.AnimState.IDLE);
				if (currentEnemy.isAlive())
					currentEnemy.setAnimationState(Enemy.AnimState.IDLE);

				// Check deaths
				if (!currentEnemy.isAlive()) {
					handleWin();
				} else if (!playerPet.isAlive()) {
					handleLoss();
				}
			}
		}
	}

	private void handleWin() {
		battleOver = true;
		playerWon = true;
		currentEnemy.setAnimationState(Enemy.AnimState.DEATH);

		if (!rewardsClaimed) {
			goldReward = 50 + (playerUser.getWorldLevel() * 10);
			playerUser.addGold(goldReward);
			playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);

			if (playerUser.getProgressLevel() % 5 == 0) {
				playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
			}

			Main.userManager.updateUser(playerUser);
			rewardsClaimed = true;
		}
	}

	private void handleLoss() {
		battleOver = true;
		playerWon = false;
		playerPet.setAnimationState(Hangpie.AnimState.DEATH);
	}

// --- Input Handling ---

	public String handleMouseClick(int x, int y) {
		if (isSettingsOpen) {
			if (modalSaveBounds != null && modalSaveBounds.contains(x, y)) {
				Main.userManager.updateUser(playerUser);
				message = "Game Saved!";
				isSettingsOpen = false;
				isExitConfirmation = false;
				return "NONE";
			} else if (modalMenuBounds != null && modalMenuBounds.contains(x, y)) {
				return "MENU";
			} else if (modalExitBounds != null && modalExitBounds.contains(x, y)) {
				if (!isExitConfirmation) {
					isExitConfirmation = true;
				} else {
					return "EXIT";
				}
				return "NONE";
			}
		} else {
			if (settingsBtnBounds != null && settingsBtnBounds.contains(x, y)) {
				isSettingsOpen = true;
				isExitConfirmation = false;
				return "NONE";
			}
		}
		return "NONE";
	}

	public void handleKeyPress(int keyCode, char keyChar) {
		if (isSettingsOpen) {
			if (keyCode == KeyEvent.VK_ESCAPE) {
				isSettingsOpen = false;
				isExitConfirmation = false;
			}
			return;
		}

		if (battleOver) {
			handleMenuInput(keyCode);
			return;
		}

		if (isAnimatingAction)
			return;

		char guess = java.lang.Character.toUpperCase(keyChar);
		if (guess < 'A' || guess > 'Z')
			return;

		if (guessedLetters.contains(guess)) {
			message = "Already guessed " + guess + "!";
			return;
		}

		guessedLetters.add(guess);

		boolean isCorrect = false;
		for (char c : secretWord.toCharArray()) {
			if (c == guess)
				isCorrect = true;
		}

		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;

		int playerDmg = 1 + (playerUser.getWorldLevel() / 2);
		int enemyDmg = 2 * playerDmg;

		if (isCorrect) {
			message = "Correct! Hit!";
			playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
			currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);
			currentEnemy.takeDamage(playerDmg);
		} else {
			message = "Wrong! Ouch!";
			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);
			playerPet.takeDamage(enemyDmg);
		}
	}

	private void handleMenuInput(int keyCode) {
		if (playerWon) {
			if (keyCode == KeyEvent.VK_ENTER) {
				initBattle();
			} else if (keyCode == KeyEvent.VK_ESCAPE) {
				exitRequested = true;
			}
		} else {
			if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE) {
				exitRequested = true;
			}
		}
	}

	public boolean isExitRequested() {
		return exitRequested;
	}

	public void render(Graphics2D g, int width, int height, ImageObserver observer) {
		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}

		// --- Top Backdrop (HUD Area) ---
		// Increased Height to prevent overlap between Clue and Letters
		int backdropH = 160;
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		// Draw Player and Enemy UI (Now Positioned BELOW the backdrop)
		drawCharacterUI(g, width, backdropH, observer);

		// Draw Settings Button
		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x, settingsBtnBounds.y, settingsBtnBounds.width,
					settingsBtnBounds.height, observer);
		}

		// Draw Message (Correct/Wrong) - Positioned just below the backdrop with NameFrame Background
		if (!message.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			FontMetrics fm = g.getFontMetrics();
			int msgW = fm.stringWidth(message);

			// Dynamic Background Calculation
			int bgW = msgW + 60; // Padding
			int bgH = 50;
			int bgX = (width - bgW) / 2;
			int bgY = backdropH + 15; // Position below backdrop

			if (nameFrameImg != null) {
				g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, observer);
			}

			g.setColor(Color.YELLOW);
			// Precise Vertical Centering
			int textX = (width - msgW) / 2;
			int textY = bgY + ((bgH - fm.getHeight()) / 2) + fm.getAscent();
			g.drawString(message, textX, textY);
		}

		// --- Character Rendering (Scaled & Centered) ---
		// Lowered groundY to height - 100 to match the yellow line visual
		int groundY = height - 50;
		int scaleFactor = 4;

		// Render Enemy
		Image enemyImg = currentEnemy.getCurrentImage();
		if (enemyImg != null) {
			int eW = enemyImg.getWidth(observer);
			int eH = enemyImg.getHeight(observer);

			if (eW > 0 && eH > 0) {
				int drawW = eW * scaleFactor;
				int drawH = eH * scaleFactor;

				// UI Center Right is at: (width - 250) + (200/2) = width - 150
				// Center character at X = (width - 150) - (drawW / 2)
				int drawX = (width - 150) - (drawW / 2);
				int drawY = groundY - drawH;

				g.drawImage(enemyImg, drawX, drawY, drawW, drawH, observer);
			}
		}

		// Render Player
		Image playerImg = playerPet.getCurrentImage();
		if (playerImg != null) {
			int pW = playerImg.getWidth(observer);
			int pH = playerImg.getHeight(observer);

			if (pW > 0 && pH > 0) {
				int drawW = pW * scaleFactor;
				int drawH = pH * scaleFactor;

				// UI Center Left is at: 30 + (200/2) = 130
				// Center character at X = 130 - (drawW / 2)
				int drawX = 130 - (drawW / 2);
				int drawY = groundY - drawH;

				g.drawImage(playerImg, drawX, drawY, drawW, drawH, observer);
			}
		}

		drawWordPuzzle(g, width, height, observer);

		if (isSettingsOpen) {
			drawSettingsModal(g, width, height);
		} else if (battleOver) {
			drawEndScreen(g, width, height);
		}
	}

	private void drawCharacterUI(Graphics2D g, int width, int backdropBottomY, ImageObserver observer) {
		// Positioned immediately BELOW the backdrop
		int topY = backdropBottomY + 10;
		
		// Revert to original margin startX
		int startX = 30;
		
		// Expanded Frame Dimensions
		int frameW = 220;
		int frameH = 70;
		
		// Revert Positions (Far Left / Far Right)
		int pFrameX = startX;
		int eFrameX = width - startX - frameW;

		// Fonts
		Font levelFont = new Font("Monospaced", Font.BOLD, 12);
		Font nameFont = new Font("Monospaced", Font.BOLD, 16);

		// --- PLAYER UI (LEFT) ---

		// Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, pFrameX, topY, frameW, frameH, null);
		}

		g.setColor(Color.WHITE);

		// 1. Player Level (Top) - Pushed down slightly
		g.setFont(levelFont);
		String pLevel = "LEVEL " + playerUser.getWorldLevel();
		FontMetrics fm = g.getFontMetrics();
		int pLevelX = pFrameX + (frameW - fm.stringWidth(pLevel)) / 2;
		int pLevelY = topY + 28; // Moved down for tighter spacing
		g.drawString(pLevel, pLevelX, pLevelY);

		// 2. Player Name (Bottom) - Pulled up slightly
		g.setFont(nameFont);
		fm = g.getFontMetrics();
		String pName = playerPet.getName();
		int pNameX = pFrameX + (frameW - fm.stringWidth(pName)) / 2;
		int pNameY = topY + 46; // Reduced gap between level and name
		g.drawString(pName, pNameX, pNameY);

		// Heart Display (Below Frame)
		drawHearts(g, playerPet, pFrameX + 10, topY + frameH + 5, observer);

		// --- ENEMY UI (RIGHT) ---

		// Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, eFrameX, topY, frameW, frameH, null);
		}

		g.setColor(Color.WHITE);

		// 1. Enemy Level
		g.setFont(levelFont);
		String eLevel = "LEVEL " + playerUser.getWorldLevel();
		fm = g.getFontMetrics();
		int eLevelX = eFrameX + (frameW - fm.stringWidth(eLevel)) / 2;
		g.drawString(eLevel, eLevelX, pLevelY);

		// 2. Enemy Name
		g.setFont(nameFont);
		fm = g.getFontMetrics();
		String eName = currentEnemy.getName();
		int eNameX = eFrameX + (frameW - fm.stringWidth(eName)) / 2;
		g.drawString(eName, eNameX, pNameY);

		// Heart Display (Below Name)
		drawHearts(g, currentEnemy, eFrameX + 10, topY + frameH + 5, observer);
	}

	private void drawHearts(Graphics2D g, Character character, int x, int y, ImageObserver observer) {
		int maxHp = character.getMaxHealth();
		int currentHp = character.getCurrentHealth();

		// 2 HP = 1 Heart
		// 1 HP = Half Heart

		int totalHearts = (maxHp + 1) / 2;

		int heartSize = 15;
		int spacing = 3;

		for (int i = 0; i < totalHearts; i++) {
			// Index 0 (Heart 1) represents HP 1 & 2
			int hpThresholdForFull = (i + 1) * 2;
			int hpThresholdForHalf = hpThresholdForFull - 1;

			Image imgToDraw;

			if (currentHp >= hpThresholdForFull) {
				imgToDraw = heartImg;
			} else if (currentHp >= hpThresholdForHalf) {
				imgToDraw = halfHeartImg;
			} else {
				imgToDraw = emptyHeartImg;
			}

			if (imgToDraw != null) {
				g.drawImage(imgToDraw, x + (i * (heartSize + spacing)), y, heartSize, heartSize, observer);
			}
		}
	}

	private void drawWordPuzzle(Graphics2D g, int width, int height, ImageObserver obs) {
		int spacing = 60;

		// Adjusted positions to avoid overlap
		// Clue Center Y approx 50
		// Letters Baseline approx 130
		int clueCenterY = 50;
		int lettersY = 130; // Moved down from 95

		g.setFont(GameConstants.SUBTITLE_FONT);
		String clueText = "CLUE: " + clue;
		FontMetrics fm = g.getFontMetrics();
		int textW = fm.stringWidth(clueText);

		// Draw Clue Background (Scaled NameFrame)
		int bgW = textW + 60; // Extra width for padding
		int bgH = 50; // Fixed height
		int bgX = (width - bgW) / 2;
		int bgY = clueCenterY - (bgH / 2); // Centered on clueCenterY

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, obs);
		}

		g.setColor(Color.CYAN);

		// Precise Vertical Centering for Clue Text
		int textX = (width - textW) / 2;
		int textY = bgY + ((bgH - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(clueText, textX, textY);

		int totalWidth = secretWord.length() * spacing;
		int startX = (width - totalWidth) / 2;
		int currentX = startX;

		for (char c : secretWord.toCharArray()) {
			if (c == ' ') {
				currentX += spacing;
				continue;
			}

			if (guessedLetters.contains(c)) {
				String letterPath = "images/utilities/letters/" + c + ".png";
				Image letterImg = AssetLoader.loadImage(letterPath, 40, 40);
				if (letterImg != null) {
					g.drawImage(letterImg, currentX, lettersY - 30, 40, 40, obs);
				} else {
					g.setColor(Color.WHITE);
					g.drawString(String.valueOf(c), currentX + 10, lettersY);
				}
			} else {
				g.setColor(Color.WHITE);
				g.fillRect(currentX, lettersY + 5, 40, 4);
			}
			currentX += spacing;
		}
	}

	private void drawSettingsModal(Graphics2D g, int width, int height) {
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, width, height);

		int mW = 400;
		int mH = 300;
		int mX = (width - mW) / 2;
		int mY = (height - mH) / 2;

		if (modalImg != null) {
			g.drawImage(modalImg, mX, mY, mW, mH, null);
		} else {
			g.setColor(Color.GRAY);
			g.fillRect(mX, mY, mW, mH);
		}

		g.setColor(Color.BLACK);
		g.setFont(GameConstants.HEADER_FONT);
		String title = "PAUSED";
		g.drawString(title, mX + (mW - g.getFontMetrics().stringWidth(title)) / 2, mY + 60);

		// Buttons
		g.setFont(GameConstants.BUTTON_FONT);
		int btnH = 40;
		int btnGap = 15;
		int startBtnY = mY + 100;

		// Save
		String saveTxt = "SAVE GAME";
		int saveW = g.getFontMetrics().stringWidth(saveTxt);
		modalSaveBounds = new Rectangle(mX + (mW - saveW) / 2 - 10, startBtnY, saveW + 20, btnH);
		g.drawString(saveTxt, mX + (mW - saveW) / 2, startBtnY + 25);

		// Menu
		String menuTxt = "MAIN MENU";
		int menuW = g.getFontMetrics().stringWidth(menuTxt);
		modalMenuBounds = new Rectangle(mX + (mW - menuW) / 2 - 10, startBtnY + btnH + btnGap, menuW + 20, btnH);
		g.drawString(menuTxt, mX + (mW - menuW) / 2, startBtnY + btnH + btnGap + 25);

		// Exit
		String exitTxt = "EXIT GAME";
		if (isExitConfirmation)
			exitTxt = "CONFIRM EXIT?";
		int exitW = g.getFontMetrics().stringWidth(exitTxt);

		g.setColor(isExitConfirmation ? Color.RED : Color.BLACK);
		modalExitBounds = new Rectangle(mX + (mW - exitW) / 2 - 10, startBtnY + (2 * (btnH + btnGap)), exitW + 20,
				btnH);
		g.drawString(exitTxt, mX + (mW - exitW) / 2, startBtnY + (2 * (btnH + btnGap)) + 25);

		// Warning text for Exit
		if (isExitConfirmation) {
			g.setFont(new Font("Monospaced", Font.PLAIN, 12));
			String warn = "Unsaved progress will be lost!";
			g.drawString(warn, mX + (mW - g.getFontMetrics().stringWidth(warn)) / 2, mY + mH - 20);
		}
	}

	private void drawEndScreen(Graphics2D g, int width, int height) {
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRect(0, 0, width, height);

		g.setFont(GameConstants.HEADER_FONT);
		FontMetrics fm = g.getFontMetrics();

		if (playerWon) {
			g.setColor(Color.GREEN);
			String title = "VICTORY!";
			g.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 50);

			g.setFont(GameConstants.UI_FONT);
			g.setColor(Color.YELLOW);
			String rewardMsg = "You earned " + goldReward + " Gold!";
			g.drawString(rewardMsg, (width - g.getFontMetrics().stringWidth(rewardMsg)) / 2, height / 2);

			g.setColor(Color.WHITE);
			g.setFont(GameConstants.BUTTON_FONT);
			String opt1 = "[ENTER] Next Battle";
			String opt2 = "[ESC] Main Menu";
			g.drawString(opt1, (width - g.getFontMetrics().stringWidth(opt1)) / 2, height / 2 + 80);
			g.drawString(opt2, (width - g.getFontMetrics().stringWidth(opt2)) / 2, height / 2 + 120);

		} else {
			g.setColor(Color.RED);
			String title = "YOU DIED";
			g.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 50);

			g.setFont(GameConstants.UI_FONT);
			g.setColor(Color.WHITE);
			String opt = "Press [ENTER] or [ESC] to Return";
			g.drawString(opt, (width - g.getFontMetrics().stringWidth(opt)) / 2, height / 2 + 50);
		}
	}
}