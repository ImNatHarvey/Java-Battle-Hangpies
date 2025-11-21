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
import models.BattleState;
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

// Logic for carrying over incomplete words to next level
	private boolean shouldCarryOverWord = false;

// Rewards
	private int goldReward = 0;
	private boolean rewardsClaimed = false;

	private String message = "";
	private Color messageColor = Color.YELLOW; // Default color

// Animation Timers & States
	private long actionStartTime = 0;
	private boolean isAnimatingAction = false;
	private final int ACTION_DURATION = 1700; // 1.7 Seconds for Attack/Damage

	private boolean isDeathAnimating = false;
	private long deathStartTime = 0;
	private final int DEATH_DURATION = 3000; // 4 Seconds for Death Animation

// Assets
	private Image bgImage;
	private Image nameFrameImg;
	private Image settingsImg;
	private Image modalImg;

// Frame used for the Level Indicator
	private Image levelFrameImg;

// Heart Assets
	private Image heartImg;
	private Image halfHeartImg;
	private Image emptyHeartImg;

	private Random random = new Random();

// Settings Modal State
	private boolean isSettingsOpen = false;
	private boolean isExitConfirmation = false;
	private boolean isMenuConfirmation = false; // NEW: For Main Menu confirm

// UI Buttons (Clickable Areas)
	private Rectangle settingsBtnBounds;

// Modal Buttons
	private Rectangle modalContinueBounds;
	private Rectangle modalSaveBounds;
	private Rectangle modalMenuBounds;
	private Rectangle modalExitBounds;

// Hover State for Modal
// 0: Continue, 1: Save, 2: Menu, 3: Exit, -1: None
	private int selectedModalOption = -1;

