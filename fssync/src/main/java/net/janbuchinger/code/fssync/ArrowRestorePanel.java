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
package net.janbuchinger.code.fssync;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ArrowRestorePanel extends JPanel {
	private Graphics2D g2d;

	private final int[] xPoints;
	private final int[] yPoints;
	private final int nPoints;

	public ArrowRestorePanel() {
		setPreferredSize(new Dimension(32, 32));
		xPoints = new int[] { 4, 14, 14, 27, 27, 14, 14 };
		yPoints = new int[] { 16, 6, 12, 12, 21, 21, 26 };
		nPoints = xPoints.length;
	}

	@Override
	protected void paintComponent(Graphics g) {
		g2d = (Graphics2D) g;
		g2d.fillPolygon(xPoints, yPoints, nPoints);
	}
}
