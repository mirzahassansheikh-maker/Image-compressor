package com.example.imagecompressor;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES = 101;
    private final ArrayList<Uri> selected = new ArrayList<>();
    private TextView selectionText, qualityText, sizeText, statusText;
    private SeekBar qualityBar, sizeBar;
    private Button compressButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(20));
        root.setBackgroundColor(0xFFF5F7FA);

        TextView title = text("Image Compressor", 26, true, 0xFF123B5D);
        root.addView(title);
        TextView intro = text("Reduce image file size on your device. Your original images are not changed.", 15, false, 0xFF526273);
        LinearLayout.LayoutParams ip = params(); ip.topMargin = dp(8); ip.bottomMargin = dp(22); root.addView(intro, ip);

        Button pick = new Button(this); pick.setText("Select images");
        root.addView(pick, params());
        pick.setOnClickListener(v -> pickImages());

        selectionText = text("No images selected", 14, false, 0xFF334155);
        LinearLayout.LayoutParams sp = params(); sp.topMargin = dp(10); sp.bottomMargin = dp(20); root.addView(selectionText, sp);

        qualityText = text("JPEG quality: 75%", 16, true, 0xFF123B5D); root.addView(qualityText, params());
        qualityBar = new SeekBar(this); qualityBar.setMax(90); qualityBar.setProgress(65);
        root.addView(qualityBar, params());
        qualityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean u) { qualityText.setText("JPEG quality: " + (p + 10) + "%"); }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });

        sizeText = text("Maximum dimension: Original size", 16, true, 0xFF123B5D);
        LinearLayout.LayoutParams mp = params(); mp.topMargin = dp(18); root.addView(sizeText, mp);
        sizeBar = new SeekBar(this); sizeBar.setMax(5); sizeBar.setProgress(0); root.addView(sizeBar, params());
        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            final int[] values = {0, 2560, 1920, 1600, 1280, 1024};
            public void onProgressChanged(SeekBar b, int p, boolean u) { sizeText.setText(p == 0 ? "Maximum dimension: Original size" : "Maximum dimension: " + values[p] + " px"); }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });

        compressButton = new Button(this); compressButton.setText("Compress and save"); compressButton.setEnabled(false);
        LinearLayout.LayoutParams cp = params(); cp.topMargin = dp(26); root.addView(compressButton, cp);
        compressButton.setOnClickListener(v -> compressSelected());

        statusText = text("Compressed images will be saved in Pictures/ImageCompressor.", 14, false, 0xFF526273);
        LinearLayout.LayoutParams tp = params(); tp.topMargin = dp(16); root.addView(statusText, tp);
        setContentView(root);
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*"); intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGES);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGES || resultCode != RESULT_OK || data == null) return;
        selected.clear();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) selected.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) selected.add(data.getData());
        selectionText.setText(selected.size() + " image(s) selected");
        compressButton.setEnabled(!selected.isEmpty());
        statusText.setText("Ready to compress. Original files will remain untouched.");
    }

    private void compressSelected() {
        compressButton.setEnabled(false);
        int quality = qualityBar.getProgress() + 10;
        int maxDimension = new int[]{0, 2560, 1920, 1600, 1280, 1024}[sizeBar.getProgress()];
        int success = 0; long before = 0, after = 0;
        for (Uri uri : selected) {
            try {
                long originalSize = getSize(uri);
                Bitmap bitmap;
                try (InputStream in = getContentResolver().openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(in); }
                if (bitmap == null) throw new Exception("Unsupported image");
                Bitmap output = bitmap;
                if (maxDimension > 0 && Math.max(bitmap.getWidth(), bitmap.getHeight()) > maxDimension) {
                    float scale = (float) maxDimension / Math.max(bitmap.getWidth(), bitmap.getHeight());
                    output = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
                }
                String name = "compressed_" + System.currentTimeMillis() + "_" + success + ".jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageCompressor");
                Uri saved = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (saved == null) throw new Exception("Could not create output file");
                try (OutputStream out = getContentResolver().openOutputStream(saved)) {
                    if (out == null || !output.compress(Bitmap.CompressFormat.JPEG, quality, out)) throw new Exception("Compression failed");
                }
                before += originalSize; after += getSize(saved); success++;
                if (output != bitmap) output.recycle(); bitmap.recycle();
            } catch (Exception e) { /* Continue processing the remaining images. */ }
        }
        String result = success + " of " + selected.size() + " image(s) saved.";
        if (success > 0) result += "\nOriginal: " + formatBytes(before) + "  →  Compressed: " + formatBytes(after);
        statusText.setText(result + "\nFolder: Pictures/ImageCompressor");
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        compressButton.setEnabled(!selected.isEmpty());
    }

    private long getSize(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getLong(0);
        } catch (Exception ignored) {}
        return 0;
    }
    private String formatBytes(long n) {
        if (n < 1024) return n + " B";
        return new DecimalFormat("0.00").format(n / 1024.0 / 1024.0) + " MB";
    }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t;
    }
    private LinearLayout.LayoutParams params() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
