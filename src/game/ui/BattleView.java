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

// This class controls the battle screen and all the fighting logic
public class BattleView {

	private User playerUser;
	private Hangpie playerPet;
	private Enemy currentEnemy;

	// Regular Enemies
	// This array stores the names and image folders for all normal enemies
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
	// This set holds the letters that the player has already guessed for fast
	// checking
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
	// These constants set the exact timing for each part of the attack animation
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
	private final int SCREEN_SHAKE_MAGNITUDE = 10; // INCREASED MAGNITUDE
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
	
	// NEW ALL-IN GUESS FIELDS
	private boolean isAllInGuessOpen = false;
	private long allInGuessStartTime = 0;
	private Rectangle allInButtonBounds;
	private boolean isAllInHovered = false;
	
	// NEW: Screen shake control for All-In
	private boolean isAllInShaking = false;
	private final int ALL_IN_SHAKE_MAGNITUDE = 5;
	private long lastAllInShakeTime = 0;
	
	// NEW: Shifting Dash Effect (All-In)
	private int currentDashOffset = 0;
	private long lastDashShiftTime = 0;
	private final int DASH_SHIFT_DURATION = 500; // Shift every 0.5 seconds
	private final int NUM_DASH_STATES = 3; // States: _ _ -, _ - _, - _ _ (0, 1, 2)
	private final int DASH_SIZE = 3; // Number of characters in the dash group
	private final int SHIFT_SPACING = 60; // Must match spacing in drawAllInGuessModal
	// END NEW FIELDS

	// UI Buttons
	// These define the clickable areas for the buttons
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

	// This function starts the battle
	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();
		this.lastGuessTime = System.currentTimeMillis();

