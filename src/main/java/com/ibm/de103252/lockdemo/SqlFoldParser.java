package com.ibm.de103252.lockdemo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;

/*
 * 01/28/2026
 *
 * SqlFoldParser.java - Fold parser for SQL syntax.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */

/**
 * A fold parser for SQL that identifies foldable regions such as:
 * <ul>
 * <li>BEGIN/END blocks</li>
 * <li>CREATE statements (procedures, functions, triggers, views, etc.)</li>
 * <li>Multi-line comments (/ * ... * /)</li>
 * <li>CASE statements</li>
 * </ul>
 *
 * This parser uses RSyntaxTextArea's syntax highlighting tokens to identify SQL
 * keywords and structure. It looks for reserved words like BEGIN, END, CREATE,
 * CASE, etc., and creates foldable regions accordingly.
 * <p>
 *
 * Note that this class may impose somewhat of a performance penalty on large
 * SQL files, since it parses the entire document each time folds are
 * reevaluated.
 *
 * @author Robert Futrell (adapted for SQL)
 * @version 1.0
 */
public class SqlFoldParser implements FoldParser {

	/**
	 * Whether to scan for multi-line comments and make them foldable.
	 */
	private boolean foldableMultiLineComments;

	/**
	 * Whether to fold individual SQL statements (terminated by semicolons).
	 */
	private boolean foldableSqlStatements;

	/**
	 * SQL keywords that start a foldable block.
	 */
	private static final char[] KEYWORD_BEGIN = "begin".toCharArray();
	private static final char[] KEYWORD_CASE = "case".toCharArray();
	private static final char[] KEYWORD_IF = "if".toCharArray();
	private static final char[] KEYWORD_LOOP = "loop".toCharArray();
	private static final char[] KEYWORD_WHILE = "while".toCharArray();
	private static final char[] KEYWORD_REPEAT = "repeat".toCharArray();

	/**
	 * SQL statement keywords that can start a foldable statement.
	 */
	private static final char[] KEYWORD_SELECT = "select".toCharArray();
	private static final char[] KEYWORD_INSERT = "insert".toCharArray();
	private static final char[] KEYWORD_UPDATE = "update".toCharArray();
	private static final char[] KEYWORD_CREATE = "create".toCharArray();
	private static final char[] KEYWORD_DELETE = "delete".toCharArray();
	private static final char[] KEYWORD_WITH = "with".toCharArray();
	private static final char[] KEYWORD_MERGE = "merge".toCharArray();
	private static final char[] KEYWORD_ALTER = "alter".toCharArray();
	private static final char[] KEYWORD_DROP = "drop".toCharArray();
	private static final char[] KEYWORD_GRANT = "grant".toCharArray();
	private static final char[] KEYWORD_REVOKE = "revoke".toCharArray();
	private static final char[] KEYWORD_CALL = "call".toCharArray();

	/**
	 * SQL keywords that end a foldable block.
	 */
	private static final char[] KEYWORD_END = "end".toCharArray();
	private static final char[] KEYWORD_UNTIL = "until".toCharArray();

	/**
	 * Ending of a multi-line comment in SQL.
	 */
	private static final char[] SQL_MLC_END = "*/".toCharArray();

	/**
	 * Creates a fold parser that identifies foldable regions in SQL code, including
	 * multi-line comments and SQL statements.
	 */
	public SqlFoldParser() {
		this(true, true);
	}

	/**
	 * Constructor.
	 *
	 * @param foldableMultiLineComments Whether to scan for multi-line comments and
	 *                                  make them foldable.
	 */
	public SqlFoldParser(boolean foldableMultiLineComments) {
		this(foldableMultiLineComments, true);
	}

	/**
	 * Constructor.
	 *
	 * @param foldableMultiLineComments Whether to scan for multi-line comments and
	 *                                  make them foldable.
	 * @param foldableSqlStatements     Whether to fold individual SQL statements.
	 */
	public SqlFoldParser(boolean foldableMultiLineComments, boolean foldableSqlStatements) {
		this.foldableMultiLineComments = foldableMultiLineComments;
		this.foldableSqlStatements = foldableSqlStatements;
	}

