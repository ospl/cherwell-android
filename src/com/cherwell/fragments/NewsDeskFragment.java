package com.cherwell.fragments;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.cherwell.android.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.activities.BaseActivity;
import com.cherwell.utilities.GeneralUtils;
import com.cherwell.utilities.Mail;
import com.cherwell.utilities.SerializableList;
import com.google.analytics.tracking.android.Tracker;

public class NewsDeskFragment extends SherlockFragment implements OnClickListener {

	private boolean cancelTask = false;
	
	private final String subjectKey = "subject";
	private final String messageKey = "message";
	private final String nameKey = "name";
	private final String emailKey = "email";
	private final String attachmentsKey = "attachments";
	
	private final String sendToEmail = "editorial@cherwell.org";
	private final String sendFromEmail = "cherwellapp@gmail.com";
	private final String sendFromPassword = "Ald4tes2k13";
	private final int SELECT_PHOTO = 2;
	private boolean sending = false;
	private Tracker analytics;

	private List<File> attachedFiles = new SerializableList();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		setHasOptionsMenu(true);
		
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);														
	    
		analytics = ((BaseActivity) getSherlockActivity()).getEasyTracker();
		analytics.sendView("News Desk");

	    View view = inflater.inflate(R.layout.news_desk_layout, container, false);

		Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getSherlockActivity(), R.array.section_list, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		((Button) view.findViewById(R.id.send_button)).setOnClickListener(this);
		((CheckBox) view.findViewById(R.id.anon)).setOnClickListener(this);
		
		if (savedInstanceState != null) {
			((EditText) view.findViewById(R.id.subject)).setText(savedInstanceState.getString(subjectKey));
			((EditText) view.findViewById(R.id.message)).setText(savedInstanceState.getString(messageKey));
			((EditText) view.findViewById(R.id.name)).setText(savedInstanceState.getString(nameKey));
			((EditText) view.findViewById(R.id.email)).setText(savedInstanceState.getString(emailKey));
			boolean enabled = !((CheckBox) view.findViewById(R.id.anon)).isChecked();
			((EditText) view.findViewById(R.id.name)).setEnabled(enabled);
			((EditText) view.findViewById(R.id.email)).setEnabled(enabled);					
			attachedFiles = (ArrayList<File>) savedInstanceState.getSerializable(attachmentsKey);
			updateAttachments(view);
		}
		
	    return view;    		
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putString(nameKey, ((EditText) getSherlockActivity().findViewById(R.id.name)).getText().toString());
		outState.putString(subjectKey, ((EditText) getSherlockActivity().findViewById(R.id.subject)).getText().toString());
		outState.putString(emailKey, ((EditText) getSherlockActivity().findViewById(R.id.email)).getText().toString());
		outState.putString(messageKey, ((EditText) getSherlockActivity().findViewById(R.id.message)).getText().toString());
		outState.putSerializable(attachmentsKey, (Serializable) attachedFiles);
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onDestroy() {
		cancelTask = true;
		super.onDestroy();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.news_desk_fragment_menu, menu);									// Inflate the menu; this adds items to the action bar if it is present.		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.attach:
	        	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
	        	photoPickerIntent.setType("image/*,video/*");
	        	startActivityForResult(photoPickerIntent, SELECT_PHOTO);
	        	break;    
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.attach).setEnabled(!sending);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
	    if (requestCode == SELECT_PHOTO) {
	        if (resultCode == Activity.RESULT_OK) {
	        	File mFile = new File(getRealPathFromURI(imageReturnedIntent.getData()));
	        	attachedFiles.add(mFile);	        	
				updateAttachments(null);
	        }
	    }
	}
	
	private String getRealPathFromURI(Uri contentUri) {
	    String res = null;
	    String[] proj = { MediaStore.Images.Media.DATA };
	    Cursor cursor = getSherlockActivity().getContentResolver().query(contentUri, proj, null, null, null);
	    if(cursor.moveToFirst()){;
	       int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	       res = cursor.getString(column_index);
	    }
	    cursor.close();
	    return res;
	}

	
	private class SendEmailTask extends AsyncTask<String, Void, String> {

		private String subject;
		private String body;
		private List<File> attachments;
		private boolean success  = true;
		
		public SendEmailTask(String subject, String body, String contactNameEmail, List<File> attachments) {
			((Button) getSherlockActivity().findViewById(R.id.send_button)).setEnabled(false);
			((Button) getSherlockActivity().findViewById(R.id.send_button)).setText("Sending");
			sending = true;
			getSherlockActivity().invalidateOptionsMenu();
			
			this.attachments = attachments;
			this.body = body + "\n\n" + contactNameEmail + "\n\n" + "Sent from the Cherwell app for Android";
			this.subject = subject;
		}

		@Override
		protected String doInBackground(String... params) {
			Mail m = new Mail(sendFromEmail, sendFromPassword);

			String[] toArr = { sendToEmail };
			m.setTo(toArr);
			m.setSubject(subject);
			m.setBody(body);
			m.setFrom(sendFromEmail);
			
			try {
				for (int i = 0; attachments != null &&  i < attachments.size(); i++) {
					m.addAttachment(attachments.get(i));
				}
				success = m.send();
			} catch (Exception e) {
				success = false;
				Log.v("user-gen", e.toString());
			}

			return "Executed";
		}

		@Override
		protected void onPostExecute(String result) {
			if (cancelTask) return;
			
			if (success) {
				  analytics.sendEvent("Tip-off email sent", "news desk page", subject, null);

				Toast.makeText(getSherlockActivity(), "Your message has been sent", Toast.LENGTH_LONG).show();
				 ((EditText) getSherlockActivity().findViewById(R.id.subject)).setText("");
				 ((EditText) getSherlockActivity().findViewById(R.id.message)).setText("");
				 ((EditText) getSherlockActivity().findViewById(R.id.name)).setText("");
				 ((EditText) getSherlockActivity().findViewById(R.id.email)).setText("");
				 ((CheckBox) getSherlockActivity().findViewById(R.id.anon)).setChecked(false);
				 ((Spinner) getSherlockActivity().findViewById(R.id.spinner)).setSelection(0);
				 attachedFiles.clear();
				 updateAttachments(null);
			}
			else
				Toast.makeText(getSherlockActivity(), "There was an error sending your message", Toast.LENGTH_LONG).show();
		
			((Button) getSherlockActivity().findViewById(R.id.send_button)).setEnabled(true);
			((Button) getSherlockActivity().findViewById(R.id.send_button)).setText("Send");
			sending = false;
			getSherlockActivity().invalidateOptionsMenu();
		}
	}

	private void updateAttachments(View v) {
		ViewGroup parent;
		if (v == null) {
			parent = (ViewGroup) getSherlockActivity().findViewById(R.id.attachment_parent);
		} else {
			parent = (ViewGroup) v.findViewById(R.id.attachment_parent);
		}
		parent.removeAllViews();

		for (int i = 0; attachedFiles != null &&  i < attachedFiles.size(); i++) {
			View attachmentView = getSherlockActivity().getLayoutInflater().inflate(R.layout.attachment_layout, parent, false);
			((TextView) attachmentView.findViewById(R.id.attachment_name)).setText(attachedFiles.get(i).getName());
			((ImageView) attachmentView.findViewById(R.id.remove_attachment)).setOnClickListener(this);
			attachmentView.setId(i);
			parent.addView(attachmentView);	
		}	
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.send_button:
				String subject = ((EditText) getSherlockActivity().findViewById(R.id.subject)).getText().toString();
				String message = ((EditText) getSherlockActivity().findViewById(R.id.message)).getText().toString();
				String section = ((Spinner) getSherlockActivity().findViewById(R.id.spinner)).getSelectedItem().toString();

				String contactNameEmail = "";
				String name = ((EditText) getSherlockActivity().findViewById(R.id.name)).getText().toString();
				String email = ((EditText) getSherlockActivity().findViewById(R.id.email)).getText().toString();
				String divider = " - ";

				boolean anon = ((CheckBox) getSherlockActivity().findViewById(R.id.anon)).isChecked();
				
				if (anon) 
					contactNameEmail = "Anonymous";
				else 
					contactNameEmail = name + divider + email;
				
				
				if (subject.length() == 0 || message.length() == 0) {
					Toast.makeText(getSherlockActivity(), "Please fill in both the subject and message fields", Toast.LENGTH_LONG).show();
					return;
				}
				if (!anon && contactNameEmail.equals(divider)) {
					Toast.makeText(getSherlockActivity(), "Please fill in both your name and email, or check the anonymous box", Toast.LENGTH_LONG).show();
				}
								
				GeneralUtils.executeTask(new SendEmailTask(section + ": " + subject, message, contactNameEmail, attachedFiles));
				break;
			case R.id.anon:
				boolean enabled = !((CheckBox) v).isChecked();
				((EditText) getSherlockActivity().findViewById(R.id.name)).setEnabled(enabled);
				((EditText) getSherlockActivity().findViewById(R.id.email)).setEnabled(enabled);					
				break;
			case R.id.remove_attachment:
				Log.v("user-gen", "got here");
				attachedFiles.remove(((View) v.getParent()).getId());
				updateAttachments(null);
			default:
				break;
		}
	}
}
