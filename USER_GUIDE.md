# LockDemo User's Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [User Interface Overview](#user-interface-overview)
4. [Core Features](#core-features)
5. [Advanced Features](#advanced-features)
6. [Common Scenarios](#common-scenarios)
7. [Keyboard Shortcuts](#keyboard-shortcuts)
8. [Troubleshooting](#troubleshooting)
9. [Tips and Best Practices](#tips-and-best-practices)

---

## Introduction

### What is LockDemo?

LockDemo is an educational and testing tool designed to demonstrate and explore database locking behavior in relational database management systems (RDBMS). It provides a visual, interactive environment for understanding how transactions, locks, and isolation levels interact in concurrent database operations.

### Who Should Use This Tool?

- **Database Administrators**: Test and understand locking behavior in production scenarios
- **Developers**: Learn about transaction isolation and concurrency control
- **Students**: Educational tool for database courses
- **QA Engineers**: Test application behavior under concurrent access patterns

### Key Benefits

- **Visual Feedback**: See locks in real-time with color-coded highlighting
- **Dual Connections**: Simulate concurrent database access
- **Multiple Databases**: Works with Apache Derby (embedded) and IBM DB2
- **Script Management**: Save and reuse test scenarios
- **Professional SQL Editor**: Syntax highlighting, auto-completion, and formatting

---

## Getting Started

### System Requirements

- **Java**: JRE 11 or higher
- **Memory**: Minimum 512 MB RAM
- **Operating System**: Windows, macOS, or Linux
- **Database**: 
  - Apache Derby (included)
  - IBM DB2 (requires JDBC driver)

### Installation

1. **Download**: Get the latest `lockdemo.jar` from the releases
2. **Verify Java**: Run `java -version` to ensure Java 11+ is installed
3. **Launch**: Double-click `lockdemo.jar` or run:
   ```bash
   java -jar lockdemo.jar
   ```

### First Launch

When you first start LockDemo:

1. **Default Database**: The application comes pre-configured with an embedded Apache Derby database
2. **Default URL**: `jdbc:derby:derbyDB;create=true`
3. **Auto-Connect**: You can immediately start testing without additional setup

---

## User Interface Overview

### Main Window Layout

The LockDemo window is divided into three main sections:

```
┌─────────────────────────────────────────────────────────┐
│  Menu Bar (File, View)                                  │
├──────────────────────┬──────────────────────────────────┤
│                      │                                  │
│   Left SQL Panel     │    Right SQL Panel               │
│   (Connection 1)     │    (Connection 2)                │
│                      │                                  │
├──────────────────────┴──────────────────────────────────┤
│                                                          │
│   Lock Display Panel (with color legend)                │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### SQL Panel Components

Each SQL panel (left and right) contains:

1. **Connection Controls**
   - JDBC URL dropdown (with history)
   - Connect/Disconnect button
   - Connection status indicator

2. **Transaction Controls**
   - Isolation level selector
   - Auto-commit checkbox
   - Commit button
   - Rollback button

3. **SQL Editor**
   - Syntax-highlighted text area
   - Line numbers
   - Auto-completion (Ctrl+Space)
   - SQL formatting (Ctrl+Shift+F)

4. **Action Buttons**
   - Execute SQL
   - Next Row (for result navigation)
   - Update Row (for editable results)

5. **Results Area**
   - Tabular display of query results
   - Status messages
   - Error messages

### Lock Display Panel

The bottom panel shows active database locks with:

- **Color-Coded Rows**:
  - 🔴 **Red**: Lock conflicts (multiple transactions on same resource)
  - 🟠 **Orange**: Waiting locks (transaction blocked)
  - 🟣 **Pink**: Exclusive locks (X mode)
  - 🟢 **Light Green**: Shared locks (S mode)

- **Legend**: Visual guide to color meanings
- **Auto-Refresh**: Updates every 250ms
- **Columns**: XID, Type, Mode, Table, Lock Name, State, etc.

### Menu Bar

#### File Menu (Left)
- New (Ctrl+N)
- Open... (Ctrl+O)
- Save (Ctrl+S)
- Save As... (Ctrl+Shift+S)

#### File Menu (Right)
- New (Ctrl+Alt+N)
- Open... (Ctrl+Alt+O)
- Save (Ctrl+Alt+S)
- Save As... (Ctrl+Alt+Shift+S)

#### View Menu
- Theme submenu:
  - Light
  - IntelliJ (default)
  - Dark

---

## Core Features

### 1. Database Connections

#### Connecting to a Database

**Derby (Embedded)**:
```
jdbc:derby:derbyDB;create=true
```

**Derby (Network)**:
```
jdbc:derby://localhost:1527/DBIDB
```

**IBM DB2**:
```
jdbc:db2://hostname:50000/database
```

**Steps**:
1. Select or enter JDBC URL in the dropdown
2. Click "Connect"
3. Wait for "Connected" status
4. Repeat for the second panel

#### Connection Management

- **Independent Connections**: Each panel maintains its own database connection
- **Connection History**: Recently used URLs are saved in the dropdown
- **Disconnect**: Click "Connect" button again to disconnect
- **Auto-Reconnect**: Not available; manual reconnection required

### 2. Transaction Control

#### Isolation Levels

Choose from four standard SQL isolation levels:

| Level | Description | Use Case |
|-------|-------------|----------|
| **Read Uncommitted** | Lowest isolation, allows dirty reads | Maximum concurrency, reporting |
| **Read Committed** | Prevents dirty reads | Default for most applications |
| **Repeatable Read** | Prevents dirty and non-repeatable reads | Financial transactions |
| **Serializable** | Highest isolation, prevents all anomalies | Critical operations |

**To Change Isolation Level**:
1. Select desired level from dropdown
2. Level applies to next transaction
3. Current transaction unaffected

#### Auto-Commit Mode

- **Enabled**: Each SQL statement commits automatically
- **Disabled**: Manual commit/rollback required
- **Toggle**: Check/uncheck "Auto commit" checkbox
- **Recommendation**: Disable for lock testing

#### Manual Transaction Control

**Commit**:
- Click "Commit" button or use keyboard shortcut
- Releases all locks held by the transaction
- Makes changes permanent

**Rollback**:
- Click "Rollback" button or use keyboard shortcut
- Releases all locks
- Undoes all changes since last commit

### 3. SQL Execution

#### Writing SQL

The SQL editor supports:

- **Multi-line statements**
- **Multiple statements** (separated by semicolons)
- **Comments**: 
  - Line comments: `-- comment`
  - Block comments: `/* comment */`
- **String literals**: Single and double quotes

#### Syntax Highlighting

The editor automatically highlights:
- **Keywords**: SELECT, INSERT, UPDATE, DELETE, etc. (blue)
- **Strings**: 'text' or "text" (green)
- **Comments**: -- or /* */ (gray)
- **Numbers**: 123, 45.67 (orange)

#### Auto-Completion

**Activation**:
- Type SQL keyword and press `Ctrl+Space`
- Or wait 300ms for automatic suggestions

**Available Completions**:
- SQL keywords (SELECT, FROM, WHERE, etc.)
- Common clauses (ORDER BY, GROUP BY, etc.)
- Transaction commands (BEGIN, COMMIT, ROLLBACK)
- 60+ SQL keywords supported

#### SQL Formatting

**Format Current SQL**:
1. Write or paste SQL in editor
2. Press `Ctrl+Shift+F`
3. SQL is automatically formatted with proper indentation

**Example**:
```sql
-- Before formatting
select * from users where id=1 and status='active'

-- After formatting (Ctrl+Shift+F)
SELECT *
FROM users
WHERE id = 1
  AND status = 'active'
```

#### Executing SQL

**Methods**:
1. Click "Execute SQL" button
2. Use keyboard shortcut (Alt+1 or Alt+6)

**Behavior**:
- Single statement: Executes immediately
- Multiple statements: Executes all in sequence
- Errors: Displayed in results area, execution stops

#### Viewing Results

**Query Results**:
- Displayed in tabular format
- Scrollable for large result sets
- Column headers show field names
- Auto-sized columns

**Navigation**:
- "Next Row" button: Move to next result row
- Keyboard shortcuts available

**Update Results**:
- Modify cell values directly
- Click "Update Row" to save changes
- Only works with updatable result sets

### 4. Lock Monitoring

#### Understanding the Lock Display

The lock display shows all active locks in the database:

**Common Columns**:
- **XID**: Transaction identifier
- **TYPE**: Lock type (TABLE, ROW)
- **MODE**: Lock mode (S, X, IS, IX, etc.)
- **TABLENAME**: Table being locked
- **LOCKNAME**: Specific lock identifier
- **STATE**: Lock state (GRANT, WAIT)
- **STATUS**: Transaction status (ACTIVE, IDLE)

#### Lock Modes

| Mode | Name | Description |
|------|------|-------------|
| **S** | Shared | Read lock, multiple allowed |
| **X** | Exclusive | Write lock, blocks all others |
| **IS** | Intent Shared | Intent to acquire S locks |
| **IX** | Intent Exclusive | Intent to acquire X locks |
| **SIX** | Shared Intent Exclusive | S lock with IX intent |
| **U** | Update | Upgrade lock, prevents deadlocks |

#### Color-Coded Highlighting

**Red (Conflict)**:
- Multiple transactions accessing same resource
- At least one exclusive lock involved
- Indicates potential blocking situation

**Orange (Waiting)**:
- Transaction is waiting for a lock
- Blocked by another transaction
- May indicate deadlock risk

**Pink (Exclusive)**:
- Transaction holds exclusive (X) lock
- Blocks all other access to resource
- Normal for UPDATE/DELETE operations

**Light Green (Shared)**:
- Transaction holds shared (S) lock
- Allows other shared locks
- Normal for SELECT operations

#### Real-Time Updates

- **Refresh Rate**: 250ms (4 times per second)
- **Automatic**: No manual refresh needed
- **Performance**: Minimal overhead

---

## Advanced Features

### 1. Script Management

#### Creating Scripts

**New Script**:
1. File → New (or Ctrl+N)
2. Prompts to save if current script modified
3. Clears editor for new content

**Writing Scripts**:
```sql
-- Example: Lock testing scenario
BEGIN;

-- Acquire lock on accounts table
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;

-- Wait here to observe lock behavior
-- Execute complementary script in other panel

-- Release lock
COMMIT;
```

#### Saving Scripts

**Save**:
- File → Save (Ctrl+S)
- Saves to current file
- If no file open, prompts for name

**Save As**:
- File → Save As (Ctrl+Shift+S)
- Always prompts for new file name
- Creates new file

**Supported Extensions**:
- `.sql` - Standard SQL scripts
- `.ddl` - Data Definition Language
- `.dml` - Data Manipulation Language

#### Loading Scripts

**Open Script**:
1. File → Open (Ctrl+O)
2. Browse to script file
3. Select and click "Open"
4. Script loads into editor

**Recent Files**:
- Last used directory remembered
- Speeds up script loading

#### Unsaved Changes Protection

The application tracks modifications:
- **Indicator**: Title bar shows "*" for unsaved changes
- **Prompts**: Warns before losing unsaved work
- **Options**: Save, Don't Save, Cancel

### 2. Theme Customization

#### Available Themes

**Light Theme**:
- Bright background
- Dark text
- Best for well-lit environments

**IntelliJ Theme** (Default):
- Balanced colors
- Professional appearance
- Comfortable for extended use

**Dark Theme**:
- Dark background
- Light text
- Reduces eye strain in low light

#### Changing Themes

**Method 1: Menu**:
1. View → Theme
2. Select desired theme
3. Application updates immediately

**Method 2: Preference**:
- Theme preference saved automatically
- Persists across application restarts

#### Theme Persistence

- Stored in user preferences
- Applies to all windows
- No configuration file needed

### 3. SQL Editor Enhancements

#### Line Numbers

- **Always Visible**: Shows line numbers on left
- **Current Line**: Highlighted in different color
- **Navigation**: Click line number to jump to line

#### Code Folding

- Not currently supported
- Planned for future release

#### Find and Replace

- Use standard OS shortcuts:
  - Find: Ctrl+F
  - Replace: Ctrl+H (if supported by component)

#### Undo/Redo

- **Undo**: Ctrl+Z
- **Redo**: Ctrl+Y or Ctrl+Shift+Z
- **Unlimited History**: Within current session

---

## Common Scenarios

### Scenario 1: Demonstrating Read Committed Isolation

**Objective**: Show that Read Committed prevents dirty reads but allows non-repeatable reads.

**Setup**:
1. Connect both panels to same database
2. Set both to "Read Committed" isolation
3. Disable auto-commit on both

**Left Panel**:
```sql
BEGIN;
UPDATE accounts SET balance = 1000 WHERE id = 1;
-- Don't commit yet
```

**Right Panel**:
```sql
BEGIN;
SELECT balance FROM accounts WHERE id = 1;
-- Shows old value (dirty read prevented)
```

**Left Panel**:
```sql
COMMIT;
```

**Right Panel**:
```sql
SELECT balance FROM accounts WHERE id = 1;
-- Now shows new value (non-repeatable read)
COMMIT;
```

**Observation**: Lock display shows X lock on left, then released after commit.

### Scenario 2: Creating a Deadlock

**Objective**: Demonstrate deadlock detection and resolution.

**Setup**:
1. Create two tables: `accounts` and `transfers`
2. Insert test data
3. Disable auto-commit on both panels

**Left Panel**:
```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- Wait 5 seconds
UPDATE transfers SET status = 'pending' WHERE id = 1;
```

**Right Panel**:
```sql
BEGIN;
UPDATE transfers SET status = 'processing' WHERE id = 1;
-- Wait 5 seconds
UPDATE accounts SET balance = balance + 100 WHERE id = 1;
```

**Result**: Database detects deadlock and rolls back one transaction.

**Observation**: Lock display shows both transactions waiting (orange), then one disappears.

### Scenario 3: Testing Serializable Isolation

**Objective**: Demonstrate phantom reads prevention.

**Setup**:
1. Set both panels to "Serializable"
2. Disable auto-commit

**Left Panel**:
```sql
BEGIN;
SELECT COUNT(*) FROM orders WHERE status = 'pending';
-- Note the count
```

**Right Panel**:
```sql
BEGIN;
INSERT INTO orders (status) VALUES ('pending');
-- Blocks until left commits
```

**Left Panel**:
```sql
SELECT COUNT(*) FROM orders WHERE status = 'pending';
-- Same count (phantom read prevented)
COMMIT;
```

**Right Panel**:
```sql
-- Now completes
COMMIT;
```

**Observation**: Right panel waits (orange) until left commits.

### Scenario 4: Lock Escalation

**Objective**: Observe when row locks escalate to table locks.

**Setup**:
1. Create table with many rows
2. Disable auto-commit

**Left Panel**:
```sql
BEGIN;
-- Lock many rows
UPDATE accounts SET balance = balance * 1.01 WHERE balance > 1000;
```

**Observation**: 
- Initially shows many row locks
- May escalate to table lock (database-dependent)
- Lock display shows transition

### Scenario 5: SELECT FOR UPDATE

**Objective**: Demonstrate pessimistic locking.

**Setup**:
1. Disable auto-commit on both panels

**Left Panel**:
```sql
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
-- Holds X lock
```

**Right Panel**:
```sql
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
-- Waits for lock
```

**Observation**: Right panel shows orange (waiting) in lock display.

**Left Panel**:
```sql
COMMIT;
-- Releases lock
```

**Right Panel**:
```sql
-- Now acquires lock and completes
COMMIT;
```

---

## Keyboard Shortcuts

### Complete Shortcut Reference

#### Left Panel Operations

| Action | Shortcut | Description |
|--------|----------|-------------|
| New Script | `Ctrl+N` | Create new empty script |
| Open Script | `Ctrl+O` | Load script from file |
| Save Script | `Ctrl+S` | Save current script |
| Save Script As | `Ctrl+Shift+S` | Save with new name |
| Execute SQL | `Alt+1` | Run SQL statement |
| Next Row | `Alt+2` | Navigate to next result row |
| Update Row | `Alt+3` | Save modified row |
| Commit | `Alt+4` | Commit transaction |
| Rollback | `Alt+5` | Rollback transaction |

#### Right Panel Operations

| Action | Shortcut | Description |
|--------|----------|-------------|
| New Script | `Ctrl+Alt+N` | Create new empty script |
| Open Script | `Ctrl+Alt+O` | Load script from file |
| Save Script | `Ctrl+Alt+S` | Save current script |
| Save Script As | `Ctrl+Alt+Shift+S` | Save with new name |
| Execute SQL | `Alt+6` | Run SQL statement |
| Next Row | `Alt+7` | Navigate to next result row |
| Update Row | `Alt+8` | Save modified row |
| Commit | `Alt+9` | Commit transaction |
| Rollback | `Alt+0` | Rollback transaction |

#### Editor Operations

| Action | Shortcut | Description |
|--------|----------|-------------|
| Auto-Complete | `Ctrl+Space` | Show SQL keyword suggestions |
| Format SQL | `Ctrl+Shift+F` | Format and indent SQL |
| Undo | `Ctrl+Z` | Undo last change |
| Redo | `Ctrl+Y` | Redo last undone change |
| Cut | `Ctrl+X` | Cut selected text |
| Copy | `Ctrl+C` | Copy selected text |
| Paste | `Ctrl+V` | Paste from clipboard |
| Select All | `Ctrl+A` | Select all text |
| Find | `Ctrl+F` | Find text (OS-dependent) |

#### Application Operations

| Action | Shortcut | Description |
|--------|----------|-------------|
| Close Window | `Alt+F4` | Exit application (Windows) |
| Close Window | `Cmd+Q` | Exit application (macOS) |

### Customizing Shortcuts

Currently, keyboard shortcuts are not customizable. This feature may be added in future releases.

---

## Troubleshooting

### Connection Issues

#### Problem: Cannot connect to Derby database

**Symptoms**:
- "Connection failed" error
- No lock display data

**Solutions**:
1. **Check JDBC URL**: Ensure format is correct
   ```
   jdbc:derby:derbyDB;create=true
   ```
2. **Verify Derby**: Ensure Derby is in classpath
3. **Check Permissions**: Ensure write access to database directory
4. **Review Logs**: Check `derby.log` for errors

#### Problem: Cannot connect to DB2 database

**Symptoms**:
- "Driver not found" error
- Connection timeout

**Solutions**:
1. **Add DB2 Driver**: Ensure DB2 JDBC driver in classpath
   ```bash
   java -cp db2jcc4.jar:lockdemo.jar com.ibm.de103252.lockdemo.LockDemo
   ```
2. **Check Network**: Verify DB2 server is accessible
3. **Verify Credentials**: Ensure username/password are correct
4. **Check Port**: Default DB2 port is 50000

### SQL Execution Issues

#### Problem: SQL statement fails to execute

**Symptoms**:
- Error message in results area
- No results displayed

**Solutions**:
1. **Check Syntax**: Verify SQL is valid for your database
2. **Check Permissions**: Ensure user has required privileges
3. **Check Connection**: Verify connection is still active
4. **Review Error**: Read error message carefully

#### Problem: Multiple statements don't execute

**Symptoms**:
- Only first statement executes
- Subsequent statements ignored

**Solutions**:
1. **Check Separators**: Ensure semicolons between statements
2. **Check Comments**: Ensure semicolons not in comments
3. **Check Quotes**: Ensure semicolons not in strings

### Lock Display Issues

#### Problem: Lock display is empty

**Symptoms**:
- No locks shown
- Table is blank

**Solutions**:
1. **Check Connection**: Ensure at least one panel is connected
2. **Execute SQL**: Run some SQL to create locks
3. **Disable Auto-Commit**: Enable manual transaction control
4. **Check Database**: Some databases may not support lock queries

#### Problem: Lock colors not showing

**Symptoms**:
- All rows same color
- No highlighting

**Solutions**:
1. **Check Theme**: Try different theme
2. **Restart Application**: May resolve rendering issues
3. **Check Data**: Ensure locks actually exist

### Script Management Issues

#### Problem: Cannot save script

**Symptoms**:
- Save fails silently
- File not created

**Solutions**:
1. **Check Permissions**: Ensure write access to directory
2. **Check Disk Space**: Ensure sufficient space available
3. **Check Path**: Ensure path is valid
4. **Try Different Location**: Save to different directory

#### Problem: Script won't load

**Symptoms**:
- Open dialog shows no files
- Selected file doesn't load

**Solutions**:
1. **Check File Extension**: Ensure file is .sql, .ddl, or .dml
2. **Check File Format**: Ensure file is plain text
3. **Check Encoding**: Ensure UTF-8 or ASCII encoding
4. **Check File Size**: Very large files may take time to load

### Performance Issues

#### Problem: Application is slow

**Symptoms**:
- Lag when typing
- Slow lock display updates
- Delayed button responses

**Solutions**:
1. **Check System Resources**: Ensure sufficient RAM available
2. **Close Other Applications**: Free up system resources
3. **Reduce Lock Display Size**: Fewer locks = faster updates
4. **Check Database**: Slow database queries affect performance

#### Problem: High CPU usage

**Symptoms**:
- Fan running constantly
- System becomes hot
- Other applications slow

**Solutions**:
1. **Check Lock Refresh**: 250ms refresh may be too frequent
2. **Disconnect Unused Panel**: Reduce database queries
3. **Close Application**: Restart to clear any issues

---

## Tips and Best Practices

### General Usage

1. **Start Simple**: Begin with basic SELECT statements before complex scenarios
2. **Use Comments**: Document your test scenarios with comments
3. **Save Frequently**: Use Ctrl+S to save work regularly
4. **Organize Scripts**: Create folders for different test categories
5. **Name Descriptively**: Use clear names like `deadlock_test_1.sql`

### Lock Testing

1. **Disable Auto-Commit**: Essential for observing lock behavior
2. **Use Both Panels**: Simulate concurrent access
3. **Watch Colors**: Pay attention to lock display highlighting
4. **Start Transactions Explicitly**: Use BEGIN or START TRANSACTION
5. **Clean Up**: Always COMMIT or ROLLBACK when done

### SQL Writing

1. **Use Formatting**: Press Ctrl+Shift+F for readable SQL
2. **Leverage Auto-Complete**: Press Ctrl+Space for suggestions
3. **Add Comments**: Explain what each section does
4. **Test Incrementally**: Run simple queries first
5. **Use Line Numbers**: Helps identify errors

### Script Management

1. **Version Control**: Use Git for script versioning
2. **Backup Scripts**: Keep copies of important scenarios
3. **Share Scripts**: Export and share with team members
4. **Template Library**: Create reusable script templates
5. **Document Scenarios**: Add README files to script folders

### Performance

1. **Limit Result Sets**: Use WHERE clauses to reduce data
2. **Close Connections**: Disconnect when not in use
3. **Monitor Resources**: Watch CPU and memory usage
4. **Optimize Queries**: Use indexes and efficient SQL
5. **Clean Up Data**: Remove test data periodically

### Learning

1. **Start with Examples**: Use provided scenarios
2. **Experiment Safely**: Use test databases only
3. **Read Documentation**: Review isolation level descriptions
4. **Try Different Databases**: Compare Derby and DB2 behavior
5. **Share Knowledge**: Document interesting findings

### Troubleshooting

1. **Check Logs**: Review derby.log for errors
2. **Restart Fresh**: Close and reopen application
3. **Simplify**: Remove complexity to isolate issues
4. **Test Connectivity**: Verify database is accessible
5. **Ask for Help**: Consult documentation or support

---

## Appendix A: SQL Reference

### Common SQL Statements

#### Data Query Language (DQL)

```sql
-- Simple SELECT
SELECT * FROM table_name;

-- SELECT with WHERE
SELECT column1, column2 FROM table_name WHERE condition;

-- SELECT with JOIN
SELECT t1.*, t2.* 
FROM table1 t1 
JOIN table2 t2 ON t1.id = t2.id;

-- SELECT FOR UPDATE (pessimistic locking)
SELECT * FROM table_name WHERE id = 1 FOR UPDATE;
```

#### Data Manipulation Language (DML)

```sql
-- INSERT
INSERT INTO table_name (column1, column2) VALUES (value1, value2);

-- UPDATE
UPDATE table_name SET column1 = value1 WHERE condition;

-- DELETE
DELETE FROM table_name WHERE condition;
```

#### Transaction Control Language (TCL)

```sql
-- Start transaction
BEGIN;
-- or
START TRANSACTION;

-- Commit transaction
COMMIT;

-- Rollback transaction
ROLLBACK;

-- Savepoint (if supported)
SAVEPOINT savepoint_name;
ROLLBACK TO savepoint_name;
```

#### Data Definition Language (DDL)

```sql
-- Create table
CREATE TABLE table_name (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100),
    balance DECIMAL(10,2)
);

-- Drop table
DROP TABLE table_name;

-- Create index
CREATE INDEX idx_name ON table_name(column_name);
```

### Derby-Specific SQL

```sql
-- Create in-memory table
CREATE TABLE temp_table (id INT) NOT LOGGED;

-- Get current transaction ID
VALUES CURRENT_ISOLATION;

-- Set isolation level
SET ISOLATION TO RR;  -- Repeatable Read
SET ISOLATION TO CS;  -- Cursor Stability (Read Committed)
SET ISOLATION TO RS;  -- Read Stability
SET ISOLATION TO UR;  -- Uncommitted Read
```

### DB2-Specific SQL

```sql
-- Lock table explicitly
LOCK TABLE table_name IN EXCLUSIVE MODE;

-- Get lock information
SELECT * FROM SYSIBMADM.LOCKS;

-- Get transaction information
SELECT * FROM SYSIBMADM.SNAPAPPL;
```

---

## Appendix B: Isolation Level Details

### Read Uncommitted

**Characteristics**:
- Lowest isolation level
- Allows dirty reads
- Allows non-repeatable reads
- Allows phantom reads

**Locking Behavior**:
- No shared locks for reads
- Exclusive locks for writes
- Minimal blocking

**Use Cases**:
- Reporting queries
- Approximate counts
- Non-critical data

**Example**:
```sql
SET ISOLATION TO UR;  -- Derby
-- or
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;  -- Standard SQL
```

### Read Committed

**Characteristics**:
- Prevents dirty reads
- Allows non-repeatable reads
- Allows phantom reads

**Locking Behavior**:
- Shared locks for reads (released immediately)
- Exclusive locks for writes (held until commit)
- Moderate blocking

**Use Cases**:
- Most OLTP applications
- Default for many databases
- Balance of consistency and concurrency

**Example**:
```sql
SET ISOLATION TO CS;  -- Derby
-- or
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;  -- Standard SQL
```

### Repeatable Read

**Characteristics**:
- Prevents dirty reads
- Prevents non-repeatable reads
- Allows phantom reads

**Locking Behavior**:
- Shared locks for reads (held until commit)
- Exclusive locks for writes (held until commit)
- More blocking than Read Committed

**Use Cases**:
- Financial transactions
- Inventory management
- Critical business logic

**Example**:
```sql
SET ISOLATION TO RS;  -- Derby
-- or
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;  -- Standard SQL
```

### Serializable

**Characteristics**:
- Highest isolation level
- Prevents dirty reads
- Prevents non-repeatable reads
- Prevents phantom reads

**Locking Behavior**:
- Range locks for reads
- Exclusive locks for writes
- Maximum blocking

**Use Cases**:
- Critical financial operations
- Regulatory compliance
- Data integrity paramount

**Example**:
```sql
SET ISOLATION TO RR;  -- Derby
-- or
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;  -- Standard SQL
```

---

## Appendix C: Lock Mode Compatibility Matrix

| Requested → <br> Held ↓ | IS | IX | S | SIX | U | X |
|-------------------------|----|----|---|-----|---|---|
| **IS** | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| **IX** | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| **S** | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| **SIX** | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **U** | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **X** | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

**Legend**:
- ✓ = Compatible (both locks can be held simultaneously)
- ✗ = Incompatible (one transaction must wait)

---

## Appendix D: Glossary

**Auto-Commit**: Mode where each SQL statement is automatically committed after execution.

**Blocking**: When one transaction must wait for another to release a lock.

**Commit**: Operation that makes all changes in a transaction permanent.

**Concurrency**: Multiple transactions executing simultaneously.

**Deadlock**: Situation where two or more transactions are waiting for each other to release locks.

**Dirty Read**: Reading uncommitted data from another transaction.

**Exclusive Lock (X)**: Lock that prevents all other access to a resource.

**Isolation Level**: Degree to which transactions are isolated from each other.

**Lock**: Mechanism to control concurrent access to database resources.

**Lock Escalation**: Converting many fine-grained locks to fewer coarse-grained locks.

**Non-Repeatable Read**: Reading different values in the same transaction.

**Phantom Read**: Seeing different rows in repeated queries within same transaction.

**Rollback**: Operation that undoes all changes in a transaction.

**Shared Lock (S)**: Lock that allows other shared locks but blocks exclusive locks.

**Transaction**: Unit of work that is atomic, consistent, isolated, and durable (ACID).

**Two-Phase Locking**: Protocol where locks are acquired in growing phase and released in shrinking phase.

---

## Appendix E: Frequently Asked Questions

### General Questions

**Q: Is LockDemo free to use?**
A: Check the license file included with the distribution.

**Q: Can I use LockDemo in production?**
A: LockDemo is designed for testing and education. Use caution in production environments.

**Q: Does LockDemo modify my database?**
A: Only if you execute INSERT, UPDATE, or DELETE statements. It's recommended to use test databases.

**Q: Can I connect to other databases besides Derby and DB2?**
A: The lock monitoring queries are database-specific. Other databases may work for SQL execution but not lock display.

### Technical Questions

**Q: Why don't I see any locks?**
A: Ensure auto-commit is disabled and you're in an active transaction.

**Q: What's the difference between STATE and STATUS columns?**
A: STATE shows lock state (GRANT, WAIT), STATUS shows transaction status (ACTIVE, IDLE).

**Q: Can I export lock data?**
A: Not directly, but you can copy from the table or take screenshots.

**Q: How often does the lock display refresh?**
A: Every 250 milliseconds (4 times per second).

**Q: Can I change the refresh rate?**
A: Not through the UI. It's defined as a constant in the code.

### Usage Questions

**Q: How do I create a deadlock?**
A: See Scenario 2 in the Common Scenarios section.

**Q: What's the best isolation level for testing?**
A: Start with Read Committed, then experiment with others.

**Q: Can I run the same script on both panels?**
A: Yes, but you'll need to load it separately in each panel.

**Q: How do I save my theme preference?**
A: It's saved automatically when you change themes.

**Q: Can I use LockDemo for performance testing?**
A: It's not designed for performance testing, but for understanding lock behavior.

---

## Support and Resources

### Getting Help

- **Documentation**: This guide and README.md
- **Script Examples**: Check the Scripts/ directory
- **Issue Tracker**: Report bugs and request features
- **Community**: Share experiences and learn from others

### Additional Resources

- **Apache Derby Documentation**: https://db.apache.org/derby/docs/
- **IBM DB2 Documentation**: https://www.ibm.com/docs/en/db2
- **SQL Standards**: ISO/IEC 9075
- **Transaction Processing**: "Transaction Processing: Concepts and Techniques" by Gray and Reuter

### Contributing

Contributions are welcome! Areas for improvement:
- Additional database support
- More example scenarios
- UI enhancements
- Documentation improvements
- Bug fixes

---

## Version History

### Current Version: 0.0.1-SNAPSHOT

**Features**:
- Dual connection panels
- Real-time lock monitoring
- Lock conflict highlighting
- Script management
- SQL syntax highlighting
- Auto-completion
- SQL formatting
- Theme support (Light, IntelliJ, Dark)
- Transaction control
- Multiple isolation levels
- Derby and DB2 support

**Recent Additions**:
- Lock conflict highlighting with color coding
- Enhanced wait state detection
- Improved SQL editor with line numbers
- Auto-completion with 60+ keywords
- SQL formatting (Ctrl+Shift+F)
- Theme switching
- Script Manager with unsaved changes protection
- Resource management improvements

---

*Last Updated: January 2026*
*LockDemo User's Guide v1.0*