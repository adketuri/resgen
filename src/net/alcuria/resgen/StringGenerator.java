package net.alcuria.resgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StringGenerator {

	private static HashSet<Character> characters = new HashSet<>();
	private static String[] reserved = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
		"throw", "throws", "transient", "true", "try", "void", "volatile", "while" };
	private final static List<String> used = new ArrayList<>();

	public static void main(String[] args) {

		if (args.length < 4) {
			System.err.println("usage: java -jar strgen.jar <lang> <csv dir> <output dir> <chars dir>");
			return;
		}
		String lang = args[0];
		String inputPath = args[1];
		String outputPath = args[2];
		String charsPath = args[3];
		System.out.println("Starting resource generator...");

		List<String> lines = new ArrayList<String>();
		lines.add("package net.alcuria.gen;\n");
		lines.add("import java.io.BufferedReader;");
		lines.add("import java.io.IOException;");
		lines.add("import net.alcuria.onlinerpg.client.ui.i18n.LocalizationManager;");
		lines.add("import com.badlogic.gdx.utils.ObjectMap;\n");

		lines.add("/**");
		lines.add(" * This class is auto-generated. You shouldn't need to modify it.");
		lines.add(" * @see <a href=\"https://github.com/adketuri/resgen\">resgen</a> for more information");
		lines.add(" * @author Andrew Keturi");
		lines.add(" */");

		lines.add("public class S {");
		lines.add("\tpublic static final ObjectMap<String, String> map = new ObjectMap<>();");
		final String csvPath = inputPath + "\\" + lang + ".csv";
		final File file = new File(csvPath);
		if (!file.exists()) {
			throw new RuntimeException("Could not find csv in: " + csvPath);
		}
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line = br.readLine();
			while (line != null) {
				final String[] split = line.split(",", 2);
				if (split.length < 1) {
					line = br.readLine();
					continue;
				}
				split[0] = split[0].replaceAll("\\s", "");
				if (used.contains(split[0])) {
					System.err.println("Duplicate key " + split[0]);
				}
				used.add(split[0]);
				for (String r : reserved) {
					if (split.equals(r)) {
						throw new RuntimeException("key found is reserved: " + split[0]);
					}
				}
				lines.add("\tpublic static String " + split[0] + ";");
				// add the strings in the key to the character map
				if (split.length > 1) {
					for (int i = 0; i < split[1].length(); i++) {
						characters.add(split[1].charAt(i));
					}
				}
				line = br.readLine();
			}

			// create list of all unique characters, write to file
			Path dir = Paths.get(charsPath);
			Files.createDirectories(dir);
			Path out = Paths.get(dir + "\\" + lang);
			List<String> chars = new ArrayList<>();
			Object[] charList = characters.toArray();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < charList.length; i++) {
				sb.append(charList[i]);
			}
			chars.add(sb.toString());
			Files.write(out, chars, Charset.forName("UTF-8"));
			System.out.println("Done! Created chars file in: " + charsPath);

			lines.add("\tstatic {");
			lines.add("\t\tS.init();");
			lines.add("\t}\n");

			lines.add("\tpublic static void init() {");
			lines.add("\t\tBufferedReader reader = LocalizationManager.getReader();");
			int readsPerMethod = 500;
			for (int i = 0; i <= used.size() / readsPerMethod; i++){
				lines.add("\t\tgenerate" + (i + 1) + "(reader);");
			}
			lines.add("\t}\n");

			for (int i = 0; i <= used.size() / readsPerMethod; i++) {
				lines.add("\tprivate static void generate" + (i + 1) + " (BufferedReader reader){");
				lines.add("\t\tString line;");
				lines.add("\t\ttry {");
				for (int j = 0; j < readsPerMethod; j++) {
					int readIndex = j + i * readsPerMethod;
					if (readIndex < used.size()) {
						String s = used.get(readIndex);
						lines.add("\t\t\tline = reader.readLine();");
						lines.add("\t\t\t" + s + " = line.split(\",\", 2).length > 1 ? line.split(\",\", 2)[1].replace(\"\\\"\", \"\") : \"!" + s + "\";");
						lines.add("\t\t\tmap.put(line.split(\",\")[0], " + s + ");");
					}
				}
				lines.add("\t\t} catch (IOException e) {");
				lines.add("\t\t\te.printStackTrace();");
				lines.add("\t\t}");
				lines.add("\t}\n");
			}

			lines.add("\tpublic static String get (String k){");
			lines.add("\t\tif (map.containsKey(k)){");
			lines.add("\t\t\treturn map.get(k);");
			lines.add("\t\t} else {");
			lines.add("\t\t\treturn \"!\" + k;");
			lines.add("\t\t}");
			lines.add("\t}");

			lines.add("}");
			br.close();

			// create necessary dirs
			dir = Paths.get(outputPath);
			Files.createDirectories(dir);

			// create file
			out = Paths.get(dir + "\\S.java");
			Files.write(out, lines, Charset.forName("UTF-8"));
			System.out.println("Done! Created file in: " + outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
