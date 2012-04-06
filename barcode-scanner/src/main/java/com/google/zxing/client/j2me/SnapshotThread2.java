package com.google.zxing.client.j2me;

import javax.microedition.lcdui.Image;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;

public class SnapshotThread2 implements Runnable{

	

	private final BarcodeTypeEditorImpl view;
	private final Object waitLock;
	private volatile boolean done;
	private final MultimediaManager multimediaManager;
	private String bestEncoding;

	SnapshotThread2(BarcodeTypeEditorImpl view) {
		this.view = view;
		waitLock = new Object();
		done = false;
		multimediaManager = BarcodeTypeEditorImpl.buildMultimediaManager();
	}

	void continueRun() {
		synchronized (waitLock) {
			waitLock.notifyAll();
		}
	}

	private void waitForSignal() {
		synchronized (waitLock) {
			try {
				waitLock.wait();
			} catch (InterruptedException ie) {
				// continue
			}
		}
	}

	void stop() {
		done = true;
		continueRun();
	}

	public void run() {
		Player player = view.getPlayer();
		do {
			waitForSignal();
			try {
				multimediaManager.setFocus(player);
				byte[] snapshot = takeSnapshot();
				Image capturedImage = Image.createImage(snapshot, 0, snapshot.length);
				LuminanceSource source = new LCDUIImageLuminanceSource(capturedImage);
				BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
				Reader reader = new MultiFormatReader();
				Result result = reader.decode(bitmap);
				view.handleDecodedText(result);
			} catch (ReaderException re) {
				// Show a friendlier message on a mere failure to read the barcode
				view.showError("Sorry, no barcode was found.");
			} catch (MediaException me) {
				view.showError(me);
			} catch (RuntimeException re) {
				view.showError(re);
			}
		} while (!done);
	}

	private byte[] takeSnapshot() throws MediaException {

		String bestEncoding = guessBestEncoding();

		VideoControl videoControl = view.getVideoControl();
		byte[] snapshot = null;
		try {
			snapshot = videoControl.getSnapshot("".equals(bestEncoding) ? null : bestEncoding);
		} catch (MediaException me) {
		}
		if (snapshot == null) {
			// Fall back on JPEG; seems that some cameras default to PNG even
			// when PNG isn't supported!
			snapshot = videoControl.getSnapshot("encoding=jpeg");
			if (snapshot == null) {
				throw new MediaException("Can't obtain a snapshot");
			}
		}
		return snapshot;
	}

	private synchronized String guessBestEncoding() throws MediaException {
		if (bestEncoding == null) {
			// Check this property, present on some Nokias?
			String supportsVideoCapture = System.getProperty("supports.video.capture");
			if ("false".equals(supportsVideoCapture)) {
				throw new MediaException("supports.video.capture is false");
			}

			bestEncoding = "";
			String videoSnapshotEncodings = System.getProperty("video.snapshot.encodings");
			if (videoSnapshotEncodings != null) {
				// We know explicitly what the camera supports; see if PNG is among them since
				// Image.createImage() should always support it
				int pngEncodingStart = videoSnapshotEncodings.indexOf("encoding=png");
				if (pngEncodingStart >= 0) {
					int space = videoSnapshotEncodings.indexOf(' ', pngEncodingStart);
					bestEncoding = space >= 0 ? videoSnapshotEncodings.substring(pngEncodingStart, space)
							: videoSnapshotEncodings.substring(pngEncodingStart);
				}
			}
		}
		return bestEncoding;
	}

}
