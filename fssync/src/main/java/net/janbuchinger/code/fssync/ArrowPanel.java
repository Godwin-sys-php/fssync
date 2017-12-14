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
package net.janbuchinger.code.fssync;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ArrowPanel extends JPanel {

	public static final int ONLINE = 0;
	public static final int OFFLINE = 1;
	public static final int SYNCHRONIZING = 2;

	private Graphics2D g2d;
	private Stroke stroke;

	private final int[] xPoints;
	private final int[] yPoints;
	private final int nPoints;

	public ArrowPanel(boolean bidirectional) {
		setPreferredSize(new Dimension(32, 32));

		if (bidirectional) {
			xPoints = new int[] { 4, 14, 14, 17, 17, 27, 17, 17, 14, 14 };
			yPoints = new int[] { 16, 6, 12, 12, 6, 16, 26, 21, 21, 26 };
		} else {
			xPoints = new int[] { 4, 17, 17, 27, 17, 17, 4 };
			yPoints = new int[] { 12, 12, 6, 16, 26, 21, 21 };
		}
		nPoints = xPoints.length;
	}

	@Override
	protected void paintComponent(Graphics g) {
		g2d = (Graphics2D) g;
		stroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(2));
		g2d.fillPolygon(xPoints, yPoints, nPoints);

		g2d.setStroke(stroke);
	}
}