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
package net.janbuchinger.code.fssync.fs;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class OperationPanel extends JPanel implements MouseListener {

	private final Operation operation;
	private final NumberPanel np;
	private final ArrowPanel ap;
	private final JLabel lbSrc, lbTrg;
	private final OperationCheckBox ckOperation;
	// private final OperationPanelObserver opo;
	private final FSSyncUI ui;

	private final Settings settings;

	public final static Color online = Color.green.darker(), unsure = Color.red.darker(),
			offline = Color.gray;

	public OperationPanel(FSSyncUI ui, Operation operation, int n, Settings settings) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.settings = settings;
		this.operation = operation;
		this.ui = ui;
		ckOperation = new OperationCheckBox(operation);
		if(!operation.isOnline()) {
			ckOperation.setEnabled(false);
		} else {
			ckOperation.setSelected(operation.isSelected());
		}
		np = new NumberPanel(n);
		np.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		np.addMouseListener(this);
		ap = new ArrowPanel(operation.isSyncBidirectional());
		if(operation.isOnline()){
			ap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			ap.addMouseListener(this);
		}
		lbSrc = new JLabel(operation.getSource().getPath());
		if(operation.isSourceOnline() && settings.getFileBrowser().length()>0){
			lbSrc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			lbSrc.addMouseListener(this);
		}
		lbTrg = new JLabel(operation.getTarget().getPath());
		if(operation.isTargetOnline() && settings.getFileBrowser().length()>0){
			lbTrg.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			lbTrg.addMouseListener(this);
		}
		add(ckOperation);
		add(np);
		add(lbSrc);
		add(ap);
		add(lbTrg);
		if (operation.isSourceOnline()) {
			lbSrc.setForeground(online);
			// srcOnline = true;
		} else if (operation.getSource().exists()) {
			lbSrc.setForeground(unsure);
		} else {
			lbSrc.setForeground(offline);
		}

		if (operation.isTargetOnline()) {
			lbTrg.setForeground(online);
		} else if (operation.getTarget().exists()) {
			lbTrg.setForeground(unsure);
		} else {
			lbTrg.setForeground(offline);
		}

		if (operation.isOnline()) {
			ap.setForeground(online);
			np.setForeground(online);
		} else {
			ap.setForeground(offline);
			np.setForeground(offline);
		}
	}

	public Operation getOperation() {
		return operation;
	}

	public final NumberPanel getNumberPanel() {
		return np;
	}

	public final ArrowPanel getArrowPanel() {
		return ap;
	}

	public final JLabel getLbSrc() {
		return lbSrc;
	}

	public final JLabel getLbTrg() {
		return lbTrg;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getButton() != MouseEvent.BUTTON1)
			return;
		if (e.getSource() == ap) {
			Vector<Operation> operations = new Vector<Operation>();
			operations.add(operation);
			ui.runOperations(operations, null);
		} else if (e.getSource() == np) {
			OperationEditorDialog oed = new OperationEditorDialog(ui.getFrame(), operation, ui.getSegments());
			oed.setVisible(true);
			if (oed.getAnswer() == OperationEditorDialog.SAVE) {
				ui.stopUIChangeWatcher();
				ui.getSegments().sort();
				ui.getSegments().save();
				ui.refresh();
				ui.startUIChangeWatcher();
			}
		} else if (e.getSource() instanceof JLabel) {
			try {
				Runtime.getRuntime()
						.exec(settings.getFileBrowser() + " \"" + ((JLabel) e.getSource()).getText() + "\"");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