// Layout Constants
	private final int TOP_BAR_Y = 20;
	private final int TOP_BAR_HEIGHT = 50;

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
		levelFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 150, 50);
		settingsImg = AssetLoader.loadImage(GameConstants.SETTINGS_BTN_IMG, 50, 50);
		modalImg = AssetLoader.loadImage(GameConstants.MODAL_IMG, 400, 300);

		// Load Hearts
		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 20, 20);
		halfHeartImg = AssetLoader.loadImage(GameConstants.HALF_HEART_IMG, 20, 20);
		emptyHeartImg = AssetLoader.loadImage(GameConstants.EMPTY_HEART_IMG, 20, 20);
	}

	private void initBattle() {
		// Reset Round State
		this.battleOver = false;
		this.playerWon = false;
		this.rewardsClaimed = false;
		this.message = "";
		this.messageColor = Color.YELLOW;

		// Reset Animation States
		this.isAnimatingAction = false;
		this.isDeathAnimating = false;

		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		// --- LOAD GAME LOGIC ---
		if (Main.saveManager.hasSave(playerUser.getUsername()) && !shouldCarryOverWord) {
			System.out.println("[Battle] Found save file. Loading...");
			loadGame();
			return; // Skip standard generation
		}

		// Standard Init
		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());

		// 1. Word Logic (New vs Carry Over)
		if (!shouldCarryOverWord) {
			generateNewWord();
		} else {
			System.out.println("[Battle] Carrying over word: " + secretWord);
			shouldCarryOverWord = false;
		}

		// 2. Generate Enemy
		generateEnemy();

		// 4. Preload Assets
		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		// Initialize button bounds
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, 50, 50);
	}

	private void loadGame() {
		BattleState save = Main.saveManager.loadBattle(playerUser.getUsername());

		// Restore Word
		this.secretWord = save.getSecretWord();
		this.clue = save.getClue();
		this.guessedLetters.clear();
		for (char c : save.getGuessedLetters().toCharArray()) {
			this.guessedLetters.add(c);
		}

		// Restore Enemy
		this.currentEnemy = new Enemy(save.getEnemyName(), save.getEnemyMaxHp(), save.getEnemyLevel(),
				save.getEnemyAtk(), save.getEnemyImageFolder());
		this.currentEnemy.setCurrentHealth(save.getEnemyHp());

		// Restore Player HP
		// Note: The specific pet instance is passed in constructor. We just set its HP.
		// Ideally we check if IDs match, but for now we assume the user equipped the
		// same pet or we enforce it.
		// For robustness, we update the current pet's HP.
		this.playerPet.setCurrentHealth(save.getPlayerPetHp());

		// Load Background (Random standard BG for now to keep it simple, or could save
		// BG ID too)
		generateBackground(false);

		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, 50, 50);

		message = "Game Loaded!";
		messageColor = Color.GREEN;
	}

	private void saveGame() {
		StringBuilder sb = new StringBuilder();
		for (java.lang.Character c : guessedLetters) {
			sb.append(c);
		}

		BattleState state = new BattleState(playerUser.getUsername(), secretWord, clue, sb.toString(),
				currentEnemy.getName(), currentEnemy.getCurrentHealth(), currentEnemy.getMaxHealth(),
				currentEnemy.getAttackPower(), currentEnemy.getFolderName(), currentEnemy.getLevel(),
				playerPet.getUniqueId(), playerPet.getCurrentHealth());

		Main.saveManager.saveBattle(state);
		message = "Game Saved!";
		messageColor = Color.CYAN;
	}

	private void generateEnemy() {
		int currentWorld = playerUser.getWorldLevel();
		int currentProg = playerUser.getProgressLevel();

		String enemyName;
		String enemyPath;

		int baseHp = 10 + ((currentWorld - 1) * 5);
		int baseAtk = currentWorld;
		int enemyHp = baseHp;
		int enemyAtk = baseAtk;

		boolean isBoss = (currentProg == 5);
		generateBackground(isBoss);

		if (isBoss) {
			// Boss Stats
			enemyHp = baseHp + 5;
			enemyAtk = baseAtk + 1;

			long seed = playerUser.getUsername().hashCode() + currentWorld;
			Random bossRandom = new Random(seed);
			int bossIndex = bossRandom.nextInt(3) + 1;

			switch (bossIndex) {
			case 1:
				enemyName = "Baal";
				enemyPath = "enemies/boss/boss1";
				break;
			case 2:
				enemyName = "Abaddon";
				enemyPath = "enemies/boss/boss2";
				break;
			case 3:
				enemyName = "Beelzebub";
				enemyPath = "enemies/boss/boss3";
				break;
			default:
				enemyName = "Baal";
				enemyPath = "enemies/boss/boss1";
				break;
			}
		} else {
			String[][] availableEnemies = { { "Lava Worm", "enemies/enemies/worm" },
					{ "Evil Eye", "enemies/enemies/evil_eye" }, { "Goblin", "enemies/enemies/goblin" },
					{ "Mushroom", "enemies/enemies/mushroom" }, { "Skeleton", "enemies/enemies/skeleton" } };
			int idx = random.nextInt(availableEnemies.length);
			enemyName = availableEnemies[idx][0];
			enemyPath = availableEnemies[idx][1];
		}

		this.currentEnemy = new Enemy(enemyName, enemyHp, currentWorld, enemyAtk, enemyPath);
	}

	private void generateBackground(boolean isBoss) {
		if (isBoss) {
			int bossBgNum = random.nextInt(2) + 1;
			String bgPath = GameConstants.BG_DIR + "battle_bg/boss_bg" + bossBgNum + ".gif";
			this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
		} else {
			int[] validBgIndices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21 };
			int bgIndex = random.nextInt(validBgIndices.length);
			int bgNum = validBgIndices[bgIndex];
			String bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
			this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
		}
	}

	private void generateNewWord() {
		this.guessedLetters.clear();
		WordBank.WordData data = WordBank.getRandomWord(playerUser.getWorldLevel());
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;
	}

	private boolean isWordCompleted() {
		for (char c : secretWord.toCharArray()) {
			if (c != ' ' && !guessedLetters.contains(c)) {
				return false;
			}
		}
		return true;
	}

	public void update() {
		if (isSettingsOpen)
			return;

		if (isAnimatingAction) {
			if (System.currentTimeMillis() - actionStartTime > ACTION_DURATION) {
				isAnimatingAction = false;
				checkRoundResult();
			}
		}

		if (isDeathAnimating) {
			if (System.currentTimeMillis() - deathStartTime > DEATH_DURATION) {
				isDeathAnimating = false;
				if (!currentEnemy.isAlive()) {
					handleWin();
				} else if (!playerPet.isAlive()) {
					handleLoss();
				}
			}
		}
	}

	private void checkRoundResult() {
		if (!currentEnemy.isAlive()) {
			startDeathSequence(currentEnemy);
		} else if (!playerPet.isAlive()) {
			startDeathSequence(playerPet);
		} else {
			playerPet.setAnimationState(Hangpie.AnimState.IDLE);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE);
		}
	}

	private void startDeathSequence(Character victim) {
		isDeathAnimating = true;
		deathStartTime = System.currentTimeMillis();

		if (victim instanceof Enemy) {
			((Enemy) victim).setAnimationState(Enemy.AnimState.DEATH);
			playerPet.setAnimationState(Hangpie.AnimState.IDLE);
		} else if (victim instanceof Hangpie) {
			((Hangpie) victim).setAnimationState(Hangpie.AnimState.DEATH);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE);
		}
	}

	private void handleWin() {
		battleOver = true;
		playerWon = true;

		// Delete Save on Win
		Main.saveManager.deleteSave(playerUser.getUsername());

		if (!isWordCompleted()) {
			shouldCarryOverWord = true;
		} else {
			shouldCarryOverWord = false;
		}

		if (!rewardsClaimed) {
			goldReward = 50 + (playerUser.getWorldLevel() * 10);
			if (playerUser.getProgressLevel() == 5) {
				goldReward *= 2;
			}

			playerUser.addGold(goldReward);

			if (playerUser.getProgressLevel() >= 5) {
				playerUser.setProgressLevel(1);
				playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
			} else {
				playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);
			}

			Main.userManager.updateUser(playerUser);
			rewardsClaimed = true;
		}
	}

	private void handleLoss() {
		battleOver = true;
		playerWon = false;
		// Delete Save on Loss
		Main.saveManager.deleteSave(playerUser.getUsername());
	}

