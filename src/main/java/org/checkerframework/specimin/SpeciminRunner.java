package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/** This class is the main runner for Specimin. Use its main() method to start Specimin. */
public class SpeciminRunner {

  /**
   * The main entry point for Specimin.
   *
   * @param args the arguments to Specimin
   */
  public static void main(String... args) {
    OptionParser optionParser = new OptionParser();

    // This option is the root of the source directory of the target files. It is used
    // for symbol resolution from source code and to organize the output directory.
    OptionSpec<String> rootOption = optionParser.accepts("root").withRequiredArg();

    // This option is the relative paths to the target file(s) - the .java file(s) containing
    // target method(s) - from the root.
    OptionSpec<String> targetFilesOption = optionParser.accepts("targetFiles").withRequiredArg();

    // The directory in which to output the results.
    OptionSpec<String> outputDirectoryOption =
        optionParser.accepts("outputDirectory").withRequiredArg();

    OptionSet options = optionParser.parse(args);

    String root = options.valueOf(rootOption);
    List<String> targetFiles = options.valuesOf(targetFilesOption);
    // Keys are paths to files, values are parsed ASTs
    Map<String, CompilationUnit> parsedTargetFiles = new HashMap<>();
    for (String targetFile : targetFiles) {
      parsedTargetFiles.put(targetFile, parseJavaFile(root, targetFile));
    }

    // TODO: actually do stuff here.

    String outputDirectory = options.valueOf(outputDirectoryOption);

    for (Entry<String, CompilationUnit> target : parsedTargetFiles.entrySet()) {
      LexicalPreservingPrinter.setup(target.getValue());
      Path targetOutputPath = Path.of(outputDirectory, target.getKey());
      try {
        Files.write(
            targetOutputPath,
            LexicalPreservingPrinter.print(target.getValue()).getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        System.out.println("failed to write output file " + targetOutputPath);
        System.out.println("with error: " + e);
      }
    }
  }

  /**
   * Use JavaParser to parse a single Java files.
   *
   * @param root the absolute path to the root of the source tree
   * @param path the path of the file to be parsed, relative to the root
   * @return the compilation unit representing the code in the file at the path, or exit with an
   *     error
   */
  private static CompilationUnit parseJavaFile(String root, String path) {
    CompilationUnit result = null;
    try {
      result = StaticJavaParser.parse(Path.of(root, path));
    } catch (IOException f) {
      System.out.println("failed to parse " + path + ". Exiting.");
      System.out.println("error: " + f);
      System.exit(1);
    }
    return result;
  }
}
