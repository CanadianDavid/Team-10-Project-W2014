package ca.ualberta.team10projectw2014;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ca.ualberta.team10projectw2014.controller.CommentDataController;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CreateCommentActivity extends Activity{
	
	private String postTitle;
	private String postUsername;
	private String postContents;
	private LocationModel postLocation;
	private Bitmap postPhoto;
	private CommentModel parentModel;
	private EditText ueditText;
	private EditText teditText;
	private EditText ceditText;
	private ImageView imageView;
	protected double longitude;
	protected double latitude;
 	protected Location bestKnownLoc = null;
 	protected CommentModel model;
 	protected LocationListenerController locationListener;
 	protected LocationManager mLocationManager;
 	protected Boolean gpsEnabled;
 	protected Boolean netEnabled;
 	private CommentDataController CDC;
 	private String photoPath = null;
 	private Uri imageUri = null;
 	private Location locationGPS;
 	private Location locationNet;
	
	@SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_comment_activity);
        CDC = new CommentDataController(this, this.getString(R.string.file_name_string));
        
        ueditText = (EditText)findViewById(R.id.cc_username);
		teditText = (EditText)findViewById(R.id.cc_title);
		ceditText = (EditText)findViewById(R.id.cc_content);
		imageView = (ImageView)findViewById(R.id.cc_image_view);
	}
	
	
	@Override 
	protected void onResume(){
		super.onResume();
		
		//Receive information from the intent
		Bundle bundle = getIntent().getExtras();
		String receivedUsername = (String) bundle.getSerializable("username");
		CommentModel receivedComment = (CommentModel) bundle.getSerializable("comment");
		fillContents(receivedUsername, receivedComment);
		
		//Set imageView to either "No Image" or the picture returned from camera
		//setPic();
		
		//TODO Check to see if GPS is enabled
        //TODO Start listening for location information
        startListeningLocation();
	}
	
	private void startListeningLocation(){
		Toast.makeText(getBaseContext(), "Starting to listen for location...", Toast.LENGTH_LONG).show();
		this.locationListener = new LocationListenerController(this);
	}
	
	// Retrieved from http://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android on March 6 at 5:00
	
	private void getLastBestLocation() {
		
		bestKnownLoc = locationListener.getLastBestLocation();

	}

	public void fillContents(String username, CommentModel parentModel){
		
		if(!checkStringIsAllWhiteSpace(username)){
			this.postUsername = username;
			setUsernameView(username);
		}
		/*else{
			if(checkStringIsAllWhiteSpace(username)){
				this.postUsername = "Anonymous";
				setUsernameView(this.postUsername);
			}
			//setUsernameView("Please set a username");
		}*/
		if(parentModel != null){
			this.parentModel = parentModel;
			this.postTitle = "RE:" + parentModel.getTitle();
			teditText.setKeyListener(null);
			setTitleView(postTitle);
		}
		else{
			if (this.postTitle == null){
				this.postTitle = null;
			}
			//setTitleView("Create a Name for Your Post");
		}
	}
	
	public boolean checkStringIsAllWhiteSpace(String string){
		boolean isWhitespace = string.matches("^\\s*$");
		boolean longerThan0 = (string.trim().length() > 0);
		return isWhitespace && !longerThan0;
	}
	
	private void setUsernameView(String name){
		ueditText.setText(name, TextView.BufferType.EDITABLE);
	}
	
	private void setTitleView(String title){
		teditText.setText(title, TextView.BufferType.EDITABLE);
	}

	public void chooseLocation(View v){
		Toast.makeText(getBaseContext(), "You Want to Choose a Location, Eh?", Toast.LENGTH_LONG).show();
	}
	
	//Starts camera activity
	public void choosePhoto(View v){
		PackageManager packageManager = this.getPackageManager();
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    // Ensure that there's a camera activity to handle the intent
		    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
		        // Create the File where the photo should go
		    	File photoFile = null;
		        try {
		            photoFile = createImageFile();
		        } catch (IOException ex) {
		        	Toast.makeText(getBaseContext(), "Could not write to file", Toast.LENGTH_LONG).show();

		        }
		        // Continue only if the File was successfully created
		        if (photoFile != null) {
		        	imageUri = Uri.fromFile(photoFile);
		            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
		                    imageUri);
		            startActivityForResult(takePictureIntent, 1);
		        }
		    }

	    }
		else{
			Toast.makeText(getBaseContext(), "Sorry, you don't have a camera!", Toast.LENGTH_LONG).show();

		}
	}
	
	//Takes thumbnail from the camera activity and puts it into postPhoto.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


         // Taken from http://www.androidhive.info/2013/09/android-working-with-camera-api/
    	if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // successfully captured the image
                // display it in image view
                setPic();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }	
    }
    
    //Sends the image taken from the camera to file.
    
    private File createImageFile() throws IOException {
 
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if(!storageDir.exists())
            storageDir.mkdirs();
        File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        photoPath = image.getAbsolutePath(); //"file:" + 

        return image;
    }
    
    private void setPic() {
            try {
     
               
                BitmapFactory.Options options = new BitmapFactory.Options();

                options.inSampleSize = 8;
     
                final Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(),
                        options);
     
                imageView.setImageBitmap(bitmap);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    	/*
    	if (photoPath == null){
    		//Placeholder
    		photoPath = null;
    	}
    	else{
	        // Get the dimensions of the View
	        int targetW = imageView.getWidth();
	        int targetH = imageView.getHeight();
	
	        // Get the dimensions of the bitmap
	        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	        bmOptions.inJustDecodeBounds = true;
	        BitmapFactory.decodeFile(photoPath, bmOptions);
	        int photoW = bmOptions.outWidth;
	        int photoH = bmOptions.outHeight;
	
	        // Determine how much to scale down the image
	        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
	
	        // Decode the image file into a Bitmap sized to fill the View
	        bmOptions.inJustDecodeBounds = false;
	        bmOptions.inSampleSize = scaleFactor;
	        bmOptions.inPurgeable = true;
	
	        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
	        imageView.setImageBitmap(bitmap);
    	}*/
		
	
	//A currently redundant method which creates a location with a title (not used yet)
	private void setLocation(){
		if (bestKnownLoc != null){
			this.postLocation = new LocationModel(String.valueOf("Lat: " + bestKnownLoc.getLatitude()) + " Long: " + String.valueOf(bestKnownLoc.getLongitude()), bestKnownLoc);
		}
		else{
			this.postLocation = new LocationModel("Unknown Location", bestKnownLoc);
		}
		//TODO Set location variable to...?
		// Location should never be null
	}
	
	//Called when the user presses "Post" button to create and store a new comment
	public void attemptCommentCreation(View v){
		
		this.postContents = ceditText.getText().toString();
		this.postUsername = ueditText.getText().toString();
		this.postTitle = teditText.getText().toString();
		
		if(checkStringIsAllWhiteSpace(this.postContents)){
			raiseContentsIncompleteError();
		}
		else if(checkStringIsAllWhiteSpace(this.postTitle)){
			raiseTitleIncompleteError();
		}
		else{
			
			if(this.parentModel != null){
				
				if(checkStringIsAllWhiteSpace(this.postUsername)){
					raiseUsernameIncompleteError();
				}
				// This should be edited so that the model handles all the getting and setting
				model = new SubCommentModel(this.parentModel);
				model.setAuthor(this.postUsername);
				model.setContent(this.postContents);
				model.setLocation(this.postLocation);
				model.setPhoto(this.postPhoto);
				model.setTitle(this.postTitle);
				model.setPhotoPath(photoPath);
				
				// Sets the current date and time for the comment
				// Referenced http://stackoverflow.com/questions/16686298/string-timestamp-to-calendar-in-java on March 2
				long timestamp = System.currentTimeMillis();
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(timestamp);
				model.setTimestamp(calendar);
				model.setNumFavourites(0);
				
				//Adds the newly created model to its referrent's list of subcomments
				this.parentModel.addSubComment((SubCommentModel) model);
			}
			else{
				if(checkStringIsAllWhiteSpace(this.postUsername)){
					raiseUsernameIncompleteError();
				}
				
				// This should be edited so that the model handles all the getting and setting
				model = new CommentModel();
				model.setAuthor(this.postUsername);
				model.setContent(this.postContents);
				model.setLocation(this.postLocation);
				model.setPhoto(this.postPhoto);
				model.setTitle(this.postTitle);
				model.setPhotoPath(photoPath);
				
				long timestamp = System.currentTimeMillis();
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(timestamp);
				model.setTimestamp(calendar);
				model.setNumFavourites(0);
				
				//TODO Add this head comment to the list of head comments on the phone
			
			}
			
			//TODO Stop listening for location information

			stopListeningLocation();
			setLocation();
			
			model.setLocation(this.postLocation);


			ArrayList<CommentModel> tempList = CDC.loadFromFile();
			tempList.add(model);
			CDC.saveToFile(tempList);
			
			//Destroy this activity so that we return to the previous one.
			goBack();
		}
	}
	
	
	private void stopListeningLocation(){
		getLastBestLocation();
		if (bestKnownLoc == null){
			Toast.makeText(getBaseContext(), "Ain't no location here", Toast.LENGTH_LONG).show();
		}
		else{
			Location location = locationListener.getCurrentBestLocation();
			bestKnownLoc.setLatitude(location.getLatitude());
			bestKnownLoc.setLongitude(location.getLongitude());
			locationListener.removeUpdates();
		}
	}
	
	private void raiseContentsIncompleteError(){
		Toast.makeText(getBaseContext(), "Please Add Some Content", Toast.LENGTH_LONG).show();
	}
	
	private void raiseUsernameIncompleteError(){
		//TODO Make it so that the user is prompted to post as anonymous
	    postUsername = "Anonymous";
	}
	
	private void raiseTitleIncompleteError(){
		Toast.makeText(getBaseContext(), "Please Create a Title", Toast.LENGTH_LONG).show();
	}
	
	private void goBack(){
		finish();
	}
	
	
}
