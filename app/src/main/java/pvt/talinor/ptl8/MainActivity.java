package pvt.talinor.ptl8;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import pvt.talinor.ptl8.auth.LoginActivity;
import pvt.talinor.ptl8.main.HelloActivity;

public class MainActivity extends AppCompatActivity implements
    FragmentManager.OnBackStackChangedListener {

  // это будет именем файла настроек
  public static final int TIME_VIBRATION = 80;
  public static final String APP_PREFERENCES = "settings";         // имя файла
  public static final String APP_PREFERENCES_COUNTER = "counter";          // счетчик открытия программы
  public static final String APP_PREFERENCES_REGISTRED = "registred";        // зарегистрирован = 1; нет = 0
  public static final String APP_PREFERENCES_TOKEN = "token";
  public static final String APP_PREFERENCES_REGISTRED_FIRST_NAME = "first_name";       // имя
  public static final String APP_PREFERENCES_REGISTRED_LAST_NAME = "last_name";        // фамилия
  public static final String APP_PREFERENCES_REGISTRED_COMPANY_NAME = "company_name";     // название комапании
  public static final String APP_PREFERENCES_REGISTRED_EMAIL = "email";            // имеил
  public static final String APP_PREFERENCES_REGISTRED_PHONE_NUMBER = "phone_number";     // номеро телефона
  public static final String APP_PREFERENCES_REGISTRED_DATE_TIME = "date_time";        // дата и время регистрации
  //-----------------------------------------------
  // парметры для настроек
  //-----------------------------------------------
  public SharedPreferences mSettings;
  public int mCounter = 0;
  public int mRegistred = 0;
  public String mFisrtName = "";
  public String mLastName = "";
  public String mCompanyName = "";
  public String mEMail = "";
  public String mPhoneNumber = "";
  public String mDateTime = "";
  //-----------------------------------------------
  // номер фрагмента по порядку
  //-----------------------------------------------
  public int fragment_index = 0;
  //-----------------------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //        https://stackoverflow.com/questions/26440279/show-icon-in-actionbar-toolbar-with-appcompat-v7-21
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setIcon(R.drawable.talinor_logo_small); //also displays wide logo
    getSupportActionBar().setLogo(R.drawable.ic_talinor_logo); //also displays wide logo
    getSupportActionBar().setTitle(getString(R.string.app_name_title));
    getSupportActionBar().setDisplayShowTitleEnabled(false); //optional

    //-----------------------------------------------
    // проверяем настройки
    //-----------------------------------------------
    // http://developer.alexanderklimov.ru/android/preference.php
    // http://developer.alexanderklimov.ru/android/theory/sharedpreferences.php
    // https://stackoverflow.com/questions/39503168/how-do-i-get-the-context-from-within-an-activity-class/39503178#39503178
    Context context = this;
    mSettings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

    if ((mSettings.contains(APP_PREFERENCES_COUNTER)) && (mSettings
        .contains(APP_PREFERENCES_REGISTRED))) {
      // Получаем число запусков из настроек
      mCounter = mSettings.getInt(APP_PREFERENCES_COUNTER, 0);
      // Получаем остальные настройки
      mRegistred = mSettings.getInt(APP_PREFERENCES_REGISTRED, 0);
      mFisrtName = mSettings.getString(APP_PREFERENCES_REGISTRED_FIRST_NAME, "");
      mLastName = mSettings.getString(APP_PREFERENCES_REGISTRED_LAST_NAME, "");
      mCompanyName = mSettings.getString(APP_PREFERENCES_REGISTRED_COMPANY_NAME, "");
      mEMail = mSettings.getString(APP_PREFERENCES_REGISTRED_EMAIL, "");
      mPhoneNumber = mSettings.getString(APP_PREFERENCES_REGISTRED_PHONE_NUMBER, "");
      mDateTime = mSettings.getString(APP_PREFERENCES_REGISTRED_DATE_TIME, "");

      mCounter++; // инкремент числа запусков

//                    //-----------------------------------------------
//                    // Выводим на экран данные из настроек
//                    //-----------------------------------------------
//                    String toast_message =  "Launch number  : "    + mCounter       + "\n" +
//                                            "Registred      : "    + mRegistered     + "\n" +
//                                            "First Name     : "    + mFirstName     + "\n" +
//                                            "Second Name    : "    + mLastName      + "\n" +
//                                            "Company Name   : "    + mCompanyName   + "\n" +
//                                            "e-mail addr    : "    + mEMail         + "\n" +
//                                            "pnone number   : "    + mPhoneNumber   + "\n" +
//                                            "date_time      : "    + mDateTime  ;
//
//                    Toast toast = Toast.makeText(getApplicationContext(), toast_message, Toast.LENGTH_LONG);
//                    toast.setGravity(Gravity.CENTER, 0, 0);
//                    toast.show();
//                    //-----------------------------------------------

      // Запоминаем данные о количестве запусков
      SharedPreferences.Editor editor = mSettings.edit();
      editor.putInt(APP_PREFERENCES_COUNTER, mCounter);
      editor.apply();
    } else {
      // Запоминаем данные в первый раз для создания всех ключей
      SharedPreferences.Editor editor = mSettings.edit();
      editor.putInt(APP_PREFERENCES_COUNTER, 1);
      editor.putInt(APP_PREFERENCES_REGISTRED, 0);
      editor.apply();
    }

    //-----------------------------------------------
    // вызываем экран приветсвия
    //-----------------------------------------------

    //-----------------------------------------------

    //-----------------------------------------------
    // проверяем нужна ли регистрация
    //-----------------------------------------------
    if (mRegistred == 0) {
      //-----------------------------------------------
      // форма регистрации
      //-----------------------------------------------
      startActivity(new Intent(this, LoginActivity.class));
      finish();
    } else {
      Intent intent_hello_screen = new Intent(this, HelloActivity.class);
      startActivity(intent_hello_screen);
    }
    //-----------------------------------------------

    //-----------------------------------------------
    // создаем фрагмент рабочего сканирования
    //-----------------------------------------------
    getSupportFragmentManager().addOnBackStackChangedListener(this);
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .add(R.id.fragment, new DevicesFragment(), "devices").addToBackStack(null).commit();
      increment_fragment_index();
    } else {
      onBackStackChanged();
    }
  }

  public int increment_fragment_index() {
    fragment_index++;
    return fragment_index;
  }

  public int decrement_fragment_index() {
    fragment_index--;
    return fragment_index;
  }

  public int get_fragment_index() {
    return fragment_index;
  }

  @Override
  public void onBackStackChanged() {
    getSupportActionBar()
        .setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }


  @Override
  public void onBackPressed() {
    //-----------------------------------------------
    // нужно проверить, какой фрагмент активен и вернуться назад по стеку
    //-----------------------------------------------
    int stack_count = get_fragment_index();

    if (stack_count > 1) {
      decrement_fragment_index();
      super.onBackPressed();
    } else {
//            super.onBackPressed();
      //-----------------------------------------------
      // вызываем экран приветсвия
      //-----------------------------------------------
      Intent intent_hello_screen = new Intent(this, HelloActivity.class);
      startActivity(intent_hello_screen);
      //-----------------------------------------------
    }
  }
  //------------------------------------
}
