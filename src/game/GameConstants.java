package game;

import java.awt.Color;
import java.awt.Font;

public class GameConstants {
    // Resolution based on Figma Design (Landscape)
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static final String GAME_TITLE = "Battle Hangpies - Arena";

    // Colors
    public static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    public static final Color ACCENT_COLOR = new Color(231, 76, 60);
    public static final Color BACKGROUND_COLOR = new Color(30, 30, 30); 
    public static final Color TEXT_COLOR = Color.WHITE;
    public static final Color SELECTION_COLOR = Color.YELLOW; 
    
    public static final Font HEADER_FONT = new Font("Monospaced", Font.BOLD, 40);
    public static final Font UI_FONT = new Font("Monospaced", Font.BOLD, 24);
    public static final Font BUTTON_FONT = new Font("Monospaced", Font.BOLD, 20);
    
    // Paths
    public static final String ASSET_DIR = "images/";
    public static final String HANGPIE_DIR = ASSET_DIR + "hangpies/";
    public static final String ENEMY_DIR = ASSET_DIR + "enemies/";
    public static final String BG_DIR = ASSET_DIR + "bg/";
    
    // Assets
    public static final String MAIN_BG = "game_interface.gif"; 
    public static final String TITLE_IMG = "title.png"; 
}