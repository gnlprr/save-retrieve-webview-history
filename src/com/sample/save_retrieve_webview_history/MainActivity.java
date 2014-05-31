package com.sample.save_retrieve_webview_history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity {
	WebView mwebview;
	static String ServerName, ServerUrl;
	int ServerId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get Values from Intent
		ServerId = getIntent().getExtras() != null ? getIntent().getExtras()
				.getInt("ID") : 0;
		ServerName = getIntent().getExtras() != null ? getIntent().getExtras()
				.getString("NAME") : "";
		ServerUrl = getIntent().getExtras() != null ? getIntent().getExtras()
				.getString("URL") : "";

		while (ServerUrl.charAt(ServerUrl.length() - 1) == '/') {
			ServerUrl = ServerUrl.substring(0, ServerUrl.length() - 1);
		}

		// Create Instance of CookieSyncManager
		CookieSyncManager.createInstance(MainActivity.this);

		// Setting up my WebView
		mwebview = (WebView) findViewById(R.id.webView1);

		mwebview.getSettings().setJavaScriptEnabled(true);
		mwebview.getSettings().setLoadsImagesAutomatically(true);
		mwebview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mwebview.setWebViewClient(new MyBrowser());
		mwebview.getSettings().setBuiltInZoomControls(true);
		mwebview.getSettings().setDisplayZoomControls(false);
		mwebview.getSettings().setLoadWithOverviewMode(true);
		mwebview.getSettings().setUseWideViewPort(true);

		// Here is the Key to Save all History!!!!
		Bundle savedWebview = restoreFromFile();

		// if not found load the Default URL
		if (savedWebview != null) {
			mwebview.restoreState(savedWebview);
		} else {
			mwebview.loadUrl("http://"
					+ ServerUrl.toLowerCase(Locale.US).replace("http://", ""));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Start CookieSyncManager
		CookieSyncManager.getInstance().startSync();
		// for when the user navigate away for a while
		mwebview.onResume();
	}

	@Override
	protected void onPause() {
		// for when the user navigate away for a while
		mwebview.onPause();
		// Stop Sync
		CookieSyncManager.getInstance().stopSync();
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// I'm just using this method for it's Timeline
		// as it's Called when the android system want to
		// Destroy this Activity.

		// Create a new Fresh Bundle
		Bundle in = new Bundle();

		// Save all My Webview History in it
		mwebview.saveState(in);

		// Check if a File for this ServerID Exist if yes DELETE
		File webviewSaveFile = new File(getFilesDir().getPath()
				+ "/webviewForServer" + ServerId);
		if (webviewSaveFile.exists()) {
			webviewSaveFile.delete();
		}

		// Create a New File for this ServerID
		FileOutputStream fos;
		try {
			fos = openFileOutput("webviewHisForServer" + ServerId,
					Context.MODE_PRIVATE);
			Parcel p = Parcel.obtain(); // creating empty parcel object
			in.writeToParcel(p, 0); // saving bundle as parcel
			fos.write(p.marshall()); // writing parcel to file
			fos.flush();
			fos.close();

			// Here is a Key Info:
			// We have to Save the Current Android Version for when we retrieve
			// the File later.... SEE restoreFromFile()
			getSharedPreferences(Helper.pref, MODE_PRIVATE)
					.edit()
					.putString(Helper.AndroidVersion_pref_Key + ServerId,
							Build.FINGERPRINT).commit();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Bundle restoreFromFile() {
		// First Check if File Exist otherwise Return NULL
		File webviewSaveFile = new File(getFilesDir().getPath()
				+ "/webviewForServer" + ServerId);
		if (!webviewSaveFile.exists())
			return null;

		// Check the SharedPreferences for what Android Version was at
		// the time this file was Created then compare it with the current
		// Version Because each Phone and Android API Save Parcel to File
		// Differently so if the user Upgraded or even updated their phone
		// the Results will not Match
		if (!getSharedPreferences(Helper.pref, MODE_PRIVATE).getString(
				Helper.AndroidVersion_pref_Key + ServerId, "").matches(
				Build.FINGERPRINT)) {
			webviewSaveFile.delete();
			return null;
		}
		// if we reach here we ARE SAFE to Read the File :)

		// New Bundle for the Return
		Bundle bundle = null;

		// the Rest is Just Boring....

		FileInputStream fis;
		try {
			fis = new FileInputStream(webviewSaveFile);

			byte fileContent[] = new byte[(int) webviewSaveFile.length()];

			fis.read(fileContent);
			fis.close();
			Parcel parcel = Parcel.obtain();
			parcel.unmarshall(fileContent, 0, fileContent.length);
			parcel.setDataPosition(0);
			bundle = parcel.readBundle();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bundle;
	}

	private class MyBrowser extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return false;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			CookieSyncManager.getInstance().sync();
		}
	}
}
