/*
 * Copyright 2017-2018 Jan Buchinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.janbuchinger.code.fssync.sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.mishmash.ChaosFx;

/**
 * This class is the core database class of FSSync.
 * <p>
 * It manages two tables: "filesystem" and "dbInfo"
 * <p>
 * The "filsystem" table contains the list of files in the state of the last
 * synchronization.
 * <ul>
 * <li><b>fileid</b>: The unique id of the file inside the database.
 * <li><b>filepath</b>: The relative file path.
 * <li><b>length</b>: The file length.
 * <li><b>modified</b>: The last modified time of the file.
 * <li><b>checksum</b>: The SHA384 checksum of the file.
 * <li><b>version</b>: The file version, <b>not used yet</b>.
 * <li><b>parentid</b>: The current version file id, <b>not used yet</b>.
 * </ul>
 * <p>
 * The "dbInfo" table always contains one record.
 * <ul>
 * <li><b>dbId</b>: a random base 36 number to uniquely identify this database.
 * <li><b>dbVersion</b>: a counter that is incremented after each
 * synchronization process.
 * <li><b>dbBuild</b>: The structural version of the database, currently 1.
 * </ul>
 * 
 * 
 * @author Jan Buchinger
 *
 */
public final class OnlineDB {
	/**
	 * The structural database version
	 */
	private final static int DB_BUILD = 1;

	/**
	 * key for table "filesystem".
	 * 
	 * This table saves the file system state of synchronization.
	 */
	private final static String tab_filesystem = "filesystem";
	/**
	 * key for field "fileid" (filesystem).
	 * 
	 * The unique id of the file in the database.
	 */
	private final static String fs_fileId = "fileid";
	/**
	 * key for field "filepath" (filesystem).
	 * 
	 * The relative path of the file.
	 */
	private final static String fs_filepath = "filepath";
	/**
	 * key for field "length" (filesystem).
	 * 
	 * The file length in bytes.
	 */
	private final static String fs_length = "length";
	/**
	 * key for field "modified" (filesystem).
	 * 
	 * The last modified time in milliseconds.
	 */
	private final static String fs_modified = "modified";
	/**
	 * key for field "checksum" (filesystem).
	 * 
	 * The SHA384 checksum of the file
	 */
	private final static String fs_checksum = "checksum";
	/**
	 * key for field "version" (filesystem).
	 * 
	 * The file version. 0 for current version. <b>Not uset yet.</b>
	 */
	private final static String fs_version = "version";
	/**
	 * key for field "parentid" (filesystem).
	 * 
	 * The parent file id. <b>Not uset yet.</b>
	 */
	private final static String fs_parentId = "parentid";

	/**
	 * key for table "dbInfo".
	 * 
	 * This table always contains one record. It stores the table meta data.
	 */
	private final static String tab_dbInfo = "dbInfo";
	/**
	 * key for field "dbId" (dbInfo).
	 * 
	 * The unique database id consisting of a base 36 number with 36 digits.
	 */
	private final static String info_dbId = "dbId";
	/**
	 * key for field "dbVersion" (dbInfo).
	 * 
	 * The current version of the database. incremented after each synchronization
	 * process.
	 */
	private final static String info_dbVersion = "dbVersion";
	/**
	 * key for field "dbBuild" (dbInfo).
	 * 
	 * The current structural version of the database.
	 */
	private final static String info_dbBuild = "dbBuild";

	/**
	 * The path to the database file.
	 */
	private final String dbPath;
	/**
	 * The unique database id.
	 */
	private String dbId;
	/**
	 * The current database version.
	 * <p>
	 * The version is incremented after each synchronization process that had
	 * changes for the file system.
	 */
	private int dbVersion;
	/**
	 * The current structural database version.
	 */
	private int dbBuild;

