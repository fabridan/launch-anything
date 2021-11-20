package bar.util;

import bar.Main;
import bar.logic.Settings;
import bar.ui.PopupTextInput;
import jnafilechooser.api.JnaFileChooser;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

public abstract class Util {

    private static Settings settings = null;

    public static void setSettings(Settings settings) {
        Util.settings = settings;
    }

    public static void registerFont(String path) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream(path));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (FontFormatException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static String readClassResource(String path) {
        StringJoiner out = new StringJoiner("\n");
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    public static File pickFile(String filterName, String... filters) {
        // FIXME: this only works on windows, make backup picking method for other OSes
        JnaFileChooser fc = new JnaFileChooser();
        if (filterName != null && filterName.length() > 0)
            fc.addFilter(filterName, filters);
        fc.showOpenDialog(null);
        return fc.getSelectedFile();
    }

    private static File previousFile = null;

    public static File pickDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (previousFile != null) fileChooser.setCurrentDirectory(previousFile);
        else fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.getSelectedFile() != null) {
                previousFile = fileChooser.getSelectedFile();
                return fileChooser.getSelectedFile();
            }
        }

        return null;
    }

    public static String popupDropDown(String title, String message, String[] options, String preselected) {
        if (options == null || options.length == 0) return null;
        Object o = JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, options, preselected != null ? preselected : options[0]);
        if (o == null) return null;
        return o.toString();
    }

    public static String popupTextInput(String title, String message, String pretext) {
        PopupTextInput dialog = new PopupTextInput(title, message, pretext);
        if (!dialog.isCancelled())
            return dialog.getText();
        return null;
    }

    public static String urlDecode(String url) {
        try {
            return decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return url;
    }

    public static String urlEncode(String url) {
        try {
            return encode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return url;
    }

    public static void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static double evaluateMathematicalExpression(String expression) {
        Expression expr = new ExpressionBuilder(expression).build();
        return expr.evaluate();
    }

    public static String getHttpRequestResult(String url) throws IOException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

    public static List<File> recursivelyListFiles(File directory) {
        AtomicInteger amount = new AtomicInteger();
        return recursivelyListFiles(directory, amount);
    }

    private static List<File> recursivelyListFiles(File directory, AtomicInteger amount) {
        if (amount.incrementAndGet() > (settings != null ? settings.getInt("recursionLimit") : 100)) {
            System.out.println("Recursion limit reached");
            return new ArrayList<>();
        }
        if (directory.exists() && directory.isDirectory()) {
            List<File> files = new ArrayList<>();
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) files.addAll(recursivelyListFiles(file, amount));
                else files.add(file);
            }
            return files;
        }
        return Collections.emptyList();
    }

    public static List<File> recursivelyListFiles(File directory, String... extension) {
        AtomicInteger amount = new AtomicInteger();
        return recursivelyListFiles(directory, amount, extension);
    }

    private static List<File> recursivelyListFiles(File directory, AtomicInteger amount, String... extension) {
        if (amount.incrementAndGet() > (settings != null ? settings.getInt("recursionLimit") : 100)) {
            System.out.println("Recursion limit reached");
            return new ArrayList<>();
        }
        if (directory.exists() && directory.isDirectory()) {
            List<File> files = new ArrayList<>();
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) files.addAll(recursivelyListFiles(file, extension));
                else if (extension == null || extension.length == 0 || Arrays.stream(extension).anyMatch(file.getName()::endsWith))
                    files.add(file);
            }
            return files;
        }
        return Collections.emptyList();
    }
}
