package utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AssetLoader {

    private static final Component component = new Component() {};
    private static final MediaTracker tracker = new MediaTracker(component);
    private static int trackerId = 0;
    
    private static final Map<String, Image> imageCache = new HashMap<>();

    public static Image loadImage(String path, int width, int height) {
        String cacheKey = path + "_" + width + "_" + height;
        
        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }
        
        Image image = null;

        try {
            if (path.toLowerCase().endsWith(".gif")) {
                if (imageCache.containsKey(path)) {
                    return imageCache.get(path);
                }
                image = Toolkit.getDefaultToolkit().createImage(path);
                imageCache.put(path, image);
                imageCache.put(cacheKey, image); 
            } else {
                File file = new File(path);
                if (file.exists()) {
                    BufferedImage original = ImageIO.read(file);
                    if (original != null) {
                        image = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                        imageCache.put(cacheKey, image);
                    }
                }
            }

            if (image == null) {
                System.err.println("Asset not found or format unsupported: " + path);
                return generatePlaceholder(width, height, Color.GRAY, "Missing");
            }

            synchronized (tracker) {
                tracker.addImage(image, trackerId);
                try {
                    tracker.waitForID(trackerId);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while loading image: " + path);
                }
                tracker.removeImage(image, trackerId);
                trackerId++;
            }

        } catch (IOException e) {
            System.err.println("Error loading image: " + path);
            return generatePlaceholder(width, height, Color.RED, "Error");
        }

        return image;
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