	/**
	 * Initializes a new database.
	 * 
	 * @param source
	 *            The <code>Operation</code>s source directory.
	 * @param destination
	 *            The <code>Operation</code>s target directory.
	 * 
	 * @throws Exception
	 *             if anything bad happens
	 */
	public static void initNewDB(File source, File destination) throws Exception {
		// the original database file
		File dbOriginalFile = new File(destination, ".fs.db");
		// the editable database file
		File dbEditFile = nextEditableDBFile(source);

		if (dbOriginalFile.exists()) {
			throw new IllegalArgumentException("database file already exists.");
		}

		// load the parallel database files
		File[] parallelDbs = source.listFiles(new EditDBsFilenameFilter());

		// generate new id
		String newId = ChaosFx.generateChaosString(36);

		// check if any parallel Operations .fs.edit.db has the same id as the newly
		// generated id. probably unnecessary.
		if (parallelDbs != null) {
			if (parallelDbs.length > 0) {
				// initialize the parallel databases ids array
				String[] ids = new String[parallelDbs.length];
				OnlineDB db;
				// load the parallel databases ids
				for (int i = 0; i < parallelDbs.length; i++) {
					db = new OnlineDB(parallelDbs[i]);
					ids[i] = db.getDbId();
				}
				// indicator for conflicting id
				boolean found;
				// loop as long as there are conflicting ids found
				boolean done = false;
				while (!done) {
					// assume no conflict
					found = false;
					// loop through the parallel ids
					for (int i = 0; i < ids.length; i++) {
						// if the newly generated id was found
						if (ids[i].equals(newId)) {
							// then indicate that a new id should be generated
							found = true;
							break;
						}
					}
					// if a conflict was found
					if (found) {
						// then generate a new id
						newId = ChaosFx.generateChaosString(36);
					} else {
						// else exit the loop
						done = true;
					}
				}
			}
		}
		// create the new database
		new OnlineDB(dbEditFile, newId);
		// synchronize the new database file to original
		if (dbEditFile.exists()) {
			FileUtils.copyFile(dbEditFile, dbOriginalFile);
		}
	}

	/**
	 * Gets the editable database file corresponding to the target database file.
	 * 
	 * @param operation
	 *            the current <code>Operation</code>.
	 * 
	 * @return the correct file to edit or <code>null</code> if there is no
	 *         corresponding file yet.
	 */
	public final static File getEditableDBFile(Operation operation) {
		return getEditableDBFile(operation.getSource(), operation.getDbOriginal());
	}

