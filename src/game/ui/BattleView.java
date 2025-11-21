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
	private final int DEATH_DURATION = 4000; // 4 Seconds for Death Animation

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

	// UI Buttons (Clickable Areas)
	private Rectangle settingsBtnBounds;
	private Rectangle modalSaveBounds;
	private Rectangle modalMenuBounds;
	private Rectangle modalExitBounds;

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
		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());

		// 1. Word Logic (New vs Carry Over)
		if (!shouldCarryOverWord) {
			generateNewWord();
		} else {
			// If true, we KEEP the secretWord and guessedLetters from the previous round
			System.out.println("[Battle] Carrying over word: " + secretWord);
			shouldCarryOverWord = false; // Reset flag
		}

		// 2. Determine Enemy Logic (Normal vs Boss) & Background
		int currentWorld = playerUser.getWorldLevel();
		int currentProg = playerUser.getProgressLevel();
		
		String enemyName;
		String enemyPath;
		
		// --- SCALING STATS ---
		// Normal: HP = 10 + (World-1)*5  | ATK = World
		// Boss:   HP = Normal + 5        | ATK = Normal + 1
		// Ex: W1 Normal: 10 HP, 1 Atk
		// Ex: W1 Boss:   15 HP, 2 Atk
		
		int baseHp = 10 + ((currentWorld - 1) * 5);
		int baseAtk = currentWorld;
		
		int enemyHp = baseHp;
		int enemyAtk = baseAtk;

		if (currentProg == 5) {
			// --- BOSS FIGHT ---
			
			// Boss Stats
			enemyHp = baseHp + 5;
			enemyAtk = baseAtk + 1;
			
			// Boss Background (Randomized every time you enter)
			int bossBgNum = random.nextInt(2) + 1; // 1 or 2
			String bgPath = GameConstants.BG_DIR + "battle_bg/boss_bg" + bossBgNum + ".gif";
			this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
			
			// Boss Selection (Deterministic per World Level so it doesn't change on retry)
			// We seed with username + world level
			long seed = playerUser.getUsername().hashCode() + currentWorld;
			Random bossRandom = new Random(seed);
			
			// Currently 3 boss types available
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
			
			System.out.println("[Battle] Boss Encounter: " + enemyName + " (HP: " + enemyHp + ", ATK: " + enemyAtk + ")");
			
		} else {
			// --- NORMAL FIGHT ---
			
			// Normal Background (From general pool 1-22)
			int[] validBgIndices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21 };
			int bgIndex = random.nextInt(validBgIndices.length);
			int bgNum = validBgIndices[bgIndex];
			String bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
			this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
			
			String[][] availableEnemies = { 
					{ "Lava Worm", "enemies/enemies/worm" },
					{ "Evil Eye", "enemies/enemies/evil_eye" }, 
					{ "Goblin", "enemies/enemies/goblin" },
					{ "Mushroom", "enemies/enemies/mushroom" }, 
					{ "Skeleton", "enemies/enemies/skeleton" } 
			};

			int idx = random.nextInt(availableEnemies.length);
			enemyName = availableEnemies[idx][0];
			enemyPath = availableEnemies[idx][1];
			
			System.out.println("[Battle] Encounter: " + enemyName + " (HP: " + enemyHp + ", ATK: " + enemyAtk + ")");
		}

		this.currentEnemy = new Enemy(enemyName, enemyHp, currentWorld, enemyAtk, enemyPath);

		// 4. Preload Assets
		this.playerPet.preloadAssets();
		this.currentEnemy.preloadAssets();

		// Initialize button bounds (Top Right)
		settingsBtnBounds = new Rectangle(GameConstants.WINDOW_WIDTH - 80, TOP_BAR_Y, 50, 50);
	}
	
	// Helper to generate a fresh word
	private void generateNewWord() {
		this.guessedLetters.clear();
		WordBank.WordData data = WordBank.getRandomWord(playerUser.getWorldLevel());
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;
		System.out.println("[Battle] New Word Generated: " + secretWord);
	}
	
	// Helper to check if word is fully guessed
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

		// --- 1. Handle Standard Attack/Damage Animation (1.7 Seconds) ---
		if (isAnimatingAction) {
			if (System.currentTimeMillis() - actionStartTime > ACTION_DURATION) {
				isAnimatingAction = false;

				// Check results after animation finishes
				checkRoundResult();
			}
		}
		
		// --- 2. Handle Death Animation (4 Seconds) ---
		if (isDeathAnimating) {
			if (System.currentTimeMillis() - deathStartTime > DEATH_DURATION) {
				isDeathAnimating = false;
				
				// Finalize battle state (Show End Screen)
				if (!currentEnemy.isAlive()) {
					handleWin();
				} else if (!playerPet.isAlive()) {
					handleLoss();
				}
			}
		}
	}

	private void checkRoundResult() {
		// Logic to check if someone died after the attack animation
		if (!currentEnemy.isAlive()) {
			startDeathSequence(currentEnemy);
		} else if (!playerPet.isAlive()) {
			startDeathSequence(playerPet);
		} else {
			// No one died, return to IDLE to allow next guess
			playerPet.setAnimationState(Hangpie.AnimState.IDLE);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE);
		}
	}

	private void startDeathSequence(Character victim) {
		isDeathAnimating = true;
		deathStartTime = System.currentTimeMillis();

		// Set specific death animation
		if (victim instanceof Enemy) {
			((Enemy) victim).setAnimationState(Enemy.AnimState.DEATH);
			playerPet.setAnimationState(Hangpie.AnimState.IDLE); // Winner stands idle
		} else if (victim instanceof Hangpie) {
			((Hangpie) victim).setAnimationState(Hangpie.AnimState.DEATH);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE); // Winner stands idle
		}
	}

	private void handleWin() {
		battleOver = true;
		playerWon = true;
		
		// --- LOGIC TO CARRY OVER WORD ---
		// If we won, but the word isn't finished, we set the flag.
		if (!isWordCompleted()) {
			shouldCarryOverWord = true;
		} else {
			shouldCarryOverWord = false;
		}
		
		if (!rewardsClaimed) {
			// Base Reward logic
			goldReward = 50 + (playerUser.getWorldLevel() * 10);
			
			// Bonus for Boss
			if (playerUser.getProgressLevel() == 5) {
				goldReward *= 2;
			}
			
			playerUser.addGold(goldReward);
			
			// Progression Logic
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
		// Pet is already in DEATH state
	}

	// --- Input Handling ---

	public String handleMouseClick(int x, int y) {
		if (isSettingsOpen) {
			if (modalSaveBounds != null && modalSaveBounds.contains(x, y)) {
				Main.userManager.updateUser(playerUser);
				message = "Game Saved!";
				messageColor = Color.YELLOW;
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

		// Block input if any animation (Action or Death) is playing
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

		// Start Action Animation (1.7 Seconds)
		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;

		if (isCorrect) {
			message = "Correct! Hit!";
			messageColor = Color.GREEN;
			playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
			currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);
			
			// Player damage logic (Attack Value from Pet)
			int dmg = playerPet.getAttackPower();
			currentEnemy.takeDamage(dmg);
			
			// --- NEW: Check for Word Completion IMMEDIATELY ---
			if (isWordCompleted()) {
				// If word is done but enemy is still alive, generate a NEW word
				if (currentEnemy.isAlive()) {
					message = "Word Cleared! New Word!";
					messageColor = Color.CYAN;
					generateNewWord();
				}
				// If enemy is dead, the `checkRoundResult` later will handle the Win.
			}
			
		} else {
			message = "Wrong! Ouch!";
			messageColor = Color.RED;
			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);
			
			// Enemy damage logic (Attack Value from Enemy)
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
		// --- [ADJUSTABLE COORDINATES] ---------------------------------------------
		// 1. Player X Position: Higher value = Moves further right
		int playerCenterX = 280; 
		
		// 2. Enemy X Position: Lower value = Moves further left
		int enemyCenterX = width - 280;

		// 3. Stats/Name UI Y Position: 
		//    140 aligns this with the new backdrop height (130 + 10)
		int statsUiY = 140; 
		// --------------------------------------------------------------------------
		
		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}

		// --- Top Backdrop (HUD Area) ---
		int backdropH = 130;
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, width, backdropH);

		// --- HUD ELEMENTS ---
		
		// 1. Level Indicator (Top Left)
		drawLevelIndicator(g, 30, TOP_BAR_Y, observer);
		
		// 2. Settings Button (Top Right)
		if (settingsImg != null && settingsBtnBounds != null) {
			g.drawImage(settingsImg, settingsBtnBounds.x, settingsBtnBounds.y, settingsBtnBounds.width,
					settingsBtnBounds.height, observer);
		}
		
		// 3. Word Puzzle & Clue (Center)
		drawWordPuzzle(g, width, height, observer);

		// 4. Draw Player and Enemy UI (Stats/Name)
		drawCharacterUI(g, width, statsUiY, playerCenterX, enemyCenterX, observer);

		// 5. Draw Message Indicator (Centered below Clue)
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

		// --- Character Rendering ---
		
		// [ADJUSTED] Lowered characters slightly horizontally (Lower Y = Higher, Higher Y = Lower on screen)
		// Previous was height - 40, now height - 20 pushes them down closer to bottom edge.
		int groundY = height - 20; 
		int scaleFactor = 4;

		// Render Enemy
		Image enemyImg = currentEnemy.getCurrentImage();
		if (enemyImg != null) {
			int eW = enemyImg.getWidth(observer);
			int eH = enemyImg.getHeight(observer);

			if (eW > 0 && eH > 0) {
				int drawW = eW * scaleFactor;
				int drawH = eH * scaleFactor;
				// Calculated based on CENTER position
				int drawX = enemyCenterX - (drawW / 2);
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
				// Calculated based on CENTER position
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

	private void drawCharacterUI(Graphics2D g, int width, int topY, int playerCenterX, int enemyCenterX, ImageObserver observer) {
		int frameW = 220;
		int frameH = 70;
		
		// Align frames so their CENTER matches the Character CENTER (Horizontal Alignment)
		int pFrameX = playerCenterX - (frameW / 2);
		int eFrameX = enemyCenterX - (frameW / 2);

		Font levelFont = new Font("Monospaced", Font.BOLD, 12);
		Font nameFont = new Font("Monospaced", Font.BOLD, 16);

		// --- PLAYER UI ---
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
		// Center the hearts within the frame width
		drawHearts(g, playerPet, pFrameX + 40, heartsY, observer);

		g.setFont(levelFont);
		// Updated to use real stats
		String pStats = "HP " + playerPet.getCurrentHealth() + "/" + playerPet.getMaxHealth() + " | ATK " + playerPet.getAttackPower();
		g.drawString(pStats, pFrameX + (frameW - g.getFontMetrics().stringWidth(pStats)) / 2, heartsY + 30);

		// --- ENEMY UI ---
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

		drawHearts(g, currentEnemy, eFrameX + 40, heartsY, observer);
		
		g.setFont(levelFont);
		String eStats = "HP " + currentEnemy.getCurrentHealth() + "/" + currentEnemy.getMaxHealth() + " | ATK " + currentEnemy.getAttackPower();
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
		// Clue Frame Logic
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

		// Letters
		int spacing = 60;
		// Moved to 110 to center vertically between clue and backdrop bottom
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
		g.drawString(title, mX + (mW - g.getFontMetrics().stringWidth(title)) / 2, mY + 60);

		g.setFont(GameConstants.BUTTON_FONT);
		int btnH = 40;
		int btnGap = 15;
		int startBtnY = mY + 100;

		String saveTxt = "SAVE GAME";
		int saveW = g.getFontMetrics().stringWidth(saveTxt);
		modalSaveBounds = new Rectangle(mX + (mW - saveW) / 2 - 10, startBtnY, saveW + 20, btnH);
		g.drawString(saveTxt, mX + (mW - saveW) / 2, startBtnY + 25);

		String menuTxt = "MAIN MENU";
		int menuW = g.getFontMetrics().stringWidth(menuTxt);
		modalMenuBounds = new Rectangle(mX + (mW - menuW) / 2 - 10, startBtnY + btnH + btnGap, menuW + 20, btnH);
		g.drawString(menuTxt, mX + (mW - menuW) / 2, startBtnY + btnH + btnGap + 25);

		String exitTxt = "EXIT GAME";
		if (isExitConfirmation)
			exitTxt = "CONFIRM EXIT?";
		int exitW = g.getFontMetrics().stringWidth(exitTxt);

		g.setColor(isExitConfirmation ? Color.RED : Color.BLACK);
		modalExitBounds = new Rectangle(mX + (mW - exitW) / 2 - 10, startBtnY + (2 * (btnH + btnGap)), exitW + 20,
				btnH);
		g.drawString(exitTxt, mX + (mW - exitW) / 2, startBtnY + (2 * (btnH + btnGap)) + 25);

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