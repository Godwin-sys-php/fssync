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
package net.janbuchinger.code.fssync.sync.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import net.janbuchinger.code.fssync.FSSync;
import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.fssync.Settings;
import net.janbuchinger.code.fssync.sync.OperationSummary;
import net.janbuchinger.code.mishmash.ui.UIFx;
import net.janbuchinger.code.mishmash.ui.models.StringListModel;

@SuppressWarnings("serial")
public final class SynchronizationProcessDialog extends JDialog implements ActionListener {

	private final JLabel processStatus;
	private final JProgressBar progressBar;
	private final JList<String> statusUpdate;
	private final StringListModel lmStatusUpdate;
	private final JButton btCancel;
	private final JMenuItem miSaveLog;
	private boolean finished;
	private final Settings settings;
	private Exception exception;

	private final Vector<String> log;
	private final boolean verbose;

	private final SwingWorker<Void, Void> sp;

	private ProgressBarCountDownThread pbcdt;

	private final SimpleDateFormat sdfLog;

	private boolean saveLog;

	public SynchronizationProcessDialog(String title, JFrame d, Settings settings,
			SwingWorker<Void, Void> sp) {
		super(d, title, true);

		this.verbose = settings.isVerbose();

		log = new Vector<String>();

		saveLog = settings.isAlwaysSaveLog();

		this.settings = settings;

		this.sp = sp;

		finished = false;

		exception = null;

		sdfLog = new SimpleDateFormat("yyyy-MM-dd-HHmmss");

		miSaveLog = new JMenuItem("Log Speichern");
		miSaveLog.addActionListener(this);
		miSaveLog.setEnabled(false);

		if (!saveLog) {
			JMenu menu = new JMenu("Logdatei");
			JMenuBar menuBar = new JMenuBar();
			menu.add(miSaveLog);
			menuBar.add(menu);
			setJMenuBar(menuBar);
		}

		processStatus = new JLabel();
		progressBar = new JProgressBar(1, 100);
		JPanel pnProgressStatus = new JPanel(new GridLayout(2, 1));
		pnProgressStatus.add(processStatus);
		pnProgressStatus.add(progressBar);

		lmStatusUpdate = new StringListModel();
		statusUpdate = new JList<String>(lmStatusUpdate);

		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);
		JPanel pnBtCancel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnBtCancel.add(btCancel);

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.setPreferredSize(new Dimension(350, 400));
		pnContent.add(pnProgressStatus, BorderLayout.NORTH);
		JScrollPane scp = new JScrollPane(statusUpdate);
		scp.getHorizontalScrollBar().setUnitIncrement(15);
		pnContent.add(scp, BorderLayout.CENTER);
		pnContent.add(pnBtCancel, BorderLayout.SOUTH);

