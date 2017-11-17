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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.janbuchinger.code.mishmash.ui.UIFx;
import net.janbuchinger.code.mishmash.ui.dialog.DialogEscapeHook;


@SuppressWarnings("serial")
public class SegmentEditorDialog extends JDialog implements ActionListener, MouseListener {
	private final Segment s;

	private final JTextField tfSegmentName;
	private final JList<Operation> jlOperations;
	private final OperationsListModel lmOperations;

	private final JButton btClose;
	private final JButton btCancel;
	private final JButton btDelete;

	public final static int CANCEL = 0;
	public final static int OK = 1;
	private int answer;
	private boolean isDelete;

	private final JButton btAddOp;
	private final JButton btRemOp;

	private boolean hasChanges;
	private final String initialName;

	private long click;
	private int clickId;

//	private final String[] segNames;
	
	private final Segments segments;

	public SegmentEditorDialog(JFrame frm, Segment s, Segments segments) {
		super(frm, "Neues Segment", true);

//		this.segNames = segNames;
		
		this.segments = segments;

		new DialogEscapeHook(this);

		if (s != null) {
			setTitle(s.getName());
			btCancel = null;
		} else {
			s = new Segment("");
			btCancel = new JButton("Abbrechen");
			btCancel.addActionListener(this);
		}
		this.s = s;

		isDelete = false;

		answer = CANCEL;

		hasChanges = false;
		initialName = s.getName();

		tfSegmentName = new JTextField(45);
		tfSegmentName.setText(s.getName());
		lmOperations = new OperationsListModel(s);
		jlOperations = new JList<Operation>(lmOperations);
		jlOperations.addMouseListener(this);

		btClose = new JButton("Ok");
		btClose.addActionListener(this);
		btDelete = new JButton("Löschen");
		btDelete.addActionListener(this);
		JPanel pnButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel pnButtonsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));

		if (btCancel != null)
			pnButtons.add(btCancel);
		else
			pnButtonsLeft.add(btDelete);

		pnButtons.add(btClose);

		JPanel pnButtonsBorder = new JPanel(new BorderLayout());
		pnButtonsBorder.add(pnButtonsLeft, BorderLayout.WEST);
		pnButtonsBorder.add(pnButtons, BorderLayout.EAST);

		btAddOp = new JButton("+");
		btAddOp.addActionListener(this);
		btRemOp = new JButton("-");
		btRemOp.addActionListener(this);
		JPanel pnAddRemOp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnAddRemOp.add(btAddOp);
		pnAddRemOp.add(btRemOp);

		JPanel pnContent = new JPanel(new GridBagLayout());
		GridBagConstraints c = UIFx.initGridBagConstraints();

		pnContent.add(new JLabel("Name"), c);
		c.gridx++;
		pnContent.add(tfSegmentName, c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		pnContent.add(new JLabel("Operationen"), c);
		c.gridy++;
		pnContent.add(UIFx.initScrollPane(jlOperations, 15), c);
		c.gridy++;
		pnContent.add(pnAddRemOp, c);
		c.gridy++;
		pnContent.add(pnButtonsBorder, c);

		setContentPane(pnContent);
		UIFx.packAndCenter(this, frm);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btClose) {
			if (!initialName.equals(tfSegmentName.getText())) {

				for (int i = 0; i < segments.size(); i++) {
					if (segments.get(i).getName().equals(tfSegmentName.getText())) {
						JOptionPane.showMessageDialog(this, "Bitte anderen Namen Wählen", "Fehler",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}

				s.setName(tfSegmentName.getText());
				hasChanges = true;
			}
			if (tfSegmentName.getText().equals("")) {
				JOptionPane.showMessageDialog(this, "Bitte Namen Wählen", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			answer = OK;
			setVisible(false);
		} else if (e.getSource() == btDelete) {
			if (JOptionPane.showConfirmDialog(this, "Segment wirklich Löschen?", "Löschen",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				isDelete = true;
				setVisible(false);
			}
		} else if (e.getSource() == btCancel) {
			setVisible(false);
		} else if (e.getSource() == btAddOp) {
			OperationEditorDialog soed = new OperationEditorDialog(this, segments);
			soed.setVisible(true);
			if (soed.getAnswer() == OperationEditorDialog.SAVE) {
				Operation op = soed.getOperation();
				// System.out.println(op);
				lmOperations.addElement(op);
			}
		} else if (e.getSource() == btRemOp) {
			if (jlOperations.getSelectedIndex() != -1) {
				if (JOptionPane.showConfirmDialog(this, "Operation wirklich Löschen?", "Löschen",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					lmOperations.remove(jlOperations.getSelectedIndex());
					jlOperations.setSelectedIndices(new int[0]);
				}
			}
		}
	}

	public boolean hasChanges() {
		return hasChanges || lmOperations.hasChanges();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (System.currentTimeMillis() - click < 500) {
			if (jlOperations.getSelectedIndex() == clickId) {
				OperationEditorDialog soed = new OperationEditorDialog(this, lmOperations.get(clickId), segments);
				soed.setVisible(true);
				if (soed.getAnswer() == OperationEditorDialog.SAVE) {
					lmOperations.set(clickId, soed.getOperation());
				}
			}
		}
		clickId = jlOperations.getSelectedIndex();
		click = System.currentTimeMillis();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public final Segment getSegment() {
		return s;
	}

	public final int getAnswer() {
		return answer;
	}

	public final boolean isDelete() {
		return isDelete;
	}
}
