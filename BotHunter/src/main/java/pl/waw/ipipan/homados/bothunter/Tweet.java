package pl.waw.ipipan.homados.bothunter;

import edu.stanford.nlp.pipeline.CoreDocument;

public class Tweet {
	private String content;
	private String[] tokens;
	private String[] tags;

	public Tweet(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public String getSimpleContent() {
		return content.replace("\n", "|").replace("\t", " ");
	}

	public void setAnnotation(CoreDocument document) {
		tokens = new String[document.tokens().size()];
		for (int i = 0; i < tokens.length; ++i)
			tokens[i] = document.tokens().get(i).originalText();
		tags = TweetTagger.extractTags(tokens,true);
	}


	public String[] getTokens() {
		return tokens;
	}

	public String[] getTags() {
		return tags;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