		// Reset Animations
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);
		
		// NEW: Reset All-In State
		this.isAllInGuessOpen = false;

		loadAssets();
		initBattle(); // This sets up the game data
	}

	// This function loads all the necessary pictures
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

	// This function sets up the current battle including enemies and words
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

		// Reset Reward Animation State
		this.isRewardAnimating = false;
		this.rewardAnimStartTime = 0;
		this.levelUpOccurred = false;
		this.levelUpFlashAlpha = 0.0f;
		
		// NEW: Reset All-In State/Shake/Dash
		this.isAllInGuessOpen = false;
		this.allInGuessStartTime = 0;
		this.isAllInHovered = false;
		this.isAllInShaking = false;
		this.currentDashOffset = 0;


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
			loadGame(); // Loads the game state from the file
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
		generateEnemy(); // Picks a new enemy based on the player's level

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
		
		// NEW: All-In Button Bounds (positioned above the timer)
		allInButtonBounds = new Rectangle(timerX, timerY - timerH - 10, timerW, timerH);
	}

	// This function pulls all saved data from the file
	private void loadGame() {
		BattleState save = Main.saveManager.loadBattle(playerUser.getUsername());

		// Restore Word
		this.secretWord = save.getSecretWord();
		this.clue = save.getClue();
		this.guessedLetters.clear();
		// Fills the set with the old guessed letters
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
		
		// NEW: All-In Button Bounds (positioned above the timer)
		allInButtonBounds = new Rectangle(timerX, timerY - timerH - 10, timerW, timerH);

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

		// NEW: Reset All-In State/Shake/Dash
		this.isAllInGuessOpen = false;
		this.allInGuessStartTime = 0;
		this.isAllInHovered = false;
		this.isAllInShaking = false;
		this.currentDashOffset = 0;


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
	// This function saves the current battle status to the file
	private void saveGame() {
		StringBuilder sb = new StringBuilder();
		// Builds the string of guessed letters from the set
		for (java.lang.Character c : guessedLetters) {
			sb.append(c);
		}

		// Creates a single object with all the save data
		BattleState state = new BattleState(playerUser.getUsername(), secretWord, clue, sb.toString(),
				currentEnemy.getName(), currentEnemy.getCurrentHealth(), currentEnemy.getMaxHealth(),
				currentEnemy.getAttackPower(), currentEnemy.getFolderName(), currentEnemy.getLevel(),
				playerPet.getUniqueId(), playerPet.getCurrentHealth());

		Main.saveManager.saveBattle(state);
		message = "Game Saved!";
		messageColor = Color.CYAN;
	}

	// This function chooses an enemy for the current fight
	private void generateEnemy() {
		int currentWorld = playerUser.getWorldLevel();
		int currentProg = playerUser.getProgressLevel();

		String enemyName;
		String enemyPath;

		// Base Stats for World 1: 10 HP, 1 ATK
		// Stats get bigger as the world level goes up
		int baseHp = 10 + ((currentWorld - 1) * 5);
		int baseAtk = 1 + ((currentWorld - 1) * 1);

		int enemyHp = baseHp;
		int enemyAtk = baseAtk;

		boolean isBoss = (currentProg == 5);
		generateBackground(isBoss);

		if (isBoss) {

			// Boss Stats: They get extra HP and ATK
			enemyHp = baseHp + 5;
			enemyAtk = baseAtk + 1;

			int bossIndex = random.nextInt(4) + 1;

			// Chooses one of the four big boss enemies randomly
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
			// Picks a random enemy from the regular enemies list
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
			// Chooses a random background from a list of valid scenes
			int[] validBgIndices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21 };
			int bgIndex = random.nextInt(validBgIndices.length);
			int bgNum = validBgIndices[bgIndex];
			String bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
			this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
		}
	}

	// This function gets a new word for the game
	private void generateNewWord(int worldLevel, int progressLevel) {
		this.guessedLetters.clear(); // Clears the guessed set for the new word
		WordBank.WordData data = WordBank.getRandomWord(worldLevel, progressLevel);
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;
	}

	// This function checks if the player has guessed all the letters in the word
	private boolean isWordCompleted() {
		for (char c : secretWord.toCharArray()) {
			// Checks if any character is not a space and is missing from the guessed set
			if (c != ' ' && !guessedLetters.contains(c)) {
				return false;
			}
		}
		return true;
	}

	// This function is called every frame to update the game status and animations
	public void update() {
		if (isSettingsOpen || isInstructionOpen || battleOver)
			return;
			
		// NEW: Check All-In Guess Time Out
		if (isAllInGuessOpen) {
			long timeElapsed = System.currentTimeMillis() - allInGuessStartTime;
			long maxTimeMillis = GameConstants.ALL_IN_GUESS_TIME_SECONDS * 1000;
			currentTimeRemaining = maxTimeMillis - timeElapsed;
			
			// Handle Dash Shift Effect
			if (System.currentTimeMillis() - lastDashShiftTime > DASH_SHIFT_DURATION) {
				currentDashOffset = (currentDashOffset + 1) % NUM_DASH_STATES;
				lastDashShiftTime = System.currentTimeMillis();
			}

			// Handle intense screen shake for ALL-IN mode
			if (isAllInShaking) {
				if (System.currentTimeMillis() - lastAllInShakeTime > 50) {
					screenShakeX = random.nextInt(ALL_IN_SHAKE_MAGNITUDE * 2 + 1) - ALL_IN_SHAKE_MAGNITUDE;
					screenShakeY = random.nextInt(ALL_IN_SHAKE_MAGNITUDE * 2 + 1) - ALL_IN_SHAKE_MAGNITUDE;
					lastAllInShakeTime = System.currentTimeMillis();
				}
			} else {
				screenShakeX = 0;
				screenShakeY = 0;
			}


			if (currentTimeRemaining <= 0) {
				currentTimeRemaining = 0;
				// Loss Condition: Time Out
				handleAllInGuessTimeOut(); 
				return;
			}
			
			// Handle panic visual for the All-In timer
			int secondsRemaining = (int) (currentTimeRemaining / 1000) + 1;
			boolean isPanicking = secondsRemaining <= GameConstants.PANIC_THRESHOLD_SECONDS && secondsRemaining > 0;
			
			// Handle rabbit flash per second
			if (secondsRemaining > 0) {
				long timeIntoSecond = timeElapsed % 1000;
				
				// Trigger flash for the first 200ms of every second
				if (timeIntoSecond < 200 && !hasFlashedThisGuess) {
					rabbitFlashStartTime = System.currentTimeMillis();
					hasFlashedThisGuess = true;
				} else if (timeIntoSecond >= 200) {
					// Reset the flag for the next second
					hasFlashedThisGuess = false;
				}
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
			
			return; // Skip normal battle updates if All-In is active
		}


		// Damage Visuals (Sprite & Screen Shake)
		updateDamageVisuals(); // Updates visual effects - screen shake

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
			// This tells the game  when the damage should happen during the animation
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
			// Uses timing to control the fade and position of characters
			if (timeElapsed > ACTION_DURATION) {
				isAnimatingAction = false;
				currentAnimTime = 0;
				playerAlpha = 1.0f;
				enemyAlpha = 1.0f;
				checkRoundResult(); // Check for win or loss after the attack finishes
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
				handleWin(); // Go to the victory screen
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
					// Makes the timer shake a little
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

				// Panic Effect for Timer Box (Flicker)
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
				handleTimeOutAttack(); // Punishes the player if the timer hits zero
			}
		}
	}

	// This updates the shaking effect when a character takes damage
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

				// Global Screen Shake Logic (Updated to shake continuously during damage duration)
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
				screenShakeX = 0; // RESET GLOBAL SHAKE
				screenShakeY = 0; // RESET GLOBAL SHAKE
				damageStartTime = 0;
			}
		}
	}
	
	// NEW: Logic for all-in guess failure (Loss Condition: Time Out / Incorrect Letter)
	private void handleAllInGuessTimeOut() {
		System.out.println("[Battle] All-In Guess Failed: Time Out/Incorrect Letter.");
		// Set Loss Message
		message = "All-In Failed! Ouch! -999 HP";
		messageColor = Color.RED;
		
		// This uses the same logic as a time out from normal game, but sets up the instant loss.
		handleTimeOutAttack(); 
		damageIndicatorPlayer = -999; // Set the large damage indicator
		playerPet.takeDamage(999); // Overwhelming damage for instant defeat
		
		isAllInGuessOpen = false; // Close Modal
		isAllInShaking = false; // Stop shake
	}
	
	// NEW: Logic for all-in guess success
	private void triggerAllInWin() {
		System.out.println("[Battle] All-In Guess Succeeded.");
		// Set Win Message
		message = "All-In Success! Instant K.O.!";
		messageColor = Color.GREEN;
		
		// Trigger Player Attack and Enemy Death
		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;
		isPlayerAttacking = true;

		playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
		currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);
		
		currentEnemy.takeDamage(999); // Overwhelming damage for instant defeat

		damageIndicatorEnemy = 999;
		damageIndicatorPlayer = 0;
		impactTriggered = true;
		
		// Force the game to acknowledge the word is complete so next step is the next enemy/boss fight logic
		for (char c : secretWord.toCharArray()) {
            if (c != ' ' && !guessedLetters.contains(c)) {
                guessedLetters.add(c);
            }
        }
		
		isAllInGuessOpen = false; // Close Modal
		isAllInShaking = false; // Stop shake
	}


	// Handles the enemy attacking when time runs out
	private void handleTimeOutAttack() {

		lastGuessTime = System.currentTimeMillis();

		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;
		isPlayerAttacking = false;

		currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
		playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);

		int dmg = currentEnemy.getAttackPower();
		playerPet.takeDamage(dmg); // Player pet takes damage

		damageIndicatorPlayer = -dmg;
		damageIndicatorEnemy = 0;

		message = "Time Out! Ouch! " + damageIndicatorPlayer + " HP";
		messageColor = Color.RED;

		impactTriggered = true;
	}


	// Checks if a character died and starts the next phase
	private void checkRoundResult() {
		if (!currentEnemy.isAlive()) {
			startDeathSequence(currentEnemy);
		} else if (!playerPet.isAlive()) {
			startDeathSequence(playerPet);
		} else {
			// If everyone is alive, set animations back to idle
			playerPet.setAnimationState(Hangpie.AnimState.IDLE);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE);
		}
	}

	// Starts the death animation for the character that lost
	private void startDeathSequence(Character victim) {
		isDeathAnimating = true;
		deathStartTime = System.currentTimeMillis();

		if (victim instanceof Enemy) { // Checks if the victim is an enemy
			((Enemy) victim).setAnimationState(Enemy.AnimState.DEATH);
			playerPet.setAnimationState(Hangpie.AnimState.IDLE);
		} else if (victim instanceof Hangpie) { // Checks if the victim is the player's pet
			((Hangpie) victim).setAnimationState(Hangpie.AnimState.DEATH);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE);
		}
	}

	// Calculates the gold and experience gained after winning
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
			playerUser.setWorldLevel(playerUser.getWorldLevel() + 1); // Go to the next world
		} else {
			playerUser.setProgressLevel(playerUser.getProgressLevel() + 1); // Go to the next stage
		}

		// Apply Rewards
		levelUpOccurred = playerPet.gainExp(expReward, playerUser.getWorldLevel()); // Gives pet EXP

		playerUser.addGold(goldReward); // Gives player gold

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

	// Handles the win condition
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

	// Handles the loss condition
	private void handleLoss() {
		battleOver = true;
		playerWon = false;

		// Death Penalty: Reset to Stage 1 of Current World
		playerUser.setProgressLevel(1); // Reset progress
		Main.userManager.updateUser(playerUser);

		// Delete Save on Loss
		Main.saveManager.deleteSave(playerUser.getUsername());
	}

	// Checks where the mouse is moving for button hovers
	public void handleMouseMove(int x, int y) {
		
		if (isAllInGuessOpen) {
			return;
		}
		
		if (isSettingsOpen) {
			selectedModalOption = -1;
			// Checks mouse position against button areas
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
		
		// NEW: Check All-In Button Hover (now above timer)
		if (allInButtonBounds != null) {
			isAllInHovered = allInButtonBounds.contains(x, y);
		}
	}

	// Handles mouse clicks
	public String handleMouseClick(int x, int y) {
		
		if (isAllInGuessOpen) {
			return "NONE";
		}
		
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
					isMenuConfirmation = true; // Needs a second click to confirm
					isExitConfirmation = false;
				} else {
					return "MENU";
				}
				return "NONE";
			} else if (modalExitBounds != null && modalExitBounds.contains(x, y)) {
				// Exit Confirmation
				if (!isExitConfirmation) {
					isExitConfirmation = true; // Needs a second click to confirm
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
				isSettingsOpen = true; // Opens the settings panel
				isExitConfirmation = false;
				isMenuConfirmation = false;
				selectedModalOption = -1;
				return "NONE";
			} else if (instructionBtnBounds != null && instructionBtnBounds.contains(x, y)) {
				isInstructionOpen = true; // Opens the instruction panel
				return "NONE";
			} else if (allInButtonBounds != null && allInButtonBounds.contains(x, y)) { // NEW: All-In Button Click
				if (isAnimatingAction || isDeathAnimating || battleOver) return "NONE"; // Cannot start mid-animation/game over
				
				// Pause normal game loop logic
				isAllInGuessOpen = true; 
				allInGuessStartTime = System.currentTimeMillis();
				currentTimeRemaining = GameConstants.ALL_IN_GUESS_TIME_SECONDS * 1000; // Reset Timer for display
				
				message = "ALL-IN GUESS: Type the word!";
				messageColor = Color.RED;
				
				// Trigger initial screen shake
				isAllInShaking = true;
				lastAllInShakeTime = System.currentTimeMillis();
				
				System.out.println("[Battle] All-In Guess Started. Word: " + secretWord);
				return "NONE";
			}
		}
		return "NONE";
	}

	// Handles keyboard input for guessing and menu control
	public void handleKeyPress(int keyCode, char keyChar) {
		
		// NEW: All-In Guess Modal Logic (Non-Sequential)
		if (isAllInGuessOpen) {
			if (currentTimeRemaining <= 0) {
				handleAllInGuessTimeOut(); // Re-trigger on key press if already timed out
				return;
			}
			
			char key = java.lang.Character.toUpperCase(keyChar);
			if (key < 'A' || key > 'Z') return; // Only process letters

			// Skip if already guessed
			if (guessedLetters.contains(key)) {
				message = "Already revealed: " + key;
				messageColor = Color.YELLOW;
				return;
			}
			
			// Check if the key is in the secret word
			boolean isCorrect = false;
			for (char c : secretWord.toCharArray()) {
				if (c == key) {
					isCorrect = true;
					break;
				}
			}

			if (isCorrect) {
				// Correct, non-sequential input
				guessedLetters.add(key); // Add the revealed letter
				
				if (isWordCompleted()) {
					// Word is completely guessed correctly
					triggerAllInWin();
				} else {
					message = "Correct! Letter '" + key + "' revealed.";
					messageColor = Color.GREEN;
				}
			} else {
				// Incorrect input - INSTANT LOSS
				System.out.println("[Battle] All-In Guess Failed: Incorrect Letter. Word: " + secretWord + ", got " + key);
				message = "All-In Failed: Wrong Letter! '" + key + "' is not in the word!";
				messageColor = Color.RED;
				
				// Re-use handleTimeOutAttack logic to start loss sequence
				handleAllInGuessTimeOut(); 
			}
			
			return; // Do not process normal guess after all-in attempt
		}
		
		
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
		if (guess < 'A' || guess > 'Z') // Checks if the key pressed is a letter
			return;

		if (guessedLetters.contains(guess)) { // Checks if the letter was already guessed
			message = "Already guessed " + guess + "!";
			messageColor = Color.YELLOW;
			return;
		}

		guessedLetters.add(guess); // Add the new correct guess

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
		// Checks every letter in the secret word
		for (char c : secretWord.toCharArray()) {
			if (c == guess) {
				isCorrect = true;
				letterCount++; // Counts how many times the correct letter appears
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
			int finalDmg = baseDmg * letterCount; // Damage is multiplied by the number of letters hit

			currentEnemy.takeDamage(finalDmg);

			// Set damage indicator for enemy
			damageIndicatorEnemy = finalDmg;
			damageIndicatorPlayer = 0;

			// Damage Indicator
			message = "Correct! Hit! +" + finalDmg + " DMG";
			messageColor = Color.GREEN;

			impactTriggered = true;

			if (isWordCompleted()) { // If the word is finished
				if (currentEnemy.isAlive()) {
					message = "Word Cleared! New Word!";
					messageColor = Color.CYAN;
					generateNewWord(playerUser.getWorldLevel(), playerUser.getProgressLevel()); // Get a new word
				}
			}

		} else {
			isPlayerAttacking = false;

			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);

			int dmg = currentEnemy.getAttackPower();
			playerPet.takeDamage(dmg); // Pet takes damage for wrong guess

			// Set damage indicator for player
			damageIndicatorPlayer = -dmg;
			damageIndicatorEnemy = 0;

			message = "Wrong! Ouch! " + damageIndicatorPlayer + " HP";
			messageColor = Color.RED;

			impactTriggered = true;
		}
	}

	// Handles input on the final win/loss screen
	private void handleMenuInput(int keyCode) {
		if (playerWon) {
			if (keyCode == KeyEvent.VK_ENTER) {
				initBattle(); // Start the next fight
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

	// This function draws everything on the screen every frame
	public void render(Graphics2D g, int width, int height, ImageObserver observer) {

		// Screen shake applies to the entire scene
		// This is the shake for the background and sprites (from damage/all-in mode)
		int globalShakeX = screenShakeX;
		int globalShakeY = screenShakeY;
		
		// Apply All-In Shake if active (this will be used by non-modal elements and background)
		if (isAllInShaking && !isAllInGuessOpen) {
		    globalShakeX = screenShakeX;
		    globalShakeY = screenShakeY;
		} else if (isAllInGuessOpen && isAllInShaking) {
		    // If the modal is open, we reset the global shake here so the main content doesn't shake, 
		    // but we let the shake variables (screenShakeX/Y) hold the value for use in drawAllInModal
		    globalShakeX = 0; 
		    globalShakeY = 0;
		}

		// *** NEW: Calculate combined UI shake offset (global shake + time panic shake) ***
		// timeShakeX/Y is non-zero only during time panic in normal mode.
		// In All-In mode, we rely on globalShakeX/Y (screenShakeX/Y) for the shake, and timeShakeX/Y is 0.
		int uiShakeX = globalShakeX + timeShakeX; 
		int uiShakeY = globalShakeY + timeShakeY;
		// END NEW CALCULATION

		// Sprite shake only applies to the damaged character sprite
		int spriteTargetShakeX = isPlayerDamaged ? spriteShakeX : isEnemyDamaged ? spriteShakeX : 0;
		int spriteTargetShakeY = isPlayerDamaged ? spriteShakeY : isEnemyDamaged ? spriteShakeY : 0;

		// UI shake only applies to the timer
		// The old uiOffsetX/Y is now obsolete as we use uiShakeX/Y everywhere.
		// int uiOffsetX = timeShakeX;
		// int uiOffsetY = timeShakeY;

		long timeElapsedDamage = System.currentTimeMillis() - damageStartTime;
		int currentDamagePulseAlpha = 0;
		if (isPlayerDamaged || isEnemyDamaged) {
			// Calculates the transparency for the red damage flash effect
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

		// Draw Rabbit Flash (Only used for standard panic, NOT all-in mode anymore)
		if (!isAllInGuessOpen && rabbitFlashStartTime > 0 && rabbitImg != null) {
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
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			} else {
				rabbitFlashStartTime = 0; // End animation
			}
		}

		int backdropH = 130;
		// Top backdrop
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		// Global shake to UI elements
		// Apply combined UI shake for all non-sprite UI
		drawLevelIndicator(g, 30 + uiShakeX, TOP_BAR_Y + uiShakeY, observer);
		
		// Instruction Button
		if (instructionBtnBounds != null && nameFrameImg != null) {
			int btnX = instructionBtnBounds.x + uiShakeX; // Use uiShakeX
			int btnY = instructionBtnBounds.y + uiShakeY; // Use uiShakeY
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
		
		// NEW: All-In Guess Button (Apply uiShakeX/Y)
		if (allInButtonBounds != null && nameFrameImg != null) {
			int btnX = allInButtonBounds.x + uiShakeX; // Use uiShakeX
			int btnY = allInButtonBounds.y + uiShakeY; // Use uiShakeY
			int btnW = allInButtonBounds.width;
			int btnH = allInButtonBounds.height;
			
			// Draw the button frame
			g.drawImage(nameFrameImg, btnX, btnY, btnW, btnH, observer);

			// Draw the text
			g.setFont(GameConstants.UI_FONT); // Use UI_FONT for size consistency
			
			String allInText = "ALL IN!"; // Updated label
			
			if (isAllInHovered) {
				g.setColor(GameConstants.SELECTION_COLOR);
				allInText = "> " + allInText + " <";
			} else {
				g.setColor(Color.BLACK);
			}

			FontMetrics fm = g.getFontMetrics();
			int textW = fm.stringWidth(allInText);
			int textX = btnX + (btnW - textW) / 2;
			int textY = btnY + (btnH - fm.getAscent()) / 2 + fm.getAscent() - 7;
			g.drawString(allInText, textX, textY);
		}


		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x + uiShakeX, settingsBtnBounds.y + uiShakeY, // Use uiShakeX/Y
					settingsBtnBounds.width, settingsBtnBounds.height, observer);
		}

		drawWordPuzzle(g, width, height, observer, uiShakeX, uiShakeY); // Use uiShakeX/Y

		// Timer gets the time-panic shake internally on top of external shake
		drawTimer(g, observer, uiShakeX, uiShakeY); // Use uiShakeX/Y

		int framesTopY = backdropH + 10;
		if (!message.isEmpty()) {
			g.setFont(GameConstants.UI_FONT);
			FontMetrics fm = g.getFontMetrics();
			int msgW = fm.stringWidth(message);

			int bgW = msgW + 60;
			int bgH = 50;
			int bgX = (width - bgW) / 2 + uiShakeX; // Use uiShakeX
			int bgY = framesTopY + uiShakeY; // Use uiShakeY

			if (nameFrameImg != null) {
				g.drawImage(nameFrameImg, bgX, bgY, bgW, bgH, observer);
			}

			g.setColor(messageColor);
			int textX = (width - msgW) / 2 + uiShakeX; // Use uiShakeX
			int textY = bgY + (bgH - fm.getAscent()) / 2 + fm.getAscent() - 7;
			g.drawString(message, textX, textY);
		}

		// Character UI Frames
		drawCharacterUIFrames1(g, width, statsUiY + uiShakeY, PLAYER_HOME_X + uiShakeX, // Use uiShakeX/Y
				ENEMY_HOME_X + uiShakeX, observer);

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
			drawSettingsModal(g, width, height); // Draw the pause screen
		} else if (isInstructionOpen) {
			drawInstructionModal(g, width, height); // Draw the how-to-play screen
		} else if (isAllInGuessOpen) { // NEW: Draw All-In Modal
			// Pass screenShakeX/Y (which are non-zero when isAllInShaking) to draw modal with shake
			drawAllInGuessModal(g, width, height, screenShakeX, screenShakeY, observer);
		} else if (battleOver) {
			drawEndScreen(g, width, height); // Draw the win or loss screen
		} else if (isRewardAnimating) {
			drawExpAnimation(g, width, height, observer, globalShakeX); // Draw the EXP gain animation
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

		int indicatorY = (int) (startY - (expIndicatorTravel * progress)); // Moves the number up
		float alpha = 1.0f - progress; // Fades the number out

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

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); 
	}
	
	// NEW: Draw All-In Guess Modal
	// shakeX/shakeY parameters are now the global shake offsets (screenShakeX/Y)
	private void drawAllInGuessModal(Graphics2D g, int width, int height, int shakeX, int shakeY, ImageObserver obs) {
		
		// 1. Full Screen Black Overlay (95% opacity)
		g.setColor(new Color(0, 0, 0, 242)); // 242/255 approx 95% opacity
		g.fillRect(0, 0, width, height);

		// 1.5. Rabbit Flash (behind modal frames)
		if (rabbitFlashStartTime > 0 && rabbitImg != null) {
			long timeElapsed = System.currentTimeMillis() - rabbitFlashStartTime;
			if (timeElapsed < RABBIT_FLASH_DURATION) {

				float alpha = 1.0f - (float) timeElapsed / RABBIT_FLASH_DURATION;
				alpha = Math.max(0.0f, Math.min(1.0f, alpha));

				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				
				// Draw rabbit image centered, applying the screen shake offsets
				g.drawImage(rabbitImg, shakeX, shakeY, width, height, obs);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			} else {
				rabbitFlashStartTime = 0; // End animation
			}
		}

		// --- All following elements must apply the screen shake (shakeX, shakeY) ---

		// 2. All-In Guess Mode Title (Top Center)
		int titleY = 50;
		g.setColor(Color.WHITE);
		g.setFont(GameConstants.HEADER_FONT);
		String title = "ALL-IN GUESS MODE";
		FontMetrics fm = g.getFontMetrics();
		g.drawString(title, (width - fm.stringWidth(title)) / 2 + shakeX, titleY + shakeY);

		// 3. Clue (Same position as standard game clue)
		int clueBarY = TOP_BAR_Y;
		int clueBarH = TOP_BAR_HEIGHT;
		int clueBarW = 600;
		int clueBarX = (width - clueBarW) / 2;

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, clueBarX + shakeX, clueBarY + shakeY, clueBarW, clueBarH, obs);
		}

		g.setFont(GameConstants.UI_FONT);
		g.setColor(Color.WHITE);
		String clueText = "CLUE: " + clue;
		FontMetrics fmClue = g.getFontMetrics();
		int clueTextX = clueBarX + (clueBarW - fmClue.stringWidth(clueText)) / 2 + shakeX;
		int clueTextY = clueBarY + (clueBarH - fmClue.getAscent()) / 2 + fmClue.getAscent() - 5 + shakeY;
		g.drawString(clueText, clueTextX, clueTextY);

		// 3.5. Message/Alert Frame (Positioned like the standard battle message frame)
		int framesTopY = 130 + 10; // backdropH + 10
		if (!message.isEmpty()) {
		    g.setFont(GameConstants.UI_FONT);
		    FontMetrics fmMsg = g.getFontMetrics();
		    int msgW = fmMsg.stringWidth(message);

		    int bgW = msgW + 60;
		    int bgH = 50;
		    int bgX = (width - bgW) / 2;
		    int bgY = framesTopY; 

		    if (nameFrameImg != null) {
		        g.drawImage(nameFrameImg, bgX + shakeX, bgY + shakeY, bgW, bgH, obs);
		    }

		    // Use Black text since the background is a light frame image
		    g.setColor(Color.BLACK); 
		    int textX = (width - msgW) / 2 + shakeX;
		    int textY = bgY + (bgH - fmMsg.getAscent()) / 2 + fmMsg.getAscent() - 7 + shakeY;
		    g.drawString(message, textX, textY);
		}


		// 4. Word Puzzle Display / Input Area (Centered Middle)
		int spacing = 60;
		int lettersHeightCenter = height / 2; // Vertical center of the screen
		
		int totalWordLength = secretWord.length();
		int totalWordDisplayWidth = totalWordLength * spacing;

		int startX = (width - totalWordDisplayWidth) / 2;
		int currentX = startX;
		
		int letterImageSize = 40;
		int letterImageOffset = 10; // offset for centering the 40px image on the 60px grid
		
		int wordIndex = 0; // Tracks the index of the non-space characters

		// Calculate shifting offset for the dashes
		int dashShiftPixel = (SHIFT_SPACING / NUM_DASH_STATES) * currentDashOffset;
		
		
		for (char c : secretWord.toCharArray()) {
			if (c == ' ') {
				currentX += spacing;
				continue;
			}
			
			// --- SHIFTING DASH EFFECT ---
			int baseLetterY = lettersHeightCenter; 
			int finalLetterX = currentX;         
			
			// Determine dash color based on the current shifting state
			boolean isDashActive = (wordIndex % DASH_SIZE) == currentDashOffset; 
			
			// 4a. Draw the underlying blank underscore / wave line
			g.setColor(isDashActive ? Color.RED : Color.WHITE);
			
			// Apply dash shift effect: Draw the underline shifted to the right 
			g.fillRect(finalLetterX + letterImageOffset + shakeX, baseLetterY + 5 + shakeY, letterImageSize, 4); 

			// 4b. Draw the letter (if revealed)
			if (guessedLetters.contains(c)) { 
				char charToDisplay = c; 
			
				String letterPath = "images/utilities/letters/" + charToDisplay + ".png";
				Image letterImg = AssetLoader.loadImage(letterPath, letterImageSize, letterImageSize);
				
				if (letterImg != null) {
					// Draw the letter PNG. Apply wave and shake offset
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
					g.drawImage(letterImg, finalLetterX + letterImageOffset + shakeX, baseLetterY - letterImageSize + shakeY, letterImageSize, letterImageSize, obs);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
				} else {
					// Fallback to text if PNG is missing. Apply wave and shake offset
					g.setColor(Color.WHITE);
					g.setFont(GameConstants.HEADER_FONT);
					g.drawString(String.valueOf(charToDisplay), finalLetterX + 10 + shakeX, baseLetterY + shakeY);
				}
			}
			
			// Increment word index only for non-space characters
			wordIndex++;
			currentX += spacing;
		}


		// 5. Timer Display (Same position as standard game timer: Bottom Center)
		// We pass shakeX/shakeY to the drawing logic now.
		drawTimer(g, obs, shakeX, shakeY); 

		// 6. Instruction Hint (Bottom of screen)
		g.setFont(GameConstants.INSTRUCTION_FONT);
		g.setColor(Color.WHITE);
		String hint = "Type a single correct letter to win! Incorrect letter or time-out results in instant loss.";
		FontMetrics fmHint = g.getFontMetrics();
		g.drawString(hint, (width - fmHint.stringWidth(hint)) / 2 + shakeX, height - 10 + shakeY);
		
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

		// Custom logic to draw text with wrapping
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
			} else if (trimmedLine.startsWith("Type")) {
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
			} else {
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

		int verticalOffset = (int) (progress * DAMAGE_INDICATOR_VERTICAL_TRAVEL); // Moves up
		float alpha = 1.0f - progress; // Fades out

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

	// Helper function to draw the player sprite, applying shake only if damaged
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

			if (isDamaged) { // Adds shake offset if the character just took a hit
				drawX += targetShakeX;
				drawY += targetShakeY;
			}

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.drawImage(playerImg, drawX, drawY, drawW, drawH, observer);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
		}
	}

	// Helper function to draw the enemy sprite, applying shake only if damaged
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

			if (isDamaged) { // Adds shake offset if the character just took a hit
				drawX += targetShakeX;
				drawY += targetShakeY;
			}

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.drawImage(enemyImg, drawX, drawY, drawW, drawH, observer);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset alpha
		}
	}

	// Helper function to draw the UI frames and content for both characters
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

	// Draws the hidden word puzzle display
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

			if (guessedLetters.contains(c)) { // Checks if the letter is in the set
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
				g.fillRect(currentX, lettersY + 5, 40, 4); // Draws the blank underscore
			}
			currentX += spacing;
		}
	}

	// Draws the settings menu screen
	private void drawSettingsModal(Graphics2D g, int width, int height) {
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

		g.setColor(Color.BLACK);
		g.setFont(GameConstants.HEADER_FONT);
		String title = "PAUSED";
		// The Y position here should be constant and not affected by global shake
		g.drawString(title, mX + (mW - g.getFontMetrics().stringWidth(title)) / 2, mY + 50);

		g.setFont(GameConstants.BUTTON_FONT);
		FontMetrics fm = g.getFontMetrics();
		int btnH = 30;
		int btnGap = 15;
		int startBtnY = mY + 150; // Adjusted start Y position inside modal for better spacing

		// Continue
		String contTxt = "CONTINUE";
		if (selectedModalOption == 0) {
			g.setColor(GameConstants.SELECTION_COLOR); // Use correct yellow selection color
			contTxt = "> " + contTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int contW = fm.stringWidth(contTxt);
		modalContinueBounds = new Rectangle(mX + (mW - contW) / 2 - 10, startBtnY, contW + 20, btnH);
		g.drawString(contTxt, mX + (mW - fm.stringWidth(contTxt)) / 2, startBtnY + 25);

		// Save Game
		int nextY = startBtnY + btnH + btnGap;
		String saveTxt = "SAVE GAME";
		if (selectedModalOption == 1) {
			g.setColor(GameConstants.SELECTION_COLOR);
			saveTxt = "> " + saveTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int saveW = fm.stringWidth(saveTxt);
		modalSaveBounds = new Rectangle(mX + (mW - saveW) / 2 - 10, nextY, saveW + 20, btnH);
		g.drawString(saveTxt, mX + (mW - fm.stringWidth(saveTxt)) / 2, nextY + 25);

		// Main Menu
		nextY += btnH + btnGap;
		String menuTxt = "MAIN MENU";
		if (isMenuConfirmation)
			menuTxt = "CONFIRM?";

		if (selectedModalOption == 2) {
			g.setColor(isMenuConfirmation ? Color.RED : GameConstants.SELECTION_COLOR);
			menuTxt = "> " + menuTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}
		int menuW = fm.stringWidth(menuTxt);
		modalMenuBounds = new Rectangle(mX + (mW - menuW) / 2 - 10, nextY, menuW + 20, btnH);
		g.drawString(menuTxt, mX + (mW - fm.stringWidth(menuTxt)) / 2, nextY + 25);

		// Exit Game
		nextY += btnH + btnGap;
		String exitTxt = "EXIT GAME";
		if (isExitConfirmation)
			exitTxt = "CONFIRM?";

		if (selectedModalOption == 3) {
			g.setColor(isExitConfirmation ? Color.RED : GameConstants.SELECTION_COLOR);
			exitTxt = "> " + exitTxt + " <";
		} else {
			g.setColor(Color.BLACK);
		}

		int exitW = fm.stringWidth(exitTxt);
		modalExitBounds = new Rectangle(mX + (mW - exitW) / 2 - 10, nextY, exitW + 20, btnH);
		g.drawString(exitTxt, mX + (mW - fm.stringWidth(exitTxt)) / 2, nextY + 25);

		if (isExitConfirmation || isMenuConfirmation) {
			g.setFont(new Font("Monospaced", Font.PLAIN, 12));
			g.setColor(Color.RED);
			String warn = "Unsaved progress will be lost!";
			g.drawString(warn, mX + (mW - g.getFontMetrics().stringWidth(warn)) / 2, mY + mH - 35);
		}
	}

	// Draws the screen that appears after a win or loss
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

	// Draws the time limit display
	private void drawTimer(Graphics2D g, ImageObserver observer, int externalOffsetX, int externalOffsetY) {

		// Timer gets the combined UI shake from render()
		int finalOffsetX = timerX + externalOffsetX; // <-- REMOVED + timeShakeX
		int finalOffsetY = timerY + externalOffsetY; // <-- REMOVED + timeShakeY

		if (nameFrameImg != null) {
			g.drawImage(nameFrameImg, finalOffsetX, finalOffsetY, timerW, timerH, observer);
		}

		// Draw Panic Flash Effect
		if (panicAlpha > 0) {
			Color panicColor = new Color(GameConstants.TIMER_PANIC_COLOR.getRed(),
					GameConstants.TIMER_PANIC_COLOR.getGreen(), GameConstants.TIMER_PANIC_COLOR.getBlue(), panicAlpha);
			g.setColor(panicColor);
			g.fillRect(finalOffsetX + 2, finalOffsetY + 2, timerW - 4, timerH - 4);
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
		int textX = finalOffsetX + (timerW - fm.stringWidth(timerText)) / 2;
		int textY = finalOffsetY + (timerH - fm.getAscent()) / 2 + fm.getAscent() - 5;

		g.drawString(timerText, textX, textY);
	}
}