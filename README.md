# LockDemo - Database Lock Demonstration Tool

This application demonstrates lock-based synchronization in RDBMS.
It connects twice to a specified database instance. You can then run SQL statements against the two different connections, monitor the locks that are held, and watch the effects.

The application is packaged with an Apache Derby runtime, which allows you to run against an in-memory Derby database.

## Features

### Core Functionality
- **Dual Connection Panels**: Execute SQL on two separate database connections simultaneously
- **Real-time Lock Monitoring**: View active locks and their states in real-time
- **Multiple Isolation Levels**: Test different transaction isolation levels (Read Uncommitted, Read Committed, Repeatable Read, Serializable)
- **Transaction Control**: Commit and rollback transactions independently on each connection

### Script Manager (NEW)
The application now includes a comprehensive Script Manager for saving and loading SQL scripts:

- **Save/Load Scripts**: Save your SQL test scenarios and load them later
- **Separate File Menus**: Independent script management for left and right panels
- **Keyboard Shortcuts**: Quick access with Ctrl+N, Ctrl+O, Ctrl+S shortcuts
- **Unsaved Changes Protection**: Prompts before losing unsaved work
- **Smart SQL Parsing**: Handles multi-statement scripts with proper comment support

For detailed information about the Script Manager, see [SCRIPT_MANAGER_GUIDE.md](SCRIPT_MANAGER_GUIDE.md).

## Quick Start

### Keyboard Shortcuts

#### Left Panel
- `Ctrl+N` - New script
- `Ctrl+O` - Open script
- `Ctrl+S` - Save script
- `Ctrl+Shift+S` - Save script as
- `1` - Execute SQL
- `2` - Next row
- `3` - Update row
- `4` - Commit
- `5` - Rollback

#### Right Panel
- `Ctrl+Alt+N` - New script
- `Ctrl+Alt+O` - Open script
- `Ctrl+Alt+S` - Save script
- `Ctrl+Alt+Shift+S` - Save script as
- `6` - Execute SQL
- `7` - Next row
- `8` - Update row
- `9` - Commit
- `0` - Rollback

## Supported Databases

- **Apache Derby** (embedded, included)
- **IBM DB2** (requires DB2 JDBC driver)

## Building

```bash
mvn clean package
```

This creates `target/lockdemo.jar` with all dependencies included.

## Running

```bash
java -jar target/lockdemo.jar
```

For DB2 connections, ensure DB2 JDBC drivers are in the classpath.

## Example Usage

1. **Connect to Database**: Enter JDBC URL and click "Connect" on both panels
2. **Set Isolation Level**: Choose the desired isolation level for each connection
3. **Load or Write SQL**: Use the Script Manager to load a saved scenario or write SQL directly
4. **Execute Statements**: Run SQL on one or both connections
5. **Monitor Locks**: Watch the lock table at the bottom to see active locks
6. **Test Scenarios**: Commit or rollback to observe lock behavior

## Documentation

- [Script Manager Guide](SCRIPT_MANAGER_GUIDE.md) - Detailed guide for the Script Manager feature

## Recent Changes

### Version 0.0.1-SNAPSHOT
- Added Script Manager functionality
- Added menu bar with File operations
- Improved keyboard shortcuts
- Added unsaved changes protection
- Enhanced SQL script parsing with comment support
