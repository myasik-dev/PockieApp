package com.pockie.app;

import android.content.Context;
import android.graphics.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;

/** Opt-in diagnostic; only processes a receipt explicitly supplied to the QA app. */
public class ReceiptOcrProbeTest {
  @Test
  public void compareSmallReceiptPreparations() throws Exception {
    Assume.assumeTrue(
        "true".equals(InstrumentationRegistry.getArguments().getString("runOcrProbe")));
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Bitmap original =
        BitmapFactory.decodeFile(
            new File(c.getExternalFilesDir(null), "chennai-silks-receipt.png").getPath());
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    try (FileWriter out = new FileWriter(new File(c.getExternalFilesDir(null), "ocr-probe.txt"))) {
      for (int region = 0; region < 2; region++)
        for (int mode = 0; mode < 6; mode++) {
          Bitmap base =
              region == 0
                  ? original
                  : Bitmap.createBitmap(
                      original,
                      0,
                      (int) (original.getHeight() * .48f),
                      original.getWidth(),
                      (int) (original.getHeight() * .4f));
          Bitmap prepared = base.copy(Bitmap.Config.ARGB_8888, true);
          if (mode >= 2) {
            int[] pixels = new int[prepared.getWidth() * prepared.getHeight()];
            prepared.getPixels(
                pixels, 0, prepared.getWidth(), 0, 0, prepared.getWidth(), prepared.getHeight());
            for (int i = 0; i < pixels.length; i++) {
              int p = pixels[i];
              int gray = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000;
              gray =
                  mode == 2
                      ? Math.max(0, Math.min(255, (gray - 120) * 255 / 110))
                      : gray < (mode == 3 ? 150 : mode == 4 ? 180 : 205) ? 0 : 255;
              pixels[i] = Color.rgb(gray, gray, gray);
            }
            prepared.setPixels(
                pixels, 0, prepared.getWidth(), 0, 0, prepared.getWidth(), prepared.getHeight());
          }
          Bitmap scaled =
              Bitmap.createScaledBitmap(
                  prepared,
                  prepared.getWidth() * 5,
                  prepared.getHeight() * 5,
                  mode == 0 || mode == 2);
          Text result =
              Tasks.await(
                  recognizer.process(InputImage.fromBitmap(scaled, 0)), 30, TimeUnit.SECONDS);
          out.write("REGION " + region + " MODE " + mode + "\n" + result.getText() + "\n\n");
          out.flush();
          scaled.recycle();
          prepared.recycle();
          if (base != original) base.recycle();
        }
    } finally {
      recognizer.close();
      original.recycle();
    }
  }
}
