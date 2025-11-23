package game.ui;

import java.awt.AlphaComposite;
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

	// Regular Enemies
	private final String[][] REGULAR_ENEMIES = { { "Riekling Scout", "enemies/enemies/goblin" },
			{ "Seeker's Gaze", "enemies/enemies/evil_eye" }, { "Spriggan", "enemies/enemies/mushroom" },
			{ "Draugr", "enemies/enemies/skeleton" }, { "Sand Worm", "enemies/enemies/worm" },
			{ "Umbra", "enemies/enemies/black" }, { "Frost Atronach", "enemies/enemies/blue" },
			{ "Crystal Atronach", "enemies/enemies/orange" }, { "Midden Heap", "enemies/enemies/tooth" },
			{ "Namira's Cur", "enemies/enemies/void_fox" }, { "Mora's Leech", "enemies/enemies/voidling" },
			{ "Hermaeus Tumor", "enemies/enemies/void_hunchback" },
			{ "Apocrypha's Host", "enemies/enemies/void_walker" }, { "Ichor Drone", "enemies/enemies/void_wing" } };

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

	// EXP/Level Up Animation State
	private boolean isRewardAnimating = false;
	private long rewardAnimStartTime = 0;
	private final int REWARD_ANIM_DURATION = 3000; 
	private boolean levelUpOccurred = false;
	private float levelUpFlashAlpha = 0.0f;
	private final int LEVEL_UP_FLASH_DURATION = 500;
	private final int LEVEL_UP_FLASH_PEAK = 200; 

	private String message = "";
	private Color messageColor = Color.YELLOW;

	// Animation Timers & States
	private long actionStartTime = 0;
	private boolean isAnimatingAction = false;
	private boolean impactTriggered = false;

	// Attack Animation Time
	private final int ACTION_DURATION = 1700;
	private final int FADE_DURATION = 300;
	private final int TELEPORT_TO_TARGET_TIME = 300;
	private final int FADE_IN_TARGET_TIME = 600;
	private final int ATTACK_HOLD_TIME = 1100;
	private final int FADE_OUT_RETURN_TIME = 1400;
	private final int RETURN_HOME_TIME = 1700;

	// Attack Animation Fields
	private boolean isPlayerAttacking;
	private long currentAnimTime = 0;
	private float playerAlpha = 1.0f;
	private float enemyAlpha = 1.0f;

	// Attack Position
	private final int PLAYER_HOME_X = 280;
	private final int ENEMY_HOME_X = GameConstants.WINDOW_WIDTH - 280;
	private final int ATTACK_PLAYER_X = ENEMY_HOME_X - 100;
	private final int ATTACK_ENEMY_X = PLAYER_HOME_X + 100;

	// Death Animation Time
	private boolean isDeathAnimating = false;
	private long deathStartTime = 0;
	private final int DEATH_DURATION = 3000;

	// Timer Logic
	private long lastGuessTime = 0;
	private long currentTimeRemaining = 0;
	private int timerX = 0, timerY = 0, timerW = 0, timerH = 0;

	// Panic Visual
	private int panicAlpha = 0;
	private int timeShakeX = 0;
	private int timeShakeY = 0;
	private long lastShakeTime = 0;
	private int redPulseAlpha = 0;
	private int lastSecondChecked = -1;
	private final int SHAKE_MAGNITUDE = 3;
	private final int MAX_RED_ALPHA = 80;

	// Damage Visual
	private long damageStartTime = 0;
	private final int DAMAGE_VISUAL_DURATION = 500;
	private int spriteShakeX = 0;
	private int spriteShakeY = 0;
	private boolean isPlayerDamaged = false;
	private boolean isEnemyDamaged = false;

	// Global Screen Shake
	private int screenShakeX = 0;
	private int screenShakeY = 0;
	private final int SCREEN_SHAKE_MAGNITUDE = 5;
	private long lastScreenShakeTime = 0;

	// Damage Indicator
	private int damageIndicatorPlayer = 0;
	private int damageIndicatorEnemy = 0;
	private long damageIndicatorStartTime = 0;
	private final int DAMAGE_INDICATOR_DURATION = 1500;
	private final int DAMAGE_INDICATOR_VERTICAL_TRAVEL = 50;

	// Rabbit Flash (Meme)
	private Image rabbitImg;
	private long rabbitFlashStartTime = 0;
	private final int RABBIT_FLASH_DURATION = 500;
	private boolean hasFlashedThisGuess = false;

	// Assets
	private Image bgImage;
	private Image nameFrameImg;
	private Image frameImg;
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
	private boolean isInstructionOpen = false;

	// UI Buttons
	private Rectangle settingsBtnBounds;
	private Rectangle instructionBtnBounds; 

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
	private final int UI_BUTTON_SIZE = 50;
	private final int MODAL_WIDTH = 400;
	private final int MODAL_HEIGHT = 500;

	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();
		this.lastGuessTime = System.currentTimeMillis();

		// Reset Animations
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		loadAssets();
		initBattle();
	}

	private void loadAssets() {
		nameFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 200, 50);
		frameImg = AssetLoader.loadImage(GameConstants.FRAME_IMG, 200, 80);

		levelFrameImg = AssetLoader.loadImage(GameConstants.NAME_FRAME_IMG, 150, 50);
		settingsImg = AssetLoader.loadImage(GameConstants.SETTINGS_BTN_IMG, UI_BUTTON_SIZE, UI_BUTTON_SIZE);
		modalImg = AssetLoader.loadImage(GameConstants.MODAL_IMG, MODAL_WIDTH, MODAL_HEIGHT);

		heartImg = AssetLoader.loadImage(GameConstants.HEART_IMG, 15, 15);
		attackImg = AssetLoader.loadImage(GameConstants.ATTACK_IMG, 15, 15);
		coinImg = AssetLoader.loadImage(GameConstants.COIN_IMG, 20, 20);

		rabbitImg = AssetLoader.loadImage(GameConstants.RABBIT_IMG, GameConstants.WINDOW_WIDTH,
				GameConstants.WINDOW_HEIGHT);
	}

	private void initBattle() {
		this.battleOver = false;
		this.playerWon = false;
		this.rewardsClaimed = false;
		this.goldReward = 0;
		this.expReward = 0;
		this.message = "";
		this.messageColor = Color.YELLOW;

		// Reset Animation States
		this.isAnimatingAction = false;
		this.isDeathAnimating = false;
		this.isPlayerAttacking = false;
		this.playerAlpha = 1.0f;
		this.enemyAlpha = 1.0f;
		this.currentAnimTime = 0;
		this.impactTriggered = false; 

		// NEW: Reset Reward Animation State
		this.isRewardAnimating = false;
		this.rewardAnimStartTime = 0;
		this.levelUpOccurred = false;
		this.levelUpFlashAlpha = 0.0f;

		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		// Reset Timer and Panic Visuals
		this.lastGuessTime = System.currentTimeMillis();
		this.panicAlpha = 0;
		this.timeShakeX = 0;
		this.timeShakeY = 0;
		this.redPulseAlpha = 0;
		this.lastSecondChecked = -1;

		// Reset Damage Visuals and Timers
		this.isPlayerDamaged = false;
		this.isEnemyDamaged = false;
		this.spriteShakeX = 0;
		this.spriteShakeY = 0;
		this.screenShakeX = 0;
		this.screenShakeY = 0;
		this.damageStartTime = 0;
		this.lastScreenShakeTime = 0;

		this.isInstructionOpen = false;

		// Reset Damage Indicators
		this.damageIndicatorPlayer = 0;
		this.damageIndicatorEnemy = 0;
		this.damageIndicatorStartTime = 0;

		// Reset Rabbit Flash
		this.rabbitFlashStartTime = 0;
		this.hasFlashedThisGuess = false;

		// Load Game Logic
		if (Main.saveManager.hasSave(playerUser.getUsername()) && !shouldCarryOverWord) {
			System.out.println("[Battle] Found save file. Loading...");
			loadGame();
			return;
		}

		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());

		// Word Logic (New vs Carry Over)
		if (!shouldCarryOverWord) {
			generateNewWord(playerUser.getWorldLevel(), playerUser.getProgressLevel());
		} else {
			System.out.println("[Battle] Carrying over word: " + secretWord);
			shouldCarryOverWord = false;
		}

		// Generate Enemy
		generateEnemy();

		// Preload Assets
		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		// UI Button Bounds
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, UI_BUTTON_SIZE, UI_BUTTON_SIZE);
		instructionBtnBounds = new Rectangle(settingsBtnBounds.x - 60, TOP_BAR_Y, UI_BUTTON_SIZE, UI_BUTTON_SIZE);

		// Set Timer UI position
		timerW = 250;
		timerH = 50;
		timerX = (GameConstants.WINDOW_WIDTH - timerW) / 2;
		timerY = GameConstants.WINDOW_HEIGHT - 120;
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
		boolean isBoss = (playerUser.getProgressLevel() == 5);
		generateBackground(isBoss); 

		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		// UI Button Bounds
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, UI_BUTTON_SIZE, UI_BUTTON_SIZE);
		instructionBtnBounds = new Rectangle(settingsBtnBounds.x - 60, TOP_BAR_Y, UI_BUTTON_SIZE, UI_BUTTON_SIZE); 

		// Set Timer UI position
		timerW = 250;
		timerH = 50;
		timerX = (GameConstants.WINDOW_WIDTH - timerW) / 2;
		timerY = GameConstants.WINDOW_HEIGHT - 120;

		// Reset Timer and Panic Visuals
		this.lastGuessTime = System.currentTimeMillis();
		this.panicAlpha = 0;
		this.timeShakeX = 0;
		this.timeShakeY = 0;
		this.redPulseAlpha = 0;
		this.lastSecondChecked = -1;

		// Reset Damage Visuals and Timers
		this.isPlayerDamaged = false;
		this.isEnemyDamaged = false;
		this.spriteShakeX = 0;
		this.spriteShakeY = 0;
		this.screenShakeX = 0;
		this.screenShakeY = 0;
		this.damageStartTime = 0;
		this.lastScreenShakeTime = 0;
		this.impactTriggered = false;

		this.isPlayerAttacking = false;
		this.playerAlpha = 1.0f;
		this.enemyAlpha = 1.0f;
		this.currentAnimTime = 0;
		this.isInstructionOpen = false;

		// Reward Animation State
		this.isRewardAnimating = false;
		this.rewardAnimStartTime = 0;
		this.levelUpOccurred = false;
		this.levelUpFlashAlpha = 0.0f;

		// Reset Damage Indicators
		this.damageIndicatorPlayer = 0;
		this.damageIndicatorEnemy = 0;
		this.damageIndicatorStartTime = 0;

		// Reset Rabbit Flash
		this.rabbitFlashStartTime = 0;
		this.hasFlashedThisGuess = false;

		message = "Game Loaded!";
		messageColor = Color.GREEN;
	}
	
	// Save Function
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

		// Base Stats for World 1: 10 HP, 1 ATK
		// Increase per World: +5 HP, +1 ATK
		int baseHp = 10 + ((currentWorld - 1) * 5);
		int baseAtk = 1 + ((currentWorld - 1) * 1);

		int enemyHp = baseHp;
		int enemyAtk = baseAtk;

		boolean isBoss = (currentProg == 5);
		generateBackground(isBoss);

		if (isBoss) {

			// Boss Stats: +5 HP, +1 ATK
			enemyHp = baseHp + 5;
			enemyAtk = baseAtk + 1;

			int bossIndex = random.nextInt(4) + 1;

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
			case 4:
				enemyName = "Sheogorath";
				enemyPath = "enemies/boss/boss4";
				break;
			default:
				enemyName = "Mehrunes Dagon";
				enemyPath = "enemies/boss/boss1";
				break;
			}
		} else {

			// Enemy Randomizer
			int enemyIndex = random.nextInt(REGULAR_ENEMIES.length);

			enemyName = REGULAR_ENEMIES[enemyIndex][0];
			enemyPath = REGULAR_ENEMIES[enemyIndex][1];
		}

		this.currentEnemy = new Enemy(enemyName, enemyHp, currentWorld, enemyAtk, enemyPath);
	}
	
	// Background Randomizer
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

	private void generateNewWord(int worldLevel, int progressLevel) {
		this.guessedLetters.clear();
		WordBank.WordData data = WordBank.getRandomWord(worldLevel, progressLevel);
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
		if (isSettingsOpen || isInstructionOpen || battleOver)
			return;

		// Damage Visuals (Sprite & Screen Shake)
		updateDamageVisuals();

		// Damage Indicator Fade
		if (damageIndicatorStartTime > 0) {
			long timeElapsed = System.currentTimeMillis() - damageIndicatorStartTime;
			if (timeElapsed > DAMAGE_INDICATOR_DURATION) {
				damageIndicatorPlayer = 0;
				damageIndicatorEnemy = 0;
				damageIndicatorStartTime = 0;
			}
		}

		if (isAnimatingAction) {
			long timeElapsed = System.currentTimeMillis() - actionStartTime;
			currentAnimTime = timeElapsed;

			// Check for impact time animation
			if (impactTriggered && timeElapsed >= ATTACK_HOLD_TIME) {
				
				// Trigger screen shake and damage indicators
				if (damageStartTime == 0) {
					damageStartTime = System.currentTimeMillis();
					damageIndicatorStartTime = System.currentTimeMillis();
				}

				if (isPlayerAttacking) {
					isEnemyDamaged = true;
				} else {
					isPlayerDamaged = true;
				}

				impactTriggered = false;
			}

			// Attack Animation State

			if (timeElapsed > ACTION_DURATION) {
				isAnimatingAction = false;
				currentAnimTime = 0;
				playerAlpha = 1.0f;
				enemyAlpha = 1.0f;
				checkRoundResult();
			} else if (timeElapsed < FADE_DURATION) {
				float fade = (float) timeElapsed / FADE_DURATION;
				playerAlpha = isPlayerAttacking ? 1.0f - fade : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : 1.0f - fade;
			} else if (timeElapsed < TELEPORT_TO_TARGET_TIME) {
				playerAlpha = isPlayerAttacking ? 0.0f : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : 0.0f;
			} else if (timeElapsed < FADE_IN_TARGET_TIME) {
				long fadeTime = timeElapsed - TELEPORT_TO_TARGET_TIME;
				float fade = (float) fadeTime / FADE_DURATION;

				playerAlpha = isPlayerAttacking ? fade : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : fade;

			} else if (timeElapsed < ATTACK_HOLD_TIME) {
				playerAlpha = 1.0f;
				enemyAlpha = 1.0f;
			} else if (timeElapsed < ATTACK_HOLD_TIME + FADE_DURATION) {
				long fadeTime = timeElapsed - ATTACK_HOLD_TIME;
				float fade = (float) fadeTime / FADE_DURATION;

				playerAlpha = isPlayerAttacking ? 1.0f - fade : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : 1.0f - fade;
			} else if (timeElapsed < FADE_OUT_RETURN_TIME) {
				playerAlpha = isPlayerAttacking ? 0.0f : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : 0.0f;
			} else if (timeElapsed < RETURN_HOME_TIME) {
				long fadeTime = timeElapsed - FADE_OUT_RETURN_TIME;
				float fade = (float) fadeTime / FADE_DURATION;

				playerAlpha = isPlayerAttacking ? fade : 1.0f;
				enemyAlpha = isPlayerAttacking ? 1.0f : fade;
			}

			playerAlpha = Math.max(0.0f, Math.min(1.0f, playerAlpha));
			enemyAlpha = Math.max(0.0f, Math.min(1.0f, enemyAlpha));

		} else if (isDeathAnimating) {
			if (System.currentTimeMillis() - deathStartTime > DEATH_DURATION) {
				isDeathAnimating = false;
				if (!currentEnemy.isAlive()) {
					if (rewardsClaimed) {
						handleWin(); 
					} else {
						// Calculate rewards and start the EXP animation
						calculateRewardsAndStartAnimation();
					}
				} else if (!playerPet.isAlive()) {
					handleLoss();
				}
			}
		} else if (isRewardAnimating) { // Handle Reward Animation
			long timeElapsed = System.currentTimeMillis() - rewardAnimStartTime;
			if (timeElapsed >= REWARD_ANIM_DURATION) {
				isRewardAnimating = false;
				handleWin(); // Proceed to victory screen
			}

			// Handle Level Up Flash
			if (levelUpOccurred) {
				// Flash effect when leveling up
				long duration = (long) LEVEL_UP_FLASH_DURATION;
				long timeIntoCycle = timeElapsed % duration;

				if (timeIntoCycle < duration / 2) {
					levelUpFlashAlpha = ((float) timeIntoCycle / (duration / 2)) * LEVEL_UP_FLASH_PEAK;
				} else {
					levelUpFlashAlpha = LEVEL_UP_FLASH_PEAK
							- ((float) (timeIntoCycle - (duration / 2)) / (duration / 2)) * LEVEL_UP_FLASH_PEAK;
				}
				levelUpFlashAlpha = Math.max(0.0f, Math.min((float) LEVEL_UP_FLASH_PEAK, levelUpFlashAlpha));
			}

		} else { // When no animation is running, handle the timer and panic logic

			// Timer Logic
			long elapsedTime = System.currentTimeMillis() - lastGuessTime;
			long maxTimeMillis = GameConstants.GUESS_TIME_LIMIT_SECONDS * 1000;
			currentTimeRemaining = maxTimeMillis - elapsedTime;

			int secondsRemaining = (int) (currentTimeRemaining / 1000) + 1;
			boolean isPanicking = secondsRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS && secondsRemaining > 0;

			if (secondsRemaining == GameConstants.PANIC_THRESHOLD_SECONDS && !hasFlashedThisGuess) {
				rabbitFlashStartTime = System.currentTimeMillis();
				hasFlashedThisGuess = true;
			}

			if (secondsRemaining > GameConstants.PANIC_THRESHOLD_SECONDS) {
				hasFlashedThisGuess = false;
			}

			if (isPanicking) {

				if (System.currentTimeMillis() - lastShakeTime > 100) {
					timeShakeX = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					timeShakeY = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					lastShakeTime = System.currentTimeMillis();
				}

				if (secondsRemaining != lastSecondChecked) {
					redPulseAlpha = MAX_RED_ALPHA;
					lastSecondChecked = secondsRemaining;
				}

				long timeSincePulse = System.currentTimeMillis() % 1000;
				if (timeSincePulse < 500) {
					redPulseAlpha = (int) (MAX_RED_ALPHA * (1.0 - (timeSincePulse / 500.0)));
				} else {
					redPulseAlpha = 0;
				}
				redPulseAlpha = Math.max(0, redPulseAlpha);

				// Panic Alpha for Timer Box (Flicker)
				long pulseTime = System.currentTimeMillis() % 1000;
				if (pulseTime < 500) {
					panicAlpha = 80;
				} else {
					panicAlpha = 0;
				}

			} else {

				timeShakeX = 0;
				timeShakeY = 0;
				redPulseAlpha = 0;
				panicAlpha = 0;
				lastSecondChecked = -1;
			}

			if (currentTimeRemaining <= 0) {
				currentTimeRemaining = 0; 
				handleTimeOutAttack();
			}
		}
	}

	private void updateDamageVisuals() {
		// Only run if a damage sequence is active
		if (isPlayerDamaged || isEnemyDamaged) {


			if (damageStartTime == 0) {
				return;
			}

			// Shake Effect Active
			long timeElapsed = System.currentTimeMillis() - damageStartTime;

			if (timeElapsed < DAMAGE_VISUAL_DURATION) {

				// Sprite Shake Logic 
				if (timeElapsed % 50 < 25) {
					spriteShakeX = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
					spriteShakeY = random.nextInt(SHAKE_MAGNITUDE * 2 + 1) - SHAKE_MAGNITUDE;
				} else {
					spriteShakeX = 0;
					spriteShakeY = 0;
				}

				// Global Screen Shake Logic
				if (System.currentTimeMillis() - lastScreenShakeTime > 20) { // Shake updates more often
					screenShakeX = random.nextInt(SCREEN_SHAKE_MAGNITUDE * 2 + 1) - SCREEN_SHAKE_MAGNITUDE;
					screenShakeY = random.nextInt(SCREEN_SHAKE_MAGNITUDE * 2 + 1) - SCREEN_SHAKE_MAGNITUDE;
					lastScreenShakeTime = System.currentTimeMillis();
				}

			} else {

				// Shake duration is over
				isPlayerDamaged = false;
				isEnemyDamaged = false;
				spriteShakeX = 0;
				spriteShakeY = 0;
				screenShakeX = 0;
				screenShakeY = 0;
				damageStartTime = 0;
			}
		}
	}

	private void handleTimeOutAttack() {

		lastGuessTime = System.currentTimeMillis();

		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;
		isPlayerAttacking = false;

		currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
		playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);

		int dmg = currentEnemy.getAttackPower();
		playerPet.takeDamage(dmg);

		damageIndicatorPlayer = -dmg;
		damageIndicatorEnemy = 0;


		message = "Time Out! Ouch! " + damageIndicatorPlayer + " HP";
		messageColor = Color.RED;

		impactTriggered = true; 
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

	private void calculateRewardsAndStartAnimation() {

		if (rewardsClaimed) {
			return;
		}

		boolean isBoss = (playerUser.getProgressLevel() == 5);

		if (isBoss) {
			goldReward = 10;
			expReward = 5;
		} else {
			goldReward = 5;
			expReward = 1;
		}

		// Handle Progression 
		if (isBoss) {
			playerUser.setProgressLevel(1);
			playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
		} else {
			playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);
		}

		// Apply Rewards 
		levelUpOccurred = playerPet.gainExp(expReward, playerUser.getWorldLevel());

		playerUser.addGold(goldReward);

		// Update user data immediately to save the progress
		Main.userManager.updateUser(playerUser);

		// Delete Save on Win
		Main.saveManager.deleteSave(playerUser.getUsername());

		// Set the message for the central box
		String rewardMsg = String.format("Gold: +%dG | Exp: +%d EXP", goldReward, expReward);

		if (levelUpOccurred) {
			message = "LEVEL UP! " + rewardMsg;
			messageColor = Color.CYAN;
		} else if (playerPet.getLevel() >= playerUser.getWorldLevel()
				&& playerPet.getCurrentExp() >= playerPet.getMaxExpForCurrentLevel()) {
			message = "EXP MAX (Defeat Boss!) " + rewardMsg;
			messageColor = Color.ORANGE;
		} else {
			message = rewardMsg;
			messageColor = Color.YELLOW;
		}

		rewardsClaimed = true;

		// Start the animation
		isRewardAnimating = true;
		rewardAnimStartTime = System.currentTimeMillis();
	}

	private void handleWin() {
		battleOver = true;
		playerWon = true;

		// Word carry over logic
		if (!isWordCompleted()) {
			shouldCarryOverWord = true;
		} else {
			shouldCarryOverWord = false;
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
		if (isInstructionOpen) {
			isInstructionOpen = false;
			return "NONE";
		}

		if (isSettingsOpen) {
			if (modalContinueBounds != null && modalContinueBounds.contains(x, y)) {
				isSettingsOpen = false;
				isExitConfirmation = false;
				isMenuConfirmation = false;
				return "NONE";
			} else if (modalSaveBounds != null && modalSaveBounds.contains(x, y)) {
				// Save Game Function
				saveGame();
				return "NONE";
			} else if (modalMenuBounds != null && modalMenuBounds.contains(x, y)) {
				// Menu Confirmation
				if (!isMenuConfirmation) {
					isMenuConfirmation = true;
					isExitConfirmation = false;
				} else {
					return "MENU";
				}
				return "NONE";
			} else if (modalExitBounds != null && modalExitBounds.contains(x, y)) {
				// Exit Confirmation
				if (!isExitConfirmation) {
					isExitConfirmation = true;
					isMenuConfirmation = false;
				} else {
					return "EXIT";
				}
				return "NONE";
			}

			isExitConfirmation = false;
			isMenuConfirmation = false;

		} else {
			if (settingsBtnBounds != null && settingsBtnBounds.contains(x, y)) {
				isSettingsOpen = true;
				isExitConfirmation = false;
				isMenuConfirmation = false;
				selectedModalOption = -1;
				return "NONE";
			} else if (instructionBtnBounds != null && instructionBtnBounds.contains(x, y)) {
				isInstructionOpen = true;
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

		if (isInstructionOpen) {
			if (keyCode == KeyEvent.VK_ESCAPE) {
				isInstructionOpen = false;
			}
			return;
		}

		if (battleOver) {
			handleMenuInput(keyCode);
			return;
		}

		if (isRewardAnimating) { 
			return;
		}

		if (isAnimatingAction || isDeathAnimating)
			return;

		if (currentTimeRemaining <= 0)
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

		// Reset Timer On Valid Guess
		lastGuessTime = System.currentTimeMillis();
		timeShakeX = 0;
		timeShakeY = 0;
		redPulseAlpha = 0;
		panicAlpha = 0;
		lastSecondChecked = -1;
		this.hasFlashedThisGuess = false;

		boolean isCorrect = false;
		// Multiplier Damage
		int letterCount = 0;
		for (char c : secretWord.toCharArray()) {
			if (c == guess) {
				isCorrect = true;
				letterCount++;
			}
		}

		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;

		// Clear active shake flags before attack starts
		isPlayerDamaged = false;
		isEnemyDamaged = false;
		damageStartTime = 0;

		if (isCorrect) {
			isPlayerAttacking = true;

			playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
			currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);

			int baseDmg = playerPet.getAttackPower();
			int finalDmg = baseDmg * letterCount; // Apply damage multiplier

			currentEnemy.takeDamage(finalDmg);

			// Set damage indicator for enemy
			damageIndicatorEnemy = finalDmg;
			damageIndicatorPlayer = 0;

			// Damage Indicator
			message = "Correct! Hit! +" + finalDmg + " DMG";
			messageColor = Color.GREEN;

			impactTriggered = true; 

			if (isWordCompleted()) {
				if (currentEnemy.isAlive()) {
					message = "Word Cleared! New Word!";
					messageColor = Color.CYAN;
					generateNewWord(playerUser.getWorldLevel(), playerUser.getProgressLevel());
				}
			}

		} else {
			isPlayerAttacking = false;

			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);

			int dmg = currentEnemy.getAttackPower();
			playerPet.takeDamage(dmg);

			// Set damage indicator for player
			damageIndicatorPlayer = -dmg;
			damageIndicatorEnemy = 0;

			message = "Wrong! Ouch! " + damageIndicatorPlayer + " HP";
			messageColor = Color.RED;

			impactTriggered = true;
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

		// Screen shake applies to the entire scene
		int globalShakeX = screenShakeX;
		int globalShakeY = screenShakeY;

		// Sprite shake only applies to the damaged character sprite
		int spriteTargetShakeX = isPlayerDamaged ? spriteShakeX : isEnemyDamaged ? spriteShakeX : 0;
		int spriteTargetShakeY = isPlayerDamaged ? spriteShakeY : isEnemyDamaged ? spriteShakeY : 0;

		// UI shake only applies to the timer
		int uiOffsetX = timeShakeX;
		int uiOffsetY = timeShakeY;

		long timeElapsedDamage = System.currentTimeMillis() - damageStartTime;
		int currentDamagePulseAlpha = 0;
		if (isPlayerDamaged || isEnemyDamaged) {
			float pulseRatio = 1.0f - (float) timeElapsedDamage / DAMAGE_VISUAL_DURATION;
			currentDamagePulseAlpha = (int) (MAX_RED_ALPHA * pulseRatio * 3.0);
			currentDamagePulseAlpha = Math.min(255, Math.max(0, currentDamagePulseAlpha));
		}

		int groundY = height - 20;
		int scaleFactor = 4;

		// Character Positions
		int pDrawX = PLAYER_HOME_X + globalShakeX;
		int eDrawX = ENEMY_HOME_X + globalShakeX;
		float currentPAlpha = playerAlpha;
		float currentEAlpha = enemyAlpha;

		int statsUiY = 140;

		boolean isAttackPhase = isAnimatingAction && currentAnimTime >= TELEPORT_TO_TARGET_TIME
				&& currentAnimTime < FADE_OUT_RETURN_TIME;

		if (isAttackPhase) {
			if (isPlayerAttacking) {
				// Player Attacking
				pDrawX = ATTACK_PLAYER_X + globalShakeX; // Player position shakes
				currentEAlpha = 1.0f;
			} else {
				// Enemy Attacking
				eDrawX = ATTACK_ENEMY_X + globalShakeX; // Enemy position shakes
				currentPAlpha = 1.0f;
			}
		}

		// Background
		if (bgImage != null) {
			g.drawImage(bgImage, globalShakeX, globalShakeY, width, height, observer);
		}

		// Red Pulse Overlay
		if (redPulseAlpha > 0) {
			g.setColor(new Color(255, 0, 0, redPulseAlpha));
			g.fillRect(globalShakeX, globalShakeY, width, height);
		}

		// Damage Pulse Overlay
		if (currentDamagePulseAlpha > 0) {
			g.setColor(new Color(255, 0, 0, currentDamagePulseAlpha));
			g.fillRect(globalShakeX, globalShakeY, width, height);
		}

		// Draw Rabbit Flash
		if (rabbitFlashStartTime > 0 && rabbitImg != null) {
			long timeElapsed = System.currentTimeMillis() - rabbitFlashStartTime;
			if (timeElapsed < RABBIT_FLASH_DURATION) {

				float alpha = 1.0f - (float) timeElapsed / RABBIT_FLASH_DURATION;
				alpha = Math.max(0.0f, Math.min(1.0f, alpha));

				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

				int rX = 0;
				int rY = 0;

				// Time shake to rabbit image if panic mode is active
				if (currentTimeRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS * 1000) {
					rX += timeShakeX;
					rY += timeShakeY;
				}

				// The rabbit image screen overlay
				g.drawImage(rabbitImg, rX, rY, width, height, observer);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
			} else {
				rabbitFlashStartTime = 0; // End animation
			}
		}

		int backdropH = 130;
		// Top backdrop
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		// Global shake to UI elements
		drawLevelIndicator(g, 30 + globalShakeX, TOP_BAR_Y + globalShakeY, observer);

		// Instruction Button
		if (instructionBtnBounds != null && nameFrameImg != null) {
			int btnX = instructionBtnBounds.x + globalShakeX;
			int btnY = instructionBtnBounds.y + globalShakeY;
			int btnW = instructionBtnBounds.width;
			int btnH = instructionBtnBounds.height;

			g.drawImage(nameFrameImg, btnX, btnY, btnW, btnH, observer);

			// "?" text
			g.setFont(GameConstants.HEADER_FONT);
			g.setColor(Color.BLACK);
			FontMetrics fm = g.getFontMetrics();
			String qText = "?";
			int textX = btnX + (btnW - fm.stringWidth(qText)) / 2;
			int textY = btnY + (btnH - fm.getAscent()) / 2 + fm.getAscent() - 10;
			g.drawString(qText, textX, textY);
		}

		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x + globalShakeX, settingsBtnBounds.y + globalShakeY,
					settingsBtnBounds.width, settingsBtnBounds.height, observer);
		}

		drawWordPuzzle(g, width, height, observer, globalShakeX, globalShakeY);

		// Timer gets the time-panic shake
		drawTimer(g, observer, uiOffsetX, uiOffsetY);

		int framesTopY = backdropH + 10;
		if (!message.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			FontMetrics fm = g.getFontMetrics();
			int msgW = fm.stringWidth(message);

			int bgW = msgW + 60;
			int bgH = 50;
			int bgX = (width - bgW) / 2 + globalShakeX;
			int bgY = framesTopY + globalShakeY;

			if (nameFrameImg != null) {
				g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, observer);
			}

			g.setColor(messageColor);
			int textX = (width - msgW) / 2 + globalShakeX;
			int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 7;
			g.drawString(message, textX, textY);
		}

		// Character UI Frames 
		drawCharacterUIFrames1(g, width, statsUiY + globalShakeY, PLAYER_HOME_X + globalShakeX,
				ENEMY_HOME_X + globalShakeX, observer);

		Image playerImg = playerPet.getCurrentImage();
		Image enemyImg = currentEnemy.getCurrentImage();

		// Character Sprites
		if (isPlayerAttacking) {
			drawEnemySprite1(g, enemyImg, eDrawX, groundY + globalShakeY, scaleFactor, currentEAlpha,
					spriteTargetShakeX, spriteTargetShakeY, isEnemyDamaged, observer);
			drawPlayerSprite1(g, playerImg, pDrawX, groundY + globalShakeY, scaleFactor, currentPAlpha, 0, 0, false,
					observer);

		} else {

			drawPlayerSprite1(g, playerImg, pDrawX, groundY + globalShakeY, scaleFactor, currentPAlpha,
					spriteTargetShakeX, spriteTargetShakeY, isPlayerDamaged, observer);
			drawEnemySprite1(g, enemyImg, eDrawX, groundY + globalShakeY, scaleFactor, currentEAlpha, 0, 0, false,
					observer);
		}

		// Damage Indicators 
		drawDamageIndicators(g, width, height, globalShakeX, globalShakeY);

		if (isSettingsOpen) {
			drawSettingsModal(g, width, height);
		} else if (isInstructionOpen) {
			drawInstructionModal(g, width, height);
		} else if (isRewardAnimating) { 
			drawExpAnimation(g, width, height, observer, globalShakeX);
		} else if (battleOver) {
			drawEndScreen(g, width, height);
		}
	}

	// Logic for the EXP/Level Up animation
	private void drawExpAnimation(Graphics2D g, int width, int height, ImageObserver observer, int offsetX) {
		long timeElapsed = System.currentTimeMillis() - rewardAnimStartTime;
		float progress = (float) timeElapsed / REWARD_ANIM_DURATION;

		// Level Up Screen Flash Effect
		if (levelUpOccurred) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, levelUpFlashAlpha / 255.0f));
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		}

		// EXP Gained Indicator Animation
		int expIndicatorTravel = 80;
		int playerGroundY = height - 20;
		int startY = playerGroundY - 150;
		int endY = startY - expIndicatorTravel;

		int indicatorY = (int) (startY - (expIndicatorTravel * progress));
		float alpha = 1.0f - progress;

		if (alpha < 0)
			alpha = 0;

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g.setFont(new Font("Monospaced", Font.BOLD, 36));

		String expText;
		Color expColor;

		if (levelUpOccurred) {
			expText = "LEVEL UP! +" + expReward + " EXP";
			expColor = Color.CYAN;
		} else {
			expText = "+" + expReward + " EXP";
			expColor = Color.YELLOW;
		}

		// EXP amount
		g.setColor(expColor);
		FontMetrics fm = g.getFontMetrics();
		int textX = PLAYER_HOME_X - (fm.stringWidth(expText) / 2) + offsetX;
		g.drawString(expText, textX, indicatorY);

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
	}

	// Instruction Modal
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

		// Instructions Content
		g.setFont(GameConstants.INSTRUCTION_FONT);
		g.setColor(Color.BLACK);

		String content = GameConstants.INSTRUCTION_TEXT;
		String[] lines = content.split("\n");

		// Word Wrapping parameters
		int wrapWidth = mW - 60; 
		int startX = mX + 30; 
		int startY = mY + 95; 
		FontMetrics fmSmall = g.getFontMetrics();
		int lineHeight = fmSmall.getHeight();
		int indent = 20;

		for (String line : lines) {
			String trimmedLine = line.trim();

			if (trimmedLine.isEmpty()) {
				startY += lineHeight / 3;
				continue;
			}

			if (trimmedLine.endsWith(":") || trimmedLine.startsWith("Objectives")) {
				int lineW = fmSmall.stringWidth(line);
				int textX = startX;
				int textY = startY + fmSmall.getAscent();
				g.drawString(line, textX, textY);
				startY += lineHeight;
			}
			else if (trimmedLine.startsWith("Type")) {
				String[] words = trimmedLine.split(" ");
				StringBuilder currentLine = new StringBuilder();
				int currentX = mX + (mW - wrapWidth) / 2; 

				for (String word : words) {
					if (fmSmall.stringWidth(currentLine.toString() + " " + word) > wrapWidth) {
						int textY = startY + fmSmall.getAscent();
						g.drawString(currentLine.toString().trim(), currentX, textY);

						startY += lineHeight;
						currentLine = new StringBuilder();
					}

					if (currentLine.length() > 0) {
						currentLine.append(" ");
					}
					currentLine.append(word);
				}

				if (currentLine.length() > 0) {
					int textY = startY + fmSmall.getAscent();
					g.drawString(currentLine.toString().trim(), currentX, textY);
					startY += lineHeight;
				}
			}
			else {
				String[] words = trimmedLine.split(" ");
				StringBuilder currentLine = new StringBuilder();
				int currentX = startX + (trimmedLine.startsWith("Your") || trimmedLine.startsWith("Defeat")
						|| trimmedLine.startsWith("Win") ? indent : 0);

				for (String word : words) {
					if (fmSmall.stringWidth(currentLine.toString() + " " + word) > wrapWidth) {
						int textY = startY + fmSmall.getAscent();
						g.drawString(currentLine.toString().trim(), currentX, textY);

						startY += lineHeight;
						currentLine = new StringBuilder();
						currentX = startX + (trimmedLine.startsWith("Your") || trimmedLine.startsWith("Defeat")
								|| trimmedLine.startsWith("Win") ? indent * 2 : indent);
					}

					if (currentLine.length() > 0) {
						currentLine.append(" ");
					}
					currentLine.append(word);
				}

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

	private void drawDamageIndicators(Graphics2D g, int width, int height, int offsetX, int offsetY) {
		if (damageIndicatorStartTime == 0)
			return;

		long timeElapsed = System.currentTimeMillis() - damageIndicatorStartTime;
		if (timeElapsed > DAMAGE_INDICATOR_DURATION)
			return;

		float progress = (float) timeElapsed / DAMAGE_INDICATOR_DURATION;

		int verticalOffset = (int) (progress * DAMAGE_INDICATOR_VERTICAL_TRAVEL);
		float alpha = 1.0f - progress;

		if (alpha < 0)
			alpha = 0;

		g.setFont(new Font("Monospaced", Font.BOLD, 30));
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		int groundY = height - 20;

		// Player Damaged
		if (damageIndicatorPlayer != 0) {
			String dmgText = String.valueOf(damageIndicatorPlayer);
			g.setColor(Color.RED);
			int x = PLAYER_HOME_X - 60 + offsetX;
			int y = groundY - 200 - verticalOffset + offsetY;
			g.drawString(dmgText, x, y);
		}

		// Enemy Damaged
		if (damageIndicatorEnemy != 0) {
			String dmgText = String.valueOf(damageIndicatorEnemy);
			g.setColor(Color.YELLOW);
			int x = ENEMY_HOME_X - 60 + offsetX;
			int y = groundY - 200 - verticalOffset + offsetY;
			g.drawString(dmgText, x, y);
		}

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
	}

	// Helper function to draw the player sprite, applying shake only if damaged.
	private void drawPlayerSprite1(Graphics2D g, Image playerImg, int pDrawX, int groundY, int scaleFactor, float alpha,
			int targetShakeX, int targetShakeY, boolean isDamaged, ImageObserver observer) {
		if (playerImg == null)
			return;

		int pW = playerImg.getWidth(observer);
		int pH = playerImg.getHeight(observer);

		if (pW > 0 && pH > 0) {
			int drawW = pW * scaleFactor;
			int drawH = pH * scaleFactor;

			int drawX = pDrawX - (drawW / 2);
			int drawY = groundY - drawH;

			if (isDamaged) {
				drawX += targetShakeX;
				drawY += targetShakeY;
			}

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.drawImage(playerImg, drawX, drawY, drawW, drawH, observer);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
		}
	}

	// Helper function to draw the enemy sprite, applying shake only if damaged.
	private void drawEnemySprite1(Graphics2D g, Image enemyImg, int eDrawX, int groundY, int scaleFactor, float alpha,
			int targetShakeX, int targetShakeY, boolean isDamaged, ImageObserver observer) {
		if (enemyImg == null)
			return;

		int eW = enemyImg.getWidth(observer);
		int eH = enemyImg.getHeight(observer);

		if (eW > 0 && eH > 0) {
			int drawW = eW * scaleFactor;
			int drawH = eH * scaleFactor;

			int drawX = eDrawX - (drawW / 2);
			int drawY = groundY - drawH;

			if (isDamaged) {
				drawX += targetShakeX;
				drawY += targetShakeY;
			}

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.drawImage(enemyImg, drawX, drawY, drawW, drawH, observer);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
		}
	}

	// Helper function to draw the UI frames and content for both characters./
	private void drawCharacterUIFrames1(Graphics2D g, int width, int topY, int playerCenterX, int enemyCenterX,
			ImageObserver observer) {

		int frameW = 200;
		int statsW = 190;
		int frameH = 70;
		int pFrameX = playerCenterX - (frameW / 2);
		int eFrameX = enemyCenterX - (frameW / 2);
		int pStatsX = playerCenterX - (statsW / 2);
		int eStatsX = enemyCenterX - (statsW / 2);
		int statsFrameY = topY + 55;
		int statsFrameH = 80;

		Font levelFont = new Font("Monospaced", Font.BOLD, 16);
		Font nameFont = new Font("Monospaced", Font.BOLD, 16);
		Font statsFont = new Font("Monospaced", Font.BOLD, 16);

		// Player UI

		// Stats Frame
		if (frameImg != null) {
			g.drawImage(frameImg, pStatsX, statsFrameY, statsW, statsFrameH, null);
		}
		// Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, pFrameX, topY, frameW, frameH, null);
		}

		// Name & Level Text
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String pLevel = "LEVEL " + playerPet.getLevel();
		FontMetrics fm = g.getFontMetrics();
		g.drawString(pLevel, pFrameX + (frameW - fm.stringWidth(pLevel)) / 2, topY + 28);
		g.setFont(nameFont);
		String pName = playerPet.getName();
		g.drawString(pName, pFrameX + (frameW - g.getFontMetrics().stringWidth(pName)) / 2, topY + 46);

		// Stats Display (Icon HP | Icon ATK)
		drawModernStats(g, playerPet, pStatsX, statsFrameY, statsW, statsFrameH, statsFont, observer);

		// EXP Bar
		int expBarH = 6;
		int expBarW = statsW - 30;
		int expBarX = pStatsX + 15;
		int expBarY = statsFrameY + statsFrameH - 15;

		// Bar Background
		g.setColor(new Color(50, 50, 50));
		g.fillRect(expBarX, expBarY, expBarW, expBarH);

		// Bar Fill
		float expPercent = (float) playerPet.getCurrentExp() / (float) playerPet.getMaxExpForCurrentLevel();
		if (expPercent > 1.0f)
			expPercent = 1.0f;
		g.setColor(Color.CYAN);
		g.fillRect(expBarX, expBarY, (int) (expBarW * expPercent), expBarH);
		g.setColor(new Color(100, 100, 100));
		g.drawRect(expBarX, expBarY, expBarW, expBarH);

		// Stats Frame
		if (frameImg != null) {
			g.drawImage(frameImg, eStatsX, statsFrameY, statsW, statsFrameH, null);
		}
		// Name Frame
		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, eFrameX, topY, frameW, frameH, null);
		}

		// Name & Level Text
		g.setColor(Color.WHITE);
		g.setFont(levelFont);
		String eLevel = "LEVEL " + currentEnemy.getLevel();
		g.drawString(eLevel, eFrameX + (frameW - fm.stringWidth(eLevel)) / 2, topY + 28);
		g.setFont(nameFont);
		String eName = currentEnemy.getName();
		g.drawString(eName, eFrameX + (frameW - g.getFontMetrics().stringWidth(eName)) / 2, topY + 46);

		// Stats Display
		drawModernStats(g, currentEnemy, eStatsX, statsFrameY, statsW, statsFrameH, statsFont, observer);
	}

	// Helper method to the specific [Heart] HP | [Sword] ATK layout
	private void drawModernStats(Graphics2D g, Character c, int x, int y, int w, int h, Font font, ImageObserver obs) {
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		String hpTxt = " " + c.getCurrentHealth() + "/" + c.getMaxHealth();
		String atkTxt = " " + c.getAttackPower();
		String sep = " | ";

		int iconSize = 15;

		int totalW = iconSize + fm.stringWidth(hpTxt) + fm.stringWidth(sep) + iconSize + fm.stringWidth(atkTxt);

		int startX = x + (w - totalW) / 2;
		int centerY = y + (h / 2) + 5;

		int iconTopY = centerY - 12;

		int currentX = startX;

		// Heart Icon
		if (heartImg != null) {
			g.drawImage(heartImg, currentX, iconTopY, iconSize, iconSize, obs);
		}
		currentX += iconSize;

		// HP Text
		g.setColor(Color.WHITE);
		g.drawString(hpTxt, currentX, centerY);
		currentX += fm.stringWidth(hpTxt);

		// Separator |
		g.setColor(Color.GRAY);
		g.drawString(sep, currentX, centerY);
		currentX += fm.stringWidth(sep);

		// Attack Icon
		if (attackImg != null) {
			g.drawImage(attackImg, currentX, iconTopY, iconSize, iconSize, obs);
		}
		currentX += iconSize;

		// Atk Text
		g.setColor(Color.WHITE);
		g.drawString(atkTxt, currentX, centerY);
	}

	private void drawWordPuzzle(Graphics2D g, int width, int height, ImageObserver obs, int offsetX, int offsetY) {
		g.setFont(GameConstants.UI_FONT);

		// Clue
		String clueText = "CLUE: " + clue;
		FontMetrics fm = g.getFontMetrics();
		int textW = fm.stringWidth(clueText);

		int bgW = textW + 60;
		int bgH = TOP_BAR_HEIGHT;
		int bgX = (width - bgW) / 2 + offsetX;
		int bgY = TOP_BAR_Y + offsetY;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, obs);
		}

		g.setColor(Color.WHITE);
		int textX = (width - textW) / 2 + offsetX;
		int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 5;
		g.drawString(clueText, textX, textY);

		// Guess Letters
		int spacing = 60;
		int lettersY = 110 + offsetY;
		int totalWidth = secretWord.length() * spacing;

		int startX = (width - totalWidth) / 2 + offsetX;
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

		int mW = MODAL_WIDTH;
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

		// Continue
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

		// Save Game
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

		// Main Menu
		nextY += btnH + btnGap;
		String menuTxt = "MAIN MENU";
		if (isMenuConfirmation)
			menuTxt = "CONFIRM?";

		if (selectedModalOption == 2) {
			g.setColor(isMenuConfirmation ? Color.RED : Color.YELLOW);
			menuTxt = "> " + menuTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int menuW = fm.stringWidth(menuTxt);
		modalMenuBounds = new Rectangle(mX + (mW - menuW) / 2 - 10, nextY, menuW + 20, btnH);
		g.drawString(menuTxt, mX + (mW - menuW) / 2, nextY + 25);

		// Exit Game
		nextY += btnH + btnGap;
		String exitTxt = "EXIT GAME";
		if (isExitConfirmation)
			exitTxt = "CONFIRM?";

		if (selectedModalOption == 3) {
			g.setColor(isExitConfirmation ? Color.RED : Color.YELLOW);
			exitTxt = "> " + exitTxt + " <";
		} else {
			g.setColor(Color.BLACK);
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

	private void drawTimer(Graphics2D g, ImageObserver observer, int offsetX, int offsetY) {

		int drawX = timerX + offsetX;
		int drawY = timerY + offsetY;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, drawX, drawY, timerW, timerH, observer);
		}

		// Draw Panic Flash Effect 
		if (panicAlpha > 0) {
			Color panicColor = new Color(GameConstants.TIMER_PANIC_COLOR.getRed(),
					GameConstants.TIMER_PANIC_COLOR.getGreen(), GameConstants.TIMER_PANIC_COLOR.getBlue(), panicAlpha);
			g.setColor(panicColor);
			g.fillRect(drawX + 2, drawY + 2, timerW - 4, timerH - 4);
		}

		int secondsRemaining = (int) Math.max(0, currentTimeRemaining / 1000) + 1;
		String timerText = String.format("TIME: %d", secondsRemaining);

		// Change text color to Red during panic
		if (secondsRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS) {
			g.setColor(GameConstants.TIMER_PANIC_COLOR);
		} else {
			g.setColor(Color.BLACK);
		}

		g.setFont(GameConstants.UI_FONT);
		FontMetrics fm = g.getFontMetrics();

		// Center the text
		int textX = drawX + (timerW - fm.stringWidth(timerText)) / 2;
		int textY = drawY + (timerH - fm.getAscent()) / 2 + fm.getAscent() - 5;

		g.drawString(timerText, textX, textY);
	}
}