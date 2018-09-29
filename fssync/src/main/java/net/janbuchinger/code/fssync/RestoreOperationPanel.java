package net.janbuchinger.code.fssync;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class RestoreOperationPanel extends JPanel{
	private final OperationCheckBox ckOpSel;
	private final NumberPanel np;
	private final JLabel lbSource;
	private final ArrowRestorePanel apr;
	private final JLabel lbTarget;
	
	public static final Color colRestore = Color.CYAN.darker();
	
	public RestoreOperationPanel(Operation op, int n) {
		super(new FlowLayout(FlowLayout.LEFT));
		ckOpSel = new OperationCheckBox(op);
		np = new NumberPanel(n);
		np.setForeground(colRestore);
		lbSource = new JLabel(op.getSourcePath());
		lbSource.setForeground(colRestore);
		apr = new ArrowRestorePanel();
		apr.setForeground(colRestore);
		lbTarget = new JLabel(op.getTargetPath());
		lbTarget.setForeground(colRestore);
		add(ckOpSel);
		add(np);
		add(lbSource);
		add(apr);
		add(lbTarget);
	}

}
