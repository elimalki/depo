package pvt.talinor.ptl8;

import static pvt.talinor.ptl8.MainActivity.APP_PREFERENCES_TOKEN;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * show list of BLE devices
 */
public class DevicesFragment extends ListFragment {

  public static final int IDD_ABOUT = 1;    // Идентификаторы для окна о программе
  public static final int IDD_CONFIRM_RESET = 2;    // Идентификатор для диалогов подтверждения Reset
  public static final int IDD_CONFIRM_LOG_RESET = 3;    // Идентификатор для диалогов подтверждения Log Reset
  public static final int IDD_SETTINGS = 4;    // Идентификатор для натсроек
  private final BluetoothAdapter bluetoothAdapter;
  //-----------------------------------------------
  public SharedPreferences mSettings;
  public int mCounter = 0;
  public int mRegistred = 0;
  public String mFisrtName = "";
  //-----------------------------------------------
  public String mLastName = "";
  public String mCompanyName = "";
  public String mEMail = "";
  public String mPhoneNumber = "";
  public Date mDateTime;
  private Menu menu;
  private BroadcastReceiver bleDiscoveryBroadcastReceiver;
  private IntentFilter bleDiscoveryIntentFilter;
  private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
  private ArrayAdapter<BluetoothDevice> listAdapter;
  private FirebaseFirestore db;

