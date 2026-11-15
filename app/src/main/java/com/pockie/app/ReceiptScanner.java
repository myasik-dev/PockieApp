package com.pockie.app;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Local OCR with bounded image size, orientation handling, and receipt-layout parsing. */
public final class ReceiptScanner {
  private static final ExecutorService IMAGES = Executors.newSingleThreadExecutor();

  private ReceiptScanner() {}

  public static Task<Transaction> scan(Context context, Uri uri) {
    Context app = context.getApplicationContext();
    return Tasks.call(
        IMAGES,
        () -> {
          Bitmap original = decode(app, uri), primary = null, footer = null;
          TextRecognizer recognizer =
              TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
          try {
            primary = resize(original);
            Text text =
                Tasks.await(
                    recognizer.process(InputImage.fromBitmap(primary, 0)), 45, TimeUnit.SECONDS);
            if (text.getText().trim().isEmpty())
              throw new IOException("No text found. Try a clearer receipt image.");
            Transaction transaction = parse(text);
            boolean clearTotal =
                text.getText()
                    .matches(
                        "(?is).*\\b(total\\s*invoice\\s*amount|grand\\s*total|total\\s*amount|amount\\s*payable)\\b.*");
            if (Math.min(original.getWidth(), original.getHeight()) < 600
                && (transaction.amountPaise == 0 || !clearTotal)) {
              // Tiny photographs often lose narrow digits. Retry the lower receipt with stronger
              // contrast and less surrounding clutter, rather than inferring a total from item
              // prices.
              footer = enhancedFooter(original);
              Text checked =
                  Tasks.await(
                      recognizer.process(InputImage.fromBitmap(footer, 0)), 45, TimeUnit.SECONDS);
              Transaction verified = parse(checked);
              if (verified.amountPaise > 0) transaction.amountPaise = verified.amountPaise;
            }
            return transaction;
          } finally {
            recognizer.close();
            if (footer != null && footer != original && footer != primary) footer.recycle();
            if (primary != null && primary != original) primary.recycle();
            original.recycle();
          }
        });
  }

  private static Transaction parse(Text text) {
    List<ReceiptParser.Line> lines = new ArrayList<>();
    for (Text.TextBlock block : text.getTextBlocks())
      for (Text.Line line : block.getLines()) {
        Rect r = line.getBoundingBox();
        if (r != null)
          lines.add(new ReceiptParser.Line(line.getText(), r.left, r.top, r.right, r.bottom));
      }
    return ReceiptParser.parse(text.getText(), lines);
  }

  private static Bitmap enhancedFooter(Bitmap original) {
    int top = (int) (original.getHeight() * .48f),
        height = Math.max(1, (int) (original.getHeight() * .4f));
    Bitmap crop =
        Bitmap.createBitmap(
            original, 0, top, original.getWidth(), Math.min(height, original.getHeight() - top));
    Bitmap contrast = crop.copy(Bitmap.Config.ARGB_8888, true);
    if (crop != original) crop.recycle();
    int[] pixels = new int[contrast.getWidth() * contrast.getHeight()];
    contrast.getPixels(
        pixels, 0, contrast.getWidth(), 0, 0, contrast.getWidth(), contrast.getHeight());
    for (int i = 0; i < pixels.length; i++) {
      int color = pixels[i];
      int gray =
          (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
      gray = Math.max(0, Math.min(255, (gray - 120) * 255 / 110));
      pixels[i] = Color.rgb(gray, gray, gray);
    }
    contrast.setPixels(
        pixels, 0, contrast.getWidth(), 0, 0, contrast.getWidth(), contrast.getHeight());
    float factor = Math.min(5f, 2600f / Math.max(contrast.getWidth(), contrast.getHeight()));
    Bitmap enlarged =
        Bitmap.createScaledBitmap(
            contrast,
            Math.max(1, Math.round(contrast.getWidth() * factor)),
            Math.max(1, Math.round(contrast.getHeight() * factor)),
            true);
    if (enlarged != contrast) contrast.recycle();
    return enlarged;
  }

  private static Bitmap decode(Context context, Uri uri) throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      BitmapFactory.decodeStream(input, null, options);
    }
    if (options.outWidth <= 0 || options.outHeight <= 0)
      throw new IOException("Unable to open receipt image.");
    options.inJustDecodeBounds = false;
    options.inSampleSize = 1;
    while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 3000)
      options.inSampleSize *= 2;
    Bitmap bitmap;
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      bitmap = BitmapFactory.decodeStream(input, null, options);
    }
    if (bitmap == null) throw new IOException("Unable to decode receipt image.");
    int orientation = ExifInterface.ORIENTATION_NORMAL;
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      orientation =
          new ExifInterface(input)
              .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    } catch (IOException ignored) {
    }
    Matrix rotate = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        rotate.setScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        rotate.setRotate(180);
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        rotate.setScale(1, -1);
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        rotate.setRotate(90);
        rotate.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_90:
        rotate.setRotate(90);
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        rotate.setRotate(270);
        rotate.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        rotate.setRotate(270);
        break;
      default:
        break;
    }
    if (!rotate.isIdentity()) {
      Bitmap upright =
          Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotate, true);
      if (upright != bitmap) bitmap.recycle();
      bitmap = upright;
    }
    return bitmap;
  }

  private static Bitmap resize(Bitmap bitmap) {
    float scale =
        Math.min(
            5f,
            Math.min(
                1200f / Math.min(bitmap.getWidth(), bitmap.getHeight()),
                2600f / Math.max(bitmap.getWidth(), bitmap.getHeight())));
    if (scale > 1.1f || Math.max(bitmap.getWidth(), bitmap.getHeight()) > 2600) {
      Bitmap resized =
          Bitmap.createScaledBitmap(
              bitmap,
              Math.max(1, Math.round(bitmap.getWidth() * scale)),
              Math.max(1, Math.round(bitmap.getHeight() * scale)),
              true);
      return resized;
    }
    return bitmap;
  }
}
