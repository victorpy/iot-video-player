// LoginActivity.java
package com.victorpy.iotvideoapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_PATH = "/api/v1/login";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String TOKEN_KEY = "token";
    private static final String REMEMBER_ME_KEY = "remember_me";
    private static final String USERNAME_KEY = "remember_username";
    private static final String PASSWORD_KEY = "remember_password";
    private static final String HOSTNAME_KEY = "remember_hostname";

    private EditText usernameInput;
    private EditText passwordInput;
    private EditText hostnameInput;
    private Button loginButton;
    private CheckBox rememberMeCheckbox;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        hostnameInput = findViewById(R.id.hostname_input);
        loginButton = findViewById(R.id.login_button);
        rememberMeCheckbox = findViewById(R.id.remember_me);

        client = new OkHttpClient();

        // Load saved credentials if Remember Me was checked
        loadSavedCredentials();

        // Button click listener to start the login process
        loginButton.setOnClickListener(v -> performLogin());
    }

    private void loadSavedCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = sharedPreferences.getBoolean(REMEMBER_ME_KEY, false);

        if (rememberMe) {
            String savedUsername = sharedPreferences.getString(USERNAME_KEY, "");
            String savedPassword = sharedPreferences.getString(PASSWORD_KEY, "");
            String savedHostname = sharedPreferences.getString(HOSTNAME_KEY, "");
            usernameInput.setText(savedUsername);
            passwordInput.setText(savedPassword);
            hostnameInput.setText(savedHostname);
            rememberMeCheckbox.setChecked(true);
        }
    }

    // Perform login by sending a POST request to the API
    private void performLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String hostname = hostnameInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || hostname.isEmpty()) {
            Toast.makeText(this, "Please enter username, password and hostname", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create request body with form-data parameters
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", username)
                .addFormDataPart("password", password)
                .build();

        String loginUrl = "https://"+hostname+LOGIN_PATH;
        // Create the request
        Request request = new Request.Builder()
                .url(loginUrl)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "multipart/form-data")
                .build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    handleLoginSuccess(responseBody, password);
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Handle successful login response
    private void handleLoginSuccess(String responseBody, String password) {
        try {
            // Parse the JSON response
            JSONObject jsonObject = new JSONObject(responseBody);
            String email = jsonObject.getString("email");
            String username = jsonObject.getString("username");
            String token = jsonObject.getString("token");

            // Save token and user info in SharedPreferences for future use
            String hostname = hostnameInput.getText().toString().trim();
            saveLoginDetails(token, username, email, password, hostname);

            // Navigate to another activity (e.g., main screen)
            runOnUiThread(() -> {
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                // Redirect to the main activity or another screen
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });

        } catch (JSONException e) {
            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to parse login response", Toast.LENGTH_SHORT).show());
        }
    }

    // Save the login details in SharedPreferences
    private void saveLoginDetails(String token, String username, String email, String password, String hostname) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TOKEN_KEY, token);

        // Save credentials if "Remember Me" is checked
        if (rememberMeCheckbox.isChecked()) {
            editor.putBoolean(REMEMBER_ME_KEY, true);
            editor.putString(USERNAME_KEY, username);
            editor.putString(PASSWORD_KEY, password);
            editor.putString(HOSTNAME_KEY, hostname);

        } else {
            editor.putBoolean(REMEMBER_ME_KEY, false);
            editor.remove(USERNAME_KEY);
            editor.remove(PASSWORD_KEY);
        }

        editor.putString("email", email);
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putString("hostname",hostname);
        editor.apply();
    }
}