	/**
	 * Returns whether multi-line comments are foldable with this parser.
	 *
	 * @return Whether multi-line comments are foldable.
	 * @see #setFoldableMultiLineComments(boolean)
	 */
	public boolean getFoldableMultiLineComments() {
		return foldableMultiLineComments;
	}

	/**
	 * Returns whether SQL statements are foldable with this parser.
	 *
	 * @return Whether SQL statements are foldable.
	 * @see #setFoldableSqlStatements(boolean)
	 */
	public boolean getFoldableSqlStatements() {
		return foldableSqlStatements;
	}

	@Override
	public List<Fold> getFolds(RSyntaxTextArea textArea) {

		List<Fold> folds = new ArrayList<>();

		Fold currentFold = null;
		int lineCount = textArea.getLineCount();
		boolean inMLC = false;
		int mlcStart = 0;
		int statementStart = -1;
		boolean inStatement = false;

		try {

			for (int line = 0; line < lineCount; line++) {

				Token t = textArea.getTokenListForLine(line);
				while (t != null && t.isPaintable()) {

					// Handle multi-line comments
					if (getFoldableMultiLineComments() && t.isComment()) {

						if (inMLC) {
							// If we found the end of an MLC that started
							// on a previous line...
							if (t.endsWith(SQL_MLC_END)) {
								int mlcEnd = t.getEndOffset() - 1;
								if (currentFold == null) {
									currentFold = new Fold(FoldType.COMMENT, textArea, mlcStart);
									currentFold.setEndOffset(mlcEnd);
									folds.add(currentFold);
									currentFold = null;
								} else {
									currentFold = currentFold.createChild(FoldType.COMMENT, mlcStart);
									currentFold.setEndOffset(mlcEnd);
									currentFold = currentFold.getParent();
								}
								inMLC = false;
								mlcStart = 0;
							}
							// Otherwise, this MLC is continuing on to yet
							// another line.
						} else {
							// If we're an MLC that ends on a later line...
							if (t.getType() != Token.COMMENT_EOL && !t.endsWith(SQL_MLC_END)) {
								inMLC = true;
								mlcStart = t.getOffset();
							}
						}

					}

					// Handle BEGIN keyword
					else if (isKeyword(t, KEYWORD_BEGIN)) {
						if (currentFold == null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
							folds.add(currentFold);
						} else {
							currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
						}

					}

					// Handle CASE keyword
					else if (isKeyword(t, KEYWORD_CASE)) {
						if (currentFold == null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
							folds.add(currentFold);
						} else {
							currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
						}
					}

					// Handle LOOP keyword
					else if (isKeyword(t, KEYWORD_LOOP)) {
						if (currentFold == null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
							folds.add(currentFold);
						} else {
							currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
						}
					}

					// Handle WHILE keyword
					else if (isKeyword(t, KEYWORD_WHILE)) {
						if (currentFold == null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
							folds.add(currentFold);
						} else {
							currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
						}
					}

					// Handle REPEAT keyword
					else if (isKeyword(t, KEYWORD_REPEAT)) {
						if (currentFold == null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
							folds.add(currentFold);
						} else {
							currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
						}
					}

					// Handle END keyword
					else if (isKeyword(t, KEYWORD_END)) {

						if (currentFold != null) {
							currentFold.setEndOffset(t.getEndOffset() - 1);
							Fold parentFold = currentFold.getParent();

							// Don't add fold markers for single-line blocks
							if (currentFold.isOnSingleLine()) {
								if (!currentFold.removeFromParent()) {
									folds.remove(folds.size() - 1);
								}
							}

							currentFold = parentFold;
						}

					}

					// Handle UNTIL keyword (ends REPEAT block)
					else if (isKeyword(t, KEYWORD_UNTIL)) {

						if (currentFold != null) {
							// Look ahead to find the semicolon that ends the UNTIL clause
							Token temp = t.getNextToken();
							int endOffset = t.getEndOffset() - 1;
							while (temp != null && temp.isPaintable()) {
								if (temp.isSingleChar(';')) {
									endOffset = temp.getEndOffset() - 1;
									break;
								}
								temp = temp.getNextToken();
							}

							currentFold.setEndOffset(endOffset);
							Fold parentFold = currentFold.getParent();

							// Don't add fold markers for single-line blocks
							if (currentFold.isOnSingleLine()) {
								if (!currentFold.removeFromParent()) {
									folds.remove(folds.size() - 1);
								}
							}

							currentFold = parentFold;
						}

					}

					// Handle SQL statement folding
					else if (getFoldableSqlStatements() && !inStatement && currentFold == null) {

						// Check if this is the start of a foldable SQL statement
						if (isStatementKeyword(t)) {
							inStatement = true;
							statementStart = t.getOffset();
						}

					}

					// Handle semicolon (end of SQL statement)
					else if (t.isSingleChar(';')) {

						if (inStatement && statementStart != -1) {
							// Create a fold for the statement
							Fold statementFold = new Fold(FoldType.CODE, textArea, statementStart);
							statementFold.setEndOffset(t.getEndOffset() - 1);

							// Only add if it spans multiple lines
							if (!statementFold.isOnSingleLine()) {
								folds.add(statementFold);
							}

							inStatement = false;
							statementStart = -1;
						}

					}

					t = t.getNextToken();

				}

			}

		} catch (BadLocationException ble) { // Should never happen
			ble.printStackTrace();
		}

		return folds;

	}

