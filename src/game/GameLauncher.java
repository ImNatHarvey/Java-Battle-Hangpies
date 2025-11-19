package game;

import models.User;
import game.ui.GameWindow;

public class GameLauncher {

    public static void launchGame(User user) {
        if (user == null) {
            System.out.println("Error: No user logged in.");
            return;
        }

        // Check if user has a Hangpie to battle with
        if (user.getInventory().isEmpty()) {
            System.out.println("*************************************************");
            System.out.println("  You cannot enter the Arena without a Hangpie!  ");
            System.out.println("  Please visit the Shop or Marketplace first.    ");
            System.out.println("*************************************************");
            return;
        }

        System.out.println("Launching Battle Arena...");
        
        // Launch AWT Window
        // We run this without blocking the main thread, 
        // but the console menu loop will naturally wait for input.
        new GameWindow(user);
    }
}