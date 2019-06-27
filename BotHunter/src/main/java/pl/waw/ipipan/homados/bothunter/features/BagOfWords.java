package pl.waw.ipipan.homados.bothunter.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.waw.ipipan.homados.bothunter.Tweet;

public class BagOfWords extends SparseFeature {

	private Map<String, Integer> lexiconMap = new HashMap<String, Integer>();
	private Map<String, Integer> lexicon;
	private static final int MIN_OCCURR = 10;
	private boolean tags;

	public BagOfWords(boolean tags) {
		this.tags = tags;
	}

	public BagOfWords(boolean tags,Path wordsFile) throws IOException {
		this(tags);
		lexicon=new HashMap<String, Integer>();
		for (String line:Files.readAllLines(wordsFile)){
			String[] parts=line.trim().split("\t");
			String lexeme=parts[1];
			Integer number=Integer.parseInt(parts[0]);
			if (lexeme.equals("UNKNOWN"))
				continue;
			lexiconMap.put(lexeme, MIN_OCCURR);
			lexicon.put(lexeme, number);
		}
	}

	public void preview(Tweet tweet) {
		for (String token : getTokens(tweet))
			if (!lexiconMap.containsKey(token))
				lexiconMap.put(token, 1);
			else
				lexiconMap.put(token, lexiconMap.get(token) + 1);
	}

	public void printWords(File file) throws IOException {
		String[] words = new String[lexicon.size() + 1];
		for (String token : lexicon.keySet())
			words[lexicon.get(token) - 1] = token;
		words[lexicon.size()] = "UNKNOWN";
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for (int i = 0; i < words.length; ++i)
			writer.write("" + (i + 1) + "\t" + words[i] + "\n");
		writer.close();
	}

	public String[] sparseFormat(Tweet tweet) {
		if (lexicon == null) {
			int counter = 0;
			lexicon = new HashMap<String, Integer>();
			for (String token : lexiconMap.keySet())
				if (lexiconMap.get(token) >= MIN_OCCURR)
					lexicon.put(token, ++counter);
		}
		Map<String, Integer> tokens = new HashMap<String, Integer>();
		for (String token : getTokens(tweet))
			if (!tokens.containsKey(token))
				tokens.put(token, 1);
			else
				tokens.put(token, tokens.get(token) + 1);
		int unknowns = 0;
		List<String> known = new LinkedList<String>();
		for (String token : tokens.keySet())
			if (lexicon.containsKey(token))
				known.add("" + (lexicon.get(token)) + ":" + tokens.get(token));
			else
				unknowns += tokens.get(token);
		known.add("" + (lexicon.size() + 1) + ":" + unknowns);
		return known.toArray(new String[known.size()]);
	}

	public List<String> getTokens(Tweet tweet) {
		List<String> result = new LinkedList<String>();
		for (String token : tweet.getTokens())
			result.add(token);
		if (tags) {
			for (String tag : tweet.getTags())
				result.add("TAG:" + tag);
			for (int i = 1; i < tweet.getTags().length; ++i)
				result.add("TAG:" + tweet.getTags()[i - 1] + "_" + tweet.getTags()[i]);
			for (int i = 2; i < tweet.getTags().length; ++i)
				result.add("TAG:" + tweet.getTags()[i - 2] + "_" +tweet.getTags()[i - 1] + "_" + tweet.getTags()[i]);
		}
		return result;
	}
}
