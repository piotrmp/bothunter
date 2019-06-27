package pl.waw.ipipan.homados.bothunter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pl.waw.ipipan.homados.bothunter.features.BagOfWords;
import pl.waw.ipipan.homados.bothunter.features.Feature;
import pl.waw.ipipan.homados.bothunter.features.Lengths;

public class TIRARunner {
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args[0].equals("GLM"))
			TIRARun_GLM(Paths.get(args[1]), Paths.get(args[2]),Paths.get(args[3]), Paths.get(args[4]), Paths.get(args[5]), args[6]);
		else if (args[0].equals("BERTpart"))
			TIRARun_BERTpart(Paths.get(args[1]), Paths.get(args[2]),Paths.get(args[3]), Paths.get(args[4]), Paths.get(args[5]), Paths.get(args[6]), Paths.get(args[7]), Paths.get(args[8]), args[9]);
		else if (args[0].equals("GLMBERTpart"))
			TIRARun_GLMBERTpart(Paths.get(args[1]), Paths.get(args[2]),Paths.get(args[3]), Paths.get(args[4]), Paths.get(args[5]), Paths.get(args[6]), Paths.get(args[7]), Paths.get(args[8]), args[9]);
	}

	private static void TIRARun_GLM(Path inputData,Path outputData, Path workdir, Path scriptsR, Path models, String language) throws IOException, SAXException, ParserConfigurationException {
		// prepare space and data
		System.out.println("Preparing the work directory...");
		Files.walk(workdir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		Files.createDirectory(workdir);
		Path dataDir = Paths.get(workdir.toString(), "data");
		Files.createDirectory(dataDir);
		Files.createDirectory(Paths.get(dataDir.toString(), "unknown"));
		Path Rdir = Paths.get(workdir.toString(), "R");
		Files.createDirectory(Rdir);
		Files.copy(Paths.get(scriptsR.toString(), "TIRA_predict_" + language + ".R"), Paths.get(Rdir.toString(), "TIRA_predict_" + language + ".R"));
		Files.copy(Paths.get(scriptsR.toString(), "bic.R"), Paths.get(Rdir.toString(), "bic.R"));
		Files.copy(Paths.get(scriptsR.toString(), "prepareGLM.R"), Paths.get(Rdir.toString(), "prepareGLM.R"));
		Path modeldir = Paths.get(workdir.toString(), "model");
		Files.createDirectory(modeldir);
		Files.copy(Paths.get(models.toString(), "glm-" + language + "-all.rds"), Paths.get(modeldir.toString(), "glm-" + language + "-all.rds"));
		Files.copy(Paths.get(models.toString(), "train1-" + language + "-all.rds"), Paths.get(modeldir.toString(), "train1-" + language + "-all.rds"));
		Files.copy(Paths.get(models.toString(), "train2-" + language + "-all.rds"), Paths.get(modeldir.toString(), "train2-" + language + "-all.rds"));
		Files.copy(Paths.get(models.toString(), "words-" + language + "-all.tsv"), Paths.get(modeldir.toString(), "words-" + language + "-all.tsv"));
		Path glmInput = Paths.get(workdir.toString(), "glm-input");
		Files.createDirectory(glmInput);
		Path outputR = Paths.get(workdir.toString(), "output-R");
		Files.createDirectory(outputR);
		System.out.println("Converting the corpus...");
		Main.convertData(inputData, dataDir, false);
		// prepare GLM input
		System.out.println("Annotating the corpus...");
		Corpus corpus = new Corpus(dataDir.toFile());
		corpus.allToTest();
		corpus.annotate();
		System.out.println("Generating features...");
		Feature[] features = new Feature[] { new Lengths(), new BagOfWords(true, Paths.get(modeldir.toString(), "words-" + language + "-all.tsv")) };
		corpus.outputFeaturesAll(features, glmInput.toFile());
		System.out.println("Running GLM model...");
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("Rscript", "TIRA_predict_" + language + ".R");
		builder.directory(Rdir.toFile());
		builder.redirectErrorStream(true);
		Process process = builder.start();
		StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(streamGobbler);
		int exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("Finished R with exit code " + exitCode);
		// interpret output
		if (exitCode==0) {
			System.out.println("Success!");
			Path finalOutput=Paths.get(outputData.toString(), language);
			Files.createDirectory(finalOutput);
			interpretOutput(Paths.get(outputR.toString(), "output.tsv"),finalOutput,language);
		}else
			System.out.println("Failure!");
		
	}



	private static void TIRARun_BERTpart(Path inputData, Path outputData,Path workdir, Path bertDir, Path bertModelDir, Path envDir, Path scriptsR, Path models, String language)
			throws IOException, SAXException, ParserConfigurationException {
		// prepare space and data
		System.out.println("Preparing the work directory...");
		Files.walk(workdir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		Files.createDirectory(workdir);
		Path dataDir = Paths.get(workdir.toString(), "data");
		Files.createDirectory(dataDir);
		Files.createDirectory(Paths.get(dataDir.toString(), "unknown"));
		Path scoringDir = Paths.get(workdir.toString(), "bert-scoring");
		Files.createDirectory(scoringDir);
		Path scoringDirTest = Paths.get(scoringDir.toString(), "test");
		Files.createDirectory(scoringDirTest);
		Path bertOutput = Paths.get(workdir.toString(), "bert-output");
		Files.createDirectory(bertOutput);
		Path Rdir = Paths.get(workdir.toString(), "R");
		Files.createDirectory(Rdir);
		Files.copy(Paths.get(scriptsR.toString(), "TIRA_predictBERT_" + language + ".R"), Paths.get(Rdir.toString(), "TIRA_predictBERT_" + language + ".R"));
		Files.copy(Paths.get(scriptsR.toString(), "prepareBERT.R"), Paths.get(Rdir.toString(), "prepareBERT.R"));
		Path modeldir = Paths.get(workdir.toString(), "model");
		Files.createDirectory(modeldir);
		Files.copy(Paths.get(models.toString(), "bert-" + language + ".rds"), Paths.get(modeldir.toString(), "bert-" + language + "-all.rds"));
		Path outputR = Paths.get(workdir.toString(), "output-R");
		Files.createDirectory(outputR);
		System.out.println("Converting the corpus...");
		Main.convertData(inputData, dataDir, false);

		// prepare BERT input
		System.out.println("Generating DL input...");
		Corpus corpus = new Corpus(dataDir.toFile());
		corpus.allToTest();
		corpus.outputBertScoring(scoringDir.toFile());

		// run BERT scoring
		String lowercasing = null;
		if (language.startsWith("en"))
			lowercasing = "--do_lower_case=true";
		else if (language.startsWith("es"))
			lowercasing = "--do_lower_case=false";
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(bertDir.toFile());
		String commandString = ". " + Paths.get(envDir.toString(), "bin", "activate") + "; python run_classifier.py --task_name=MRPC --do_predict=true --data_dir=" + scoringDirTest + " --vocab_file="
				+ Paths.get(bertModelDir.toString(), "vocab.txt") + " --bert_config_file=" + Paths.get(bertModelDir.toString(), "bert_config.json") + " --init_checkpoint="
				+ Paths.get(bertModelDir.toString(), "model-part") + " --max_seq_length=128 " + lowercasing + " --output_dir=" + bertOutput;
		System.out.println("Executing command: " + commandString);
		builder.command("bash", "-c", commandString);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(streamGobbler);
		while (process.isAlive()) {
			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int allCases = inputData.toFile().listFiles().length;
			int finishedCases = countLines(Paths.get(bertOutput.toString(), "test_results.tsv")) / 100;
			System.out.println("BERT processed " + finishedCases + " out of " + allCases);
		}
		int exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("BERT finished with exit code " + exitCode);

		// run R script
		System.out.println("Running GLM model...");
		builder = new ProcessBuilder();
		builder.command("Rscript", "TIRA_predictBERT_" + language + ".R");
		builder.directory(Rdir.toFile());
		builder.redirectErrorStream(true);
		process = builder.start();
		streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
		executor = Executors.newSingleThreadExecutor();
		executor.submit(streamGobbler);
		exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("Finished R with exit code " + exitCode);
		// interpret output
		if (exitCode==0) {
			System.out.println("Success!");
			Path finalOutput=Paths.get(outputData.toString(), language);
			Files.createDirectory(finalOutput);
			interpretOutput(Paths.get(outputR.toString(), "output.tsv"),finalOutput,language);
		}else
			System.out.println("Failure!");
		
	}

	private static void TIRARun_GLMBERTpart(Path inputData, Path outputData, Path workdir, Path bertDir, Path bertModelDir, Path envDir, Path scriptsR, Path models, String language)
			throws IOException, SAXException, ParserConfigurationException {
		// prepare space and data
		System.out.println("Preparing the work directory...");
		Files.walk(workdir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		Files.createDirectory(workdir);
		Path dataDir = Paths.get(workdir.toString(), "data");
		Files.createDirectory(dataDir);
		Files.createDirectory(Paths.get(dataDir.toString(), "unknown"));
		Path scoringDir = Paths.get(workdir.toString(), "bert-scoring");
		Files.createDirectory(scoringDir);
		Path scoringDirTest = Paths.get(scoringDir.toString(), "test");
		Files.createDirectory(scoringDirTest);
		Path bertOutput = Paths.get(workdir.toString(), "bert-output");
		Files.createDirectory(bertOutput);
		Path Rdir = Paths.get(workdir.toString(), "R");
		Files.createDirectory(Rdir);
		Files.copy(Paths.get(scriptsR.toString(), "TIRA_predictGLMBERT_" + language + ".R"), Paths.get(Rdir.toString(), "TIRA_predictGLMBERT_" + language + ".R"));
		Files.copy(Paths.get(scriptsR.toString(), "prepareBERT.R"), Paths.get(Rdir.toString(), "prepareBERT.R"));
		Files.copy(Paths.get(scriptsR.toString(), "bic.R"), Paths.get(Rdir.toString(), "bic.R"));
		Files.copy(Paths.get(scriptsR.toString(), "prepareGLM.R"), Paths.get(Rdir.toString(), "prepareGLM.R"));
		Path modeldir = Paths.get(workdir.toString(), "model");
		Files.createDirectory(modeldir);
		Files.copy(Paths.get(models.toString(), "train1-" + language + "-all.rds"), Paths.get(modeldir.toString(), "train1-" + language + "-all.rds"));
		Files.copy(Paths.get(models.toString(), "train2-" + language + "-all.rds"), Paths.get(modeldir.toString(), "train2-" + language + "-all.rds"));
		Files.copy(Paths.get(models.toString(), "words-" + language + "-all.tsv"), Paths.get(modeldir.toString(), "words-" + language + "-all.tsv"));
		Files.copy(Paths.get(models.toString(), "glmbert-" + language + ".rds"), Paths.get(modeldir.toString(), "glmbert-" + language + "-all.rds"));
		Path glmInput = Paths.get(workdir.toString(), "glm-input");
		Files.createDirectory(glmInput);
		Path outputR = Paths.get(workdir.toString(), "output-R");
		Files.createDirectory(outputR);
		System.out.println("Converting the corpus...");
		Main.convertData(inputData, dataDir, false);

		// prepare BERT input
		System.out.println("Generating DL input...");
		Corpus corpus = new Corpus(dataDir.toFile());
		corpus.allToTest();
		corpus.outputBertScoring(scoringDir.toFile());

		// prepare GLM input
		System.out.println("Annotating the corpus...");
		corpus.annotate();
		System.out.println("Generating features...");
		Feature[] features = new Feature[] { new Lengths(), new BagOfWords(true, Paths.get(modeldir.toString(), "words-" + language + "-all.tsv")) };
		corpus.outputFeaturesAll(features, glmInput.toFile());

		// run BERT scoring
		String lowercasing = null;
		if (language.startsWith("en"))
			lowercasing = "--do_lower_case=true";
		else if (language.startsWith("es"))
			lowercasing = "--do_lower_case=false";
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(bertDir.toFile());
		String commandString = ". " + Paths.get(envDir.toString(), "bin", "activate") + "; python run_classifier.py --task_name=MRPC --do_predict=true --data_dir=" + scoringDirTest + " --vocab_file="
				+ Paths.get(bertModelDir.toString(), "vocab.txt") + " --bert_config_file=" + Paths.get(bertModelDir.toString(), "bert_config.json") + " --init_checkpoint="
				+ Paths.get(bertModelDir.toString(), "model-part") + " --max_seq_length=128 " + lowercasing + " --output_dir=" + bertOutput;
		System.out.println("Executing command: " + commandString);
		builder.command("bash", "-c", commandString);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(streamGobbler);
		while (process.isAlive()) {
			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int allCases = inputData.toFile().listFiles().length;
			int finishedCases = countLines(Paths.get(bertOutput.toString(), "test_results.tsv")) / 100;
			System.out.println("BERT processed " + finishedCases + " out of " + allCases);
		}
		int exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("BERT finished with exit code " + exitCode);

		// run R script
		System.out.println("Running GLM model...");
		builder = new ProcessBuilder();
		builder.command("Rscript", "TIRA_predictGLMBERT_" + language + ".R");
		builder.directory(Rdir.toFile());
		builder.redirectErrorStream(true);
		process = builder.start();
		streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
		executor = Executors.newSingleThreadExecutor();
		executor.submit(streamGobbler);
		exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("Finished R with exit code " + exitCode);
		// interpret output
		if (exitCode==0) {
			System.out.println("Success!");
			Path finalOutput=Paths.get(outputData.toString(), language);
			Files.createDirectory(finalOutput);
			interpretOutput(Paths.get(outputR.toString(), "output.tsv"),finalOutput,language);
		}else
			System.out.println("Failure!");
	}

	private static int countLines(Path path){
		int result=0;
		try {
			result=Files.readAllLines(path).size();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return result;
	}
	
	private static void interpretOutput(Path path, Path path2,String lang) throws IOException {
		for (String line:Files.readAllLines(path)) {
			String[] parts=line.trim().split("\t");
			String id=parts[0];
			double score=Double.parseDouble(parts[1]);
			boolean bot=(score>0.5);
			BufferedWriter writer=new BufferedWriter(new FileWriter(Paths.get(path2.toString(), id+".xml").toFile()));
			writer.write("<author id=\""+id+"\"\n");
			writer.write("\tlang=\""+lang+"\"\n");
			writer.write("\ttype=\""+(bot?"bot":"human")+"\"\n");
			writer.write("\tgender=\""+(bot?"bot":"female")+"\"\n");
			writer.write("/>");
			writer.close();
		}
	}

}
