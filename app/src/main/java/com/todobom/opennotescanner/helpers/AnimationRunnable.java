package com.todobom.opennotescanner.helpers;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.todobom.opennotescanner.OpenNoteScannerActivity;
import com.todobom.opennotescanner.R;

import org.opencv.core.Point;
import org.opencv.core.Size;

import static com.todobom.opennotescanner.helpers.Utils.decodeSampledBitmapFromUri;

public class AnimationRunnable implements Runnable {

	private static final String TAG = "AnimationRunnable";

	private final OpenNoteScannerActivity activity;
	private Size imageSize;
	private Point[] previewPoints = null;
	public Size previewSize = null;
	public String fileName = null;
	public int width;
	public int height;
	private Bitmap bitmap;

	public AnimationRunnable(OpenNoteScannerActivity activity, String filename, ScannedDocument document) {
		this.activity = activity;
		this.fileName = filename;
		this.imageSize = document.processed.size();

		if (document.quadrilateral != null) {
			this.previewPoints = document.previewPoints;
			this.previewSize = document.previewSize;
		}
	}

	public double hipotenuse(Point a, Point b) {
		return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
	}

	@Override
	public void run() {
		final ImageView imageView = (ImageView) activity.findViewById(R.id.scannedAnimation);

		Display display = activity.getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getRealSize(size);

		int width = Math.min(size.x, size.y);
		int height = Math.max(size.x, size.y);

		// ATENTION: captured images are always in landscape, values should be swapped
		double imageWidth = imageSize.height;
		double imageHeight = imageSize.width;

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

		if (previewPoints != null) {
			double documentLeftHeight = hipotenuse(previewPoints[0], previewPoints[1]);
			double documentBottomWidth = hipotenuse(previewPoints[1], previewPoints[2]);
			double documentRightHeight = hipotenuse(previewPoints[2], previewPoints[3]);
			double documentTopWidth = hipotenuse(previewPoints[3], previewPoints[0]);

			double documentWidth = Math.max(documentTopWidth, documentBottomWidth);
			double documentHeight = Math.max(documentLeftHeight, documentRightHeight);

			Log.d(TAG, "device: " + width + "x" + height + " image: " + imageWidth + "x" + imageHeight + " document: " + documentWidth + "x" + documentHeight);


			Log.d(TAG, "previewPoints[0] x=" + previewPoints[0].x + " y=" + previewPoints[0].y);
			Log.d(TAG, "previewPoints[1] x=" + previewPoints[1].x + " y=" + previewPoints[1].y);
			Log.d(TAG, "previewPoints[2] x=" + previewPoints[2].x + " y=" + previewPoints[2].y);
			Log.d(TAG, "previewPoints[3] x=" + previewPoints[3].x + " y=" + previewPoints[3].y);

			// ATENTION: again, swap width and height
			double xRatio = width / previewSize.height;
			double yRatio = height / previewSize.width;

			params.topMargin = (int) (previewPoints[3].x * yRatio);
			params.leftMargin = (int) ((previewSize.height - previewPoints[3].y) * xRatio);
			params.width = (int) (documentWidth * xRatio);
			params.height = (int) (documentHeight * yRatio);
		} else {
			params.topMargin = height / 4;
			params.leftMargin = width / 4;
			params.width = width / 2;
			params.height = height / 2;
		}

		bitmap = decodeSampledBitmapFromUri(fileName, params.width, params.height);

		imageView.setImageBitmap(bitmap);

		imageView.setVisibility(View.VISIBLE);

		TranslateAnimation translateAnimation = new TranslateAnimation(
				Animation.ABSOLUTE, 0, Animation.ABSOLUTE, -params.leftMargin,
				Animation.ABSOLUTE, 0, Animation.ABSOLUTE, height - params.topMargin
		);

		ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0);

		AnimationSet animationSet = new AnimationSet(true);

		animationSet.addAnimation(scaleAnimation);
		animationSet.addAnimation(translateAnimation);

		animationSet.setDuration(600);
		animationSet.setInterpolator(new AccelerateInterpolator());

		animationSet.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				imageView.setVisibility(View.INVISIBLE);
				imageView.setImageBitmap(null);
				if (AnimationRunnable.this.bitmap != null) {
					AnimationRunnable.this.bitmap.recycle();
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		imageView.startAnimation(animationSet);

	}
}