	/**
	 * Checks if a token matches a specific SQL keyword (case-insensitive).
	 *
	 * @param t       The token to check.
	 * @param keyword The keyword character array to match against.
	 * @return Whether the token matches the keyword.
	 */
	private boolean isKeyword(Token t, char[] keyword) {
		if (t.getType() != Token.RESERVED_WORD && t.getType() != Token.RESERVED_WORD_2) {
			return false;
		}
		return is(t, Token.RESERVED_WORD, keyword) || is(t, Token.RESERVED_WORD_2, keyword);
	}
	
	public boolean is(Token t, int type, char[] lexeme) {
		if (t.getType()==type && t.length()==lexeme.length) {
			for (int i=0; i<t.length(); i++) {
				if (Character.toLowerCase(t.getTextArray()[t.getTextOffset()+i])!=lexeme[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	/**
	 * Checks if a token is a SQL statement keyword that can start a foldable
	 * statement.
	 *
	 * @param t The token to check.
	 * @return Whether the token is a statement keyword (SELECT, INSERT, UPDATE,
	 *         DELETE, etc.).
	 */
	private boolean isStatementKeyword(Token t) {
		// @formatter:off
		return isKeyword(t, KEYWORD_SELECT) ||
		       isKeyword(t, KEYWORD_INSERT) ||
		       isKeyword(t, KEYWORD_UPDATE) ||
		       isKeyword(t, KEYWORD_CREATE) ||
		       isKeyword(t, KEYWORD_DELETE) ||
		       isKeyword(t, KEYWORD_WITH) ||
		       isKeyword(t, KEYWORD_MERGE) ||
		       isKeyword(t, KEYWORD_ALTER) ||
		       isKeyword(t, KEYWORD_DROP) ||
		       isKeyword(t, KEYWORD_GRANT) ||
		       isKeyword(t, KEYWORD_REVOKE) ||
		       isKeyword(t, KEYWORD_CALL)
		       ;
		// @formatter:on
	}

	/**
	 * Sets whether multi-line comments are foldable with this parser.
	 *
	 * @param foldable Whether multi-line comments are foldable.
	 * @see #getFoldableMultiLineComments()
	 */
	public void setFoldableMultiLineComments(boolean foldable) {
		this.foldableMultiLineComments = foldable;
	}

}