  public DevicesFragment() {
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothAdapter_Set_On();

    bleDiscoveryBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {

          final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                updateScan(device);
              }
            });
          }
        }
        if (intent.getAction().equals((BluetoothAdapter.ACTION_DISCOVERY_FINISHED))) {
          stopScan();
        }
      }
    };
    bleDiscoveryIntentFilter = new IntentFilter();
    bleDiscoveryIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    bleDiscoveryIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
  }

  /**
   * sort by name, then address. sort named devices first
   */
  static int compareTo(BluetoothDevice a, BluetoothDevice b) {
    boolean aValid = a.getName() != null && !a.getName().isEmpty();
    boolean bValid = b.getName() != null && !b.getName().isEmpty();
    if (aValid && bValid) {
      int ret = a.getName().compareTo(b.getName());
      if (ret != 0) {
        return ret;
      }
      return a.getAddress().compareTo(b.getAddress());
    }
    if (aValid) {
      return -1;
    }
    if (bValid) {
      return +1;
    }
    return a.getAddress().compareTo(b.getAddress());
  }
  //=============================================================

  //----------------------------------------
  // Yes No Dialog
  //----------------------------------------
  public AlertDialog getDialog(final Context context, int ID) {

    AlertDialog.Builder builder = new AlertDialog.Builder(context);

    switch (ID) {
      case IDD_ABOUT: // Диалоговое окно About
        //-----------------------------------------------
        // проверяем настройки
        //-----------------------------------------------
        // http://developer.alexanderklimov.ru/android/preference.php
        // http://developer.alexanderklimov.ru/android/theory/sharedpreferences.php
        // https://stackoverflow.com/questions/39503168/how-do-i-get-the-context-from-within-an-activity-class/39503178#39503178
//                Context context = getContext();
        //----------------------------------------------- м
        // Выводим на экран данные из настроек
        //-----------------------------------------------
        String toast_message = "First Name : " + mFisrtName + "\n" +
            "Second Name: " + mLastName + "\n" +
            "Company Name: " + mCompanyName + "\n" +
            "E-mail: " + mEMail + "\n" +
            "Phone number: " + mPhoneNumber + "\n" +
            "Date: " + mDateTime.toString();

//                Toast toast = Toast.makeText(getContext(), toast_message, Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
        //-----------------------------------------------

        builder.setTitle(R.string.dialog_about_title_user_info);
        builder.setMessage(toast_message);
        builder.setCancelable(
            true);    // это разрешает пользователю закрывать диалоговое окно с помощью хардварной кнопки Back
        builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() { // Кнопка ОК
              @Override
              public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss(); // Отпускает диалоговое окно
              }
            });
        return builder.create();

      case IDD_CONFIRM_RESET: // Диалоговое окно CONFIRM RESET

        builder.setTitle(R.string.alert_dialog_title_reset_user_sets);
        builder.setMessage(R.string.alert_dialog_confirm_mess);
        builder.setCancelable(
            true);    // это разрешает пользователю закрывать диалоговое окно с помощью хардварной кнопки Back
        builder.setPositiveButton(R.string.alert_dialog_positive_btn,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int whichButton) {

                //-----------------------------------------------
                // DEBUG
                //-----------------------------------------------
                // Для отладки обнуляем/удаляем файл настроек
                //-----------------------------------------------
                int mCounter = 0;
                SharedPreferences mSettings = getContext()
                    .getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mSettings.edit();
                if ((mSettings.contains(MainActivity.APP_PREFERENCES_COUNTER))) {
                  // Получаем число запусков из настроек
                  mCounter = mSettings.getInt(MainActivity.APP_PREFERENCES_COUNTER, 0);
                }
                // очищаем все настройки
                editor.clear().apply();
                // Запоминаем данные о количестве запусков
                editor.putInt(MainActivity.APP_PREFERENCES_COUNTER, mCounter);
                editor.apply();
                //-----------------------------------------------
                // Сообщаем, что настройки обнулили
                //-----------------------------------------------
                Toast toast = Toast
                    .makeText(getContext(), getString(R.string.toast_Reset_User_Info_text),
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                //-----------------------------------------------
//                        Toast toast = Toast.makeText(context, R.string.alert_dialog_toast_positive_mes, Toast.LENGTH_LONG);
//                        toast.show();
                //-----------------------------------------------
                dialog.cancel();

              }
            });
        builder.setNegativeButton(R.string.alert_dialog_negative_btn,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int whichButton) {

                Toast toast = Toast
                    .makeText(context, R.string.alert_dialog_toast_negative_mes, Toast.LENGTH_LONG);
                toast.show();
                //-----------------------------------------------
                dialog.cancel();
              }
            });
        return builder.create();
      default:
        return null;
    }
  }

  //-------------------------------------------------------------
  // автоматически включаем BlueTooth адаптер, если он существует
  //-------------------------------------------------------------
  public boolean BluetoothAdapter_Set_On() {
//        https://stackoverflow.com/questions/5735053/toggling-bluetooth-on-and-off
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (bluetoothAdapter == null) {
      return false;
    } else {
      if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
        bluetoothAdapter.enable();
      }
    }
    return true;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
      @Override
      public View getView(int position, View view, ViewGroup parent) {
        BluetoothDevice device = listItems.get(position);

        if (view == null) {
          view = getActivity().getLayoutInflater()
              .inflate(R.layout.device_list_item, parent, false);
        }
        TextView text1 = view.findViewById(R.id.text1);
        TextView text2 = view.findViewById(R.id.text2);
        if (device.getName() == null || device.getName().isEmpty()) {
          text1.setText("<unnamed>");
        } else {
          text1.setText(device.getName());
        }
        text2.setText(device.getAddress());
        return view;
      }
    };

    mSettings = getContext()
        .getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);

    String email = mSettings.getString(APP_PREFERENCES_TOKEN, null);
    db = FirebaseFirestore.getInstance();
    if (email != null && !email.isEmpty()) {
      db.collection("users").document(email).get().addOnCompleteListener(
          task -> {
            DocumentSnapshot documentSnapshot = task.getResult();
            if (documentSnapshot != null) {
              mFisrtName = (String) documentSnapshot.get("first_name");
              mLastName = (String) documentSnapshot.get("last_name");
              mPhoneNumber = (String) documentSnapshot.get("phone_number");
              mEMail = email;
              mCompanyName = (String) documentSnapshot.get("company_name");
              mDateTime = (Date) documentSnapshot.getDate("date_creation");
            }
          });
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setListAdapter(null);
    View header = getActivity().getLayoutInflater()
        .inflate(R.layout.device_list_header, null, false);
    getListView().addHeaderView(header, null, false);
    setEmptyText("initializing...");
    ((TextView) getListView().getEmptyView()).setTextSize(18);
    setListAdapter(listAdapter);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_devices, menu);
    this.menu = menu;
    if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
      menu.findItem(R.id.bt_settings).setEnabled(false);
    }
    if (bluetoothAdapter == null || !getActivity().getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      menu.findItem(R.id.ble_scan).setEnabled(false);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    stopScan();
    getActivity().unregisterReceiver(bleDiscoveryBroadcastReceiver);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // место, куда попадаем при нажатии на кнопки CONNECT(SCAN); STOP; BLUETOOTH SETTINGS
    AlertDialog alertDialog;

    int id = item.getItemId();
    if (id == R.id.ble_scan) {
      startScan();
      return true;
    } else if (id == R.id.ble_scan_stop) {
      stopScan();
      return true;
    } else if (id == R.id.bt_settings) {
      Intent intent = new Intent();
      intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
      startActivity(intent);
      return true;
    } else if (id == R.id.bt_user_info) {

      alertDialog = getDialog(getContext(), IDD_ABOUT);
      alertDialog.show();

//            //-----------------------------------------------
//            // проверяем настройки
//            //-----------------------------------------------
//            // http://developer.alexanderklimov.ru/android/preference.php
//            // http://developer.alexanderklimov.ru/android/theory/sharedpreferences.php
//            // https://stackoverflow.com/questions/39503168/how-do-i-get-the-context-from-within-an-activity-class/39503178#39503178
//            Context context = getContext();
//            mSettings  = context.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
//            // Получаем число запусков из настроек
//            mCounter     = mSettings.getInt(MainActivity.APP_PREFERENCES_COUNTER, 0);
//            // Получаем остальные настройки
//            mRegistered   = mSettings.getInt(MainActivity.APP_PREFERENCES_REGISTRED,0);
//            mFirstName   = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_FIRST_NAME,"");
//            mLastName    = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_LAST_NAME,"");
//            mCompanyName = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_COMPANY_NAME,"");
//            mEMail       = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_EMAIL,"");
//            mPhoneNumber = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_PHONE_NUMBER,"");
//            mDateTime    = mSettings.getString(MainActivity.APP_PREFERENCES_REGISTRED_DATE_TIME,"");
//            //-----------------------------------------------
//            // Выводим на экран данные из настроек
//            //-----------------------------------------------
//            String toast_message =  "Launch number  : "    + mCounter       + "\n" +
//                    "Registred      : "    + mRegistered     + "\n" +
//                    "First Name     : "    + mFirstName     + "\n" +
//                    "Second Name    : "    + mLastName      + "\n" +
//                    "Company Name   : "    + mCompanyName   + "\n" +
//                    "e-mail addr    : "    + mEMail         + "\n" +
//                    "pnone number   : "    + mPhoneNumber   + "\n" +
//                    "date_time      : "    + mDateTime  ;
//
//            Toast toast = Toast.makeText(getContext(), toast_message, Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.CENTER, 0, 0);
//            toast.show();
//            //-----------------------------------------------

      return true;
    } else if (id == R.id.bt_reset_user_info) {
      alertDialog = getDialog(getContext(), IDD_CONFIRM_RESET);
      alertDialog.show();
//            //-----------------------------------------------
//            // DEBUG
//            //-----------------------------------------------
//            // Для отладки обнуляем/удаляем файл настроек
//            //-----------------------------------------------
//            int mCounter = 0;
//            SharedPreferences mSettings  = getContext().getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = mSettings.edit();
//            if ( (mSettings.contains(MainActivity.APP_PREFERENCES_COUNTER)) )
//            {
//                // Получаем число запусков из настроек
//                mCounter            = mSettings.getInt(MainActivity.APP_PREFERENCES_COUNTER, 0);
//            }
//            // очищаем все настройки
//            editor.clear().apply();
//            // Запоминаем данные о количестве запусков
//            editor.putInt(MainActivity.APP_PREFERENCES_COUNTER, mCounter);
//            editor.apply();
//            //-----------------------------------------------
//            // Сообщаем, что настройки обнулили
//            //-----------------------------------------------
//            Toast toast = Toast.makeText(getContext(), getString(R.string.toast_Reset_User_Info_text), Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.CENTER, 0, 0);
//            toast.show();
//            //-----------------------------------------------

//            //-----------------------------------------------
//            // форма регистрации
//            //-----------------------------------------------
//            Intent intent = new Intent(DevicesFragment.this, RegisterActivity.class);
//            startActivity(intent);
//            //-----------------------------------------------
      return true;
    } else if (id == R.id.bt_home_site) {
      String url = getString(R.string.home_site_link);
      try {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

//                Toast toast = Toast.makeText(getContext(), getText(R.string.home_site_link), Toast.LENGTH_LONG);
//                toast.show();
      } catch (Exception ignored) {
      }
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  private void startScan() {

    // включаем блутуз перед началом сканирования
    BluetoothAdapter_Set_On();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getText(R.string.location_permission_title));
        builder.setMessage(getText(R.string.location_permission_message));
        builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                DevicesFragment.this
                    .requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
              }
            });
        builder.show();
        return;
      }
    }

    listItems.clear();

    listAdapter.notifyDataSetChanged();

    setEmptyText(getText(R.string.setEmptyText_Scanning));

    menu.findItem(R.id.ble_scan).setVisible(false);
    menu.findItem(R.id.ble_scan_stop).setVisible(true);

    bluetoothAdapter.startDiscovery();
    //  BluetoothLeScanner.startScan(...) would return more details, but that's not needed here
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    // ignore requestCode as there is only one in this fragment
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
          DevicesFragment.this.startScan();
        }
      }, 1); // run after onResume to avoid wrong empty-text
    } else {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(getText(R.string.location_denied_title));
      builder.setMessage(getText(R.string.location_denied_message));
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }
  }

  private void updateScan(BluetoothDevice device) {
    if (listItems.indexOf(device) < 0) {
      listItems.add(device);
      Collections.sort(listItems, new Comparator<BluetoothDevice>() {
        @Override
        public int compare(BluetoothDevice a, BluetoothDevice b) {
          return compareTo(a, b);
        }
      });
      listAdapter.notifyDataSetChanged();
    }
  }

  private void stopScan() {
    setEmptyText(getText(R.string.setEmptyText_No_BlueTooth_Dev_Found));
//        setEmptyText("<no bluetooth devices found>");
    if (menu != null) {
      menu.findItem(R.id.ble_scan).setVisible(true);
      menu.findItem(R.id.ble_scan_stop).setVisible(false);
    }
    bluetoothAdapter.cancelDiscovery();
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // сюда попадаем при выборе насканированного устройства из списка
    stopScan();
    BluetoothDevice device = listItems.get(position - 1);

    Bundle args = new Bundle();
    args.putString("device", device.getAddress());

    Fragment fragment = new TerminalFragment();
    fragment.setArguments(args);
    getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal")
        .addToBackStack(null).commit();
    // http://qaru.site/questions/5314785/calling-a-method-of-the-mainactivity-from-another-class
    ((MainActivity) getActivity()).increment_fragment_index();

  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().registerReceiver(bleDiscoveryBroadcastReceiver, bleDiscoveryIntentFilter);
    if (bluetoothAdapter == null || !getActivity().getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      setEmptyText(getText(R.string.setEmptyText_BlueTooth_LE_Not_Support));
    } else if (!bluetoothAdapter.isEnabled()) {
      setEmptyText(getText(R.string.setEmptyText_BlueTooth_Is_Disables));
    } else {
      setEmptyText(getText(R.string.setEmptyText_Refresh_Mess));
    }
  }
  //-----------------------------------------------------


}
