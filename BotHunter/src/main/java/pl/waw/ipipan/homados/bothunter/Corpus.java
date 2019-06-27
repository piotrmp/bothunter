package pl.waw.ipipan.homados.bothunter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import pl.waw.ipipan.homados.bothunter.features.Feature;
import pl.waw.ipipan.homados.bothunter.features.SparseFeature;

public class Corpus {
	public List<User> users;
	List<User> testUsers;
	List<User> trainUsers;
	Random rand;

	public Corpus(File dir) throws IOException {
		users = new LinkedList<User>();
		Path botDir = Paths.get(dir.toString(), "bot");
		if (botDir.toFile().exists())
			for (File file : botDir.toFile().listFiles()) {
				users.add(new User(file, true));
			}
		Path humanDir = Paths.get(dir.toString(), "human");
		if (humanDir.toFile().exists())
			for (File file : humanDir.toFile().listFiles()) {
				users.add(new User(file, false));
			}
		Path unknownDir = Paths.get(dir.toString(), "unknown");
		if (unknownDir.toFile().exists())
			for (File file : unknownDir.toFile().listFiles()) {
				users.add(new User(file, null));
			}
		rand = new Random(3);
		testUsers = new LinkedList<User>();
		trainUsers = new LinkedList<User>(users);
		for (int i = 0; i < users.size() * 0.2; ++i)
			testUsers.add(trainUsers.remove(rand.nextInt(trainUsers.size())));
		System.out.println("Read " + users.size() + " users.");
	}

	public void outputBertTuning(File dir) throws IOException {
		if (!trainUsers.isEmpty())
			outputPairs(trainUsers, Paths.get(dir.toString(), "train.tsv").toFile(), rand, false, 3);
		if (!testUsers.isEmpty())
			outputPairs(testUsers, Paths.get(dir.toString(), "test.tsv").toFile(), rand, false, 3);
	}

	public void outputBertScoring(File dir) throws IOException {
		if (!trainUsers.isEmpty())
			outputPairs(trainUsers, Paths.get(dir.toString(), "train", "test.tsv").toFile(), rand, true, 4);
		if (!testUsers.isEmpty())
			outputPairs(testUsers, Paths.get(dir.toString(), "test", "test.tsv").toFile(), rand, true, 4);
	}

	private static void outputPairs(List<User> users, File file, Random rand, boolean forScoring, long seed) throws IOException {
		rand.setSeed(seed);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("Quality\tID1\tID2\tUser1\tUser2\n");
		int counter = 0;
		for (int j = 0; j < users.size(); ++j)
			for (int i = 0; i < users.get(j).tweets.size(); ++i) {
				if (!forScoring && ++counter % 5 != 0)
					continue;
				User user = users.get(j);
				Tweet tweet = user.tweets.get(i);
				int newI = (i + rand.nextInt(user.tweets.size() - 1) + 1) % user.tweets.size();
				Tweet sameTweet = user.tweets.get(newI);
				int score = 1;
				if (forScoring)
					score = (user.isBot == null || user.isBot) ? 1 : 0;
				writer.write("" + score + "\t" + user.id + "\t" + user.id + "\t" + tweet.getSimpleContent() + "\t" + sameTweet.getSimpleContent() + "\n");
				if (!forScoring) {
					int otherJ = (j + rand.nextInt(users.size() - 1) + 1) % users.size();
					User otherUser = users.get(otherJ);
					Tweet otherTweet = otherUser.tweets.get(i);
					writer.write("0\t" + user.id + "\t" + otherUser.id + "\t" + tweet.getSimpleContent() + "\t" + otherTweet.getSimpleContent() + "\n");
				}
			}
		writer.close();
	}

	public void annotate() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		int counter = 0;
		for (User user : users) {
			System.out.println("Annotating user " + (++counter) + " / " + users.size() + " : " + user.id);
			for (Tweet tweet : user.tweets) {
				CoreDocument document = new CoreDocument(tweet.getContent());
				pipeline.annotate(document);
				tweet.setAnnotation(document);
			}
		}
	}

	public void outputFeaturesAll(Feature[] features, File dir) throws IOException {
		if (!trainUsers.isEmpty())
			previewFeatures(trainUsers, features);
		if (!trainUsers.isEmpty())
			outputFeatures(trainUsers, features, Paths.get(dir.toString(), "train.tsv").toFile(), Paths.get(dir.toString(), "train.csr").toFile());
		if (!testUsers.isEmpty())
			outputFeatures(testUsers, features, Paths.get(dir.toString(), "test.tsv").toFile(), Paths.get(dir.toString(), "test.csr").toFile());
		outputUsers(users, Paths.get(dir.toString(), "users.tsv").toFile());
	}

	private static void outputUsers(List<User> users, File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for (User user : users)
			writer.write(user.id + "\t" + (user.isBot == null ? "?" : (user.isBot ? "1" : "0")) + "\n");
		writer.close();
	}

	private static void previewFeatures(List<User> set, Feature[] features) {
		for (User user : set)
			for (Tweet tweet : user.tweets)
				for (Feature feature : features)
					feature.preview(tweet);
	}

	public static void outputFeatures(List<User> set, Feature[] features, File denseFile, File sparseFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(denseFile));
		writer.write("ID");
		for (Feature feature : features)
			if (!(feature instanceof SparseFeature))
				for (String name : feature.names())
					writer.write("\t" + name);
		writer.write("\n");
		for (User user : set)
			for (Tweet tweet : user.tweets) {
				writer.write(user.id);
				for (Feature feature : features)
					if (!(feature instanceof SparseFeature)) {
						for (String value : feature.values(tweet))
							writer.write("\t" + value);
					}
				writer.write("\n");
			}
		writer.close();
		writer = new BufferedWriter(new FileWriter(sparseFile));
		int counter = 0;
		for (User user : set)
			for (Tweet tweet : user.tweets) {
				writer.write("" + (++counter));
				for (Feature feature : features)
					if (feature instanceof SparseFeature)
						for (String string : ((SparseFeature) feature).sparseFormat(tweet))
							writer.write(" " + string);
				writer.newLine();
			}
		writer.close();
	}

	public void allToTrain() {
		trainUsers.addAll(testUsers);
		testUsers.clear();
	}

	public void allToTest() {
		testUsers.addAll(trainUsers);
		trainUsers.clear();
	}
}
