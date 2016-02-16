package com.hansongwang.android_apps_demos;

import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import com.hansongwang.android_apps_demos.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class UploadPhotoActivity extends ActionBarActivity {

	private static String logtag = "AppsDemos";

	ProgressDialog prgDialog;
	private static int RESULT_LOAD_IMG = 1;
	String imgPath, fileName;
	RequestParams params = new RequestParams();
	String encodedString;
	Bitmap bitmap;

	ImageView ivImageToUpload;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_photo);
		prgDialog = new ProgressDialog(this);
		prgDialog.setCancelable(false);

		ivImageToUpload = (ImageView) findViewById(R.id.ivImageToUpload);
	}

	public void loadImagefromGallery(View view) {
		Intent galleryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {

				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				imgPath = cursor.getString(columnIndex);
				cursor.close();
				ivImageToUpload.setImageBitmap(BitmapFactory.decodeFile(imgPath));
				String fileNameSegments[] = imgPath.split("/");
				fileName = fileNameSegments[fileNameSegments.length - 1];
				params.put("filename", fileName);

			} else {
				Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			Log.e(logtag, e.toString());
			Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
		}

	}

	public void uploadImage(View v) {
		if (imgPath != null && !imgPath.isEmpty()) {
			prgDialog.setMessage("Converting Image to Binary Data");
			prgDialog.show();
			encodeImagetoString();
		} else {
			Toast.makeText(getApplicationContext(), "You must select image from gallery before you try to upload",
					Toast.LENGTH_LONG).show();
		}
	}

	public void encodeImagetoString() {
		new AsyncTask<Void, Void, String>() {
			protected void onPreExecute() {
			};

			@Override
			protected String doInBackground(Void... params) {
				BitmapFactory.Options options = null;
				options = new BitmapFactory.Options();
				options.inSampleSize = 3;
				bitmap = BitmapFactory.decodeFile(imgPath, options);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
				byte[] byte_arr = stream.toByteArray();
				encodedString = Base64.encodeToString(byte_arr, 0);
				return "";
			}

			@Override
			protected void onPostExecute(String msg) {
				prgDialog.setMessage("Calling Upload");
				params.put("image", encodedString);
				triggerImageUpload();
			}
		}.execute(null, null, null);
	}

	public void triggerImageUpload() {
		makeHTTPCall();
	}

	public void makeHTTPCall() {
		prgDialog.setMessage("Invoking JSP");
		AsyncHttpClient client = new AsyncHttpClient();
		client.post("http://resume-hansongwang.rhcloud.com/uploadimg.jsp", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				prgDialog.hide();
				Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
			}

			@Override
			public void onFailure(int statusCode, Throwable error, String content) {
				prgDialog.hide();
				if (statusCode == 404) {
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				} else if (statusCode == 500) {
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(getApplicationContext(),
							"Error Occured, please check your Internet \n Most Common Error: \n1. Device not connected to Internet\n2. Web App is not deployed in App server\n3. App server is not running\n HTTP Status code : "
									+ statusCode,
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (prgDialog != null) {
			prgDialog.dismiss();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(com.hansongwang.android_apps_demos.R.menu.main, menu);
		return true;
	}

}
