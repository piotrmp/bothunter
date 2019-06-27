package pl.waw.ipipan.homados.bothunter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class User {
	public List<Tweet> tweets;
	public String id;
	public Boolean isBot;

	public User(File file, Boolean isBot) throws IOException {
		this.isBot = isBot;
		tweets = new LinkedList<Tweet>();
		id = file.getName().split("\\.")[0];
		Path path = Paths.get(file.toString());
		String content = "";
		for (String line : Files.readAllLines(path)) {
			if (!line.trim().equals("<END>"))
				content = content + (content.equals("") ? "" : "\n") + line.trim();
			else {
				tweets.add(new Tweet(content));
				content = "";
			}
		}
	}
}
