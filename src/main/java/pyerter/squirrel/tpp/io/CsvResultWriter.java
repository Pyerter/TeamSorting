package pyerter.squirrel.tpp.io;

import com.opencsv.CSVWriter;
import pyerter.squirrel.tpp.TeamSortingLogger;
import pyerter.squirrel.tpp.core.TeamSorterResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvResultWriter {

    public static String targetResultDirectoryName = "experiments";

    public static String getFilePath(String baseName) {
        File targetDirectory = new File(targetResultDirectoryName);
        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) return null;
        }

        return getNextExperimentName(targetDirectory, baseName);
    }

    public static String getNextExperimentName(File directory, String baseName) {
        File[] files = directory.listFiles((d, name) -> name.matches(baseName + "\\d+\\.csv"));

        Pattern pattern = Pattern.compile(String.format("%s(\\d+)\\.csv", Pattern.quote(baseName)));

        int nextIndex = 1; // Default start index
        if (files != null && files.length > 0) {
            // Find the highest numbered experiment
            nextIndex = Arrays.stream(files)
                    .map(file -> {
                        Matcher matcher = pattern.matcher(file.getName());
                        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
                    })
                    .max(Comparator.naturalOrder())
                    .orElse(0) + 1;
        }

        return new File(directory, baseName + nextIndex + ".csv").getAbsolutePath();
    }

    public static String writeResultToFile(TeamSorterResult result, String baseName) {
        String filePath = getFilePath(baseName);
        boolean recoverWithNormalPrint = false;
        if (filePath == null) recoverWithNormalPrint = true;
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeAll(result.getCsvPrint());
            return filePath;
        } catch (IOException e) {
            System.out.println("IOException while attempting to write result to csv: " + filePath + " > " + e.getMessage());
            recoverWithNormalPrint = true;
        } catch (Exception e) {
            System.out.println("Exception while attempting to write result to csv: " + filePath + " > " + e.getMessage());
            recoverWithNormalPrint = true;
        }
        return null;
    }

    public static void printNormalResult(TeamSortingLogger logger) {

    }

}
