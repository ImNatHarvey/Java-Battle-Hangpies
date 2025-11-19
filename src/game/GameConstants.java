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
    
    // Fonts
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 28);
    public static final Font UI_FONT = new Font("Arial", Font.PLAIN, 16);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    
    // Paths
    public static final String ASSET_DIR = "images/";
    public static final String HANGPIE_DIR = ASSET_DIR + "hangpies/";
    public static final String ENEMY_DIR = ASSET_DIR + "enemies/";
    public static final String BG_DIR = ASSET_DIR + "bg/";
    
    // Backgrounds
    public static final String MAIN_BG = "game_interface.gif"; // The GIF you requested
}