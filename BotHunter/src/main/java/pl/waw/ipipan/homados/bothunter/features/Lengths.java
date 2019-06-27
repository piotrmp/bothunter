package pl.waw.ipipan.homados.bothunter.features;

import pl.waw.ipipan.homados.bothunter.Tweet;

public class Lengths implements Feature {

	public String[] names() {
		return new String[] {"lengthCharacters","lengthTokens"};
	}

	public String[] values(Tweet tweet) {
		return new String[] {""+tweet.getContent().length(),""+tweet.getTokens().length};
	}

	public void preview(Tweet tweet) {
	}

}
