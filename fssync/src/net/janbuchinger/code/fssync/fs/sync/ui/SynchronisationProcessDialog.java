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
package net.janbuchinger.code.fssync.fs.sync.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Iterator;
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

import net.janbuchinger.code.fssync.fs.Operation;
import net.janbuchinger.code.fssync.fs.Settings;
import net.janbuchinger.code.fssync.fs.sync.OperationSummary;
import net.janbuchinger.code.mishmash.ui.UIFx;

import org.apache.commons.io.FileUtils;

@SuppressWarnings("serial")
public final class SynchronisationProcessDialog extends JDialog implements ActionListener {

	private final JLabel processStatus;
	private final JProgressBar progressBar;
	private final JList<String> statusUpdate;
	private final SynchronisationStatusListModel lmStatusUpdate;
	private final JButton btCancel;
	private final JMenuItem miSaveLog;
	private boolean cancelled;
	private boolean finished;
	private final Settings settings;
	private Exception exception;

	private final Vector<String> log;
	private final boolean verbose;

	public SynchronisationProcessDialog(String title, JFrame d, Settings settings) {
		super(d, title, true);

		this.verbose = settings.isVerbose();

		log = new Vector<String>();

		this.settings = settings;

		cancelled = false;
		finished = false;

		exception = null;

		miSaveLog = new JMenuItem("Log Speichern");
		miSaveLog.addActionListener(this);
		miSaveLog.setEnabled(false);

		if (!settings.isAlwaysSaveLog()) {
			JMenu menu = new JMenu("Menü");
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

		lmStatusUpdate = new SynchronisationStatusListModel();
		statusUpdate = new JList<String>(lmStatusUpdate);

		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);
		JPanel pnBtCancel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnBtCancel.add(btCancel);

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.setPreferredSize(new Dimension(350, 400));
		pnContent.add(pnProgressStatus, BorderLayout.NORTH);
		JScrollPane sp = new JScrollPane(statusUpdate);
		sp.getHorizontalScrollBar().setUnitIncrement(15);
		pnContent.add(sp, BorderLayout.CENTER);
		pnContent.add(pnBtCancel, BorderLayout.SOUTH);

		setContentPane(pnContent);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		UIFx.sizeAndCenter(this, 0.8, 0.8);
	}

	@Override
	public synchronized final void actionPerformed(ActionEvent e) {
		if (e.getSource() == btCancel) {
			cancelled = true;
			if (!finished) {
				btCancel.setEnabled(false);
			} else {
				setVisible(false);
			}
		} else if (e.getSource() == miSaveLog) {
			saveLog();
		}
	}

	public synchronized final void saveLog() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		File file = new File(settings.getLogFilesDir(), "FSSync-Log-" + sdf.format(System.currentTimeMillis())
				+ ".txt");
		StringBuilder data = new StringBuilder();
		Iterator<String> iStatusUpdate = log.iterator();
		while (iStatusUpdate.hasNext()) {
			data.append(iStatusUpdate.next() + "\r\n");
		}
		if (exception != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			data.append(sw.toString());
		}
		try {
			FileUtils.writeStringToFile(file, data.toString(), Charset.defaultCharset());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		addStatus("Logdatei Gespeichert " + file.getAbsolutePath());
	}

	public synchronized final void setProcessStatusText(String text) {
		processStatus.setText(text);
	}

	public synchronized final boolean isCancelled() {
		return cancelled;
	}

	public synchronized final void setProgress(int progress) {
		progressBar.setValue(progress);
	}

	public synchronized final void setFinished(String finalStatus) {
		addStatus(finalStatus);
		if (settings.isAlwaysSaveLog() || exception != null) {
			saveLog();
		}
		setDeterminate(true);
		miSaveLog.setEnabled(true);
		btCancel.setText("Schliessen");
		btCancel.setEnabled(true);
		progressBar.setValue(100);
		progressBar.setEnabled(false);
		processStatus.setText("Fertig");
		finished = true;
	}

	public synchronized final void setCancelled(String finalStatus) {
		addStatus(finalStatus);
		if (settings.isAlwaysSaveLog() || exception != null) {
			saveLog();
		}
		setDeterminate(true);
		miSaveLog.setEnabled(true);
		btCancel.setText("Schliessen");
		btCancel.setEnabled(true);
		progressBar.setEnabled(false);
		processStatus.setText("Abgebrochen");
		finished = true;
	}

	public synchronized final void setDeterminate(boolean flag) {
		progressBar.setIndeterminate(!flag);
	}

	public static final int foreign_integrate = 0;
	public static final int foreign_restore = 1;
	public static final int foreign_ignore = 2;

	public synchronized final int requestForeignFileHandling() {
		JRadioButton btIntegrate = new JRadioButton("Änderungen Holen (Bidirektional Synchronisieren)");
		JRadioButton btIgnore = new JRadioButton("Änderungen Ignorieren");
		JRadioButton btRestore = new JRadioButton("Änderungen Löschen");
		ButtonGroup bg = new ButtonGroup();
		bg.add(btIntegrate);
		bg.add(btIgnore);
		bg.add(btRestore);
		JPanel pnButtons = new JPanel(new GridLayout(3, 1));
		btIgnore.setSelected(true);
		pnButtons.add(btIgnore);
		pnButtons.add(btIntegrate);
		pnButtons.add(btRestore);
		btIgnore.setSelected(true);
		JOptionPane.showMessageDialog(this, pnButtons, "Unerwartete Änderungen im Zieldateisystem",
				JOptionPane.WARNING_MESSAGE);
		return btIntegrate.isSelected() ? foreign_integrate : btIgnore.isSelected() ? foreign_ignore
				: foreign_restore;
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
			rb = new JRadioButton(o.getRemotePath() + (c == newestVersion ? " (Neueste Version)" : ""));
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
		lmStatusUpdate.addStatus(status);
		log.add(status);
		statusUpdate.ensureIndexIsVisible(lmStatusUpdate.getSize() - 1);
	}

	public synchronized final void addStatusVerbose(String status) {
		if (verbose) {
			addStatus(status);
		} else {
			log.add(status);
		}
	}

	public synchronized boolean approveSummary(OperationSummary operationSummary, boolean isBiDirectional,
			int priorityOnConflict) {
		OperationSummaryDialog osd = new OperationSummaryDialog(this, operationSummary, isBiDirectional);
		osd.getModel().select(priorityOnConflict);
		osd.setVisible(true);
		return osd.isApproved();
	}
}
