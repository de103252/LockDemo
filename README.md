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


## Quick Start

### Keyboard Shortcuts

#### Left Panel
- `Ctrl+N` - New script
- `Ctrl+O` - Open script
- `Ctrl+S` - Save script
- `Ctrl+Shift+S` - Save script as
- `Alt-1` - Execute SQL
- `Alt-2` - Next row
- `Alt-3` - Update row
- `Alt-4` - Commit
- `Alt-5` - Rollback

#### Right Panel
- `Ctrl+Alt+N` - New script
- `Ctrl+Alt+O` - Open script
- `Ctrl+Alt+S` - Save script
- `Ctrl+Alt+Shift+S` - Save script as
- `Alt-6` - Execute SQL
- `Alt-7` - Next row
- `Alt-8` - Update row
- `Alt-9` - Commit
- `Alt-0` - Rollback

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

- **[User's Guide](USER_GUIDE.md)** - Comprehensive documentation covering all features
- [Script Manager Guide](SCRIPT_MANAGER_GUIDE.md) - Detailed guide for the Script Manager feature

## Recent Changes

### Version 0.0.1-SNAPSHOT (Latest)
- **Lock Conflict Highlighting**: Color-coded lock display (red=conflict, orange=waiting, pink=exclusive, green=shared)
- **Enhanced SQL Editor**: Syntax highlighting, line numbers, auto-completion (60+ keywords)
- **SQL Formatting**: Format SQL with Ctrl+Shift+F
- **Theme Support**: Light, IntelliJ, and Dark themes with persistence
- **Improved Resource Management**: Proper cleanup of connections and threads
- **Enhanced Wait Detection**: Better detection of waiting locks across databases

### Version 0.0.1-SNAPSHOT (Previous)

### Version 0.0.1-SNAPSHOT
- Added Script Manager functionality
- Added menu bar with File operations
- Improved keyboard shortcuts
- Added unsaved changes protection
- Enhanced SQL script parsing with comment support
