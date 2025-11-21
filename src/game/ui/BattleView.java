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
	private int expReward = 0;
	private boolean rewardsClaimed = false;

	private String message = "";
	private Color messageColor = Color.YELLOW; // Default color

	// Animation Timers & States
	private long actionStartTime = 0;
	private boolean isAnimatingAction = false;
	private final int ACTION_DURATION = 1700; // 1.7 Seconds for Attack/Damage

	private boolean isDeathAnimating = false;
	private long deathStartTime = 0;
	private final int DEATH_DURATION = 3000; // 3 Seconds for Death Animation

	// --- TIMER LOGIC ---
	private long lastGuessTime;
	private long currentTimeRemaining;
	private int timerX, timerY, timerW, timerH;
	
	// --- PANIC VISUALS (NEW) ---
	private int panicAlpha = 0; // Alpha for the timer box flicker
	private int currentShakeX = 0;
	private int currentShakeY = 0;
	private long lastShakeTime = 0;
	private int redPulseAlpha = 0; // Alpha for the full screen red pulse
	private int lastSecondChecked = -1; // For tracking the per-second pulse
	private final int SHAKE_MAGNITUDE = 3; // Max shake displacement in pixels
	private final int MAX_RED_ALPHA = 80; // Max opacity for red pulse (0-255)


	// Assets
	private Image bgImage;
	private Image nameFrameImg;
	private Image frameImg; // The general frame for stats
	private Image settingsImg;
	private Image modalImg;

	// Frame used for the Level Indicator
	private Image levelFrameImg;

	// Heart Assets
	private Image heartImg;

	// Icon Assets
	private Image coinImg;
	private Image attackImg;

	private Random random = new Random();

	// Settings Modal State
	private boolean isSettingsOpen = false;
	private boolean isExitConfirmation = false;
	private boolean isMenuConfirmation = false;

	// UI Buttons
	private Rectangle settingsBtnBounds;

	// Modal Buttons
	private Rectangle modalContinueBounds;
	private Rectangle modalSaveBounds;
	private Rectangle modalMenuBounds;
	private Rectangle modalExitBounds;

	// Hover State for Modal
	// Continue, Save, Menu, Exit, None
	private int selectedModalOption = -1;

	// Layout Constants
	private final int TOP_BAR_Y = 20;
	private final int TOP_BAR_HEIGHT = 50;

	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();
		this.lastGuessTime = System.currentTimeMillis(); // Initialize timer

		// Reset animations
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		loadAssets();
		initBattle();
	}

	private void loadAssets() {
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 200, 50);
		// UPDATED: Reduced width from 220 to 200 to match the name frame
		frameImg = AssetLoader.loadImage(GameConstants.FRAME_IMG, 200, 80);

		levelFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 150, 50);
		settingsImg = AssetLoader.loadImage(GameConstants.SETTINGS_BTN_IMG, 50, 50);
		modalImg = AssetLoader.loadImage(GameConstants.MODAL_IMG, 400, 300);

		// Assuming user has locally updated to 15x15 icons
		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 15, 15);
		attackImg = AssetLoader.loadImage(GameConstants.ATTACK_IMG, 15, 15);
		coinImg = AssetLoader.loadImage(GameConstants.COIN_IMG, 20, 20);
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

		// Reset Timer and Panic Visuals
		this.lastGuessTime = System.currentTimeMillis();
		this.panicAlpha = 0;
		this.currentShakeX = 0;
		this.currentShakeY = 0;
		this.redPulseAlpha = 0;
		this.lastSecondChecked = -1;


		// Load Game Logic
		if (Main.saveManager.hasSave(playerUser.getUsername()) && !shouldCarryOverWord) {
			System.out.println("[Battle] Found save file. Loading...");
			loadGame();
			return;
		}

		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());

		// Word Logic (New vs Carry Over)
		if (!shouldCarryOverWord) {
			generateNewWord();
		} else {
			System.out.println("[Battle] Carrying over word: " + secretWord);
			shouldCarryOverWord = false;
		}

		// Generate Enemy
		generateEnemy();

		// Preload Assets
		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, 50, 50);

		// Set Timer UI position to the bottom center (White Box area)
		timerW = 250; 
		timerH = 50;  
		timerX = (GameConstants.WINDOW_WIDTH - timerW) / 2;
		timerY = GameConstants.WINDOW_HEIGHT - 120; // Near the bottom edge
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
		this.playerPet.setCurrentHealth(save.getPlayerPetHp());

		// Load Background (Random standard BG)
		generateBackground(false);

		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, 50, 50);

		// Set Timer UI position
		timerW = 250; 
		timerH = 50;  
		timerX = (GameConstants.WINDOW_WIDTH - timerW) / 2;
		timerY = GameConstants.WINDOW_HEIGHT - 120;

		// Reset Timer and Panic Visuals
		this.lastGuessTime = System.currentTimeMillis();
		this.panicAlpha = 0;
		this.currentShakeX = 0;
		this.currentShakeY = 0;
		this.redPulseAlpha = 0;
		this.lastSecondChecked = -1;

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

		// --- SCALING LOGIC ---
		// Base Stats for World 1: 10 HP, 1 ATK
		// Increase per World: +5 HP, +1 ATK

		int baseHp = 10 + ((currentWorld - 1) * 5);
		int baseAtk = 1 + ((currentWorld - 1) * 1);

		int enemyHp = baseHp;
		int enemyAtk = baseAtk;

		boolean isBoss = (currentProg == 5);
		generateBackground(isBoss);

		if (isBoss) {
			// Boss Stats: +5 HP, +1 ATK relative to normal
			enemyHp = baseHp + 5;
			enemyAtk = baseAtk + 1;

			long seed = playerUser.getUsername().hashCode() + currentWorld;
			Random bossRandom = new Random(seed);
			int bossIndex = bossRandom.nextInt(3) + 1;

			switch (bossIndex) {
			case 1:
				enemyName = "Mehrunes Dagon";
				enemyPath = "enemies/boss/boss1";
				break;
			case 2:
				enemyName = "Molag Bal";
				enemyPath = "enemies/boss/boss2";
				break;
			case 3:
				enemyName = "Hermaeus Mora";
				enemyPath = "enemies/boss/boss3";
				break;
			default:
				enemyName = "Mehrunes Dagon";
				enemyPath = "enemies/boss/boss1";
				break;
			}
		} else {
			String[][] availableEnemies = { { "Lava Wrym", "enemies/enemies/worm" },
					{ "Seeker's Gaze", "enemies/enemies/evil_eye" }, { "Riekling Scout", "enemies/enemies/goblin" },
					{ "Spriggan", "enemies/enemies/mushroom" }, { "Draugr", "enemies/enemies/skeleton" } };
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
		if (isSettingsOpen || battleOver)
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
		
		// --- TIMER AND PANIC UPDATE LOGIC ---
		if (!isAnimatingAction && !isDeathAnimating) {
			long elapsedTime = System.currentTimeMillis() - lastGuessTime;
			long maxTimeMillis = GameConstants.GUESS_TIME_LIMIT_SECONDS * 1000;
			currentTimeRemaining = maxTimeMillis - elapsedTime;
			
			// +1 is used to display the current second rather than the next second
			int secondsRemaining = (int) (currentTimeRemaining / 1000) + 1; 
			boolean isPanicking = secondsRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS && secondsRemaining > 0;

			if (isPanicking) {
				// --- Screen Shake Logic (every 100ms) ---
				if (System.currentTimeMillis() - lastShakeTime > 100) {
					// Generate random shake offsets
					currentShakeX = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					currentShakeY = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					lastShakeTime = System.currentTimeMillis();
				}
				
				// --- Red Pulse Logic (Per visual second) ---
				if (secondsRemaining != lastSecondChecked) {
					redPulseAlpha = MAX_RED_ALPHA; // Max opacity at the start of the second
					lastSecondChecked = secondsRemaining;
				}

				// Fade out Red Pulse Alpha (fades over a fixed period like 500ms)
				// Use the time since the last second change to calculate fade amount
				long timeSincePulse = System.currentTimeMillis() % 1000; 
				if (timeSincePulse < 500) {
					// Linear fade from MAX_RED_ALPHA to 0 over 500ms
					redPulseAlpha = (int) (MAX_RED_ALPHA * (1.0 - (timeSincePulse / 500.0)));
				} else {
					redPulseAlpha = 0;
				}
				redPulseAlpha = Math.max(0, redPulseAlpha);
				
				// --- Panic Alpha for Timer Box (Flicker) ---
				// Simple flicker for the timer box visual feedback
				long pulseTime = System.currentTimeMillis() % 1000;
				if (pulseTime < 500) {
					panicAlpha = 80;
				} else {
					panicAlpha = 0;
				}
				
			} else {
				// Reset shake and pulse outside of panic mode
				currentShakeX = 0;
				currentShakeY = 0;
				redPulseAlpha = 0;
				panicAlpha = 0;
				lastSecondChecked = -1;
			}
			
			// Timer Expired Logic
			if (currentTimeRemaining <= 0) {
				currentTimeRemaining = 0; // Clamp
				
				// Time is up! Enemy attacks.
				handleTimeOutAttack();
			}
		}
	}

	private void handleTimeOutAttack() {
		message = "Time Out! Ouch!";
		messageColor = Color.RED;
		
		lastGuessTime = System.currentTimeMillis(); // Reset timer for next guess
		
		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;
		
		currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
		playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);
		
		int dmg = currentEnemy.getAttackPower();
		playerPet.takeDamage(dmg);
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
			boolean isBoss = (playerUser.getProgressLevel() == 5);

			// Calculate Rewards
			if (isBoss) {
				goldReward = 10;
				expReward = 5;
			} else {
				goldReward = 5;
				expReward = 1;
			}

			// Handle Progression
			if (isBoss) {
				// Beat the Boss -> Advance World
				playerUser.setProgressLevel(1);
				playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
			} else {
				// Normal Stage -> Advance Stage
				playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);
			}

			// Apply Rewards (After World Level update to lift level cap if boss)
			playerUser.addGold(goldReward);
			boolean leveledUp = playerPet.gainExp(expReward, playerUser.getWorldLevel());

			if (leveledUp) {
				message = "LEVEL UP!";
				messageColor = Color.CYAN;
			} else if (playerPet.getLevel() >= playerUser.getWorldLevel()
					&& playerPet.getCurrentExp() >= playerPet.getMaxExpForCurrentLevel()) {
				message = "EXP MAX (Defeat Boss!)";
				messageColor = Color.ORANGE;
			}

			Main.userManager.updateUser(playerUser);
			rewardsClaimed = true;
		}
	}

	private void handleLoss() {
		battleOver = true;
		playerWon = false;

		// Death Penalty: Reset to Stage 1 of Current World
		playerUser.setProgressLevel(1);
		Main.userManager.updateUser(playerUser);

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
		
		// Don't process input if the timer has just run out (wait for attack animation)
		if (currentTimeRemaining <= 0) return;

		char guess = java.lang.Character.toUpperCase(keyChar);
		if (guess < 'A' || guess > 'Z')
			return;

		if (guessedLetters.contains(guess)) {
			message = "Already guessed " + guess + "!";
			messageColor = Color.YELLOW;
			return;
		}

		guessedLetters.add(guess);

		// --- RESET TIMER ON VALID GUESS ---
		lastGuessTime = System.currentTimeMillis();
		currentShakeX = 0;
		currentShakeY = 0;
		redPulseAlpha = 0;
		panicAlpha = 0;
		lastSecondChecked = -1;

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
		int offsetX = currentShakeX;
		int offsetY = currentShakeY;
		
		int playerCenterX = 280;
		int enemyCenterX = width - 280;
		int statsUiY = 140;

		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}
		
		// --- DRAW RED PULSE OVERLAY (FULL SCREEN) ---
		if (redPulseAlpha > 0) {
			g.setColor(new Color(255, 0, 0, redPulseAlpha));
			g.fillRect(0, 0, width, height);
		}

		// Backdrop is generally static, but we'll apply shake for consistency of the background frame itself
		int backdropH = 130;
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		// --- APPLY OFFSETS TO UI COMPONENTS ---

		drawLevelIndicator(g, 30 + offsetX, TOP_BAR_Y + offsetY, observer);

		// Settings Button (Offset)
		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x + offsetX, settingsBtnBounds.y + offsetY,
					settingsBtnBounds.width, settingsBtnBounds.height, observer);
		}

		// Word Puzzle (Offset)
		drawWordPuzzle(g, width, height, observer, offsetX, offsetY);

		// Timer UI (Offset)
		drawTimer(g, observer, offsetX, offsetY);
		
		// Message Box (Offset)
		int framesTopY = backdropH + 10;
		if (!message.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			FontMetrics fm = g.getFontMetrics();
			int msgW = fm.stringWidth(message);

			int bgW = msgW + 60;
			int bgH = 50;
			int bgX = (width - bgW) / 2 + offsetX; // Apply Offset
			int bgY = framesTopY + offsetY;        // Apply Offset

			if (nameFrameImg != null) {
				g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, observer);
			}

			g.setColor(messageColor);
			int textX = (width - msgW) / 2 + offsetX; // Apply Offset
			int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 7;
			g.drawString(message, textX, textY);
		}

		// Character UI (Offset is applied to the frame/stat boxes)
		drawCharacterUI(g, width, statsUiY + offsetY, playerCenterX + offsetX, enemyCenterX + offsetX, observer);


		int groundY = height - 20;
		int scaleFactor = 4;

		// Characters (NO OFFSET)
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
	
	private void drawTimer(Graphics2D g, ImageObserver observer, int offsetX, int offsetY) {
		
		int drawX = timerX + offsetX; // Apply Offset
		int drawY = timerY + offsetY; // Apply Offset

		// Draw the timer background frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, drawX, drawY, timerW, timerH, observer);
		}

		// Draw Panic Flash Effect (Timer Box Flicker)
		if (panicAlpha > 0) {
			// Creates the "flash" effect when time is low
			Color panicColor = new Color(GameConstants.TIMER_PANIC_COLOR.getRed(), 
										 GameConstants.TIMER_PANIC_COLOR.getGreen(), 
										 GameConstants.TIMER_PANIC_COLOR.getBlue(), 
										 panicAlpha);
			g.setColor(panicColor);
			g.fillRect(drawX + 2, drawY + 2, timerW - 4, timerH - 4);
		}

		// Calculate text
		// Add +1 to seconds remaining because we want to show the current second we are in, not the full seconds passed.
		int secondsRemaining = (int) Math.max(0, currentTimeRemaining / 1000) + 1;
		String timerText = String.format("TIME: %d", secondsRemaining);
		
		// Change text color to Red during panic
		if (secondsRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS) {
			g.setColor(GameConstants.TIMER_PANIC_COLOR);
		} else {
			g.setColor(Color.BLACK); // Draw black text over the light brown frame
		}
		
		g.setFont(GameConstants.UI_FONT);
		FontMetrics fm = g.getFontMetrics();

		// Center the text
		int textX = drawX + (timerW - fm.stringWidth(timerText)) / 2;
		int textY = drawY + (timerH - fm.getAscent()) / 2 + fm.getAscent() - 5;
		
		g.drawString(timerText, textX, textY);
	}


	private void drawLevelIndicator(Graphics2D g, int x, int y, ImageObserver observer) {
		int w = 150;
		int h = TOP_BAR_HEIGHT;
		if (levelFrameImg != null) {
			// x and y already contain the offset
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
		// UPDATED: frameW is now 200 to match name frame
		int frameW = 200;
		int statsW = 190;

		int frameH = 70;
		int pFrameX = playerCenterX - (frameW / 2);
		int eFrameX = enemyCenterX - (frameW / 2);

		int pStatsX = playerCenterX - (statsW / 2);
		int eStatsX = enemyCenterX - (statsW / 2);

		// Changed: Unified level and stats font size to 16 to match name font size
		Font levelFont = new Font("Monospaced", Font.BOLD, 16); 
		Font nameFont = new Font("Monospaced", Font.BOLD, 16);
		Font statsFont = new Font("Monospaced", Font.BOLD, 16); // Now size 16

		// --- PLAYER UI ---

		int statsFrameY = topY + 55;
		int statsFrameH = 80;

		// 1. Stats Frame
		// topY, playerCenterX, enemyCenterX all contain the offset
		if (frameImg != null) {
			g.drawImage(frameImg, pStatsX, statsFrameY, statsW, statsFrameH, null);
		}

		// 2. Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, pFrameX, topY, frameW, frameH, null);
		}

		// 3. Name & Level
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String pLevel = "LEVEL " + playerPet.getLevel();
		FontMetrics fm = g.getFontMetrics();
		g.drawString(pLevel, pFrameX + (frameW - fm.stringWidth(pLevel)) / 2, topY + 28);

		g.setFont(nameFont);
		String pName = playerPet.getName();
		g.drawString(pName, pFrameX + (frameW - g.getFontMetrics().stringWidth(pName)) / 2, topY + 46);

		// 4. NEW Stats Display (Icon HP | Icon ATK)
		drawModernStats(g, playerPet, pStatsX, statsFrameY, statsW, statsFrameH, statsFont, observer);

		// 5. EXP BAR (Moved inside frame at bottom)
		int expBarH = 6;
		int expBarW = statsW - 30; // Padding inside frame
		int expBarX = pStatsX + 15;
		int expBarY = statsFrameY + statsFrameH - 15; // Near bottom

		// Bar Background
		g.setColor(new Color(50, 50, 50));
		g.fillRect(expBarX, expBarY, expBarW, expBarH);

		// Bar Fill
		float expPercent = (float) playerPet.getCurrentExp() / (float) playerPet.getMaxExpForCurrentLevel();
		if (expPercent > 1.0f)
			expPercent = 1.0f;

		g.setColor(Color.CYAN);
		g.fillRect(expBarX, expBarY, (int) (expBarW * expPercent), expBarH);

		// Bar Border
		g.setColor(new Color(100, 100, 100));
		g.drawRect(expBarX, expBarY, expBarW, expBarH);

		// --- ENEMY UI ---

		// 1. Stats Frame
		if (frameImg != null) {
			g.drawImage(frameImg, eStatsX, statsFrameY, statsW, statsFrameH, null);
		}

		// 2. Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, eFrameX, topY, frameW, frameH, null);
		}

		// 3. Name & Level
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String eLevel = "LEVEL " + currentEnemy.getLevel();
		g.drawString(eLevel, eFrameX + (frameW - g.getFontMetrics().stringWidth(eLevel)) / 2, topY + 28);

		g.setFont(nameFont);
		String eName = currentEnemy.getName();
		g.drawString(eName, eFrameX + (frameW - g.getFontMetrics().stringWidth(eName)) / 2, topY + 46);

		// 4. New Stats Display
		drawModernStats(g, currentEnemy, eStatsX, statsFrameY, statsW, statsFrameH, statsFont, observer);
	}

	// Helper method to draw the specific [Heart] HP | [Sword] ATK layout
	private void drawModernStats(Graphics2D g, Character c, int x, int y, int w, int h, Font font, ImageObserver obs) {
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		String hpTxt = " " + c.getCurrentHealth() + "/" + c.getMaxHealth();
		String atkTxt = " " + c.getAttackPower();
		String sep = " | ";

		int iconSize = 15; // Assuming the user set this to 15 globally

		// Calculate total width to center everything
		int totalW = iconSize + fm.stringWidth(hpTxt) + fm.stringWidth(sep) + iconSize + fm.stringWidth(atkTxt);

		int startX = x + (w - totalW) / 2;
		int centerY = y + (h / 2) + 5; // Approximate vertical center of the stats frame
		
		// Icon vertical adjustment: Adjusted offset from 15 to 12 to center the 15px icon with the size 16 font text.
		int iconTopY = centerY - 12;

		int currentX = startX;

		// Draw Heart Icon
		if (heartImg != null) {
			g.drawImage(heartImg, currentX, iconTopY, iconSize, iconSize, obs);
		}
		currentX += iconSize;

		// Draw HP Text
		g.setColor(Color.WHITE);
		g.drawString(hpTxt, currentX, centerY);
		currentX += fm.stringWidth(hpTxt);

		// Draw Separator
		g.setColor(Color.GRAY);
		g.drawString(sep, currentX, centerY);
		currentX += fm.stringWidth(sep);

		// Draw Attack Icon
		if (attackImg != null) {
			g.drawImage(attackImg, currentX, iconTopY, iconSize, iconSize, obs);
		}
		currentX += iconSize;

		// Draw Atk Text
		g.setColor(Color.WHITE);
		g.drawString(atkTxt, currentX, centerY);
	}

	private void drawWordPuzzle(Graphics2D g, int width, int height, ImageObserver obs, int offsetX, int offsetY) {
		g.setFont(GameConstants.UI_FONT);
		
		// --- CLUE (CENTERED) ---
		String clueText = "CLUE: " + clue;
		FontMetrics fm = g.getFontMetrics();
		int textW = fm.stringWidth(clueText);

		int bgW = textW + 60;
		int bgH = TOP_BAR_HEIGHT;
		int bgX = (width - bgW) / 2 + offsetX; // Apply Offset
		int bgY = TOP_BAR_Y + offsetY;         // Apply Offset

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, obs);
		}

		g.setColor(Color.WHITE);
		int textX = (width - textW) / 2 + offsetX; // Apply Offset
		int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 5;
		g.drawString(clueText, textX, textY);
		
		// --- GUESS LETTERS (CENTERED) ---
		int spacing = 60;
		int lettersY = 110 + offsetY; // Apply Offset to vertical position
		int totalWidth = secretWord.length() * spacing;
		
		// Calculate starting X to center the entire word puzzle structure
		int startX = (width - totalWidth) / 2 + offsetX; // Apply Offset
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
			String rewardMsg = "REWARD: " + goldReward + "G | " + expReward + " EXP";
			if (playerUser.getProgressLevel() == 1 && playerUser.getWorldLevel() > 1) {
				rewardMsg += " (BOSS DEFEATED!)";
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

			String subMsg = "Progress Reset to Stage 1";
			g.drawString(subMsg, (width - g.getFontMetrics().stringWidth(subMsg)) / 2, height / 2);

			String opt = "Press [ENTER] or [ESC] to Return";
			g.drawString(opt, (width - g.getFontMetrics().stringWidth(opt)) / 2, height / 2 + 50);
		}
	}
}