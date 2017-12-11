/*
 * Copyright 2017 Jan Buchinger
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import net.janbuchinger.code.mishmash.ChaosFx;

public final class OnlineDB {

	private final static String key_tablename = "filesystem";
	private final static String key_filepath = "filepath";
	private final static String key_length = "length";
	private final static String key_modified = "modified";
	private final static String key_checksum = "checksum";
	private final static String key_version = "version";
	private final static String key_fileId = "fileid";
	private final static String key_parentId = "parentid";

	private final static String key_dbinfo_tablename = "dbInfo";
	private final static String key_dbinfo_dbid = "dbId";
	private final static String key_dbinfo_dbversion = "dbVersion";
	// private final static String key_dbinfo_lastSync = "synced";
	// private final static String key_dbinfo_syncInterval = "syncInterval";

	private final String dbPath;

	private String dbId;

	private int dbVersion;

	public static void initNewDB(File source, File destination) throws Exception {
		File[] parallelDbs = source.listFiles(new EditDBsFilenameFilter());

		String newId = ChaosFx.generateChaosString(36);

		// int nameNumber = 0;

		// File dbEdit = new File(source, ".fs.edit.db");
		if (parallelDbs.length > 0) {
			String[] ids = new String[parallelDbs.length];
			// nameNumber = ids.length;
			OnlineDB db;
			for (int i = 0; i < parallelDbs.length; i++) {
				db = new OnlineDB(parallelDbs[i]);
				ids[i] = db.getDbId();
			}
			boolean found, done = false;
			while (!done) {
				found = false;
				for (int i = 0; i < ids.length; i++) {
					if (ids[i].equals(newId)) {
						found = true;
						break;
					}
				}
				if (found) {
					newId = ChaosFx.generateChaosString(36);
				} else {
					done = true;
				}
			}
		}

		File dbOriginalFile = new File(destination, ".fs.db");

		new OnlineDB(dbOriginalFile, newId);
	}

	public final static File getCorrectFile(File[] editFiles, File original) {
		File correctFile = null;
		String searchId = "";
		try {
			searchId = new OnlineDB(original).getDbId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		OnlineDB db;
		for (File f : editFiles) {
			try {
				db = new OnlineDB(f);
				if (db.getDbId().equals(searchId)) {
					correctFile = f;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return correctFile;
	}

	public String getDbId() {
		return dbId;
	}

	public final int getDbVersion() {
		return dbVersion;
	}

	public OnlineDB(File target) throws Exception {
		this(target, null);
	}

	public OnlineDB(File target, String dbId) throws Exception {

		dbPath = target.getPath();

		if (dbId == null) {
			Connection c = connect();

			Statement st = null;
			ResultSet rs = null;

			String sql = "SELECT " + key_dbinfo_dbid + ", " + key_dbinfo_dbversion + " FROM "
					+ key_dbinfo_tablename + ";";

			try {
				st = c.createStatement();
				rs = st.executeQuery(sql);
				if (rs.next()) {
					this.dbId = rs.getString(1);
					dbVersion = rs.getInt(2);
				} else {
					throw new Exception("database inconsistent");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			disconnect(c, st, rs);
		} else {
			this.dbId = dbId;
			createDatabase();
		}

	}

	private void createDatabase() {
		Connection c = connect();

		Statement st = null;

		try {
			String sql = "CREATE TABLE IF NOT EXISTS " + key_tablename + " (" + key_fileId
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + key_parentId + " INTEGER DEFAULT 0, "
					+ key_filepath + " STRING NOT NULL, " + key_length + " INTEGER NOT NULL, " + key_modified
					+ " INTEGER NOT NULL, " + key_checksum + " STRING NOT NULL, " + key_version
					+ " INTEGER DEFAULT 0, CONSTRAINT name_unique UNIQUE (" + key_filepath + ", "
					+ key_version + "));";
			st = c.createStatement();
			st.execute(sql);
			sql = "CREATE TABLE IF NOT EXISTS " + key_dbinfo_tablename + " (" + key_dbinfo_dbid
					+ " STRING PRIMARY KEY, " + key_dbinfo_dbversion + " INTEGER);";
			st.execute(sql);
			sql = "INSERT INTO " + key_dbinfo_tablename + "(" + key_dbinfo_dbid + ", " + key_dbinfo_dbversion
					+ ") VALUES('" + dbId + "', 0);";
			st.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		disconnect(c, st);
	}

	private Connection connect() {
		String url = "jdbc:sqlite:" + dbPath;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}

	// private Connection connect(String dbPath) {
	// // SQLite connection string
	// String url = "jdbc:sqlite:" + dbPath;
	// Connection conn = null;
	// try {
	// conn = DriverManager.getConnection(url);
	// } catch (SQLException e) {
	// System.out.println(e.getMessage());
	// }
	// return conn;
	// }

	private final void disconnect(Connection c, Statement st) {
		disconnect(c, st, null);
	}

	private final void disconnect(Connection c, Statement st, ResultSet rs) {
		disconnect(c, st, rs, null);
	}

	private void disconnect(Connection c, Statement st1, ResultSet rs, Statement st2) {
		if (rs != null)
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		if (st1 != null)
			try {
				st1.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		if (st2 != null)
			try {
				st2.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		if (c != null)
			try {
				c.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	public void updateFile(RelativeFile dataFile) {
		String sql = "UPDATE " + key_tablename + " SET " + key_length + " = ?, " + key_modified + " = ?, "
				+ key_checksum + " = ? WHERE " + key_filepath + " = ?;";

		Connection c = connect();
		PreparedStatement st = null;
		try {
			st = c.prepareStatement(sql);

			st.setLong(1, dataFile.getLength());
			st.setLong(2, dataFile.getModified());
			st.setString(3, dataFile.getChecksum());
			st.setString(4, dataFile.getRelativePath());

			st.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		disconnect(c, st);
	}

	public void removeFileByPath(String relativePath) {
		String sql = "DELETE FROM " + key_tablename + " WHERE " + key_filepath + " = ?;";

		Connection c = connect();
		PreparedStatement st = null;

		try {
			st = c.prepareStatement(sql);

			st.setString(1, relativePath);

			st.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		disconnect(c, st);
	}

	public RelativeFile getFileByPath(String filePath) {
		String sql = "SELECT " + key_length + ", " + key_modified + ", " + key_checksum + " FROM "
				+ key_tablename + " WHERE " + key_filepath + " = ?;";
		RelativeFile df = null;
		String checksum;
		long length, modified;

		Connection c = connect();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = c.prepareStatement(sql);

			st.setString(1, filePath);

			rs = st.executeQuery();

			if (rs.next()) {
				length = rs.getLong(1);
				modified = rs.getLong(2);
				checksum = rs.getString(3);
				df = new RelativeFile(filePath, length, modified, checksum);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		disconnect(c, st, rs);

		return df;
	}

	public Vector<RelativeFile> listAll() {

		Vector<RelativeFile> data = new Vector<RelativeFile>();

		String sql = "SELECT " + key_filepath + ", " + key_length + ", " + key_modified + ", " + key_checksum
				+ " FROM " + key_tablename + " WHERE " + key_version + "=0;";

		ResultSet rs = null;
		Connection c = connect();
		Statement st = null;

		String filepath, checksum;
		long modified;
		long length;

		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
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

		disconnect(c, st, rs);

		return data;
	}

	public synchronized void add(RelativeFile dataFile) {
		String sql = "INSERT INTO " + key_tablename + "(" + key_filepath + ", " + key_length + ", "
				+ key_modified + ", " + key_checksum + ") VALUES (?, ?, ?, ?);";
		Connection c = connect();
		PreparedStatement st = null;

		try {
			st = c.prepareStatement(sql);

			st.setString(1, dataFile.getRelativePath());
			st.setLong(2, dataFile.getLength());
			st.setLong(3, dataFile.getModified());
			st.setString(4, dataFile.getChecksum());

			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		disconnect(c, st);
	}

	public final long getFileSystemLength() {
		String sql = "SELECT SUM(" + key_length + ") FROM " + key_tablename + ";";
		long l = 0;
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			if (rs.next()) {
				l = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		disconnect(c, st, rs);
		return l;
	}

	public final int getFileCount() {
		String sql = "SELECT COUNT(" + key_filepath + ") FROM " + key_tablename + ";";
		int l = 0;
		Connection c = connect();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = c.createStatement();
			rs = st.executeQuery(sql);
			if (rs.next()) {
				l = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		disconnect(c, st, rs);
		return l;
	}

	public void incrementVersion() {
		dbVersion++;
		String sql = "UPDATE " + key_dbinfo_tablename + " SET " + key_dbinfo_dbversion + "=" + dbVersion + ";";
		Connection c = connect();
		Statement st = null;

		try {
			st = c.createStatement();
			st.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
