/** Copyleft (C) 2016 by Aaron Digulla. Use as you wish. This copyright notice can be removed. */

package bug507401;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Check whether a file is on a network drive.
 * 
 * <p>
 * Based on ideas from <a href=
 * "https://sites.google.com/site/baohuagu/how-to-detect-if-a-drive-is-network-drive">How
 * to detect if a drive is network drive?</a> and <a href=
 * "http://stackoverflow.com/questions/9163707/java-how-to-determine-the-type-of-drive-a-file-is-located-on">Java:
 * how to determine the type of drive a file is located on?</a>
 * 
 * @author Aaron Digulla
 */
public class DangerousPathChecker {

	private final static boolean IS_WINDOWS = isWindows();

	// Along the lines of org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS
	private static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.startsWith("windows");
	}

	public boolean isDangerous(File file) {
		if (!IS_WINDOWS) {
			return false;
		}

		// Make sure the file is absolute
		file = file.getAbsoluteFile();
		String path = file.getPath();
		// System.out.println("Checking [" + path + "]");

		// UNC paths are dangerous
		if (path.startsWith("//") || path.startsWith("\\\\")) {
			// We might want to check for \\localhost or \\127.0.0.1 which would be OK, too
			return true;
		}

		String driveLetter = path.substring(0, 1);
		String colon = path.substring(1, 2);
		if (!":".equals(colon)) {
			throw new IllegalArgumentException("Expected 'X:': " + path);
		}

		return isNetworkDrive(driveLetter);
	}

	/**
	 * Use the command <code>net</code> to determine what this drive is.
	 * <code>net use</code> will return an error for anything which isn't a share.
	 * 
	 * <p>
	 * Another option would be <code>fsinfo</code> but my gut feeling is that
	 * <code>net</code> should be available and on the path on every installation of
	 * Windows.
	 */
	private boolean isNetworkDrive(String driveLetter) {
		List<String> cmd = Arrays.asList("cmd", "/c", "net", "use", driveLetter + ":");

		try {
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();

			p.getOutputStream().close();

			StringBuilder consoleOutput = new StringBuilder();

			String line;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				while ((line = in.readLine()) != null) {
					consoleOutput.append(line).append("\r\n");
				}
			}

			int rc = p.waitFor();
			// System.out.println(consoleOutput);
			// System.out.println("rc=" + rc);
			return rc == 0;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to run 'net use' on " + driveLetter, e);
		}
	}

	public static void main(String[] args) {
		DangerousPathChecker checker = new DangerousPathChecker();

		checker.check("");
		checker.check("//server/path");
		checker.check("\\\\server\\path");
		checker.check("c:");
		checker.check("c:/windows");
		checker.check("c:\\windows");
		// On my computer, Z: is a mapped network drive
		checker.check("z:");
		checker.check("z:/folder");
		checker.check("z:\\folder");
	}

	private void check(String path) {
		File file = new File(path).getAbsoluteFile();
		System.out.println(isDangerous(file) + " - " + file);
	}
}
