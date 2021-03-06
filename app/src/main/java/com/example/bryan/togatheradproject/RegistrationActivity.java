package com.example.bryan.togatheradproject;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    private EditText editText_Email;
    private EditText editText_Username;
    private EditText editText_Password;
    private EditText editText_RePassword;
    private Button button_Create;
    private Button button_Cancel;
    private FirebaseAuth mAuth;

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            clearEditText();
        }
    }

    private void clearEditText() {
        editText_Email.setText("");
        editText_Password.setText("");
        editText_RePassword.setText("");
        editText_Username.setText("");
    }

    private void login(String email, String password) {
        Log.d(TAG, "Login: in");
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "onSuccess: login successful, signed in"+ FirebaseAuth.getInstance().getCurrentUser().getUid());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: Could not sign in user" + e);
            }
        });
        Log.d(TAG, "Login: out");
    }


    protected void createUser(final String regisEmail,final String regisPassword) {
        Log.d(TAG, "regisEmail :" + regisEmail);
        Log.d(TAG, "regisPassword: " + regisPassword);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(regisEmail, regisPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail: success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(getApplicationContext(), "Information verified, please click the button again",
                                            Toast.LENGTH_SHORT).show();
                                    login(regisEmail, regisPassword);
                                    updateUI(user);
                                }
                                else {
                                    Log.w(TAG, "createUserWithEmail: failure", task.getException());
                                    Toast.makeText(RegistrationActivity.this, "Account creation successful",
                                            Toast.LENGTH_SHORT).show();
                                    updateUI(null);
                                }
                            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private String whyError = "";

    public boolean checkIfPasswordSame(String password1, String password2) {
        boolean result = true;
        if (!password1.equals(password2)) {
            whyError = "Passwords are not the same";
            result = false;
        }
        return result;
    }

    public boolean checkIfPasswordValid(String password) {
        boolean result = true;
        if (password.length() < 6) {
            whyError = "Password needs to be more than 6 characters";
            result = false;
        }
        return result;
    }

    public boolean checkUserValidity(String username) {
        boolean result = true;
        if (username.length() < 4) {
            whyError = "Username must be 4 or more characters";
            result = false;
        }
        return result;
    }

    public boolean checkEmailValidity(String email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        } else {
            whyError = "invalid email";
        }
        return isValid;
    }

    public boolean checkIfDataNotBlank(String email, String name, String password, String rePassword) {
        boolean result = true;
        if (email.equals("") || name.equals("") || password.equals("") || rePassword.equals("")) {
            whyError = "Please fill all of the fields properly";
            result = false;
        }
        return result;
    }

    protected void establish(){
        editText_Email = findViewById(R.id.editText_RegistrationActivity_email);
        editText_Username = findViewById(R.id.editText_RegistrationActivity_username);
        editText_Password = findViewById(R.id.editText_RegistrationActivity_password);
        editText_RePassword = findViewById(R.id.editText_RegistrationActivity_rePassword);
        button_Create = findViewById(R.id.button_RegistrationActivity_create);
        button_Cancel = findViewById(R.id.button_RegistrationActivity_cancel);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        Log.d(TAG, "onCreate: in " + TAG);
        mAuth = FirebaseAuth.getInstance();

        //initialise widget
        establish();

        button_Create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //retrieve information from widgets
                String regisEmail = editText_Email.getText().toString();
                String regisName = editText_Username.getText().toString();
                String regisPassword = editText_Password.getText().toString();
                String regisRePassword = editText_RePassword.getText().toString();

                //validating input
                if (validate(regisEmail, regisName, regisPassword, regisRePassword)) {

                    //register user to the authentication
                    createUser(regisEmail, regisPassword);

                    //generate unique id
                    final String uid = UUID.randomUUID().toString();
                    ArrayList<String> emptyList = new ArrayList<>();

                    //create new user object
                    final User newUser = new User(regisPassword, regisName, regisEmail, 0, emptyList,uid);

                    //update the database
                    addUserToDatabase(uid, newUser);
                } else {
                    clearEditText();
                    Toast.makeText(getApplicationContext(), whyError, Toast.LENGTH_SHORT).show();
                    whyError = "";
                    button_Create.setEnabled(true);
                }
                Log.d(TAG, "onClick: create button - out");
            }
        });

        button_Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_Cancel.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Registration cancelled", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        Log.d(TAG, "onCreate: out " + TAG);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        button_Cancel.performClick();
        button_Cancel.setPressed(true);
        button_Cancel.invalidate();
        button_Cancel.setPressed(false);
        button_Cancel.invalidate();
    }

    private boolean validate(String regisEmail, String regisName, String regisPassword, String regisRePassword) {
        if (checkIfDataNotBlank(regisEmail, regisName, regisPassword, regisRePassword)
                && checkEmailValidity(regisEmail)
                && checkIfPasswordSame(regisPassword, regisRePassword)
                && checkIfPasswordValid(regisPassword)
                && checkUserValidity(regisName)) {
            return true;
        } else {
            return false;
        }
    }

    private void addUserToDatabase(final String uid, final User newUser) {
        FirebaseFirestore.getInstance().collection(Constants.USER).document(uid).set(newUser)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Account creation successful");
                        Intent intent = new Intent(getApplicationContext(), InterestActivity.class);
                        intent.putExtra(Constants.USER, newUser);
                        startActivity(intent);
                        finish();
                        button_Create.invalidate();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Account creation failed");
                    }
                });
    }
}