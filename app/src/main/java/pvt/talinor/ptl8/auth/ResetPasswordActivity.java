package pvt.talinor.ptl8.auth;

import static pvt.talinor.ptl8.MainActivity.TIME_VIBRATION;

import android.content.Context;
import android.content.Intent;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import pvt.talinor.ptl8.LoadingDialog;
import pvt.talinor.ptl8.R;

/**
 * <p> Created by Rubezhin Evgenij on 7/1/2019. <br>
 * Copyright (c) 2019 LineUp. <br> Project: bm71term, pvt.talinor.ptl8.auth </p>
 *
 * @author Rubezhin Evgenij
 * @version 1.0
 */
public class ResetPasswordActivity extends AppCompatActivity {

  @BindView(R.id.et_email)
  EditText et_email;
  private FirebaseAuth mAuth;
  private LoadingDialog loader;
  private Vibrator vibe;

  @OnClick(R.id.new_user_start_logo)
  public void onLogoClicked() {
    vibe.vibrate(TIME_VIBRATION);
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_site_link))));
  }

  @OnClick(R.id.btn_new_user_send_info)
  public void onSendClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String mEMail = et_email.getText().toString();
    if (mEMail.isEmpty()) {
      Toast.makeText(this, "Email field is empty", Toast.LENGTH_SHORT).show();
      return;
    }
    mEMail = et_email.getText().toString().trim();

    showDialog();
    Task<Void> task = mAuth.sendPasswordResetEmail(mEMail);
    task.addOnSuccessListener(aVoid -> {
      Toast.makeText(ResetPasswordActivity.this, "Link to reset the password sent to the mail",
          Toast.LENGTH_SHORT).show();
      hideDialog();
      et_email.getText().clear();
    });
    task.addOnFailureListener(e -> {
      Toast.makeText(ResetPasswordActivity.this, "Can not send link to email",
          Toast.LENGTH_SHORT).show();
      hideDialog();
      et_email.getText().clear();
    });
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_reset_password);
    ButterKnife.bind(this);

    vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
