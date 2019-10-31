package pvt.talinor.ptl8.auth;

import static pvt.talinor.ptl8.MainActivity.TIME_VIBRATION;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import pvt.talinor.ptl8.LoadingDialog;
import pvt.talinor.ptl8.MainActivity;
import pvt.talinor.ptl8.R;
import pvt.talinor.ptl8.emailSender.GMailSender;

public class RegisterActivity extends AppCompatActivity {

  @BindView(R.id.et_first_name)
  EditText et_first_name;
  @BindView(R.id.et_second_name)
  EditText et_second_name;
  @BindView(R.id.et_company_name)
  EditText et_company_name;
  @BindView(R.id.et_email)
  EditText et_email;
  @BindView(R.id.et_password)
  EditText et_password;
  @BindView(R.id.et_phone_number)
  EditText et_phone_number;

  private SharedPreferences mSettings;
  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private LoadingDialog loader;

  private String mFirstName;
  private String mLastName;
  private String mCompanyName;
  private String mEMail;
  private String mPhoneNumber;
  private Date currentDate;
  private Vibrator vibe;

  @OnClick(R.id.btn_new_user_send_info)
  public void onNewUserClicked() {
    vibe.vibrate(TIME_VIBRATION);
    mFirstName = et_first_name.getText().toString();
    mLastName = et_second_name.getText().toString();
    mCompanyName = et_company_name.getText().toString();
    mEMail = et_email.getText().toString();
    mPhoneNumber = et_phone_number.getText().toString();
    String mPassword = et_password.getText().toString();

    if (mEMail.isEmpty()) {
      Toast.makeText(this, "Email is empty", Toast.LENGTH_SHORT).show();
      return;
    }
    if (mPassword.isEmpty()) {
      Toast.makeText(this, "Password is empty", Toast.LENGTH_SHORT).show();
      return;
    } else if (mPassword.length() < 6) {
      Toast.makeText(this, "Password must have more than 6 symbols", Toast.LENGTH_SHORT).show();
      return;
    }
    // Текущее время
    currentDate = new Date();

    showDialog();
    mAuth.createUserWithEmailAndPassword(mEMail, mPassword)
        .addOnCompleteListener(this, task -> {
          if (task.isSuccessful()) {
            FirebaseUser user = mAuth.getCurrentUser();
            signUpSuccess(user);
          } else {
            hideDialog();
            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                Toast.LENGTH_SHORT).show();
          }
        });
  }

  private void signUpSuccess(FirebaseUser user) {
    if (mEMail == null) {
      return;
    }
    Map<String, Object> data = new HashMap<>();
    data.put("email", mEMail);
    data.put("first_name", mFirstName);
    data.put("last_name", mLastName);
    data.put("phone_number", mPhoneNumber);
    data.put("company_name", mCompanyName);
    data.put("date_creation", currentDate);

    Task<Void> task = db.collection("users").document(mEMail).set(data);
    task.addOnSuccessListener(aVoid -> {
      sendVerificationEmail(user);
      sendToEmail();
    });
    task.addOnFailureListener(e -> {
      hideDialog();
      Toast.makeText(RegisterActivity.this, "Authentication failed.",
          Toast.LENGTH_SHORT).show();
    });
  }

  void sendToEmail() {
    String body = "New user with email \"" + mEMail + "\" is registered in Talinor";
    new Thread(() -> {
      try {
        GMailSender sender = new GMailSender("talinorbm71@gmail.com",
            "talinor_test_app");
        sender.sendMail("New user is registered in Talinor", body,
            "talinorbm71@gmail.com", "app@talinor.co.uk");
      } catch (Exception e) {
        Log.e("TAG", e.getMessage(), e);
      }
    }).start();
  }

  void sendVerificationEmail(FirebaseUser user) {
    Task<Void> verifyTask = user.sendEmailVerification();

    verifyTask.addOnSuccessListener(aVoid1 -> {
      Toast.makeText(RegisterActivity.this, "Please, confirm email to start work!",
          Toast.LENGTH_SHORT).show();
      finish();
    });
    verifyTask.addOnFailureListener(e -> {
      hideDialog();
      Toast.makeText(RegisterActivity.this, "Authentication failed.",
          Toast.LENGTH_SHORT).show();
    });
  }

  @OnClick(R.id.new_user_start_logo)
  public void onLogoClicked() {
    vibe.vibrate(TIME_VIBRATION);
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_site_link))));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_registration);
    ButterKnife.bind(this);

    vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    mSettings = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
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