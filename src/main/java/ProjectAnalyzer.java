import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProjectAnalyzer {
    private static Map<String, String> EXTENSION_LANGUAGE_MAP = new HashMap<>();
    private static Map<String, Integer> LANGUAGE_FILE_COUNT = new HashMap<>();
    private static Map<String, Integer> LANGUAGE_LINE_COUNT  = new HashMap<>();
    private static Set<String> IGNORE_FOLDERS = new HashSet<>();
    private static int totalFiles = 0;

    static{
        EXTENSION_LANGUAGE_MAP.put("java", "java");
        EXTENSION_LANGUAGE_MAP.put("vue", "Vue");
        EXTENSION_LANGUAGE_MAP.put("html", "HTML");
        EXTENSION_LANGUAGE_MAP.put("css", "CSS");
        EXTENSION_LANGUAGE_MAP.put("scss", "SCSS");
        EXTENSION_LANGUAGE_MAP.put("js", "JavaScript");
        EXTENSION_LANGUAGE_MAP.put("ts", "TypeScript");

        IGNORE_FOLDERS.add("node_modules");
        IGNORE_FOLDERS.add("__pycache__");
        IGNORE_FOLDERS.add(".idea");
        IGNORE_FOLDERS.add(".git");
        IGNORE_FOLDERS.add("public");
    }

    public static void main(String[] args) {
        if(args.length != 1){
            System.out.println("Usage java ProjectAnalyzer <project_directory>");
            return;
        }

        File projectDirectory = new File(args[0]);

        if(!projectDirectory.isDirectory()){
            System.out.println("Project directory does not exist");
            return;
        }

        LocalDateTime timestamp = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestampString = timestamp.format(formatter);
        String outputFilename = projectDirectory.getName() + "_" + timestampString + ".txt";
        File outputFile = new File(outputFilename);

        try(FileWriter writer = new FileWriter(outputFile)){
            analyzeDirectory(projectDirectory, writer);

            writer.write("Total files: " + totalFiles + "\n\n");
            writer.write("Language file statistics\n");

            for(String language : LANGUAGE_FILE_COUNT.keySet()){
                int count = LANGUAGE_FILE_COUNT.get(language);
                double percentage = (double) count / totalFiles * 100;
                writer.write(String.format("%s: %d files (%.2f%%)%n", language, count, percentage));
            }

            writer.write("\n\nLanguage line statistics:\n");

            int totalLines = 0;

            for(String language : LANGUAGE_LINE_COUNT.keySet()){
                int lines = LANGUAGE_LINE_COUNT.get(language);
                totalLines += lines;
                writer.write(language + ": " + lines + " lines\n");
            }

            double kilometersOfCode = linesToKilometers(totalLines);
            writer.write(String.format("Total lines of code: %d%n", totalLines));
            writer.write(String.format("Kilometers of code: %.2f m%n", kilometersOfCode));
            System.out.println("Analysis completed. Output written to: " + outputFile.getAbsolutePath());
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    private static void analyzeDirectory (File directory, FileWriter writer) throws IOException {
        File[] files = directory.listFiles();
        if(files == null) return;

        for (File file : files) {
            if(file.isDirectory()){
                if(IGNORE_FOLDERS.contains(file.getName())){
                    continue;
                }
                analyzeDirectory(file, writer);
            } else {
                totalFiles++;
                String extension = getFileExtension(file);
                String language = EXTENSION_LANGUAGE_MAP.get(extension);

                if(language != null){
                    if(extension.equals("vue")){

                        language = detectVueTypescript(file) ? "VueTS" : "VueJS";

                    }
                    LANGUAGE_FILE_COUNT.put(language, LANGUAGE_FILE_COUNT.getOrDefault(language, 0) + 1);


                    int lines = countLines(file);
                    LANGUAGE_LINE_COUNT.put(language, LANGUAGE_LINE_COUNT.getOrDefault(language, 0) + lines);
                }


            }

        }
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return lastIndex == -1 ? "" : name.substring(lastIndex + 1);
    }

    private static int countLines (File file) {
        try {
            return (int) Files.lines(file.toPath()).count();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static boolean detectVueTypescript (File file) {
        try{
            String content = new String(Files.readAllBytes(file.toPath()));
            return content.contains("<script setup lang=\"ts\">") ||  content.contains("<script lang=\"ts\">") ||
                    content.contains("<script lang=\"ts\" setup>");
        }
        catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    private static double linesToKilometers(int lines) {
        final int CHARS_PER_LINE = 47;
        final double IDE_SCREEN_SIZE_CM = 19.0;
        final double CHARS_PER_METER = (double) CHARS_PER_LINE * 100 / IDE_SCREEN_SIZE_CM;


        return  lines / CHARS_PER_METER ;
    }
}
