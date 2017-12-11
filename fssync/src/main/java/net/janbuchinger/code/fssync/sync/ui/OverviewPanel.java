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
package net.janbuchinger.code.fssync.sync.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.janbuchinger.code.fssync.sync.OperationSummary;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class OverviewPanel extends JPanel {

	private final OperationSummary operationSummary;
	private final boolean isBiDirectional;

	private final JLabel lbSrcNew, lbDstNew;
	private final JLabel lbSrcMod, lbDstMod;
	private final JLabel lbSrcDeltaMod, lbDstDeltaMod;
	private final JLabel lbSrcDel, lbDstDel;
	private final JLabel lbSrcSum, lbDstSum;
	private final JLabel lbSrcCopy, lbDstCopy;
	private final JLabel lbSrcFree, lbDstFree;
	private final JLabel lbCopyTotal;

	public OverviewPanel(OperationSummary operationSummary, boolean isBidirectional) {
		super(new BorderLayout());
		this.isBiDirectional = isBidirectional;
		this.operationSummary = operationSummary;
		JPanel pnOverview = new JPanel(new GridBagLayout());

		lbSrcNew = new JLabel();
		lbDstNew = new JLabel();
		lbSrcMod = new JLabel();
		lbDstMod = new JLabel();
		lbSrcDeltaMod = new JLabel();
		lbDstDeltaMod = new JLabel();
		lbSrcDel = new JLabel();
		lbDstDel = new JLabel();
		lbSrcSum = new JLabel();
		lbDstSum = new JLabel();
		lbSrcCopy = new JLabel();
		lbDstCopy = new JLabel();
		lbSrcFree = new JLabel();
		lbDstFree = new JLabel();
		lbCopyTotal = new JLabel();

		GridBagConstraints c = UIFx.initGridBagConstraints();
		c.insets = new Insets(15, 15, 5, 15);

		if (isBiDirectional) {
			pnOverview.add(new JLabel(""), c);
			c.gridx++;
			pnOverview.add(new JLabel("Quelle"), c);
			c.gridx++;
			pnOverview.add(new JLabel("Ziel"), c);
			c.gridy++;
			c.gridx = 0;
		}
		pnOverview.add(new JLabel("Neu"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcNew, c);
			c.gridx++;
		}
		pnOverview.add(lbDstNew, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Geändert"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcMod, c);
			c.gridx++;
		}
		pnOverview.add(lbDstMod, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Wachstum"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcDeltaMod, c);
			c.gridx++;
		}
		pnOverview.add(lbDstDeltaMod, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Löschen"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcDel, c);
			c.gridx++;
		}
		pnOverview.add(lbDstDel, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Effekt"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcSum, c);
			c.gridx++;
		}
		pnOverview.add(lbDstSum, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Kopieren"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcCopy, c);
			c.gridx++;
		}
		pnOverview.add(lbDstCopy, c);
		c.gridy++;
		c.gridx = 0;
		pnOverview.add(new JLabel("Frei"), c);
		c.gridx++;
		if (isBiDirectional) {
			pnOverview.add(lbSrcFree, c);
			c.gridx++;
		}
		pnOverview.add(lbDstFree, c);
		c.gridy++;
		c.gridx = 0;
		if (isBiDirectional) {
			pnOverview.add(new JLabel("Kopieren Total"), c);
			c.gridx++;
			pnOverview.add(lbCopyTotal, c);
			c.gridy++;
			c.gridx = 0;
		}
		JPanel pnOverviewFlow = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnOverviewFlow.add(pnOverview);
		add(pnOverviewFlow, BorderLayout.NORTH);
	}

	public void refresh() {
		lbSrcNew.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeSourceNew()));
		lbDstNew.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeDestinationNew()));
		lbSrcMod.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeSourceModified()));
		lbDstMod.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeDestinationModified()));
		lbSrcDeltaMod.setText(FSFx.formatFileLength(operationSummary.getDeltaModSource()));
		lbDstDeltaMod.setText(FSFx.formatFileLength(operationSummary.getDeltaModDestination()));
		lbSrcDel.setText(FSFx.formatFileLength(operationSummary.getRmSizeSource()));
		lbDstDel.setText(FSFx.formatFileLength(operationSummary.getRmSizeDestination()));
		lbSrcSum.setText(FSFx.formatFileLength(operationSummary.getSumSource()));
		lbDstSum.setText(FSFx.formatFileLength(operationSummary.getSumDestination()));
		lbSrcCopy.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeSource()));
		lbDstCopy.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeDestination()));
		lbSrcFree.setText(FSFx.formatFileLength(operationSummary.getFreeSpaceSource()));
		lbDstFree.setText(FSFx.formatFileLength(operationSummary.getFreeSpaceDestination()));
		lbCopyTotal.setText(FSFx.formatFileLength(operationSummary.getUpdateSizeTotal()));
	}

}
