package game.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import game.GameConstants;
import main.Main; // Needed to access global managers for saving import models.Enemy; import models.Hangpie; import models.User; import utils.AssetLoader; import utils.WordBank;
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
	private boolean exitRequested = false; // Signal to GameWindow

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
	private Random random = new Random();

	public BattleView(User user, Hangpie pet) {
		this.playerUser = user;
		this.playerPet = pet;
		this.guessedLetters = new HashSet<>();

		// Ensure full health at start
		this.playerPet.setCurrentHealth(this.playerPet.getMaxHealth());
		// Reset animation state
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		initBattle();
	}

	private void initBattle() {
		// Reset Round State
		this.guessedLetters.clear();
		this.battleOver = false;
		this.playerWon = false;
		this.rewardsClaimed = false;
		this.message = "";
		this.playerPet.setAnimationState(Hangpie.AnimState.IDLE);

		// 1. Pick Word
		WordBank.WordData data = WordBank.getRandomWord(playerUser.getWorldLevel());
		this.secretWord = data.word.toUpperCase();
		this.clue = data.clue;

		// 2. Pick Random Background
		int bgNum = random.nextInt(19) + 1;
		String bgPath = GameConstants.BG_DIR + "battle_bg/bg" + bgNum + ".gif";
		this.bgImage = AssetLoader.loadImage(bgPath, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

		// 3. Spawn Enemy
		// Difficulty Scaling: Base + (World Level * Multiplier)
		int hp = 50 + (playerUser.getWorldLevel() * 15);
		int atk = 5 + (playerUser.getWorldLevel() * 2);

		// FIX: Updated path to match the file structure (double enemies folder based on
		// logs)
		this.currentEnemy = new Enemy("Skeleton Warrior", hp, playerUser.getWorldLevel(), atk,
				"enemies/enemies/skeleton");
	}

	public void update() {
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
			// Calculate Rewards
			goldReward = 50 + (playerUser.getWorldLevel() * 10);

			// Apply to User
			playerUser.addGold(goldReward);

			// Simple progression: Every win increases progress level
			playerUser.setProgressLevel(playerUser.getProgressLevel() + 1);

			// Optional: Increase World Level every 5 wins
			if (playerUser.getProgressLevel() % 5 == 0) {
				playerUser.setWorldLevel(playerUser.getWorldLevel() + 1);
			}

			// Save Data
			Main.userManager.updateUser(playerUser);
			rewardsClaimed = true;
		}
	}

	private void handleLoss() {
		battleOver = true;
		playerWon = false;
		playerPet.setAnimationState(Hangpie.AnimState.DEATH);
		// No rewards on loss
	}

	public void handleKeyPress(int keyCode, char keyChar) {
		// 1. Menu Input (If battle is over)
		if (battleOver) {
			handleMenuInput(keyCode);
			return;
		}

		// 2. Game Input (If fighting)
		if (isAnimatingAction)
			return; // Block input during animation

		char guess = Character.toUpperCase(keyChar);
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

		// Trigger Animation
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

		// Check instant word completion
		if (checkWinCondition()) {
			// Wait for update() to process the win logic after animation
			// But we can flag state here if needed
		}
	}

	private void handleMenuInput(int keyCode) {
		if (playerWon) {
			// Victory Menu
			if (keyCode == KeyEvent.VK_ENTER) {
				// Proceed / Next Battle
				initBattle();
			} else if (keyCode == KeyEvent.VK_ESCAPE) {
				// Return to Main Menu
				exitRequested = true;
			}
		} else {
			// Defeat Menu
			if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE) {
				exitRequested = true;
			}
		}
	}

	private boolean checkWinCondition() {
		for (char c : secretWord.toCharArray()) {
			if (!guessedLetters.contains(c))
				return false;
		}
		return true;
	}

	public boolean isExitRequested() {
		return exitRequested;
	}

	public void render(Graphics2D g, int width, int height, ImageObserver observer) {
		// 1. Background
		if (bgImage != null) {
			g.drawImage(bgImage, 0, 0, width, height, observer);
		}

		// 2. UI Bars
		g.setColor(new Color(0, 0, 0, 150));

		// Top Bar (Health Bars & Puzzle Background)
		// Increased height to accommodate the puzzle being moved up
		g.fillRect(0, 0, width, 180);

		// Bottom Bar REMOVED as requested

		// 3. HUD Text
		g.setColor(Color.WHITE);
		g.setFont(GameConstants.UI_FONT);

		// Player HP
		g.setColor(Color.GREEN);
		g.drawString(playerPet.getName() + " HP: " + playerPet.getCurrentHealth() + "/" + playerPet.getMaxHealth(), 50,
				50);

		// Enemy HP
		g.setColor(Color.RED);
		String enemyText = currentEnemy.getName() + " HP: " + currentEnemy.getCurrentHealth() + "/"
				+ currentEnemy.getMaxHealth();
		int enemyTextW = g.getFontMetrics().stringWidth(enemyText);
		g.drawString(enemyText, width - enemyTextW - 50, 50);

		// Message Center (Moved slightly down to not overlap with puzzle)
		if (!message.isEmpty()) {
			g.setColor(Color.YELLOW);
			int msgW = g.getFontMetrics().stringWidth(message);
			g.drawString(message, (width - msgW) / 2, 210);
		}

		// 4. Draw Characters
		// Y Position: Kept as height - 380 based on your feedback that the Hangpie
		// position was good.
		int charSize = 320;
		int floorY = height - 380;

		// Enemy (Right)
		int enemyX = width - charSize - 100;
		Image enemyImg = currentEnemy.getCurrentImage();
		if (enemyImg != null) {
			g.drawImage(enemyImg, enemyX, floorY, charSize, charSize, observer);
		} else {
			// Debug placeholder if image fails
			g.setColor(Color.RED);
			g.fillRect(enemyX, floorY, 100, 100);
			g.setColor(Color.WHITE);
			g.drawString("Enemy Missing", enemyX, floorY);
		}

		// Player (Left)
		int playerX = 100;
		Image playerImg = playerPet.getCurrentImage();
		if (playerImg != null) {
			g.drawImage(playerImg, playerX, floorY, charSize, charSize, observer);
		}

		// 5. Word Puzzle (Moved to TOP)
		drawWordPuzzle(g, width, height, observer);

		// 6. End Game Overlay
		if (battleOver) {
			drawEndScreen(g, width, height);
		}
	}

	private void drawWordPuzzle(Graphics2D g, int width, int height, ImageObserver obs) {
		int spacing = 60;

		// Move to TOP of screen
		int clueY = 100;
		int lettersY = 150;

		// Clue
		g.setColor(Color.CYAN);
		g.setFont(GameConstants.SUBTITLE_FONT);
		String clueText = "CLUE: " + clue;
		// Centering the clue
		int clueW = g.getFontMetrics().stringWidth(clueText);
		g.drawString(clueText, (width - clueW) / 2, clueY);

		// Calculate centered position for letters
		int totalWidth = secretWord.length() * spacing;
		int startX = (width - totalWidth) / 2;

		int currentX = startX;

		for (char c : secretWord.toCharArray()) {
			if (guessedLetters.contains(c)) {
				// Draw Letter Asset
				String letterPath = "images/utilities/letters/" + c + ".png";
				Image letterImg = AssetLoader.loadImage(letterPath, 40, 40);
				if (letterImg != null) {
					g.drawImage(letterImg, currentX, lettersY - 30, 40, 40, obs);
				} else {
					g.setColor(Color.WHITE);
					g.drawString(String.valueOf(c), currentX + 10, lettersY);
				}
			} else {
				// Draw Underscore
				g.setColor(Color.WHITE);
				g.fillRect(currentX, lettersY + 5, 40, 4);
			}
			currentX += spacing;
		}
	}

	private void drawEndScreen(Graphics2D g, int width, int height) {
		// Dim background
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