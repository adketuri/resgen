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
		lines.add("import net.alcuria.onlinerpg.client.ui.i18n.LocalizationManager;");
		lines.add("import com.badlogic.gdx.utils.ObjectMap;\n");
		lines.add("/** This class is auto-generated. You shouldn't need to modify it.*/");
		lines.add("public class S {");
		lines.add("  public static final ObjectMap<String, String> map = new ObjectMap<String, String>();");
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
				lines.add("  public static String " + split[0] + ";");
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

			lines.add("  static {");
			lines.add("     S.init();");
			lines.add("  }");

			// add initialized map
			lines.add("  public static void init() {");
			lines.add("    try {");
			lines.add("      BufferedReader reader = LocalizationManager.getReader();");
			lines.add("      String line;");
			for (String s : used) {
				lines.add("      line = reader.readLine();");
				lines.add("      " + s + " = line.split(\",\", 2).length > 1 ? line.split(\",\", 2)[1].replace(\"\\\"\", \"\") : \"!" + s + "\";");
				lines.add("      map.put(line.split(\",\")[0], " + s + ");");
			}
			lines.add("    } catch (Exception e){");
			lines.add("      throw new RuntimeException(e);");
			lines.add("    }");
			lines.add("  }");

			lines.add("  public static String get (String k){");
			lines.add("    if (map.containsKey(k)){");
			lines.add("      return map.get(k);");
			lines.add("    } else {");
			lines.add("      return \"!\" + k;");
			lines.add("    }");
			lines.add("  }");

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
