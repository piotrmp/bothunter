package pl.waw.ipipan.homados.bothunter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.vdurmont.emoji.EmojiParser;

public class TweetTagger {
	public static String[] extractTags(String[] tokens,boolean original) {
		List<String> result = new LinkedList<String>();
		if (original)
			result.add("^");
		for (int i = 0; i < tokens.length; ++i) {
			String token = tokens[i];
			if (token.startsWith("@") && token.length() > 1)
				result.add("@mention");
			else if (token.startsWith("#") && token.length() > 1)
				result.add("#hastag");
			else if (token.equals("RT"))
				result.add("RT");
			else if (token.startsWith("http://") || token.startsWith("https://"))
				result.add("URL");
			else if (isNumber(token))
				result.add("number");
			else if (isWord(token) && token.length() > 1) {
				if (hasRepetitions(token))
					result.add("wooord");
				else if (token.equals(token.toLowerCase()))
					result.add("word");
				else if (token.equals(token.toUpperCase()))
					result.add("WORD");
				else if (token.substring(1).equals(token.substring(1).toLowerCase()))
					result.add("Word");
				else
					result.add("wOrD");
			} else if (isWord(token) && token.equals(token.toLowerCase()))
				result.add("w");
			else if (isWord(token) && token.equals(token.toUpperCase()))
				result.add("W");
			else if (token.equals(":"))
				result.add(":");
			else if (token.equals("(") || token.equals("{") || token.equals("["))
				result.add("(");
			else if (token.equals(")") || token.equals("}") || token.equals("]"))
				result.add(")");
			else if (hasOnly(token, new char[] { '-', '‒', '–', '—', '―', '~' }))
				result.add("-");
			else if (equalsAny(token, new String[] { "\'", "\"", "’", "‘", "’", "“", "”" }))
				result.add("\'");
			else if (token.equals("."))
				result.add(".");
			else if (token.equals(","))
				result.add(",");
			else if (token.equals("!"))
				result.add("!");
			else if (token.equals("?"))
				result.add("?");
			else if (token.equals("/"))
				result.add("/");
			else if (EmojiParser.extractEmojis(token).size() == 1)
				result.add("E");
			else if (EmojiParser.extractEmojis(token).size() > 1)
				result.add("EE");
			else if (equalsAny(token, new String[] { "$", "+", "&", "<", ">", "%", "*", "#" }))
				result.add("*");
			else if (hasOnly(token, new char[] { '$', '+', '&', '<', '>', '%', '*', '#', '.' }))
				result.add("**");
			else if (token.equals("…"))
				result.add("…");
			else if (isAlphanum(token))
				result.add("w0rd");
			else if (token.contains("/"))
				result.addAll(Arrays.asList(extractTags(separate(token, "/"), false)));
			else if (token.contains("&"))
				result.addAll(Arrays.asList(extractTags(separate(token, "&"), false)));
			else if (token.contains("-"))
				result.addAll(Arrays.asList(extractTags(separate(token, "-"), false)));
			else
				result.add("<?>");
		}
		if (original)
			result.add("$");
		return result.toArray(new String[result.size()]);
	}

	private static String[] separate(String token, String sep) {
		String[] parts=token.split(sep);
		String[] result=new String[parts.length*2-1];
		result[0]=parts[0];
		for (int i=1;i<parts.length;++i) {
			result[(i-1)*2+1]=sep;
			result[(i-1)*2+2]=parts[i];
		}
		return result;
	}

	private static boolean hasOnly(String token, char[] cs) {
		all: for (int i = 0; i < token.length(); ++i) {
			for (int j = 0; j < cs.length; ++j)
				if (token.charAt(i) == cs[j])
					continue all;
			return false;
		}
		return true;
	}

	private static boolean isNumber(String token) {
		try {
			Double.parseDouble(token);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isWord(String token) {
		token = token.replaceAll("’", "'");
		if (token.equals("'s") || token.equals("n't") || token.equals("'ve"))
			return true;
		for (int i = 0; i < token.length(); ++i)
			if (!Character.isAlphabetic(token.charAt(i)))
				return false;
		return true;
	}

	private static boolean isAlphanum(String token) {
		for (int i = 0; i < token.length(); ++i)
			if (!Character.isAlphabetic(token.charAt(i)) && !Character.isDigit(token.charAt(i)))
				return false;
		return true;
	}

	private static boolean hasRepetitions(String token) {
		int reps = 0;
		char character = 'x';
		for (int i = 0; i < token.length(); ++i)
			if (reps == 0) {
				reps = 1;
				character = token.charAt(i);
			} else if (character == token.charAt(i)) {
				reps++;
				if (reps == 3)
					return true;
			} else {
				reps = 1;
				character = token.charAt(i);
			}
		return false;
	}

	private static boolean equalsAny(String token, String[] strings) {
		for (String string : strings)
			if (token.equals(string))
				return true;
		return false;
	}
}
