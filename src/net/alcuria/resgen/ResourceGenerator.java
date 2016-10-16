package net.alcuria.resgen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ResourceGenerator {

	public static String className = null;
	public static String rootPath = null;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("usage: java -jar resgen.jar <project path> <generated filename>");
			return;
		}
		rootPath = args[0];
		className = args[1];
		System.out.println("Starting resource generator...");
		ResourceGenerator resgen = new ResourceGenerator();

		List<String> lines = new ArrayList<String>();
		lines.add("package net.alcuria.gen;\n\n/** This class is auto-generated. You shouldn't need to modify it.*/\npublic class " + className + " {");
		resgen.walk(rootPath + "\\android\\assets", lines, "\t");
		lines.add("}");

		try {
			// create necessary dirs
			final String resourcePath = rootPath + "\\core\\gen\\net\\alcuria\\gen";
			Path dir = Paths.get(resourcePath);
			Files.createDirectories(dir);

			// create file
			Path file = Paths.get(resourcePath + "\\" + className + ".java");
			Files.write(file, lines, Charset.forName("UTF-8"));
			System.out.println("Done! Created file in: " + resourcePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void walk(String path, List<String> lines, String whitespace) {

		File rootFile = new File(path);
		File[] list = rootFile.listFiles();

		if (list == null) {
			return;
		}

		HashSet<String> used = new HashSet<>();
		for (File f : list) {
			if (f.isDirectory()) {
				lines.add(whitespace + "public static class " + f.getName() + " {");
				walk(f.getAbsolutePath(), lines, whitespace + "\t");
				lines.add(whitespace + "}\n");
			} else {
				String nameWithoutExtension = f.getName().replaceFirst("[.][^.]+$", "");
				if (nameWithoutExtension == null || nameWithoutExtension.length() < 1) {
					continue;
				}
				if (used.contains(nameWithoutExtension)) {
					nameWithoutExtension = f.getName().replace('.', '_');
				}
				used.add(nameWithoutExtension);
				final String resourcePath = f.getAbsolutePath().replace(rootPath + "\\android\\assets\\", "").replace('\\', '/');
				lines.add(String.format("%spublic static String %s = \"%s\";", whitespace, nameWithoutExtension, resourcePath));
			}
		}
	}
}
