package pl.waw.ipipan.homados.bothunter.features;

import pl.waw.ipipan.homados.bothunter.Tweet;

public interface Feature {
	public void preview(Tweet tweet);
	public String[] names();
	public String[] values(Tweet tweet);
}
