package pvt.talinor.ptl8.auth;

import static pvt.talinor.ptl8.MainActivity.APP_PREFERENCES_REGISTRED;
import static pvt.talinor.ptl8.MainActivity.APP_PREFERENCES_TOKEN;
import static pvt.talinor.ptl8.MainActivity.TIME_VIBRATION;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import pvt.talinor.ptl8.LoadingDialog;
import pvt.talinor.ptl8.MainActivity;
import pvt.talinor.ptl8.R;

/**
 * <p> Created by Rubezhin Evgenij on 6/18/2019. <br>
 * Copyright (c) 2019 LineUp. <br> Project: bm71term, pvt.talinor.ptl8.auth </p>
 *
 * @author Rubezhin Evgenij
 * @version 1.0
 */
public class LoginActivity extends AppCompatActivity {

  @BindView(R.id.et_email)
  EditText et_email;
  @BindView(R.id.et_password)
  EditText et_password;
  Vibrator vibe;
  private FirebaseAuth mAuth;
  private SharedPreferences mSettings;
  private LoadingDialog loader;

  @OnClick(R.id.btn_new_user_send_info)
  public void onNewUserClicked() {
    vibe.vibrate(TIME_VIBRATION);
    // получаем заполненые поля в переменные
    String mEMail = et_email.getText().toString();
    String mPassword = et_password.getText().toString();

    if (mEMail.isEmpty()) {
      Toast.makeText(this, "Email field is empty", Toast.LENGTH_SHORT).show();
      return;
    }
    if (mPassword.isEmpty()) {
      Toast.makeText(this, "Password field is empty", Toast.LENGTH_SHORT).show();
      return;
    } else if (mPassword.length() < 6) {
      Toast.makeText(this, "Password must have more than 6 symbols", Toast.LENGTH_SHORT).show();
      return;
    }

    showDialog();
    mAuth.signInWithEmailAndPassword(mEMail, mPassword)
        .addOnCompleteListener(this, task -> {
          if (task.isSuccessful()) {
            FirebaseUser user = mAuth.getCurrentUser();
            successLogin(user);
          } else {
            hideDialog();
            Toast.makeText(LoginActivity.this, "Authentication failed",
                Toast.LENGTH_SHORT).show();
          }
        });
  }

  public void successLogin(FirebaseUser user) {
    if (user.isEmailVerified()) {
      SharedPreferences.Editor editor = mSettings.edit();
      editor.putInt(APP_PREFERENCES_REGISTRED, 1);
      editor.putString(APP_PREFERENCES_TOKEN, user.getEmail());
      editor.apply();

      hideDialog();

      startActivity(new Intent(this, MainActivity.class));
      finish();
    } else {
      hideDialog();
      Toast.makeText(this, "Please, verify your email!",
          Toast.LENGTH_SHORT).show();
    }
  }

  @OnClick(R.id.sign_up)
  public void onRegisterClicked() {
    vibe.vibrate(TIME_VIBRATION);
    startActivity(new Intent(this, RegisterActivity.class));
  }

  @OnClick(R.id.forgot_password)
  public void onForgotPasswordClicked() {
    vibe.vibrate(TIME_VIBRATION);
    startActivity(new Intent(this, ResetPasswordActivity.class));
  }

  @OnClick(R.id.new_user_start_logo)
  public void onLogoClicked() {
    vibe.vibrate(TIME_VIBRATION);
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_site_link))));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    ButterKnife.bind(this);

    vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    mSettings = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
    mAuth = FirebaseAuth.getInstance();
  }

  private void showDialog() {
    loader = new LoadingDialog();
    loader.show(getFragmentManager(), "LOADER_DIALOG");
  }

  private void hideDialog() {
    if (loader != null) {
      loader.dismiss();
    }
  }
}
