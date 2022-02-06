package com.example.avekshaka;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private Button mupdatebtn;
    private EditText mnameEV,mPhoneEV,mcrendential;
    private ImageView mprofileIV;

    private  static  final int CAMERA_REQUEST_CODE=200;
    private  static  final int STORAGE_REQUEST_CODE=300;

    //image picker constants
    private  static  final int IMAGE_PICKER_GALLERY_CODE=400;
    private  static  final int IMAGE_PICKER_CAMERA_CODE=500;

    // permission arrays
    private String[] camerapermissions;
    private String[] storagepermissions;
    private Uri imageUri;

    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mupdatebtn=findViewById(R.id.savechangesbtn);
        mprofileIV= findViewById(R.id.memoryIV);
        mcrendential=findViewById(R.id.credentialtv);
        mPhoneEV=findViewById(R.id.Phonetv);
        mnameEV=findViewById(R.id.NameTV);

        camerapermissions= new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagepermissions= new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);
        checkUser();
        mprofileIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // detect image
                showImagePicker();
            }
        });
        mupdatebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start updating profile
                inputData();
            }
        });
    }

    private String fullname,phoneno,credential;
    private void inputData() {
        //input data;
        fullname=mnameEV.getText().toString().trim();
        phoneno=mPhoneEV.getText().toString().trim();
        credential=mcrendential.getText().toString().trim();

        updateProfile();

    }
    private void updateProfile() {
        progressDialog.setMessage("Saving Account info...");
        progressDialog.show();
        String timeStamp=""+System.currentTimeMillis();
        if(imageUri==null) {
            //save info without image
            //setup data to save
            HashMap<String, Object> hashMap = new HashMap<>();

            hashMap.put("name",""+fullname);
            hashMap.put("phone",""+phoneno);
            hashMap.put("credential",""+credential);
            //update to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //db updated
                            progressDialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully..,", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed to update db
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });


        }
        else{
            //save info with image
            //name and path of image
            String filePathandName ="profile_image/"+firebaseAuth.getUid();
            //upload image
            StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathandName);
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //get url of uploaded image
                            Task<Uri> uriTask =taskSnapshot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());
                            Uri dowloadIamgeUri=uriTask.getResult();
                            if(uriTask.isSuccessful()){
                                //setup data to save
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("name",""+fullname);
                                hashMap.put("phone",""+phoneno);
                                hashMap.put("credential",""+credential);
                                hashMap.put("profileImage",""+dowloadIamgeUri);//url of uploaded image

                                //save to db
                                DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                //db updated
                                                progressDialog.dismiss();
                                                Toast.makeText(ProfileActivity.this, "Profile updated successfully..,", Toast.LENGTH_SHORT).show();
                                                finish();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull  Exception e) {
                                        //failed to update db
                                        progressDialog.dismiss();
                                        Toast.makeText(ProfileActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull  Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void checkUser() {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null){
            startActivity(new Intent(ProfileActivity.this,LoginActivity.class));
            finish();

        }else{
            loadmyInfo();
        }
    }
    private void loadmyInfo() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            String name=""+ds.child("name").getValue();
                            String phone=""+ds.child("phone").getValue();
                            String profileImage=""+ds.child("profileImage").getValue();
                            String creden=""+ds.child("credential").getValue();
                            String timestamp=""+ds.child("timestamp").getValue();
                            String uid=""+ds.child("uid").getValue();

                            mnameEV.setText(name);
                            mPhoneEV.setText(phone);
                           mcrendential.setText(creden);

                            try{
                                Picasso.get().load(profileImage).placeholder(R.drawable.memory).into(mprofileIV);
                            }
                            catch(Exception e){
                                mprofileIV.setImageResource(R.drawable.memory);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void showImagePicker() {
        //options to display in dialog
        String[] options= {"Camera","Gallery"};
        //dialog
        AlertDialog.Builder builder =  new AlertDialog.Builder(this);
        builder.setTitle("Pick image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle clicks
                        if(which==0){
                            //camera clicked
                            if(checkCameraPermission()){//camera permission allowed
                                PickImagefromCamera();
                            }else{//denied,request
                                requestCameraPermission();
                            }
                        }
                        else{
                            //gallery clicked
                            if(ckeckStoragePermission()){//storage permission allowed
                                PickImagefromGallery();
                            }else{
                                //denied,request
                                requestStoragePermission();
                            }
                        }
                    }
                })
                .show();
    }

    private boolean checkCameraPermission(){
        boolean result= ContextCompat.checkSelfPermission
                (this,Manifest.permission.CAMERA)== (PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission
                (this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== (PackageManager.PERMISSION_GRANTED);
        return  result&&result1;
    }
    private  void  requestCameraPermission(){
        ActivityCompat.requestPermissions(this,camerapermissions,CAMERA_REQUEST_CODE);
    }

    private boolean ckeckStoragePermission(){
        boolean result=ContextCompat.checkSelfPermission
                (this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== (PackageManager.PERMISSION_GRANTED);
        return  result;
    }
    private  void  requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagepermissions,STORAGE_REQUEST_CODE);
    }
    private void PickImagefromGallery(){
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICKER_GALLERY_CODE);
    }
    private void PickImagefromCamera(){
        ContentValues contentValues =new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Image Description");
        imageUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,IMAGE_PICKER_CAMERA_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull  int[] grantResults) {
        switch (requestCode){


            case CAMERA_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean cameraAccepted=grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted=grantResults[1] ==PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        //permission allowed
                        PickImagefromCamera();
                    }
                    else{
                        Toast.makeText(this,"Camera permission are necessary...",Toast.LENGTH_SHORT).show();
                        //permission denied
                    }
                }
            }

            case STORAGE_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean storageAccepted=grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        //permission allowed
                        PickImagefromGallery();
                    }
                    else{
                        Toast.makeText(this,"Storage permission are necessary...",Toast.LENGTH_SHORT).show();
                        //permission denied
                    }
                }
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode==RESULT_OK){
            if(requestCode==IMAGE_PICKER_GALLERY_CODE){
                //get picked image
                imageUri =data.getData();
                //set to imageview
                mprofileIV.setImageURI(imageUri);
            }
            else if(requestCode==IMAGE_PICKER_CAMERA_CODE){
                //set to imageview
                mprofileIV.setImageURI(imageUri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}