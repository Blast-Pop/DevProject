package main;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LogParser {

    public static List<Point> parseLog(InputStream inputStream) {
        List<Point> movementData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && !line.contains("Clicked")) {
                    try {
                        int x = Integer.parseInt(parts[1].trim());
                        int y = Integer.parseInt(parts[2].trim());
                        movementData.add(new Point(x, y));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return movementData;
    }
}
