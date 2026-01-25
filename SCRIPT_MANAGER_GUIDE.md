# Script Manager Feature Guide

## Overview

The Script Manager feature has been added to the LockDemo application to enable users to save, load, and manage SQL scripts for both connection panels. This enhancement makes it easier to work with complex SQL scenarios and reuse common test cases.

## Features

### 1. Script File Operations

#### New Script
- **Menu**: File (Left/Right) → New
- **Keyboard Shortcut**: 
  - Left Panel: `Ctrl+N`
  - Right Panel: `Ctrl+Alt+N`
- **Description**: Creates a new empty script. Prompts to save if current script has unsaved changes.

#### Open Script
- **Menu**: File (Left/Right) → Open...
- **Keyboard Shortcut**: 
  - Left Panel: `Ctrl+O`
  - Right Panel: `Ctrl+Alt+O`
- **Description**: Opens a file chooser dialog to load an existing SQL script file.
- **Supported Formats**: `.sql`, `.ddl`, `.dml`

#### Save Script
- **Menu**: File (Left/Right) → Save
- **Keyboard Shortcut**: 
  - Left Panel: `Ctrl+S`
  - Right Panel: `Ctrl+Alt+S`
- **Description**: Saves the current script to the currently open file. If no file is open, prompts for a file name (Save As).

#### Save Script As
- **Menu**: File (Left/Right) → Save As...
- **Keyboard Shortcut**: 
  - Left Panel: `Ctrl+Shift+S`
  - Right Panel: `Ctrl+Alt+Shift+S`
- **Description**: Saves the current script to a new file. Prompts for file name and location.

### 2. Unsaved Changes Protection

The Script Manager tracks modifications to your scripts and will prompt you before:
- Opening a new script
- Creating a new script
- Closing the application (if implemented)

This prevents accidental loss of work.

### 3. File Format Support

The Script Manager supports the following SQL file extensions:
- `.sql` - Standard SQL scripts
- `.ddl` - Data Definition Language scripts
- `.dml` - Data Manipulation Language scripts

When saving, if you don't specify an extension, `.sql` is automatically added.

### 4. Smart SQL Statement Parsing

The ScriptManager includes intelligent SQL parsing that:
- Handles multi-statement scripts (separated by semicolons)
- Respects quoted strings (single and double quotes)
- Ignores semicolons within comments
- Supports both line comments (`--`) and block comments (`/* */`)

## Usage Examples

### Example 1: Creating a Test Scenario

1. Click **File (Left) → New** to start a fresh script
2. Enter your SQL statements:
   ```sql
   BEGIN;
   UPDATE accounts SET balance = balance - 100 WHERE id = 1;
   -- Wait here to demonstrate locking
   ```
3. Click **File (Left) → Save As...**
4. Name it `lock_test_left.sql`
5. Repeat for the right panel with complementary SQL

### Example 2: Loading a Saved Scenario

1. Click **File (Left) → Open...**
2. Navigate to your saved script
3. Select `lock_test_left.sql`
4. The script loads into the left SQL editor
5. Repeat for the right panel

### Example 3: Quick Save Workflow

1. Make changes to your script
2. Press `Ctrl+S` (left panel) or `Ctrl+Alt+S` (right panel)
3. Script is saved immediately if a file is already open
4. Otherwise, you'll be prompted for a file name

## Technical Details

### ScriptManager Class

The `ScriptManager` class provides the following key methods:

- `loadScript(File file)` - Load a script from a file
- `saveScript(File file, String content)` - Save content to a file
- `save(String content)` - Save to the current file
- `parseStatements(String script)` - Parse multi-statement scripts
- `isModified(String currentContent)` - Check if content has changed
- `getCurrentFile()` - Get the currently open file
- `getCurrentFileName()` - Get the name of the current file

### Integration Points

The Script Manager is integrated into:
- **SqlPanel**: Each SQL panel has its own ScriptManager instance
- **LockDemo**: Menu bar with separate File menus for left and right panels
- **File Chooser**: Configured with SQL file filters and default directories

## Keyboard Shortcuts Summary

| Action | Left Panel | Right Panel |
|--------|-----------|-------------|
| New | `Ctrl+N` | `Ctrl+Alt+N` |
| Open | `Ctrl+O` | `Ctrl+Alt+O` |
| Save | `Ctrl+S` | `Ctrl+Alt+S` |
| Save As | `Ctrl+Shift+S` | `Ctrl+Alt+Shift+S` |

## Best Practices

1. **Save Frequently**: Use `Ctrl+S` to save your work regularly
2. **Organize Scripts**: Create a dedicated folder for your test scenarios
3. **Descriptive Names**: Use clear file names like `deadlock_scenario_1.sql`
4. **Comment Your Scripts**: Add comments to explain what each script demonstrates
5. **Version Control**: Consider using Git to track changes to your test scripts

## Future Enhancements

Potential improvements for future versions:
- Recent files list
- Script templates library
- Auto-save functionality
- Script comparison tool
- Syntax highlighting in the SQL editor

## Troubleshooting

### Issue: File won't save
- **Solution**: Check file permissions and ensure the directory exists

### Issue: Script doesn't load
- **Solution**: Verify the file is a valid text file with SQL content

### Issue: Keyboard shortcuts don't work
- **Solution**: Ensure the application window has focus

## Support

For issues or questions about the Script Manager feature, please refer to the main README.md or contact the development team.