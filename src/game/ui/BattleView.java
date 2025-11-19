package game.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import game.GameConstants;
import main.Main;
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
	private Set<Character> guessedLetters;
	private boolean battleOver = false;
	private boolean playerWon = false;
	private boolean exitRequested = false;
	
	private boolean isBossFight = false;

	// Rewards
	private int goldReward = 0;
	private boolean rewardsClaimed = false;

	private String message = "";

	// Animation Timers
	private long actionStartTime = 0;
	private boolean isAnimatingAction = false;
	private final int ANIMATION_DURATION = 2500;
	
	// Death Animation Timers
	private boolean isAnimatingDeath = false;
	private long deathStartTime = 0;
	private final int DEATH_DURATION = 2500; // 1.5 seconds for death animation

	// Assets
	private Image bgImage;
	private Random random = new Random();

	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();

		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		initBattle();
	}

	private void loadNewPuzzle() {
		this.guessedLetters.clear();
		WordBank.WordData data = WordBank.getRandomWord(playerUser.getWorldLevel());
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;
	}

	private void initBattle() {
		// Reset Round State
		this.battleOver = false;
		this.playerWon = false;
		this.rewardsClaimed = false;
		this.isAnimatingAction = false;
		this.isAnimatingDeath = false;
		this.message = "";
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);
		
		loadNewPuzzle();

		// Boss Logic
		this.isBossFight = (playerUser.getProgressLevel() % 5 == 0);

		// --- 1. Background ---
		String bgPath;
		if (isBossFight) {
			int bgNum = random.nextInt(2) + 1; 
			bgPath = GameConstants.BG_DIR + "battle_bg/boss_bg" + bgNum + ".gif";
		} else {
			int bgNum = random.nextInt(19) + 1;
			bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
		}
		this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

		// --- 2. Enemy Spawning ---
		int baseHp = 50 + (playerUser.getWorldLevel() * 15);
		int baseAtk = 5 + (playerUser.getWorldLevel() * 2);
		
		String enemyFolder = "worm";
		String enemyName = "Enemy";
		String scanPath;

		if (isBossFight) {
			scanPath = "images/enemies/boss/";
			baseHp *= 3;   
			baseAtk = (int)(baseAtk * 1.5);
			enemyName = "BOSS";
		} else {
			scanPath = "images/enemies/enemies/";
			enemyName = "Monster";
		}
		
		File directory = new File(scanPath);
		if (directory.exists() && directory.isDirectory()) {
			File[] subFolders = directory.listFiles(File::isDirectory);
			
			if (subFolders != null && subFolders.length > 0) {
				File selected = subFolders[random.nextInt(subFolders.length)];
				enemyFolder = selected.getName();
				
				String rawName = selected.getName();
				enemyName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);
				
				if (isBossFight) {
					enemyName = "Dark " + enemyName;
				}
			}
		}

		String fullAssetPath = (isBossFight ? "enemies/boss/" : "enemies/enemies/") + enemyFolder;

		this.currentEnemy = new Enemy(enemyName, baseHp, playerUser.getWorldLevel(), baseAtk, fullAssetPath);
		System.out.println("[Battle] Level " + playerUser.getProgressLevel() + " | Spawning: " + enemyName);
	}

	public void update() {
		// 1. Handle Action Animation (Attack/Damage)
		if (isAnimatingAction) {
			if (System.currentTimeMillis() - actionStartTime > ANIMATION_DURATION) {
				isAnimatingAction = false;
				
				// Check for Deaths FIRST
				if (!currentEnemy.isAlive() || !playerPet.isAlive()) {
					startDeathSequence();
					return; // Exit immediately, don't load new puzzle
				}

				// If both alive, reset to IDLE
				playerPet.setAnimationState(Hangpie.AnimState.IDLE);
				currentEnemy.setAnimationState(Enemy.AnimState.IDLE);

				// If enemy alive but word done, Next Puzzle
				if (checkWinCondition()) {
					message = "Word Complete! Next Puzzle...";
					loadNewPuzzle();
				}
			}
		}
		
		// 2. Handle Death Animation (Wait for it to finish before showing menu)
		if (isAnimatingDeath) {
			if (System.currentTimeMillis() - deathStartTime > DEATH_DURATION) {
				isAnimatingDeath = false;
				
				// Now trigger the actual game over state
				if (!currentEnemy.isAlive()) {
					handleWin();
				} else {
					handleLoss();
				}
			}
		}
	}
	
	private void startDeathSequence() {
		isAnimatingDeath = true;
		deathStartTime = System.currentTimeMillis();
		
		if (!currentEnemy.isAlive()) {
			currentEnemy.setAnimationState(Enemy.AnimState.DEATH);
			playerPet.setAnimationState(Hangpie.AnimState.IDLE); // Player stands victoriously
		} else {
			playerPet.setAnimationState(Hangpie.AnimState.DEATH);
			currentEnemy.setAnimationState(Enemy.AnimState.IDLE); // Enemy stands victoriously
		}
	}

	private void handleWin() {
		battleOver = true;
		playerWon = true;
		// Animation state is already set in startDeathSequence

		if (!rewardsClaimed) {
			int multiplier = isBossFight ? 50 : 10;
			goldReward = 50 + (playerUser.getWorldLevel() * multiplier);
			
			playerUser.addGold(goldReward);
			playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);

			if (isBossFight) {
				playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
			}

			Main.userManager.updateUser(playerUser);
			rewardsClaimed = true;
		}
	}

	private void handleLoss() {
		battleOver = true;
		playerWon = false;
		// Animation state is already set in startDeathSequence
	}

	public void handleKeyPress(int keyCode, char keyChar) {
		// Block inputs during ANY animation or if battle is over
		if (battleOver) {
			handleMenuInput(keyCode);
			return;
		}

		// FIX: Strictly block guessing while animations are playing
		if (isAnimatingAction || isAnimatingDeath) return;

		char guess = Character.toUpperCase(keyChar);
		if (guess < 'A' || guess > 'Z') return;

		if (guessedLetters.contains(guess)) {
			message = "Already guessed " + guess + "!";
			return;
		}

		guessedLetters.add(guess);

		boolean isCorrect = false;
		for (char c : secretWord.toCharArray()) {
			if (c == guess) isCorrect = true;
		}

		actionStartTime = System.currentTimeMillis();
		isAnimatingAction = true;

		if (isCorrect) {
			message = "Correct! Hit!";
			playerPet.setAnimationState(Hangpie.AnimState.ATTACK);
			currentEnemy.setAnimationState(Enemy.AnimState.DAMAGE);
			currentEnemy.takeDamage(playerPet.getAttackPower());
		} else {
			message = "Wrong! Ouch!";
			currentEnemy.setAnimationState(Enemy.AnimState.ATTACK);
			playerPet.setAnimationState(Hangpie.AnimState.DAMAGE);
			playerPet.takeDamage(currentEnemy.getAttackPower());
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

	private boolean checkWinCondition() {
		for (char c : secretWord.toCharArray()) {
			if (c == ' ') continue;
			if (!guessedLetters.contains(c)) return false;
		}
		return true;
	}

	public boolean isExitRequested() {
		return exitRequested;
	}

	public void render(Graphics2D g, int width, int height, ImageObserver observer) {
		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}

		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, width, 180);
		
		// --- LEVEL DISPLAY ---
		g.setFont(GameConstants.HEADER_FONT);
		g.setColor(Color.WHITE);
		String levelStr = "LEVEL " + playerUser.getProgressLevel();
		int lvlW = g.getFontMetrics().stringWidth(levelStr);
		g.drawString(levelStr, (width - lvlW) / 2, 50);

		// --- BOSS INDICATOR ---
		if (isBossFight) {
			g.setColor(new Color(100, 0, 0, 100)); // Red tint
			g.fillRect(0, 0, width, 180);
			
			g.setColor(Color.WHITE);
			g.drawString(levelStr, (width - lvlW) / 2, 50);
			
			g.setFont(GameConstants.UI_FONT);
			g.setColor(Color.RED);
			String bossText = "- BOSS BATTLE -";
			g.drawString(bossText, (width - g.getFontMetrics().stringWidth(bossText))/2, 85);
		}

		// --- HUD STATS ---
		g.setColor(Color.WHITE);
		g.setFont(GameConstants.UI_FONT);

		g.setColor(Color.GREEN);
		g.drawString(playerPet.getName() + " HP: " + playerPet.getCurrentHealth() + "/" + playerPet.getMaxHealth(), 50, 50);

		g.setColor(Color.RED);
		String enemyText = currentEnemy.getName() + " HP: " + currentEnemy.getCurrentHealth() + "/" + currentEnemy.getMaxHealth();
		int enemyTextW = g.getFontMetrics().stringWidth(enemyText);
		g.drawString(enemyText, width - enemyTextW - 50, 50);

		if (!message.isEmpty()) {
			g.setColor(Color.YELLOW);
			int msgW = g.getFontMetrics().stringWidth(message);
			g.drawString(message, (width - msgW) / 2, 210);
		}

		// --- CHARACTERS ---
		int charSize = 320;
		int floorY = height - 380;

		int enemyX = width - charSize - 100;
		Image enemyImg = currentEnemy.getCurrentImage();
		
		int renderSize = isBossFight ? 500 : charSize;
		int renderY = isBossFight ? floorY - 180 : floorY;
		int renderX = isBossFight ? enemyX - 130 : enemyX;
		
		if (enemyImg != null) {
			g.drawImage(enemyImg, renderX, renderY, renderSize, renderSize, observer);
		} else {
			g.setColor(Color.RED);
			g.fillRect(renderX, renderY, 100, 100);
			g.setColor(Color.WHITE);
			g.drawString("Enemy Missing", renderX, renderY);
		}

		int playerX = 100;
		Image playerImg = playerPet.getCurrentImage();
		if (playerImg != null) {
			g.drawImage(playerImg, playerX, floorY, charSize, charSize, observer);
		}

		drawWordPuzzle(g, width, height, observer);

		if (battleOver) {
			drawEndScreen(g, width, height);
		}
	}

	private void drawWordPuzzle(Graphics2D g, int width, int height, ImageObserver obs) {
		int spacing = 60;
		int clueY = 110; 
		if (isBossFight) clueY = 130; 
		int lettersY = 160;

		g.setColor(Color.CYAN);
		g.setFont(GameConstants.SUBTITLE_FONT);
		String clueText = "CLUE: " + clue;
		int clueW = g.getFontMetrics().stringWidth(clueText);
		g.drawString(clueText, (width - clueW) / 2, clueY);

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

	private void drawEndScreen(Graphics2D g, int width, int height) {
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRect(0, 0, width, height);

		g.setFont(GameConstants.HEADER_FONT);
		FontMetrics fm = g.getFontMetrics();

		if (playerWon) {
			g.setColor(Color.GREEN);
			String title = isBossFight ? "BOSS DEFEATED!" : "VICTORY!";
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