		setContentPane(pnContent);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		UIFx.sizeAndCenter(this, 0.8, 0.8);
	}

	@Override
	public synchronized final void actionPerformed(ActionEvent e) {
		if (e.getSource() == btCancel) {
			if (!finished) {
				sp.cancel(false);
				btCancel.setEnabled(false);
				setProcessStatusText("Prozess wird Abgebrochen...");
				addStatus("Prozess wird so bald wie möglich Beendet, bitte um einen Moment Geduld...");
			} else {
				setVisible(false);
			}
		} else if (e.getSource() == miSaveLog) {
			saveLog();
		}
	}

	public synchronized final void saveLog() {
		if (finished) {
			File file = new File(settings.getLogFilesDir(),
					"FSSync-Log-" + sdfLog.format(System.currentTimeMillis()) + ".txt");
			StringBuilder data = new StringBuilder();
			for (String status : log) {
				data.append(status.concat("\r\n"));
			}
			try {
				FileUtils.writeStringToFile(file, data.toString(), Charset.defaultCharset());
				addStatus("Logdatei Gespeichert: " + file.getPath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			if (!saveLog) {
				addStatus("Logdatei wird später gespeichert");
				saveLog = true;
			}
		}
	}

	private void saveErrorLog() {
		if (exception != null) {
			File file = new File(settings.getLogFilesDir(),
					"FSSync-Error-Log-" + sdfLog.format(System.currentTimeMillis()) + ".txt");
			FSSync.saveErrorLog(file, exception);
		}
	}

	public synchronized final void setProcessStatusText(String text) {
		processStatus.setText(text);
	}

	public synchronized final void setProgress(int progress) {
		if (!finished) {
			progressBar.setValue(progress);
		}
	}

	public synchronized final void setFinished(String finalStatus) {
		setDeterminate(true);
		progressBar.setValue(100);
		addStatus(finalStatus);
		finished = true;
		if (saveLog) {
			saveLog();
		}
		saveErrorLog();
		abortCountDown();
		miSaveLog.setEnabled(true);
		btCancel.setText("Schliessen");
		btCancel.setEnabled(true);
		progressBar.setEnabled(false);
		processStatus.setText("Fertig");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	public synchronized final void setCancelled(String finalStatus) {
		setDeterminate(true);
		addStatus(finalStatus);
		finished = true;
		if (saveLog) {
			saveLog();
		}
		saveErrorLog();
		abortCountDown();
		miSaveLog.setEnabled(true);
		btCancel.setText("Schliessen");
		btCancel.setEnabled(true);
		progressBar.setEnabled(false);
		processStatus.setText("Abgebrochen");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	public synchronized final void setDeterminate(boolean flag) {
		if (!finished) {
			progressBar.setIndeterminate(!flag);
		}
	}

	public static final int foreign_integrate = 0;
	public static final int foreign_restore = 1;
	public static final int foreign_ignore = 2;
	public static final int foreign_cancelled = 3;

	public synchronized final int requestForeignFileHandling() {
		Color green = Color.GREEN.darker();
		Color red = Color.RED.darker();
		JRadioButton btIntegrate = new JRadioButton("Änderungen Holen (Bidirektional Synchronisieren)");
		btIntegrate.setForeground(red);
		JLabel lbIntegrate = new JLabel("<html>Klicken Sie hier wenn Sie ausnahmsweise neue Dateien in das"
				+ " Zielverzeichnis<br>kopiert oder Dateien im Zielverzeichnis geändert haben.</html>");
		lbIntegrate.setForeground(red);
		JRadioButton btIgnore = new JRadioButton("Änderungen Ignorieren");
		btIgnore.setForeground(green);
		JLabel lbIgnore = new JLabel("<html>Belassen Sie die Auswahl hier wenn Sie nicht wissen wieso"
				+ " Änderungen<br>im Zielordner aufgetreten sind.</html>");
		lbIgnore.setForeground(green);
		JRadioButton btRestore = new JRadioButton("Änderungen Löschen");
		JLabel lbRestore = new JLabel(
				"<html>Klicken Sie hier wenn Sie sicher gehen möchten dass fremde Änderungen"
						+ " im<br>Zielordner rückgängig gemacht werden und unbekannte Dateien gelöscht"
						+ " werden.</html>");
		lbRestore.setForeground(red);
		btRestore.setForeground(red);
		ButtonGroup bg = new ButtonGroup();
		bg.add(btIntegrate);
		bg.add(btIgnore);
		bg.add(btRestore);
		btIgnore.setSelected(true);
		JPanel pnButtons = new JPanel(new GridBagLayout());
		GridBagConstraints c = UIFx.initGridBagConstraints();
		pnButtons.add(btIgnore, c);
		c.gridy++;
		pnButtons.add(lbIgnore, c);
		c.gridy++;
		pnButtons.add(new JSeparator(), c);
		c.gridy++;
		pnButtons.add(btIntegrate, c);
		c.gridy++;
		pnButtons.add(lbIntegrate, c);
		c.gridy++;
		pnButtons.add(new JSeparator(), c);
		c.gridy++;
		pnButtons.add(btRestore, c);
		c.gridy++;
		pnButtons.add(lbRestore, c);
		btIgnore.setSelected(true);
		int answer = JOptionPane.showConfirmDialog(this, pnButtons,
				"Unerwartete Änderungen im Zieldateisystem", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (answer == JOptionPane.CANCEL_OPTION) {
			sp.cancel(false);
			return foreign_cancelled;
		} else {
			return btIntegrate.isSelected() ? foreign_integrate
					: btIgnore.isSelected() ? foreign_ignore : foreign_restore;
		}
	}

	public synchronized final boolean requestContinueRestore() {
		JRadioButton btRestore = new JRadioButton("Wiedeherstellung Fortsetzen");
		JRadioButton btAbort = new JRadioButton("Operation Abbrechen");
		JLabel lbInfo = new JLabel("Es wurden fehlerhafte Dateien gefunden!");
		ButtonGroup bg = new ButtonGroup();
		bg.add(btRestore);
		bg.add(btAbort);
		JPanel pnButtons = new JPanel(new GridLayout(3, 1));
		pnButtons.add(lbInfo);
		pnButtons.add(btRestore);
		pnButtons.add(btAbort);
		btAbort.setSelected(true);
		JOptionPane.showMessageDialog(this, pnButtons, "Beschädigte Dateien", JOptionPane.WARNING_MESSAGE);
		return btRestore.isSelected();
	}

	public synchronized final int requestSourceForRestore(Vector<Operation> sources) {
		Vector<JRadioButton> rbx = new Vector<JRadioButton>();
		JLabel lbInfo = new JLabel("Es gibt mehrere Quellen zum Wiederherstellen, bitte eine wählen:");
		ButtonGroup bg = new ButtonGroup();
		JPanel pnButtons = new JPanel(new GridLayout(sources.size() + 1, 1));
		pnButtons.add(lbInfo);
		JRadioButton rb;
		SimpleDateFormat sdf = UIFx.getDateTimeFormat();
		int newestVersion = -1;
		long newestDate = 0;
		for (int i = 0; i < sources.size(); i++) {
			if (newestDate < sources.get(i).getDbOriginal().lastModified()) {
				newestVersion = i;
				newestDate = sources.get(i).getDbOriginal().lastModified();
			}
		}
		int c = 0;
		for (Operation o : sources) {
			rb = new JRadioButton(o.getTargetPath() + " [" + sdf.format(o.getDbOriginal().lastModified())
					+ "] " + (c == newestVersion ? " (Neueste Version)" : ""));
			if (c == newestVersion)
				rb.setSelected(true);
			c++;
			bg.add(rb);
			rbx.add(rb);
			pnButtons.add(rb);
		}

		JOptionPane.showMessageDialog(this, pnButtons, "Konflikt", JOptionPane.WARNING_MESSAGE);

		int selected = -1;

		for (int i = 0; i < rbx.size(); i++) {
			if (rbx.get(i).isSelected()) {
				selected = i;
				break;
			}
		}

		return selected;
	}

	public synchronized final boolean retryOnOutOfMemoryWarning(String storage, long updateSize) {
		JLabel lbInfo = new JLabel("Kein Speicherplatz mehr auf dem " + storage + "! Es werden "
				+ (updateSize / 1024 / 1024) + " MB benötigt.");
		JRadioButton btRetry = new JRadioButton("Erneut Versuchen");
		JRadioButton btAbort = new JRadioButton("Operation Abbrechen");
		ButtonGroup bg = new ButtonGroup();
		bg.add(btRetry);
		bg.add(btAbort);
		JPanel pnButtons = new JPanel(new GridLayout(3, 1));
		btRetry.setSelected(true);
		pnButtons.add(lbInfo);
		pnButtons.add(btRetry);
		pnButtons.add(btAbort);
		btAbort.setSelected(true);
		JOptionPane.showMessageDialog(this, pnButtons, "Speichermangel", JOptionPane.WARNING_MESSAGE);
		return btRetry.isSelected();
	}

	public synchronized final void setException(Exception exception) {
		this.exception = exception;
	}

	public synchronized final void addStatus(String status) {
		lmStatusUpdate.addElement(status);
		log.add(status);
		statusUpdate.ensureIndexIsVisible(lmStatusUpdate.getSize() - 1);
	}

	public synchronized boolean approveSummary(OperationSummary operationSummary, boolean isBiDirectional,
			int priorityOnConflict) {
		OperationSummaryDialog osd = new OperationSummaryDialog(this, operationSummary, isBiDirectional);
		osd.getModel().select(priorityOnConflict);
		osd.setVisible(true);
		return osd.isApproved();
	}

	public final void startCountDown(long t) {
		if (t > 0) {
			if (pbcdt != null) {
				pbcdt.cancel(false);
			} else {
				pbcdt = new ProgressBarCountDownThread(this, t);
				pbcdt.execute();
			}
		} else {
			progressBar.setIndeterminate(true);
		}
	}

	public final void abortCountDown() {
		if (pbcdt != null) {
			pbcdt.cancel(false);
			pbcdt = null;
		}
	}

	public final void setCountDownPaused(boolean paused) {
		if (pbcdt != null) {
			pbcdt.setPaused(paused);
		}
	}

	public void passMessages(Vector<StatusMessage> messages) {
		for (StatusMessage message : messages) {
			if (!message.isVerbose() || (verbose && message.isVerbose())) {
				lmStatusUpdate.addElement(message.getMessage());
			}
			log.add(message.getMessage());
		}
		statusUpdate.ensureIndexIsVisible(lmStatusUpdate.getSize() - 1);
	}
}
