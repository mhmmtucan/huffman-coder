package com.example.malkoch.huffman_coder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    Button loginBtn;
    EditText username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginBtn = (Button) findViewById(R.id.loginBtn);
        username = (EditText) findViewById(R.id.username);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });
    }
    public void Login() {
        String user= username.getText().toString().trim();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("EXTRA_USERNAME", user);
        startActivity(intent);

    }
}
