package views;

import static main.Main.currentUser;
import static main.Main.scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLog
{
	public static void showActivityLog()
	{
		System.out.println("\n--- My Activity Log ---");
		System.out.println("[DATE & TIME]\t      [USER]\t[Activity]");
		List<String> userLogs = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader("activity_log.txt")))
		{
			String line;
			
			while ((line = reader.readLine()) != null)
			{
				// We check if the line contains the user's name
				if (line.contains("| " + currentUser.getUsername() + " |"))
				{
					userLogs.add(line);
				}
			}
		}
		
		catch (IOException e)
		{
			System.err.println("Error reading activity log: " + e.getMessage());
		}
		
		if (userLogs.isEmpty())
		{
			System.out.println("You have no activity logged yet.");
		}
		
		else
		{
			// Show the newest logs first
			for (int i = userLogs.size() - 1; i >= 0; i--)
			{
				System.out.println(userLogs.get(i));
			}
		}
		
		System.out.println("\n(Press Enter to go back to the dashboard)");
        scanner.nextLine();
	}
}
