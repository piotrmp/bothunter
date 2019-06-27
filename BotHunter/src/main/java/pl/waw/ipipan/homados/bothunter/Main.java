package pl.waw.ipipan.homados.bothunter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.waw.ipipan.homados.bothunter.features.BagOfWords;
import pl.waw.ipipan.homados.bothunter.features.Feature;
import pl.waw.ipipan.homados.bothunter.features.Lengths;

public class Main {
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		// Convert data to a simplified format
		convertData(Paths.get(args[0] + "en"), Paths.get(args[1] + "data/en"), true);
		convertData(Paths.get(args[0] + "es"), Paths.get(args[1] + "data/es"), true);
		// Process the corpus
		mainProcess(args, "en");
		mainProcess(args, "es");
	}

	public static void mainProcess(String[] args, String language) throws IOException, SAXException, ParserConfigurationException {
		// Read the corpus
		Corpus corpus = new Corpus(new File(args[1] + "data/" + language));

		// For training-test split
		// Prepare files for tuning bert
		corpus.outputBertTuning(new File(args[1] + "bert-input/" + language));
		// Prepare files for scoring by bert
		corpus.outputBertScoring(new File(args[1] + "bert-scoring/" + language));
		// Annotate the corpus
		corpus.annotate();
		// Generate the features for GLM
		Feature[] features = new Feature[] { new Lengths(), new BagOfWords(true) };
		corpus.outputFeaturesAll(features, new File(args[1] + "glm-input/" + language));
		((BagOfWords) features[1]).printWords(new File(args[1] + "glm-input/" + language + "/words.tsv"));

		// For all data in train
		corpus.allToTrain();
		// Prepare files for tuning bert
		corpus.outputBertTuning(new File(args[1] + "bert-input/" + language + "-all"));
		// Prepare files for scoring by bert
		corpus.outputBertScoring(new File(args[1] + "bert-scoring/" + language + "-all"));
		// Generate the features for GLM
		features = new Feature[] { new Lengths(), new BagOfWords(true) };
		corpus.outputFeaturesAll(features, new File(args[1] + "glm-input/" + language + "-all"));
		((BagOfWords) features[1]).printWords(new File(args[1] + "model/words-" + language + "-all.tsv"));
	}

	public static void convertData(Path from, Path to, boolean known) throws IOException, SAXException, ParserConfigurationException {
		List<String> lines = null;
		if (known) {
			Path truth = Paths.get(from.toString(), "truth.txt");
			lines = Files.readAllLines(truth);
		} else {
			lines = new LinkedList<String>();
			for (File file : from.toFile().listFiles())
				if (file.getName().toLowerCase().endsWith(".xml"))
					lines.add(file.getName().substring(0, file.getName().length() - ".xml".length()) + ":::unknown");
		}
		for (String line : lines) {
			String[] parts = line.split(":::");
			String id = parts[0];
			String type = parts[1];
			BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(to.toString(), type, id + ".txt").toFile()));
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Paths.get(from.toString(), id + ".xml").toFile());
			NodeList nodes = doc.getElementsByTagName("document");
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element element = (Element) nodes.item(i);
				String content = element.getTextContent().replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<");
				writer.write(content + "\n<END>\n");
			}
			writer.close();
		}
	}
}
