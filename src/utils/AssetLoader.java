package utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class AssetLoader {

    public static Image loadImage(String path, int width, int height) {
        // Special handling for GIFs to preserve animation
        if (path.toLowerCase().endsWith(".gif")) {
            Image gif = Toolkit.getDefaultToolkit().createImage(path);
            // Scaling GIFs in AWT can be tricky; we return it as-is 
            // and let drawImage handle the scaling in the paint method.
            return gif; 
        }

        try {
            File file = new File(path);
            if (file.exists()) {
                BufferedImage original = ImageIO.read(file);
                // Scale static images immediately for better performance
                return original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            } else {
                System.err.println("Asset not found: " + path);
                return generatePlaceholder(width, height, Color.GRAY, "Missing: " + file.getName());
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + path);
            return generatePlaceholder(width, height, Color.RED, "Error");
        }
    }

    private static Image generatePlaceholder(int width, int height, Color color, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.drawString(text, 10, height / 2);
        g2d.dispose();
        return image;
    }
}