// --- Input Handling ---

	public void handleMouseMove(int x, int y) {
		if (isSettingsOpen) {
			selectedModalOption = -1;
			if (modalContinueBounds != null && modalContinueBounds.contains(x, y)) {
				selectedModalOption = 0;
			} else if (modalSaveBounds != null && modalSaveBounds.contains(x, y)) {
				selectedModalOption = 1;
			} else if (modalMenuBounds != null && modalMenuBounds.contains(x, y)) {
				selectedModalOption = 2;
			} else if (modalExitBounds != null && modalExitBounds.contains(x, y)) {
				selectedModalOption = 3;
			}
		}
	}

	public String handleMouseClick(int x, int y) {
		if (isSettingsOpen) {
			if (modalContinueBounds != null && modalContinueBounds.contains(x, y)) {
				isSettingsOpen = false;
				isExitConfirmation = false;
				isMenuConfirmation = false;
				return "NONE";
			} else if (modalSaveBounds != null && modalSaveBounds.contains(x, y)) {
				// SAVE GAME ACTION
				saveGame();
				return "NONE";
			} else if (modalMenuBounds != null && modalMenuBounds.contains(x, y)) {
				// MENU CONFIRMATION
				if (!isMenuConfirmation) {
					isMenuConfirmation = true;
					isExitConfirmation = false; // Clear other confirm
				} else {
					return "MENU";
				}
				return "NONE";
			} else if (modalExitBounds != null && modalExitBounds.contains(x, y)) {
				// EXIT CONFIRMATION
				if (!isExitConfirmation) {
					isExitConfirmation = true;
					isMenuConfirmation = false; // Clear other confirm
				} else {
					return "EXIT";
				}
				return "NONE";
			}

			// Reset confirmations if clicked elsewhere on modal
			isExitConfirmation = false;
			isMenuConfirmation = false;

		} else {
			if (settingsBtnBounds != null && settingsBtnBounds.contains(x, y)) {
				isSettingsOpen = true;
				isExitConfirmation = false;
				isMenuConfirmation = false;
				selectedModalOption = -1;
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
				isMenuConfirmation = false;
			}
			return;
		}

		if (battleOver) {
			handleMenuInput(keyCode);
			return;
		}

		if (isAnimatingAction || isDeathAnimating)
			return;

		char guess = java.lang.Character.toUpperCase(keyChar);
		if (guess < 'A' || guess > 'Z')
			return;

		if (guessedLetters.contains(guess)) {
			message = "Already guessed " + guess + "!";
			messageColor = Color.YELLOW;
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

		if (isCorrect) {
			message = "Correct! Hit!";
			messageColor = Color.GREEN;
			playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
			currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);

			int dmg = playerPet.getAttackPower();
			currentEnemy.takeDamage(dmg);

			if (isWordCompleted()) {
				if (currentEnemy.isAlive()) {
					message = "Word Cleared! New Word!";
					messageColor = Color.CYAN;
					generateNewWord();
				}
			}

		} else {
			message = "Wrong! Ouch!";
			messageColor = Color.RED;
			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);

			int dmg = currentEnemy.getAttackPower();
			playerPet.takeDamage(dmg);
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
		int playerCenterX = 280;
		int enemyCenterX = width - 280;
		int statsUiY = 140;

		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}

		int backdropH = 130;
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		drawLevelIndicator(g, 30, TOP_BAR_Y, observer);

		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x, settingsBtnBounds.y, settingsBtnBounds.width,
					settingsBtnBounds.height, observer);
		}

		drawWordPuzzle(g, width, height, observer);
		drawCharacterUI(g, width, statsUiY, playerCenterX, enemyCenterX, observer);

		int framesTopY = backdropH + 10;
		if (!message.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			FontMetrics fm = g.getFontMetrics();
			int msgW = fm.stringWidth(message);

			int bgW = msgW + 60;
			int bgH = 50;
			int bgX = (width - bgW) / 2;
			int bgY = framesTopY;

			if (nameFrameImg != null) {
				g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, observer);
			}

			g.setColor(messageColor);
			int textX = (width - msgW) / 2;
			int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 7;
			g.drawString(message, textX, textY);
		}

		int groundY = height - 20;
		int scaleFactor = 4;

		Image enemyImg = currentEnemy.getCurrentImage();
		if (enemyImg != null) {
			int eW = enemyImg.getWidth(observer);
			int eH = enemyImg.getHeight(observer);

			if (eW > 0 && eH > 0) {
				int drawW = eW * scaleFactor;
				int drawH = eH * scaleFactor;
				int drawX = enemyCenterX - (drawW / 2);
				int drawY = groundY - drawH;
				g.drawImage(enemyImg, drawX, drawY, drawW, drawH, observer);
			}
		}

		Image playerImg = playerPet.getCurrentImage();
		if (playerImg != null) {
			int pW = playerImg.getWidth(observer);
			int pH = playerImg.getHeight(observer);

			if (pW > 0 && pH > 0) {
				int drawW = pW * scaleFactor;
				int drawH = pH * scaleFactor;
				int drawX = playerCenterX - (drawW / 2);
				int drawY = groundY - drawH;
				g.drawImage(playerImg, drawX, drawY, drawW, drawH, observer);
			}
		}

		if (isSettingsOpen) {
			drawSettingsModal(g, width, height);
		} else if (battleOver) {
			drawEndScreen(g, width, height);
		}
	}

	private void drawLevelIndicator(Graphics2D g, int x, int y, ImageObserver observer) {
		int w = 150;
		int h = TOP_BAR_HEIGHT;
		if (levelFrameImg != null) {
			g.drawImage(levelFrameImg, x, y, w, h, observer);
		}
		g.setColor(new Color(231, 76, 60));
		g.setFont(new Font("Monospaced", Font.BOLD, 28));
		String levelTxt = playerUser.getWorldLevel() + " - " + playerUser.getProgressLevel();
		FontMetrics fm = g.getFontMetrics();
		int textX = x + (w - fm.stringWidth(levelTxt)) / 2;
		int textY = y + (h - fm.getAscent()) / 2 + fm.getAscent() - 5;
		g.drawString(levelTxt, textX, textY);
	}

	private void drawCharacterUI(Graphics2D g, int width, int topY, int playerCenterX, int enemyCenterX,
			ImageObserver observer) {
		int frameW = 220;
		int frameH = 70;
		int pFrameX = playerCenterX - (frameW / 2);
		int eFrameX = enemyCenterX - (frameW / 2);

		Font levelFont = new Font("Monospaced", Font.BOLD, 12);
		Font nameFont = new Font("Monospaced", Font.BOLD, 16);

		// PLAYER UI
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, pFrameX, topY, frameW, frameH, null);
		}
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String pLevel = "LEVEL " + playerPet.getLevel();
		FontMetrics fm = g.getFontMetrics();
		g.drawString(pLevel, pFrameX + (frameW - fm.stringWidth(pLevel)) / 2, topY + 28);

		g.setFont(nameFont);
		String pName = playerPet.getName();
		g.drawString(pName, pFrameX + (frameW - g.getFontMetrics().stringWidth(pName)) / 2, topY + 46);

		int heartsY = topY + frameH + 5;
		int heartSize = 15;
		int heartSpacing = 3;

		int pMaxHp = playerPet.getMaxHealth();
		int pTotalHearts = (pMaxHp + 1) / 2;
		int pTotalWidth = (pTotalHearts * heartSize) + (Math.max(0, pTotalHearts - 1) * heartSpacing);
		int pHeartsX = pFrameX + (frameW - pTotalWidth) / 2;
		drawHearts(g, playerPet, pHeartsX, heartsY, observer);

		g.setFont(levelFont);
		String pStats = "HP " + playerPet.getCurrentHealth() + "/" + playerPet.getMaxHealth() + " | ATK "
				+ playerPet.getAttackPower();
		g.drawString(pStats, pFrameX + (frameW - g.getFontMetrics().stringWidth(pStats)) / 2, heartsY + 30);

		// ENEMY UI
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, eFrameX, topY, frameW, frameH, null);
		}
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String eLevel = "LEVEL " + currentEnemy.getLevel();
		g.drawString(eLevel, eFrameX + (frameW - g.getFontMetrics().stringWidth(eLevel)) / 2, topY + 28);

		g.setFont(nameFont);
		String eName = currentEnemy.getName();
		g.drawString(eName, eFrameX + (frameW - g.getFontMetrics().stringWidth(eName)) / 2, topY + 46);

		int eMaxHp = currentEnemy.getMaxHealth();
		int eTotalHearts = (eMaxHp + 1) / 2;
		int eTotalWidth = (eTotalHearts * heartSize) + (Math.max(0, eTotalHearts - 1) * heartSpacing);
		int eHeartsX = eFrameX + (frameW - eTotalWidth) / 2;
		drawHearts(g, currentEnemy, eHeartsX, heartsY, observer);

		g.setFont(levelFont);
		String eStats = "HP " + currentEnemy.getCurrentHealth() + "/" + currentEnemy.getMaxHealth() + " | ATK "
				+ currentEnemy.getAttackPower();
		g.drawString(eStats, eFrameX + (frameW - g.getFontMetrics().stringWidth(eStats)) / 2, heartsY + 30);
	}

	private void drawHearts(Graphics2D g, Character character, int x, int y, ImageObserver observer) {
		int maxHp = character.getMaxHealth();
		int currentHp = character.getCurrentHealth();
		int totalHearts = (maxHp + 1) / 2;
		int heartSize = 15;
		int spacing = 3;

		for (int i = 0; i < totalHearts; i++) {
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
		g.setFont(GameConstants.UI_FONT);
		String clueText = "CLUE: " + clue;
		FontMetrics fm = g.getFontMetrics();
		int textW = fm.stringWidth(clueText);

		int bgW = textW + 60;
		int bgH = TOP_BAR_HEIGHT;
		int bgX = (width - bgW) / 2;
		int bgY = TOP_BAR_Y;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, obs);
		}

		g.setColor(Color.WHITE);
		int textX = (width - textW) / 2;
		int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 5;
		g.drawString(clueText, textX, textY);

		int spacing = 60;
		int lettersY = 110;
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
		g.drawString(title, mX + (mW - g.getFontMetrics().stringWidth(title)) / 2, mY + 50);

		g.setFont(GameConstants.BUTTON_FONT);
		FontMetrics fm = g.getFontMetrics();
		int btnH = 30;
		int btnGap = 15;
		int startBtnY = mY + 80;

		// --- 1. CONTINUE ---
		String contTxt = "CONTINUE";
		if (selectedModalOption == 0) {
			g.setColor(Color.YELLOW);
			contTxt = "> " + contTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int contW = fm.stringWidth(contTxt);
		modalContinueBounds = new Rectangle(mX + (mW - contW) / 2 - 10, startBtnY, contW + 20, btnH);
		g.drawString(contTxt, mX + (mW - contW) / 2, startBtnY + 25);

		// --- 2. SAVE GAME ---
		int nextY = startBtnY + btnH + btnGap;
		String saveTxt = "SAVE GAME";
		if (selectedModalOption == 1) {
			g.setColor(Color.YELLOW);
			saveTxt = "> " + saveTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int saveW = fm.stringWidth(saveTxt);
		modalSaveBounds = new Rectangle(mX + (mW - saveW) / 2 - 10, nextY, saveW + 20, btnH);
		g.drawString(saveTxt, mX + (mW - saveW) / 2, nextY + 25);

		// --- 3. MAIN MENU ---
		nextY += btnH + btnGap;
		String menuTxt = "MAIN MENU";
		if (isMenuConfirmation)
			menuTxt = "CONFIRM?"; // Logic added here

		if (selectedModalOption == 2) {
			g.setColor(isMenuConfirmation ? Color.RED : Color.YELLOW);
			menuTxt = "> " + menuTxt + " <";
		} else {
			g.setColor(isMenuConfirmation ? Color.RED : Color.BLACK);
		}
		int menuW = fm.stringWidth(menuTxt);
		modalMenuBounds = new Rectangle(mX + (mW - menuW) / 2 - 10, nextY, menuW + 20, btnH);
		g.drawString(menuTxt, mX + (mW - menuW) / 2, nextY + 25);

		// --- 4. EXIT GAME ---
		nextY += btnH + btnGap;
		String exitTxt = "EXIT GAME";
		if (isExitConfirmation)
			exitTxt = "CONFIRM?"; // Logic added here

		if (selectedModalOption == 3) {
			g.setColor(isExitConfirmation ? Color.RED : Color.YELLOW);
			exitTxt = "> " + exitTxt + " <";
		} else {
			g.setColor(isExitConfirmation ? Color.RED : Color.BLACK);
		}

		int exitW = fm.stringWidth(exitTxt);
		modalExitBounds = new Rectangle(mX + (mW - exitW) / 2 - 10, nextY, exitW + 20, btnH);
		g.drawString(exitTxt, mX + (mW - exitW) / 2, nextY + 25);

		if (isExitConfirmation || isMenuConfirmation) {
			g.setFont(new Font("Monospaced", Font.PLAIN, 12));
			g.setColor(Color.RED);
			String warn = "Unsaved progress will be lost!";
			g.drawString(warn, mX + (mW - g.getFontMetrics().stringWidth(warn)) / 2, mY + mH - 35);
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
			if (playerUser.getProgressLevel() == 1 && playerUser.getWorldLevel() > 1) {
				rewardMsg += " BOSS DEFEATED!";
			}
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