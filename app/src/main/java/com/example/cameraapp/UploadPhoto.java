package com.example.cameraapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class UploadPhoto extends AppCompatActivity {

    public String encodeTobase64(Bitmap image)
    {
        Bitmap imagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);
        return imageEncoded;
    }

    public boolean validateIP( String ip)
    {
        Pattern pat = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return pat.matcher(ip).matches();
    }
    public String sendPostRequest(String requestURL, HashMap<String,String>postDataParams)
    {
        URL url=null;
        String response = "";
        HttpURLConnection connection;
        try {
            url = new URL(requestURL);
            connection = (HttpURLConnection)  url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(1000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");

            OutputStream outputStream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(getPostDataString(postDataParams));
            writer.flush();
            writer.close();
            outputStream.close();
            int responseCode = connection.getResponseCode();

            if(responseCode == HttpURLConnection.HTTP_OK)
            {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line=br.readLine()) != null )
                {
                    response += line;
                }
            }
            else
            {
                response ="Error uploading photo";
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = e.getMessage();
        }
        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    Spinner photoCategorySpinner;
    ImageView imageView;

    Button uploadButton;
    Button backButton;

    EditText editTextIP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        Intent intent = getIntent();
        Bitmap photo = (Bitmap) intent.getParcelableExtra("photo");
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(photo);
        photoCategorySpinner = (Spinner) findViewById(R.id.photoCategorySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.photoCategoryArray, android.R.layout.simple_spinner_item);
        photoCategorySpinner.setAdapter(adapter);

        editTextIP = (EditText)findViewById(R.id.editTextIP);

        uploadButton = (Button)findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v->{
            String ip = editTextIP.getText().toString();
            if(ip=="")
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Please enter IP", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            if(!validateIP(ip))
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Invalid IP", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            String urlString = "http://" + ip + ":45/upload";
            if(photoCategorySpinner.getSelectedItem().toString() != "")
            {
                urlString = urlString + "?category=" + photoCategorySpinner.getSelectedItem().toString();
            }
            else
            {
                urlString = urlString + "?category=other";
            }
            final String response;
            if(photo!=null)
            {
                //call http post request here.
                //https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
                String attachmentName = "photoFile";
                String photoString = encodeTobase64(photo);
                HashMap<String,String> postData = new HashMap<>();
                postData.put(attachmentName, photoString);
                postData.put("extension", "png");
                postData.put("photoWidth", String.valueOf(photo.getWidth()));
                postData.put("photoHeight", String.valueOf(photo.getHeight()));

                String finalUrlString = urlString;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String response = sendPostRequest(finalUrlString, postData);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast = Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            });
                        }}).start();
            }
            else
            {
                //error return
                Toast toast = Toast.makeText(getApplicationContext(), "Invalid photo", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(v->{
            super.onBackPressed();
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
    }
}