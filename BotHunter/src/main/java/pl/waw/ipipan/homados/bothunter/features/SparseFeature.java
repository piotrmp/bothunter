package pl.waw.ipipan.homados.bothunter.features;
import pl.waw.ipipan.homados.bothunter.Tweet;

public abstract class SparseFeature implements Feature {

	public String[] names() {
		return null;
	}

	public String[] values(Tweet tweet) {
		return null;
	}

	public String[] sparseFormat(Tweet tweet) {
		return null;
	}
}
