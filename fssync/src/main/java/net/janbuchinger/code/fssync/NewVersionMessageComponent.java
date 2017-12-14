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

import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

@SuppressWarnings("serial")
public class NewVersionMessageComponent extends JPanel implements HyperlinkListener {

	public NewVersionMessageComponent(String newVersion) {
		super(new GridLayout(1, 1));
		JTextPane tpMsg = new JTextPane();
		tpMsg.setContentType("text/html");
		String msg = "<html><p>Neue Version verfügbar: <b>" + newVersion;
		msg += "</b><br><a href=\"http://code.janbuchinger.net\">code.janbuchinger.net</a></p></html>";
		tpMsg.setBackground(getBackground());
		tpMsg.setEditable(false);
		tpMsg.addHyperlinkListener(this);
		tpMsg.setText(msg);
		add(tpMsg);
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType().toString().equals("ACTIVATED")) {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(e.getURL().toURI());
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

}
