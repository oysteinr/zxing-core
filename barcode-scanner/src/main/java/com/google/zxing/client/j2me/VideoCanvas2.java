package com.google.zxing.client.j2me;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

import org.openxdata.midp.mvc.AbstractView;

public class VideoCanvas2 extends Canvas implements CommandListener{


	private static final Command exit = new Command("Exit", Command.EXIT, 1);
	private static final Command history = new Command("History", Command.ITEM, 0);

	private final BarcodeTypeEditorImpl view;
	private final SnapshotThread2 snapshotThread;

	VideoCanvas2(BarcodeTypeEditorImpl view) {
		this.view = view;
		addCommand(exit);
		addCommand(history);
		setCommandListener(this);
		snapshotThread = new SnapshotThread2(view);
		new Thread(snapshotThread).start();
	}

	protected void paint(Graphics graphics) {
		// do nothing
	}

	protected void keyPressed(int keyCode) {
		// Any valid game key will trigger a capture
		if (getGameAction(keyCode) != 0) {
			snapshotThread.continueRun();
		} else {
			super.keyPressed(keyCode);
		}
	}

	public void commandAction(Command command, Displayable displayable) {
		int type = command.getCommandType();
//		if (command == history) {
//			zXingMIDlet.historyRequest();
		 if (type == Command.EXIT || type == Command.STOP || type == Command.BACK || type == Command.CANCEL) {
			snapshotThread.stop();
			view.stop();
		}
	}

}
