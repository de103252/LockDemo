package com.ibm.de103252.lockdemo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Manages loading and saving of SQL scripts.
 * Provides functionality to parse multi-statement scripts and handle file operations.
 */
public class ScriptManager {
    private File currentFile;
    private String lastLoadedContent;
    private static final String PREF_LAST_DIRECTORY = "lastDirectory";
    
    /**
     * Load a SQL script from a file.
     *
     * @param file The file to load
     * @return The content of the file
     * @throws IOException if the file cannot be read
     */
    public String loadScript(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist: " + file);
        }
        
        Path path = file.toPath();
        lastLoadedContent = Files.readString(path, StandardCharsets.UTF_8);
        currentFile = file;
        
        // Save the directory for next time
        saveLastDirectory(file.getParentFile());
        
        return lastLoadedContent;
    }
    
    /**
     * Save SQL script content to a file.
     *
     * @param file The file to save to
     * @param content The SQL content to save
     * @throws IOException if the file cannot be written
     */
    public void saveScript(File file, String content) throws IOException {
        if (file == null) {
            throw new IOException("File cannot be null");
        }
        
        Path path = file.toPath();
        Files.writeString(path, content, StandardCharsets.UTF_8);
        currentFile = file;
        lastLoadedContent = content;
        
        // Save the directory for next time
        saveLastDirectory(file.getParentFile());
    }
    
    /**
     * Save content to the currently open file.
     * 
     * @param content The SQL content to save
     * @throws IOException if no file is currently open or if the file cannot be written
     */
    public void save(String content) throws IOException {
        if (currentFile == null) {
            throw new IOException("No file is currently open. Use saveAs instead.");
        }
        saveScript(currentFile, content);
    }
    
    /**
     * Parse a SQL script into individual statements.
     * Statements are separated by semicolons, but semicolons within quotes are ignored.
     * 
     * @param script The SQL script to parse
     * @return List of individual SQL statements
     */
    public List<String> parseStatements(String script) {
        if (script == null || script.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        
        char[] chars = script.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            char next = (i + 1 < chars.length) ? chars[i + 1] : '\0';
            
            // Handle line comments
            if (!inSingleQuote && !inDoubleQuote && !inBlockComment && c == '-' && next == '-') {
                inLineComment = true;
                currentStatement.append(c);
                continue;
            }
            
            if (inLineComment) {
                currentStatement.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            
            // Handle block comments
            if (!inSingleQuote && !inDoubleQuote && !inLineComment && c == '/' && next == '*') {
                inBlockComment = true;
                currentStatement.append(c);
                continue;
            }
            
            if (inBlockComment) {
                currentStatement.append(c);
                if (c == '*' && next == '/') {
                    currentStatement.append(next);
                    i++; // Skip next character
                    inBlockComment = false;
                }
                continue;
            }
            
            // Handle quotes
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                currentStatement.append(c);
                continue;
            }
            
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                currentStatement.append(c);
                continue;
            }
            
            // Handle statement separator
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String statement = currentStatement.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                currentStatement = new StringBuilder();
                continue;
            }
            
            currentStatement.append(c);
        }
        
        // Add the last statement if it doesn't end with semicolon
        String lastStatement = currentStatement.toString().trim();
        if (!lastStatement.isEmpty()) {
            statements.add(lastStatement);
        }
        
        return statements;
    }
    
    /**
     * Get the currently open file.
     * 
     * @return The current file, or null if no file is open
     */
    public File getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Get the name of the currently open file.
     * 
     * @return The file name, or "Untitled" if no file is open
     */
    public String getCurrentFileName() {
        return currentFile != null ? currentFile.getName() : "Untitled";
    }
    
    /**
     * Check if the content has been modified since last load/save.
     * 
     * @param currentContent The current content to compare
     * @return true if content has been modified
     */
    public boolean isModified(String currentContent) {
        if (lastLoadedContent == null) {
            return currentContent != null && !currentContent.isEmpty();
        }
        return !lastLoadedContent.equals(currentContent);
    }
    
    /**
     * Clear the current file reference.
     */
    public void clearCurrentFile() {
        currentFile = null;
        lastLoadedContent = null;
    }
    
    /**
     * Get a file extension filter for SQL files.
     * 
     * @return Array of SQL file extensions
     */
    public static String[] getSqlFileExtensions() {
        return new String[] { "sql", "ddl", "dml" };
    }
    
    /**
     * Check if a file has a SQL extension.
     * 
     * @param file The file to check
     * @return true if the file has a SQL extension
    
    /**
     * Save the last used directory to preferences.
     */
    private void saveLastDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            Preferences prefs = Preferences.userRoot().node(ScriptManager.class.getName());
            prefs.put(PREF_LAST_DIRECTORY, directory.getAbsolutePath());
            try {
                prefs.flush();
            } catch (Exception e) {
                // Ignore preference save errors
            }
        }
    }
    
    /**
     * Get the last used directory from preferences.
     * 
     * @return The last used directory, or user's home directory if none saved
     */
    public File getLastDirectory() {
        Preferences prefs = Preferences.userRoot().node(ScriptManager.class.getName());
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
        File dir = new File(lastDir);
        
        // Verify directory still exists
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        
        // Fall back to user home if saved directory no longer exists
        return new File(System.getProperty("user.home"));
    }
     
    public static boolean isSqlFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return Arrays.stream(getSqlFileExtensions())
                     .anyMatch(ext -> name.endsWith("." + ext));
    }
}

