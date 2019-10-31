package pvt.talinor.ptl8.main;

import static pvt.talinor.ptl8.MainActivity.TIME_VIBRATION;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pvt.talinor.ptl8.MainActivity;
import pvt.talinor.ptl8.R;
import pvt.talinor.ptl8.auth.LoginActivity;

public class HelloActivity extends AppCompatActivity {

  //------------------------------------
  //    https://indyvision.net/android-snippets/android-tutorial-implement-press-back-again-to-exit/
  boolean doubleBackToExitPressedOnce = false;
  private SharedPreferences mSettings;
  private Vibrator vibe;

  @OnClick(R.id.hello_screen_start_logo)
  public void onStartLogoClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String url = getString(R.string.home_site_link);
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);

    } catch (Exception ignored) {
    }
  }

  @OnClick(R.id.btn_log_out)
  public void onLogOutClikced() {
    vibe.vibrate(TIME_VIBRATION);
    SharedPreferences.Editor editor = mSettings.edit();
    editor.clear();
    editor.apply();

    Intent intent = new Intent(this, LoginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @OnClick(R.id.btn_Support)
  public void onSupportClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String url = getString(R.string.talinor_support_link);
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);

    } catch (Exception ignored) {
    }
  }

  @OnClick(R.id.btn_Download)
  public void onDownloadClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String url = getString(R.string.talinor_download_link);
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);

    } catch (Exception ignored) {
    }
  }

  @OnClick(R.id.btn_Login_to_IOT)
  public void onLoginClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String url = getString(R.string.talinor_login_to_iot_link);
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);

    } catch (Exception ignored) {
    }
  }

  @OnClick(R.id.btn_T2Q_Form)
  public void onT2QClicked() {
    vibe.vibrate(TIME_VIBRATION);
    String url = getString(R.string.talinor_forms_link);
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);
    } catch (Exception ignored) {
    }
  }

  @OnClick(R.id.btn_Keypad)
  public void onKeypadClicked() {
    finish();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hello_screen);
    ButterKnife.bind(this);

    vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    mSettings = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);

  }

  @Override
  public void onBackPressed() {
    if (doubleBackToExitPressedOnce) {
      finishAffinity();
      return;
    }

    this.doubleBackToExitPressedOnce = true;
    Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

    new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
  }
  //------------------------------------
}
