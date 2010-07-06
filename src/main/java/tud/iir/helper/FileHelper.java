package tud.iir.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

// TODO Remove all functionalities that are provided by apache commons.
/**
 * The FileHelper helps with file concerning tasks.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class FileHelper {

    private static final Logger LOGGER = Logger.getLogger(FileHelper.class);

    public static boolean isFileName(String name) {
        name = name.trim();

        Pattern pattern = Pattern.compile("\\.[A-Za-z0-9]{2,5}$", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(name);

        if (m.find()) {
            // System.out.println("is file!" + m.group());
            return true;
        }

        return false;
    }

    public static boolean isVideoFile(String fileType) {
        fileType = fileType.toLowerCase();
        if (fileType.equals("mp4") || fileType.equals("flv") || fileType.equals("avi") || fileType.equals("mpeg2") || fileType.equals("divx")
                || fileType.equals("mov") || fileType.equals("xvid")) {
            return true;
        }
        return false;
    }

    public static boolean isAudioFile(String fileType) {
        fileType = fileType.toLowerCase();
        if (fileType.equals("mp3") || fileType.equals("ogg") || fileType.equals("aac") || fileType.equals("wav") || fileType.equals("flac")) {
            return true;
        }
        return false;
    }

    public static String getFileName(String path) {
        String fileName = "";
        int lastDot = path.lastIndexOf(".");
        if (lastDot > -1) {
            fileName = path.substring(0, lastDot);
        }
        return fileName;
    }

    public static String getFileType(String path) {
        String fileType = "";
        int lastDot = path.lastIndexOf(".");
        if (lastDot > -1) {
            fileType = path.substring(lastDot + 1, path.length());
        }
        return fileType;
    }

    public static String readHTMLFileToString(String path, boolean stripTags) {

        String contents = readFileToString(path);

        if (stripTags) {
            contents = StringHelper.unescapeHTMLEntities(contents);
            contents = StringHelper.removeHTMLTags(contents, true, false, false, false); // TODO remove JS, CSS, comments and merge?
            return contents;
        }

        return contents;
    }

    public static String readFileToString(String path) {

        StringBuilder contents = new StringBuilder();

        try {
            FileReader in = new FileReader(path);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            do {
                line = br.readLine();
                if (line == null)
                    break;
                contents.append(line).append("\n");
            } while (line != null);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(path + ", " + e.getMessage());
        }

        return contents.toString();
    }

    public static ArrayList<String> readFileToArray(String path) {
        ArrayList<String> list = new ArrayList<String>();

        try {
            FileReader in = new FileReader(path);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            do {
                line = br.readLine();
                if (line == null)
                    break;
                list.add(line);
            } while (line != null);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(path + ", " + e.getMessage());
        }

        return list;
    }

    public static int performActionOnEveryLine(String filePath, LineAction la) {

        int lineNumber = 1;

        try {
            FileReader in = new FileReader(filePath);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            do {
                line = br.readLine();
                if (line == null)
                    break;

                la.performAction(line, lineNumber++);

            } while (line != null && la.looping);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }

        return lineNumber - 1;
    }

    public static void writeToFile(String filePath, StringBuilder string) {
        writeToFile(filePath, string.toString());
    }

    /**
     * Writes a Collection of Objects to a file. Each Object's {{@link #toString()} invocation represents a line.
     * 
     * @param filePath
     * @param lines
     * @author Philipp Katz
     */
    public static void writeToFile(String filePath, Collection<?> lines) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            for (Object line : lines) {
                fileWriter.write(line.toString());
                fileWriter.write(System.getProperty("line.separator"));
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }
    }

    public static void writeToFile(String filePath, String string) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(string);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }
    }

    /**
     * Add some text to a file.
     * 
     * @param filePath The path to the file.
     * @param string The text to append.
     * @param before If true, the text will be appended before all other content, if false it will be appended to the end of the file.
     */
    public static void appendToFile(String filePath, StringBuilder string, boolean before) {
        appendToFile(filePath, string.toString(), before);
    }

    public static void appendToFile(String filePath, String string, boolean before) {
        try {
            String currentContent = readFileToString(filePath);
            FileWriter fileWriter = new FileWriter(filePath);
            if (before) {
                fileWriter.write(string);
            }
            fileWriter.write(currentContent);
            if (!before) {
                fileWriter.write(string);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }
    }

    public static Object deserialize(String filePath) {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        Object obj = null;

        try {
            fis = new FileInputStream(filePath);
            in = new ObjectInputStream(fis);
            obj = in.readObject();

        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }

        return obj;
    }

    public static void serialize(Object obj, String filePath) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(filePath);
            out = new ObjectOutputStream(fos);
            out.writeObject(obj);
            out.close();
            fos.close();
            out = null;
            fos = null;
        } catch (IOException e) {
            LOGGER.error("could not serialize object, " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error("could not serialize object, " + e.getMessage() + ", exiting now!");
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("could not serialize object, " + e.getMessage());
        }
    }

    public static String rename(File inputFile, String newName) {
        String fullPath = inputFile.getAbsolutePath();

        String oldName = inputFile.getName().replaceAll("\\..*", "");
        String newPath = fullPath.replaceAll(oldName + "\\.", newName + ".");

        return newPath;
    }

    /**
     * Copy a file.
     * 
     * @param sourceFile The file to copy.
     * @param destinationFile The destination of the file.
     */
    public static void copyFile(String sourceFile, String destinationFile) {
        try {
            File f1 = new File(sourceFile);
            File f2 = new File(destinationFile);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            // System.out.println("File copied.");
        } catch (FileNotFoundException ex) {
            // System.out.println(ex.getMessage() + " in the specified directory.");
            LOGGER.error(ex.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void copyDirectory(String srcPath, String dstPath) {
        copyDirectory(new File(srcPath), new File(dstPath));
    }

    public static void copyDirectory(File srcPath, File dstPath) {

        if (srcPath.isDirectory()) {

            if (!dstPath.exists()) {
                dstPath.mkdir();
            }

            String files[] = srcPath.list();

            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {

            if (!srcPath.exists()) {
                LOGGER.warn("File or directory does not exist.");
                return;
            } else {

                try {
                    InputStream in = new FileInputStream(srcPath);
                    OutputStream out = new FileOutputStream(dstPath);
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        // System.out.println("Directory copied.");
    }

    /**
     * Delete a file or a directory.
     * 
     * @param filename The name of the file or directory.
     * @param deleteNonEmptyDirectory If true, and filename is a directory, it will be deleted with all its contents.
     * @return True if the deletion was successful, false otherwise.
     */
    public static boolean delete(String filename, boolean deleteNonEmptyDirectory) {
        File f = new File(filename);

        if (!f.exists()) {
            LOGGER.error("file can not be deleted because it does not exist");
            return false;
        }

        if (!f.canWrite()) {
            LOGGER.error("file can not be deleted because of lack of permission");
            return false;
        }

        // if it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0 && !deleteNonEmptyDirectory) {
                LOGGER.error("file can not be deleted because it is a non-empty directory");
                return false;
            } else {
                for (File directoryFile : f.listFiles()) {
                    directoryFile.delete();
                }
            }
        }

        // attempt to delete it
        return f.delete();
    }

    public static boolean delete(String filename) {
        return delete(filename, true);
    }

    public static boolean move(File file, String newPath) {
        File newFile = new File(newPath);
        return file.renameTo(new File(newFile, file.getName()));
    }

    /**
     * Add a header to all files from a certain folder.
     * 
     * @param folderPath The path to the folder.
     * @param header The header text to append.
     */
    public static void addFileHeader(String folderPath, StringBuilder header) {
        File[] files = getFiles(folderPath);
        for (File file : files) {
            FileHelper.appendToFile(file.getAbsolutePath(), header, true);
        }
    }

    /**
     * Get all files from a certain folder.
     * 
     * @param folderPath The path to the folder.
     * @return An array of files that are in that folder.
     */
    public static File[] getFiles(String folderPath) {
        return getFiles(folderPath, "");
    }

    public static File[] getFiles(String folderPath, String substring) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (substring.length() > 0) {
                ArrayList<File> matchingFiles = new ArrayList<File>();

                for (File file : files) {
                    if (file.getName().indexOf(substring) > -1)
                        matchingFiles.add(file);
                }

                return matchingFiles.toArray(new File[0]);
            } else {
                return files;
            }
        }

        return new File[0];
    }

    /**
     * Get the number of lines in an ASCII document.
     * 
     * @param fileName The name of the file.
     * @return The number of lines.
     */
    public static int getNumberOfLines(String fileName) {
        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
            }
        };
        return FileHelper.performActionOnEveryLine(fileName, la);
    }

    /**
     * Zip some text and save to a file. http://www.java2s.com/Tutorial/Java/0180__File/ZipafilewithGZIPOutputStream.htm
     * 
     * @param text The text to be zipped.
     * @param filenameOutput The name of the zipped file.
     * @return True if zipping and saving was successfully, false otherwise.
     */
    public static boolean zip(String text, String filenameOutput) {

        try {
            FileOutputStream out = new FileOutputStream(filenameOutput);
            GZIPOutputStream zipout = new GZIPOutputStream(out);

            StringReader in = new StringReader(text);
            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte) c);
            }
            in.close();
            zipout.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    public static String zipString(String text) {
        StringOutputStream out = null;
        try {
            StringInputStream in = new StringInputStream(text);
            out = new StringOutputStream();
            GZIPOutputStream zipout = new GZIPOutputStream(out);

            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte) c);
            }
            in.close();
            zipout.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return out.toString();
        }

        return out.toString();
    }

    /**
     * Unzip a file.
     * 
     * @param filenameInput The name of the zipped file.
     * @param filenameOutput The target name of the unzipped file.
     */
    public static void unzipFile(String filenameInput, String filenameOutput) {
        String unzippedContent = unzipFileToString(filenameInput);
        writeToFile(filenameOutput, unzippedContent);
    }

    public static void unzipFile(String filenameInput) {
        String unzippedContent = unzipFileToString(filenameInput);
        String filenameOutput = getFileName(filenameInput);
        writeToFile(filenameOutput, unzippedContent);
    }

    public static void unzipFile7z(String filenameInput) {
        try {
            Process p = Runtime.getRuntime().exec("7z e " + filenameInput);
            InputStream in = p.getInputStream();
            while (in.read() != -1) {
                // keep waiting until all output is rendered
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void unzipFileCmd(String filenameInput, String consoleCommand) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(consoleCommand + " " + filenameInput);
            InputStream in = p.getInputStream();
             while (in.read() != -1) {
                // keep waiting until all output is rendered
            }
            p.getInputStream().close();
            p.destroy();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Unzip a file and return the unzipped string.
     * 
     * @param filename The name of the zipped file.
     * @return The unzipped content of the file.
     */
    public static String unzipFileToString(String filename) {
        InputStream in = null;
        try {
            in = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            return "";
        }

        return unzipInputStreamToString(in);
    }

    /**
     * Unzip a string.
     * 
     * @param zippedString The zipped string.
     * @return The unzipped string.
     */
    /*
     * public static String unzipString(String zippedString) { InputStream in = new StringInputStream(zippedString); //return unzipInputStreamToString(in);
     * StringOutputStream out = new StringOutputStream(); try { GZIPInputStream zipin = new GZIPInputStream(in); int chunkSize = 8192; byte[] buffer = new
     * byte[chunkSize]; int length; while ((length = zipin.read(buffer, 0, chunkSize)) != -1) { out.write(buffer, 0, length); } out.close(); zipin.close(); }
     * catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); } return out.toString(); }
     */

    /**
     * Unzip a input stream to string.
     * 
     * @param in The input stream with the zipped content.
     * @return The unzipped string.
     */
    public static String unzipInputStreamToString(InputStream in) {
        StringOutputStream out = new StringOutputStream();
        try {
            GZIPInputStream zipin = new GZIPInputStream(in);
            int chunkSize = 8192;
            byte[] buffer = new byte[chunkSize];

            int length;
            while ((length = zipin.read(buffer, 0, chunkSize)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            out.close();
            zipin.close();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return out.toString();
    }

    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            return true;
        }
        return false;
    }

    public static void main(String[] a) {

        // //////////////////////// add license to every file //////////////////////////
        // FileHelper.copyDirectory("src/tud", "data/temp/src/tud");
        // StringBuilder sb = new StringBuilder();
        // sb.append("/*\n");
        // sb.append(" * Copyright 2010 TU Dresden\n");
        // sb.append(" * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not\n");
        // sb.append(" * use this file except in compliance with the License. You may obtain a copy of\n");
        // sb.append(" * the License at\n");
        // sb.append(" *\n");
        // sb.append(" *     http://www.apache.org/licenses/LICENSE-2.0\n");
        // sb.append(" *\n");
        // sb.append(" * Unless required by applicable law or agreed to in writing, software\n");
        // sb.append(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n");
        // sb.append(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        // sb.append(" * See the License for the specific language governing permissions and\n");
        // sb.append(" * limitations under the License.\n");
        // sb.append(" */\n\n");
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/query", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/control", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/fact", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/object", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/gui", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/knowledge", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/news", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/reporting", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/tagging", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/datasetcrawler", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/test", sb);
        // System.exit(0);
        // ////////////////////////add license to every file //////////////////////////
        writeToFile("temp/test.txt", Arrays.asList(new String[] {"one","two","three","four"}));
        System.exit(0);
        
        FileHelper.move(new File("abc.txt"), "data");
        System.exit(0);
        FileHelper.unzipFile7z("wpc_1262_20091020_0017_1.log.gz");
        // FileHelper.unzipFile7z("abc.log.gz");
        // FileHelper.unzipFile("abc.log.gz", "abc_unzipped.log");
        System.exit(0);

        FileHelper.zip("abc -1 sdf sdjfosd fs- 12\\n-1\\abc", "test.txt.gz");

        String unzippedText = FileHelper.unzipFileToString("test.txt.gz");
        System.out.println(unzippedText);

        String zippedString = FileHelper.zipString("abc -1 def");
        System.out.println(zippedString);

        FileHelper.unzipFile("test.txt.gz", "unzipped.txt");

        // System.out.println(FileHelper.unzipString(zippedString));

        System.exit(0);

        isFileName("asdsf sd fs. afjh jerk.");
        isFileName("abc.com");
        isFileName("all.html");
        isFileName("ab.ai");
        isFileName("  abasdf.mpeg2 ");

        System.out.println(rename(new File("data/test/sampleTextForTagging.txt"), "sampleTextForTagging_tagged"));
        
    }
}