	/**
	 * Gets the editable database file corresponding to the target database file.
	 * 
	 * @param sourceDir
	 *            the current <code>Operation</code>s source directory.
	 * @param originalDB
	 *            the original database file in the target directory of the current
	 *            <code>Operation</code>.
	 * 
	 * @return the correct file to edit or <code>null</code> if there is no
	 *         corresponding file yet.
	 */
	public final static File getEditableDBFile(File sourceDir, File originalDB) {
		// load the parallel database files
		File[] editFiles = sourceDir.listFiles(new EditDBsFilenameFilter());
		// assume that there is no corresponding file yet
		File correctFile = null;
		// the temporary database file to get the original database id
		File tmpOriginalDB = new File(sourceDir, ".fs.tmp.db");
		try {
			// copy the temporary file back into the source directory to not open it in the
			// target directory
			FileUtils.copyFile(originalDB, tmpOriginalDB);
			// the original databases id to be searched
			String searchId = "";

			try {
				// get the original databases id
				searchId = new OnlineDB(tmpOriginalDB).getDbId();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			// the current database to be checked
			OnlineDB db;
			// loop through the parallel database files
			for (File f : editFiles) {
				try {
					// try initializing the current database to be checked
					db = new OnlineDB(f);
					// if the current databases id equals the searched databases id
					if (db.getDbId().equals(searchId)) {
						// then remember the correct file
						correctFile = f;
						// and break
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			// finally delete the temporary database file
			if (tmpOriginalDB != null) {
				if (tmpOriginalDB.exists()) {
					if (!tmpOriginalDB.delete()) {
						System.err.println("Could not delete the temporary file");
					}
				}
			}
		}
		// return the correct file or null if it wasn't set
		return correctFile;

	}

	/**
	 * Gets the next non existing editable database file.
	 * 
	 * @param source
	 *            the source directory of the <code>Operation</code>.
	 * 
	 * @return the next non existing editable database file.
	 */
	public final static File nextEditableDBFile(File source) {
		File dbEditFile = new File(source, ".fs.edit.db");
		int c = 1;
		while (dbEditFile.exists()) {
			dbEditFile = new File(source, ".fs.edit" + (c++) + ".db");
		}
		return dbEditFile;
	}

	/**
	 * Gets this databases unique id.
	 * 
	 * @return the base 36 id.
	 */
	public String getDbId() {
		return dbId;
	}

	/**
	 * Gets the database version.
	 * <p>
	 * The version is incremented after each synchronization process that had
	 * changes for the file system.
	 * 
	 * @return the database version.
	 */
	public final int getDbVersion() {
		return dbVersion;
	}

	/**
	 * This constructor is used for existing databases.
	 * 
	 * @param target
	 *            The database file.
	 * @throws Exception
	 *             If the database file does not exist.
	 */
	public OnlineDB(File target) throws Exception {
		this(target, null);
	}

	/**
	 * This constructor is used to potentially create a new database.
	 * 
	 * @param database
	 *            the database file.
	 * @param dbId
	 *            the new database id or <code>null</code> to open an existing
	 *            database.
	 * 
	 * @throws Exception
	 *             a new file is being created that already exists or if an existing
	 *             file is being opened that does not exist.
	 */
	private OnlineDB(File database, String dbId) throws Exception {
		// the database path String
		dbPath = database.getPath();
		// if the database id is null
		if (dbId == null) {
			// then open existing database
			if (!database.exists()) {
				// the file must exist
				throw new FileNotFoundException("Database file not found");
			}
			// initialize build version management if not available
			initVersionManagement();
			// initialize database meta data
			initDBInfo();
			// upgrade structure if necessary
			upgrade();
		} else {
			if (database.exists()) {
				throw new IllegalArgumentException("Database file already exists");
			}
			this.dbId = dbId;
			createDatabase();
		}
	}

	/**
	 * This method checks if the database has the meta data field "dbBuild"
	 * (structural database version).
	 * <p>
	 * If the field does not exist it is added and initialized with version number
	 * 1.
	 */
	private void initVersionManagement() {
		// query tab_dbInfo fields
		String sql = "PRAGMA table_info(" + tab_dbInfo + ");";
		// connect and prepare
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;

		try {
			// create statement
			st = c.createStatement();
			// query
			rs = st.executeQuery(sql);
			// assume the field as not available
			boolean versioningAvailable = false;
			// loop through fields
			while (rs.next()) {
				// if the field info_dbBuild is found
				if (rs.getString(2).equals(info_dbBuild)) {
					// then version management is available
					versioningAvailable = true;
					// break
					break;
				}
			}
			// add info_dbBuild if not available and initialize it with version number 1
			if (!versioningAvailable) {
				sql = "ALTER TABLE " + tab_dbInfo + " ADD COLUMN " + info_dbBuild + " INTEGER DEFAULT 1;";
				st.execute(sql);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// disconnect resources
		disconnect(c, st, rs);
	}

	/**
	 * Initializes the table meta data.
	 * 
	 * @throws Exception
	 *             if the meta data record is missing or if an SQLException occurs.
	 */
	private void initDBInfo() throws Exception {
		// query the meta data record
		String sql = "SELECT " + info_dbId + ", " + info_dbVersion + ", " + info_dbBuild + " FROM "
				+ tab_dbInfo + ";";
		// connect and prepare
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;

		try {
			// create statement
			st = c.createStatement();
			// query
			rs = st.executeQuery(sql);
			// if the meta data record is available
			if (rs.next()) {
				// then initialize the meta data
				this.dbId = rs.getString(1);
				dbVersion = rs.getInt(2);
				dbBuild = rs.getInt(3);
			} else {
				// the meta data record is missing
				throw new Exception("database inconsistent");
			}
		} catch (SQLException e) {
			// a field might be missing...
			throw new Exception("database inconsistent");
		}
		// release resources
		disconnect(c, st, rs);
	}

	/**
	 * Upgrades the database structure if necessary.
	 */
	private void upgrade() {
		if (dbBuild != DB_BUILD) {
			// // for future build version 2
			// if (dbBuild == 1) {
			// // upgrade to build version 2
			// }
		}
	}

	/**
	 * creates the new database.
	 */
	private void createDatabase() {
		// connect
		Connection c = connect();
		Statement st = null;
		try {
			// create table tab_filesystem
			String sql = "CREATE TABLE IF NOT EXISTS " + tab_filesystem + " (" + fs_fileId
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + fs_parentId + " INTEGER DEFAULT 0, "
					+ fs_filepath + " STRING NOT NULL, " + fs_length + " INTEGER NOT NULL, " + fs_modified
					+ " INTEGER NOT NULL, " + fs_checksum + " STRING NOT NULL, " + fs_version
					+ " INTEGER DEFAULT 0, CONSTRAINT name_unique UNIQUE (" + fs_filepath + ", " + fs_version
					+ "));";
			st = c.createStatement();
			st.execute(sql);
			// create table tab_dbInfo
			sql = "CREATE TABLE IF NOT EXISTS " + tab_dbInfo + " (" + info_dbId + " STRING PRIMARY KEY, "
					+ info_dbVersion + " INTEGER, " + info_dbBuild + " INTEGER);";
			st.execute(sql);
			// initialize table meta data
			sql = "INSERT INTO " + tab_dbInfo + "(" + info_dbId + ", " + info_dbVersion + ", " + info_dbBuild
					+ ") VALUES('" + dbId + "', 0, " + DB_BUILD + ");";
			st.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st);
	}

	/**
	 * Initializes a new SQL <code>Connection</code> to the database.
	 * 
	 * @return a new SQL <code>Connection</code> to the database or
	 *         <code>null</code> if an SQLException occurred.
	 */
	private Connection connect() {
		String url = "jdbc:sqlite:" + dbPath;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * Closes the specified resources.
	 * 
	 * @param c
	 *            The <code>Connection</code> to close.
	 * @param st
	 *            The <code>Statement</code> to close.
	 */
	private final void disconnect(Connection c, Statement st) {
		disconnect(c, st, null);
	}

	/**
	 * Closes the specified resources.
	 * 
	 * @param c
	 *            The <code>Connection</code> to close.
	 * @param st
	 *            The <code>Statement</code> to close.
	 * @param rs
	 *            The <code>ResultSet</code> to close.
	 */
	private final void disconnect(Connection c, Statement st, ResultSet rs) {
		disconnect(c, st, rs, null);
	}

	/**
	 * Closes the specified resources.
	 * 
	 * @param c
	 *            The <code>Connection</code> to close.
	 * @param st
	 *            The <code>Statement</code> to close.
	 * @param rs
	 *            The <code>ResultSet</code> to close.
	 * @param st2
	 *            The second <code>Statement</code> to close.
	 */
	private void disconnect(Connection c, Statement st1, ResultSet rs, Statement st2) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (st1 != null) {
			try {
				st1.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (st2 != null) {
			try {
				st2.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (c != null) {
			try {
				c.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Lists all current versions contained in the file system table.
	 * 
	 * @return all current versions contained in the file system table.
	 */
	public Vector<RelativeFile> listAll() {
		// select all current versions from file system
		String sql = "SELECT " + fs_filepath + ", " + fs_length + ", " + fs_modified + ", " + fs_checksum
				+ " FROM " + tab_filesystem + " WHERE " + fs_version + " = 0;";
		// connect and prepare
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;
		// temporary values
		String filepath, checksum;
		long modified;
		long length;
		// initialize the list of files to return
		Vector<RelativeFile> data = new Vector<RelativeFile>();

		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			// fill the list of files to return
			while (rs.next()) {
				filepath = rs.getString(1);
				length = rs.getLong(2);
				modified = rs.getLong(3);
				checksum = rs.getString(4);
				data.add(new RelativeFile(filepath, length, modified, checksum));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st, rs);
		// return the list of files
		return data;
	}

	/**
	 * Introduces a file to the file system table.
	 * 
	 * @param relativePath
	 *            The relative path to the file
	 * @param length
	 *            The file length in bytes.
	 * @param modified
	 *            The current modification time in milliseconds.
	 * @param checksum
	 *            The SHA384 checksum of the file.
	 */
	public synchronized void add(String relativePath, long length, long modified, String checksum) {
		String sql = "INSERT INTO " + tab_filesystem + "(" + fs_filepath + ", " + fs_length + ", "
				+ fs_modified + ", " + fs_checksum + ") VALUES (?, ?, ?, ?);";
		// connect
		Connection c = connect();
		PreparedStatement st = null;

		try {
			// prepare
			st = c.prepareStatement(sql);
			// set prepared data
			st.setString(1, relativePath);
			st.setLong(2, length);
			st.setLong(3, modified);
			st.setString(4, checksum);
			// execute prepared statement
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st);
	}

	/**
	 * Updates a file record in the file system table.
	 * 
	 * @param relativePath
	 *            The path to the file to be updated.
	 * @param length
	 *            The new file length in bytes.
	 * @param modified
	 *            The new modification time in milliseconds.
	 * @param checksum
	 *            The new checksum.
	 */
	public void updateFile(String relativePath, long length, long modified, String checksum) {
		// update the file of relativePath
		String sql = "UPDATE " + tab_filesystem + " SET " + fs_length + " = ?, " + fs_modified + " = ?, "
				+ fs_checksum + " = ? WHERE " + fs_filepath + " = ?;";
		// connect
		Connection c = connect();
		PreparedStatement st = null;
		try {
			// prepare
			st = c.prepareStatement(sql);
			// set prepared data
			st.setLong(1, length);
			st.setLong(2, modified);
			st.setString(3, checksum);
			st.setString(4, relativePath);
			// execute prepared statement
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st);
	}

	/**
	 * Deletes a file record from the file system table.
	 * 
	 * @param relativePath
	 *            The relative path to the file to be removed.
	 */
	public void removeFileByPath(String relativePath) {
		// delete file
		String sql = "DELETE FROM " + tab_filesystem + " WHERE " + fs_filepath + " = ?;";
		// connect
		Connection c = connect();
		PreparedStatement st = null;

		try {
			// prepare
			st = c.prepareStatement(sql);
			// set prepared data
			st.setString(1, relativePath);
			// execute delete command
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st);
	}

	/**
	 * Gets a file from the file system table.
	 * 
	 * @param filePath
	 *            The relative path to the file to get.
	 * 
	 * @return The requested file or <code>null</code> if the file was not found.
	 */
	public RelativeFile getFileByPath(String filePath) {
		// Select the file by path
		String sql = "SELECT " + fs_length + ", " + fs_modified + ", " + fs_checksum + " FROM "
				+ tab_filesystem + " WHERE " + fs_filepath + " = ?;";
		// assume file not found
		RelativeFile df = null;
		// temporary variables
		String checksum;
		long length, modified;
		// connect
		Connection c = connect();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			// prepare
			st = c.prepareStatement(sql);
			// set prepared data
			st.setString(1, filePath);
			// execute query
			rs = st.executeQuery();
			// if the file was found
			if (rs.next()) {
				// initialize the file
				length = rs.getLong(1);
				modified = rs.getLong(2);
				checksum = rs.getString(3);
				df = new RelativeFile(filePath, length, modified, checksum);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st, rs);
		// return the file if found
		return df;
	}

	/**
	 * Gets the total file length of all indexed files in bytes.
	 * 
	 * @return the total file length of all indexed files in bytes.
	 */
	public final long getTotalFileLength() {
		// select the sum of all file lengths
		String sql = "SELECT SUM(" + fs_length + ") FROM " + tab_filesystem + ";";
		// assume empty
		long totalLength = 0;
		// connect
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;

		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			// if there is an answer
			if (rs.next()) {
				// get the sum
				totalLength = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st, rs);
		// return the total file length
		return totalLength;
	}

	/**
	 * Gets the total number of files indexed in the database.
	 * 
	 * @return the total number of files indexed in the database.
	 */
	public final int getFileCount() {
		// select count all files
		String sql = "SELECT COUNT(" + fs_filepath + ") FROM " + tab_filesystem + ";";
		// assume empty
		int totalFilesCount = 0;
		// connect
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			// if there is an answer
			if (rs.next()) {
				// get the total files count
				totalFilesCount = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st, rs);
		// return files count
		return totalFilesCount;
	}

	/**
	 * Gets the current emptiness of the file system table.
	 * 
	 * @return <code>true</code> if there are no files in the file system table.
	 */
	public final boolean isEmpty() {
		// select everything, limit 1
		String sql = "SELECT * FROM " + tab_filesystem + " LIMIT 1;";
		// connect
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;
		// assume empty
		boolean isEmpty = true;
		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			// if there is a record
			if (rs.next()) {
				// then empty is false
				isEmpty = false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st, rs);
		// return emptiness state
		return isEmpty;
	}

	/**
	 * Increments the database version counter by 1
	 */
	public void incrementVersion() {
		// increment current version
		dbVersion++;
		// update the meta data
		String sql = "UPDATE " + tab_dbInfo + " SET " + info_dbVersion + "=" + dbVersion + ";";
		// connect
		Connection c = connect();
		Statement st = null;

		try {
			st = c.createStatement();
			// execute update
			st.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// release resources
		disconnect(c, st);
	}
}
// before comments: 558 lines, after comments: 920 lines