package com.google.zxing.client.j2me;

import java.io.IOException;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

import org.openxdata.mforms.midp.forms.BarcodeTypeEditor;
import org.openxdata.mforms.midp.forms.TypeEditorListener;
import org.openxdata.mforms.model.QuestionData;
import org.openxdata.mforms.model.ValidationRule;

import com.google.zxing.Result;

public class BarcodeTypeEditorImpl extends BarcodeTypeEditor {

	private static final int ALERT_TIMEOUT_MS = 5 * 1000;

	private Canvas canvas;
	private Player player;
	private VideoControl videoControl;
	private Alert confirmation;
	private Alert alert;
	// private Menu history;
	// private Vector resultHistory;

	private QuestionData questionData;
	private TypeEditorListener listener;

	private String result;

	public void startEdit(QuestionData data, ValidationRule validationRule, boolean singleQtnEdit, int pos, int count,
			TypeEditorListener listener) {

		this.questionData = data;
		this.listener = listener;

		try {
			player = createPlayer();
			player.realize();

			MultimediaManager multimediaManager = buildMultimediaManager();
			multimediaManager.setZoom(player);
			multimediaManager.setExposure(player);
			multimediaManager.setFlash(player);
			videoControl = (VideoControl) player.getControl("VideoControl");
			canvas = new VideoCanvas2(this);
			canvas.setFullScreenMode(true);
			videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
			videoControl.setDisplayLocation(0, 0);

			videoControl.setDisplaySize(canvas.getWidth(), canvas.getHeight());

			screen = canvas;

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MediaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Set up one confirmation and alert object to re-use
		confirmation = new Alert(null);
		confirmation.setType(AlertType.CONFIRMATION);
		confirmation.setTimeout(ALERT_TIMEOUT_MS);
		Command yes = new Command("Yes", Command.OK, 1);
		confirmation.addCommand(yes);
		Command no = new Command("No", Command.CANCEL, 1);
		confirmation.addCommand(no);
		alert = new Alert(null);
		alert.setTimeout(ALERT_TIMEOUT_MS);

	}

	public void commandAction(Command c, Displayable s) {

	}

	static MultimediaManager buildMultimediaManager() {
		if (BarcodeTypeEditor.isJSR234Available())
			return new AdvancedMultimediaManager();
		return new DefaultMultimediaManager();

	}

	private static Player createPlayer() throws IOException, MediaException {
		// Try a workaround for Nokias, which want to use capture://image in some cases
		Player player = null;
		String platform = System.getProperty("microedition.platform");
		if (platform != null && platform.indexOf("Nokia") >= 0) {
			try {
				player = Manager.createPlayer("capture://image");
			} catch (MediaException me) {
				// if this fails, just continue with capture://video
			} catch (NullPointerException npe) { // Thanks webblaz... for this improvement:
				// The Nokia 2630 throws this if image/video capture is not supported
				// We should still try to continue

				// } catch (Error e) {
				// Ugly, but, it seems the Nokia N70 throws "java.lang.Error: 136" here
				// We should still try to continue
			}
		}
		if (player == null) {
			try {
				player = Manager.createPlayer("capture://video");
			} catch (NullPointerException npe) {
				// The Nokia 2630 throws this if image/video capture is not supported
				throw new MediaException("Image/video capture not supported on this phone");
			}
		}
		return player;
	}

	void stop() {
		System.out.println("STOP");
		listener.endEdit(false, questionData, null);
		// destroyApp(false);
		// notifyDestroyed();
	}

	Displayable getCanvas() {
		return canvas;
	}

	Player getPlayer() {
		return player;
	}

	VideoControl getVideoControl() {
		return videoControl;
	}

	void handleDecodedText(Result theResult) {
		result = theResult.getText();
		if (result != null) {
			System.out.println("Fekk resultat!! - " + theResult);
			showAlert("Barcode Detected", result);

			// Set the result in the barcode field
			questionData.setAnswer(result);

			// Let the controller know we are finished collecting data
			listener.endEdit(true, questionData, null);
		}

		// ParsedResult result = ResultParser.parseResult(theResult);
		// String resultString = result.toString();
		// int i = 0;
		// while (i < resultHistory.size()) {
		// if (resultString.equals(resultHistory.elementAt(i).toString())) {
		// break;
		// }
		// i++;
		// }
		// if (i == resultHistory.size()) {
		// resultHistory.addElement(result);
		// history.append(result.getDisplayResult(), null);
		// }
		// barcodeAction(result);
	}

	void showError(String message) {
		alert.setTitle("Error");
		alert.setString(message);
		alert.setType(AlertType.ERROR);
		showAlert(alert);
	}

	void showError(Throwable t) {
		String message = t.getMessage();
		if (message != null && message.length() > 0) {
			showError(message);
		} else {
			showError(t.toString());
		}
	}

	private void showAlert(String title, String text) {
		alert.setTitle(title);
		alert.setString(text);
		alert.setType(AlertType.INFO);
		showAlert(alert);
	}

	private void showAlert(Alert alert) {
		// Display display = Display.getDisplay(this);
		// Display display = this.getDisplay();
		display.setCurrent(alert, canvas);
	}
}
