package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;

import java.util.*;

/**
 * Tracks line and column positions in YAML content for better error reporting.
 */
public class LocationTracker {
    private final String content;
    private final String sourceName;
    private final Map<String, SourceLocation> keyLocations;

    public LocationTracker(String content, String sourceName) {
        this.content = content;
        this.sourceName = sourceName;
        this.keyLocations = new HashMap<>();
        mapKeyLocations();
    }

    private void mapKeyLocations() {
        String[] lines = content.split("\n");
        Stack<String> pathStack = new Stack<>();
        Stack<Integer> indentStack = new Stack<>();
        Map<String, Integer> listIndexes = new HashMap<>();

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String trimmedLine = line.trim();

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            // Calculate indentation level
            int indentation = getIndentation(line);

            // Adjust path stack based on indentation
            adjustPathStack(pathStack, indentStack, indentation, listIndexes);

            // Check if this line defines a key
            if (trimmedLine.contains(":") && !trimmedLine.startsWith("- ")) {
                String key = extractKey(trimmedLine);
                if (key != null && !key.isEmpty()) {
                    String fullPath = buildPath(pathStack, key);
                    int column = line.indexOf(key) + 1;
                    keyLocations.put(fullPath, new SourceLocation(lineIndex + 1, column, sourceName));

                    // Debug output
                    System.out.println("Mapped key: " + fullPath + " -> " + (lineIndex + 1) + ":" + column);

                    // If this is a parent key (has children), add it to the path
                    if (trimmedLine.endsWith(":") || isListItem(lines, lineIndex)) {
                        pathStack.push(key);
                        indentStack.push(indentation);
                        // Reset list index for this level
                        listIndexes.put(fullPath, -1);
                    }
                }
            } else if (trimmedLine.startsWith("- ")) {
                // Handle list items
                if (!pathStack.isEmpty()) {
                    String currentPath = buildPath(pathStack, "");
                    if (currentPath.endsWith(".")) {
                        currentPath = currentPath.substring(0, currentPath.length() - 1);
                    }

                    // Increment list index for this path
                    int itemIndex = listIndexes.getOrDefault(currentPath, -1) + 1;
                    listIndexes.put(currentPath, itemIndex);

                    String indexedKey = currentPath + "[" + itemIndex + "]";

                    // If the list item has a key after the dash
                    String listItemContent = trimmedLine.substring(2).trim();
                    if (listItemContent.contains(":")) {
                        String itemKey = extractKey(listItemContent);
                        if (itemKey != null && !itemKey.isEmpty()) {
                            String itemPath = indexedKey + "." + itemKey;
                            int keyColumn = line.indexOf(itemKey) + 1;
                            keyLocations.put(itemPath,
                                new SourceLocation(lineIndex + 1, keyColumn, sourceName));

                            // Debug output
                            System.out.println("Mapped array item key: " + itemPath + " -> " + (lineIndex + 1) + ":" + keyColumn);

                            // For objects in arrays, we need to track their structure
                            if (listItemContent.endsWith(":") || hasNestedContent(lines, lineIndex, indentation)) {
                                pathStack.push(indexedKey + "." + itemKey);
                                indentStack.push(indentation + 2); // Account for the "- " prefix
                            }
                        }
                    } else {
                        // Just a list item without a key (primitive value)
                        keyLocations.put(indexedKey,
                            new SourceLocation(lineIndex + 1, indentation + 3, sourceName)); // After "- "

                        // Debug output
                        System.out.println("Mapped array item: " + indexedKey + " -> " + (lineIndex + 1) + ":" + (indentation + 3));
                    }

                    // Also map just the array index without any nested key for better matching
                    keyLocations.put(indexedKey,
                        new SourceLocation(lineIndex + 1, indentation + 1, sourceName));
                }
            }
        }
    }

    private int getIndentation(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 4; // Assume 4 spaces per tab
            } else {
                break;
            }
        }
        return indent;
    }

    private void adjustPathStack(Stack<String> pathStack, Stack<Integer> indentStack, int currentIndent, Map<String, Integer> listIndexes) {
        // Pop from stacks while current indentation is less than or equal to the top indentation
        while (!indentStack.isEmpty() && currentIndent <= indentStack.peek()) {
            indentStack.pop();
            if (!pathStack.isEmpty()) {
                String poppedPath = pathStack.pop();
                // Clean up list indexes for this path level
                String currentPath = buildPath(pathStack, poppedPath);
                listIndexes.remove(currentPath);
            }
        }
    }

    private boolean hasNestedContent(String[] lines, int currentLineIndex, int currentIndent) {
        // Check if the next non-empty, non-comment line is indented more
        for (int i = currentLineIndex + 1; i < lines.length; i++) {
            String nextLine = lines[i].trim();
            if (nextLine.isEmpty() || nextLine.startsWith("#")) {
                continue;
            }
            return getIndentation(lines[i]) > currentIndent + 2; // +2 for the "- " prefix
        }
        return false;
    }


    private String extractKey(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("- ")) {
            trimmed = trimmed.substring(2).trim();
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0) {
            String key = trimmed.substring(0, colonIndex).trim();
            // Remove quotes if present
            if ((key.startsWith("\"") && key.endsWith("\"")) ||
                (key.startsWith("'") && key.endsWith("'"))) {
                key = key.substring(1, key.length() - 1);
            }
            return key.isEmpty() ? null : key;
        }
        return null;
    }

    private String buildPath(List<String> pathStack, String key) {
        if (pathStack.isEmpty()) {
            return key;
        }
        return String.join(".", pathStack) + "." + key;
    }

    private String buildPath(Stack<String> pathStack, String key) {
        return buildPath(new ArrayList<>(pathStack), key);
    }

    private boolean isListItem(String[] lines, int lineIndex) {
        // Check if the next non-empty line is indented more (indicating children)
        for (int i = lineIndex + 1; i < lines.length; i++) {
            String nextLine = lines[i].trim();
            if (nextLine.isEmpty() || nextLine.startsWith("#")) {
                continue;
            }
            return getIndentation(lines[i]) > getIndentation(lines[lineIndex]);
        }
        return false;
    }

    /**
     * Get source location for a specific key path.
     */
    public SourceLocation getLocationForKey(String keyPath) {
        SourceLocation location = keyLocations.get(keyPath);
        if (location != null) {
            return location;
        }

        // Try exact matches first, then partial matches
        String bestMatch = null;
        int bestScore = 0;

        for (String trackedPath : keyLocations.keySet()) {
            if (trackedPath.equals(keyPath)) {
                return keyLocations.get(trackedPath);
            }

            // Check for partial matches
            int score = calculatePathSimilarity(trackedPath, keyPath);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = trackedPath;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            return keyLocations.get(bestMatch);
        }

        // Try to find parent path
        String parentPath = getParentPath(keyPath);
        if (parentPath != null && !parentPath.equals(keyPath)) {
            SourceLocation parentLocation = getLocationForKey(parentPath);
            if (parentLocation.getLine() > 1 || parentLocation.getColumn() > 1) {
                return parentLocation;
            }
        }

        // Default fallback
        return new SourceLocation(1, 1, sourceName);
    }

    private int calculatePathSimilarity(String trackedPath, String targetPath) {
        String[] trackedParts = trackedPath.split("\\.");
        String[] targetParts = targetPath.split("\\.");

        int score = 0;
        int minLength = Math.min(trackedParts.length, targetParts.length);

        for (int i = 0; i < minLength; i++) {
            if (trackedParts[i].equals(targetParts[i])) {
                score += 10;
            } else if (trackedParts[i].contains(targetParts[i]) || targetParts[i].contains(trackedParts[i])) {
                score += 5;
            }
        }

        // Penalty for length difference
        score -= Math.abs(trackedParts.length - targetParts.length) * 2;

        return Math.max(0, score);
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Handle array indexes
        if (path.contains("[")) {
            int bracketIndex = path.lastIndexOf('[');
            int dotIndex = path.lastIndexOf('.', bracketIndex);
            if (dotIndex > 0) {
                return path.substring(0, dotIndex);
            }
            return path.substring(0, bracketIndex);
        }

        // Handle regular dot notation
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return path.substring(0, lastDotIndex);
        }

        return null;
    }

    /**
     * Get source location for a map that should contain a specific key.
     */
    public SourceLocation getLocationForMissingKey(String parentPath, String missingKey) {
        // Find the location of the parent object
        SourceLocation parentLoc = null;

        if (parentPath != null && !parentPath.isEmpty()) {
            parentLoc = getLocationForKey(parentPath);
        }

        // If no specific parent path, try to find the best context
        if (parentLoc == null || (parentLoc.getLine() == 1 && parentLoc.getColumn() == 1)) {
            // Look for sibling keys to get a better location
            for (Map.Entry<String, SourceLocation> entry : keyLocations.entrySet()) {
                String keyPath = entry.getKey();
                if (keyPath.contains(".") && keyPath.substring(keyPath.lastIndexOf('.') + 1).equals(missingKey)) {
                    // Found a sibling with the same key name - good hint
                    return entry.getValue();
                }

                if (parentPath != null && keyPath.startsWith(parentPath + ".")) {
                    // Found a key in the same parent context
                    parentLoc = entry.getValue();
                }
            }
        }

        if (parentLoc != null && (parentLoc.getLine() > 1 || parentLoc.getColumn() > 1)) {
            return parentLoc;
        }

        // Fallback to searching for similar structure
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains(":") && !line.startsWith("#")) {
                return new SourceLocation(i + 1, 1, sourceName);
            }
        }

        return new SourceLocation(1, 1, sourceName);
    }

    /**
     * Get source location by line and column (1-based).
     */
    public SourceLocation getLocation(int line, int column) {
        return new SourceLocation(line, column, sourceName);
    }

    /**
     * Create a default location for the root of the file.
     */
    public SourceLocation getRootLocation() {
        return new SourceLocation(1, 1, sourceName);
    }

    /**
     * Debug method to print all tracked key locations.
     */
    public void printKeyLocations() {
        System.out.println("=== Key Locations ===");
        for (Map.Entry<String, SourceLocation> entry : keyLocations.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}
