package net.janbuchinger.code.fssync.fs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class OperationCheckBox extends JCheckBox implements ActionListener {
	private final Operation operation;

	public OperationCheckBox(Operation operation) {
		super();
		this.operation = operation;
		addActionListener(this);
	}

	@Override
	public void setSelected(boolean b) {
		super.setSelected(b);
		operation.setSelected(b);
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (!b)
			setSelected(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this) {
			operation.setSelected(isSelected());
		}
	}
}
