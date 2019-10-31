package pvt.talinor.ptl8;

import static pvt.talinor.ptl8.MainActivity.TIME_VIBRATION;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener,
    InputHandler.IOnDataArriveListener {

  private static final String TAG = ".TerminalFragment";
  // маски для дебажных событийб аналог #define
  private static final int DEBUG_Send_Loop_bm = (1 << 0);
  private static final int DEBUG_Show_Send_Toast_bm = (1 << 1);
  private static final int DEBUG_Show_Send_Common_Toast_bm = (1 << 2);
  private static final int DEBUG_Show_Test_Err_bm = (1 << 3);
  private static final int DEBUG_Show_Toast_KeySeq_bm = (1 << 4);
  private static final int DEBUG_Show_Toast_HomeSite_bm = (1 << 5);
  private static final int DEBUG_Show_Btn_Links_bm = (1 << 6);
  private static final int DEBUG_MODE = 0;
  // для организации конечного автомата
  private static final int MAX_Key_Seq_Count = 3;
  //    private static final int DEBUG_MODE = DEBUG_Send_Loop_bm | DEBUG_Show_Send_Toast_bm | DEBUG_Show_Send_Common_Toast_bm | DEBUG_Show_Test_Err_bm | DEBUG_Show_Toast_KeySeq_bm | DEBUG_Show_Toast_HomeSite_bm | DEBUG_Show_Btn_Links_bm;
  public err_code_detect_state_en ecd_state = err_code_detect_state_en.ERR_CODE_NO;
  public int[] KeySeq_Buf = new int[MAX_Key_Seq_Count];
  public int KeySecuence_count_max = MAX_Key_Seq_Count;
  public int KeySecuence_count = 0;
  public tvKeySequence_en tvKeySequence_State = tvKeySequence_en.KeySeq_HIDE;
  InputHandler btInputHandler;
  TextView tvTopLine, tvBottomLine, tvKeySequence;
  LinearLayout llKS, llBtnLinks;
  Button bt0,
      bt1,
      bt2,
      bt3,
      bt4,
      bt5,
      bt6,
      bt7,
      bt8,
      bt9,
      btA,
      btB,
      btC,
      btD,
      btStar,
      btSharp,
      btSend,
      btLink_Support;
  ImageButton imbt_link_video, imbt_link_home;
  //----------------------------------------
  // Btn Link Listener
  //----------------------------------------
  View.OnClickListener bt_Link_Listener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      Object rawTag = view.getTag();

      byte tag = 0;

      if (rawTag != null && (rawTag instanceof Byte)) {
        tag = ((Byte) rawTag).byteValue();

        String url = getString(R.string.home_site_link);
        // https://developer.android.com/guide/topics/resources/string-resource.html
        Resources res = getResources();
        String[] web_link_btn_list = res.getStringArray(R.array.web_link_btn_list);

        switch (tag) {
          case 0x00:
            url = web_link_btn_list[0];
            break;
          case 0x01:
            url = web_link_btn_list[1];
            break;
          case 0x02:
            url = web_link_btn_list[2];
            break;
          default:
            break;
        }
        try {
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(url));
          startActivity(i);

          if ((DEBUG_MODE & DEBUG_Show_Btn_Links_bm) == DEBUG_Show_Btn_Links_bm) {
            Toast.makeText(getContext(), url.toString(), Toast.LENGTH_LONG).show();
          }

        } catch (Exception ignored) {
        }
      }

    }
  };
  // елси тапнули по строчке расширенного ввода
  View.OnClickListener tvKeySequence_Tap = new View.OnClickListener() {
    @Override
    public void onClick(View view) {

      if ((DEBUG_MODE & DEBUG_Show_Test_Err_bm) == DEBUG_Show_Test_Err_bm) {
        // имитируем появление статуса ошибки
        // http://www.cyberforum.ru/android-dev/thread1011444.html
        tvBottomLine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alert, 0, 0, 0);
        ecd_state = err_code_detect_state_en.ERR_CODE_YES;
      }

      if (tvKeySequence_State != tvKeySequence_en.KeySeq_DISABLE) {
        Update_tvKeySequence_According_InputSate(tvKeySequence_en.KeySeq_DISABLE);
        return;
      }

      if (tvKeySequence_State != tvKeySequence_en.KeySeq_ENABLE) {
        Update_tvKeySequence_According_InputSate(tvKeySequence_en.KeySeq_ENABLE);
        return;
      }


    }
  };
  // если тапнули по строчке с кодом ошибки
  View.OnClickListener tv_botline_tap = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (ecd_state != err_code_detect_state_en.ERR_CODE_NO) {
        if ((DEBUG_MODE & DEBUG_Show_Test_Err_bm) == DEBUG_Show_Test_Err_bm) {
          String url = getString(R.string.home_site_link);

//                https://developer.android.com/guide/topics/resources/string-resource.html
          Resources res = getResources();
          String[] web_link_err_code_list = res.getStringArray(R.array.web_link_err_code_list);

          switch (ecd_state) {
            case ERR_CODE_YES: {
              url = web_link_err_code_list[0];
              ecd_state = err_code_detect_state_en.ERR_CODE_1;
            }
            ;
            break;
            case ERR_CODE_1: {
              url = web_link_err_code_list[1];
              ecd_state = err_code_detect_state_en.ERR_CODE_2;
            }
            ;
            break;
            case ERR_CODE_2: {
              url = web_link_err_code_list[2];
              ecd_state = err_code_detect_state_en.ERR_CODE_3;
            }
            ;
            break;
            case ERR_CODE_3: {
              url = web_link_err_code_list[3];
              ecd_state = err_code_detect_state_en.ERR_CODE_NO;
              tvBottomLine.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            ;
            break;

            default: {
              url = getString(R.string.home_site_link);
              ecd_state = err_code_detect_state_en.ERR_CODE_NO;
              tvBottomLine.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            ;
            break;
          }

          try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);

            Toast.makeText(getContext(), url.toString(), Toast.LENGTH_LONG).show();
          } catch (Exception ignored) {
          }
        } // if ( (DEBUG_MODE & DEBUG_Show_Test_Err_bm) == DEBUG_Show_Test_Err_bm)
      }


    }
  };
  private Menu menu;
  private String deviceAddress;
  private String newline = "\r\n";
  private SerialSocket socket;
  private SerialService service;
  private boolean initialStart = true;
  private Connected connected = Connected.False;

//    public enum Btn_Res_Index_en { BTN_RES_IND_OK, BTN_RES_IND_CANCEL, BTN_RES_IND_YES, BTN_RES_IND_NO }

//    public static final int IDD_ABOUT               = 1;    // Идентификаторы для окна о программе
//    public static final int IDD_CONFIRM_RESET       = 2;    // Идентификатор для диалогов подтверждения Reset
//    public static final int IDD_CONFIRM_LOG_RESET   = 3;    // Идентификатор для диалогов подтверждения Log Reset
//    public static final int IDD_SETTINGS            = 4;    // Идентификатор для натсроек
  //----------------------------------------
  // Sending KeySequence
  //----------------------------------------
  View.OnClickListener btSendKeySequence_Send = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (tvKeySequence_State == tvKeySequence_en.KeySeq_ENABLE) {

        for (int i = (MAX_Key_Seq_Count - 1); i >= 0; i--) {
          if (KeySeq_Buf[i] != -1) {
            byte b = ((byte) KeySeq_Buf[i]);
            send(b);

            return;
          }
        }

      }


    }
  };
  private Vibrator vibe;
  /*
  //----------------------------------------
  // Yes No Dialog
  //----------------------------------------
  public AlertDialog getDialog(final Context context, int ID, final byte send_byte) {

      AlertDialog.Builder builder = new AlertDialog.Builder(context);

      switch(ID) {
          case IDD_ABOUT: // Диалоговое окно About
              builder.setTitle(R.string.dialog_about_title);
              builder.setMessage(R.string.dialog_about_message);
              builder.setCancelable(true);    // это разрешает пользователю закрывать диалоговое окно с помощью хардварной кнопки Back
              builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // Кнопка ОК
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {

//                        if (mListener!= null) { listener.dialogResult((int)Btn_Res_Index_en.BTN_RES_IND_OK.ordinal()); }

                      dialog.dismiss(); // Отпускает диалоговое окно
                  }
              });
              return builder.create();

          case IDD_CONFIRM_RESET: // Диалоговое окно CONFIRM RESET
              builder.setTitle(R.string.alert_dialog_title_reset);
              builder.setMessage(R.string.alert_dialog_mess);
              builder.setCancelable(true);    // это разрешает пользователю закрывать диалоговое окно с помощью хардварной кнопки Back
              builder.setPositiveButton(R.string.alert_dialog_positive_btn, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {

//                        if (mListener!= null) { mListener.dialogResult((int)Btn_Res_Index_en.BTN_RES_IND_YES.ordinal()); }

                      send(send_byte);

                      Toast toast = Toast.makeText(context, R.string.alert_dialog_toast_positive_mes, Toast.LENGTH_LONG);
                      toast.show();
                      dialog.cancel();

                  }
              });
              builder.setNegativeButton(R.string.alert_dialog_negative_btn, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {

//                        if (mListener!= null) { listener.dialogResult((int)Btn_Res_Index_en.BTN_RES_IND_NO.ordinal()); }

                      Toast toast = Toast.makeText(context, R.string.alert_dialog_toast_negative_mes, Toast.LENGTH_LONG);
                      toast.show();
                      dialog.cancel();
                  }
              });
              return builder.create();

          case IDD_CONFIRM_LOG_RESET: // Диалоговое окно CONFIRM LOG RESET
              builder.setTitle(R.string.alert_dialog_title_logreset);
              builder.setMessage(R.string.alert_dialog_mess);
              builder.setCancelable(true);    // это разрешает пользователю закрывать диалоговое окно с помощью хардварной кнопки Back
              builder.setPositiveButton(R.string.alert_dialog_positive_btn, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {

//                        if (mListener!= null) { listener.dialogResult((int)Btn_Res_Index_en.BTN_RES_IND_YES.ordinal()); }

                      send(send_byte);

                      Toast toast = Toast.makeText(context, R.string.alert_dialog_toast_positive_mes, Toast.LENGTH_LONG);
                      toast.show();
                      dialog.cancel();

                  }
              });
              builder.setNegativeButton(R.string.alert_dialog_negative_btn, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {

//                                if (mListener!= null) { listener.dialogResult((int)Btn_Res_Index_en.BTN_RES_IND_NO.ordinal()); }

                      Toast toast = Toast.makeText(context, R.string.alert_dialog_toast_negative_mes, Toast.LENGTH_LONG);
                      toast.show();
                      dialog.cancel();
                  }
              });
              return builder.create();

          default:
              return null;
      }
  }
*/
/*

//----------------------------------------
// Vibro
//----------------------------------------
    public void vibro(Context context)
    {
        //----------------------------------------
        // http://developer.alexanderklimov.ru/android/theory/vibrator.php
        //----------------------------------------
        long mills = 1000L;
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(mills);
        }
        //----------------------------------------

    }
*/
  //----------------------------------------
// Sending Stream
//----------------------------------------
  View.OnClickListener btListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      Object rawTag = view.getTag();
      vibe.vibrate(TIME_VIBRATION);

      byte tag = 0;

      if (rawTag != null && (rawTag instanceof Byte)) {
        // main stream
        tag = ((Byte) rawTag).byteValue();

        //                    AlertDialog alertDialog;

        // если строчка мультиввода запрещена
        if ((tvKeySequence_State == tvKeySequence_en.KeySeq_DISABLE) || (tvKeySequence_State
            == tvKeySequence_en.KeySeq_HIDE)) {
          switch (tag) {
            case 0x00:
              send(tag);
              break;
            case 0x01:
              send(tag);
              break;
            case 0x02:
              send(tag);
              break;
            case 0x03:
              send(tag);
              break;
            case 0x04:
              send(tag);
              break;
            case 0x05:
              send(tag);
              break;
            case 0x06:
              send(tag);
              break;
            case 0x07:
              send(tag);
              break;
            case 0x08:
              send(tag);
              break;
            case 0x09:
              send(tag);
              break;
            case 0x0A:
              send(tag);
              break;
            case 0x0B:
              send(tag);
              break;
            case 0x0C:
              send(tag);
              break;
            case 0x0D:
              send(tag);
              break;
            case 0x0E:
              send(tag);
              break;
            case 0x0F:
              send(tag);
              break;
            // если необхходимо подтверждение действий
            //                    case (byte)0xe1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_RESET, tag);     alertDialog.show();  break;
            //                    case (byte)0xd1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_LOG_RESET, tag); alertDialog.show();  break;
            default:
              break;
          }

          if ((DEBUG_MODE & DEBUG_Send_Loop_bm) == DEBUG_Send_Loop_bm) {   // тест "петля"
            // debug "loop" stream
            String str = " Test_ ";
            int str_len = str.length() - 1;
            byte[] buffer = str.getBytes();

            switch (tag) {
              case 0x00:
                buffer[str_len] = '0';
                send_array(buffer);
                break;
              case 0x01:
                buffer[0] = 0x01;
                send(buffer[0]);
                buffer[0] = ' ';
                break;
              case 0x02:
                buffer[0] = 0x02;
                send(buffer[0]);
                buffer[0] = ' ';
                break;
              case 0x03:
                buffer[str_len] = '3';
                send_array(buffer);
                break;
              case 0x04:
                buffer[str_len] = '4';
                send_array(buffer);
                break;
              case 0x05:
                buffer[str_len] = '5';
                send_array(buffer);
                break;
              case 0x06:
                buffer[str_len] = '6';
                send_array(buffer);
                break;
              case 0x07:
                buffer[str_len] = '7';
                send_array(buffer);
                break;
              case 0x08:
                buffer[str_len] = '8';
                send_array(buffer);
                break;
              case 0x09:
                buffer[str_len] = '9';
                send_array(buffer);
                break;
              case 0x0A:
                buffer[str_len] = 'A';
                send_array(buffer);
                break;
              case 0x0B:
                buffer[str_len] = 'B';
                send_array(buffer);
                break;
              case 0x0C:
                buffer[str_len] = 'C';
                send_array(buffer);
                break;
              case 0x0D:
                buffer[str_len] = 'D';
                send_array(buffer);
                break;
              case 0x0E:
                buffer[str_len] = '*';
                send_array(buffer);
                break;
              case 0x0F:
                buffer[str_len] = '#';
                send_array(buffer);
                break;
              case (byte) 0xe1:
                buffer[str_len] = 'R';
                send_array(buffer);
                break;
              case (byte) 0xd1:
                buffer[str_len] = 'L';
                send_array(buffer);
                break;
              default:
                break;
            } //  switch(tag)
          } // if ( (DEBUG_MODE & DEBUG_Send_Loop_bm) == DEBUG_Send_Loop_bm)
        } // if (tvKeySequence_State == tvKeySequence_en.KeySeq_DISABLE)

        // если строчка мультиввода разрешена
        if (tvKeySequence_State == tvKeySequence_en.KeySeq_ENABLE) {
          if (KeySecuence_count >= KeySecuence_count_max) {
            return;
          }

          if (KeySecuence_count == 0) {
            switch (tag) {
              case 0x00:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x00;
                tvKeySequence.append(getResources().getString(R.string.btn0_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 0
              case 0x01:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x01;
                tvKeySequence.append(getResources().getString(R.string.btn1_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 1
              case 0x02:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x02;
                tvKeySequence.append(getResources().getString(R.string.btn2_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 2
              case 0x03:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x03;
                tvKeySequence.append(getResources().getString(R.string.btn3_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 3
              case 0x04:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x04;
                tvKeySequence.append(getResources().getString(R.string.btn4_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 4
              case 0x05:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x05;
                tvKeySequence.append(getResources().getString(R.string.btn5_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 5
              case 0x06:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x06;
                tvKeySequence.append(getResources().getString(R.string.btn6_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 6
              case 0x07:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x07;
                tvKeySequence.append(getResources().getString(R.string.btn7_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 7
              case 0x08:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x08;
                tvKeySequence.append(getResources().getString(R.string.btn8_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 8
              case 0x09:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x09;
                tvKeySequence.append(getResources().getString(R.string.btn9_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 9
              case 0x0A:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x0A;
                tvKeySequence.append(getResources().getString(R.string.btnA_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // M/B
              case 0x0B:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x0B;
                tvKeySequence.append(getResources().getString(R.string.btnB_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Up
              case 0x0C:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x0C;
                tvKeySequence.append(getResources().getString(R.string.btnC_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Down
              case 0x0D:
                KeySecuence_count_max = 2;
                KeySeq_Buf[0] = 0x0D;
                tvKeySequence.append(getResources().getString(R.string.btnD_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // E/S
              case 0x0E:
                KeySecuence_count_max = 3;
                KeySeq_Buf[0] = 0x0E;
                tvKeySequence.append(getResources().getString(R.string.btnStar_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // *
              case 0x0F:
                KeySecuence_count_max = 1;
                KeySeq_Buf[0] = 0x0F;
                tvKeySequence.append(getResources().getString(R.string.btnSharp_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // #
              // если необхходимо подтверждение действий
              //                    case (byte)0xe1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_RESET, tag);     alertDialog.show();  break;
              //                    case (byte)0xd1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_LOG_RESET, tag); alertDialog.show();  break;
              default:
                break;
            }
            return;
          }

          if ((KeySecuence_count == 1) && (KeySeq_Buf[0] == 0x0D)) {
            switch (tag) {
              case 0x00:
                KeySeq_Buf[1] = 0xD0;
                tvKeySequence.append(getResources().getString(R.string.btn0_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 0
              case 0x01:
                KeySeq_Buf[1] = 0xD1;
                tvKeySequence.append(getResources().getString(R.string.btn1_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 1
              case 0x02:
                KeySeq_Buf[1] = 0xD2;
                tvKeySequence.append(getResources().getString(R.string.btn2_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 2
              case 0x03:
                KeySeq_Buf[1] = 0xD3;
                tvKeySequence.append(getResources().getString(R.string.btn3_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 3
              case 0x04:
                KeySeq_Buf[1] = 0xD4;
                tvKeySequence.append(getResources().getString(R.string.btn4_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 4
              case 0x05:
                KeySeq_Buf[1] = 0xD5;
                tvKeySequence.append(getResources().getString(R.string.btn5_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 5
              case 0x06:
                KeySeq_Buf[1] = 0xD6;
                tvKeySequence.append(getResources().getString(R.string.btn6_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 6
              case 0x07:
                KeySeq_Buf[1] = 0xD7;
                tvKeySequence.append(getResources().getString(R.string.btn7_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 7
              case 0x08:
                KeySeq_Buf[1] = 0xD8;
                tvKeySequence.append(getResources().getString(R.string.btn8_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 8
              case 0x09:
                KeySeq_Buf[1] = 0xD9;
                tvKeySequence.append(getResources().getString(R.string.btn9_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 9
              case 0x0A:
                KeySeq_Buf[1] = 0xDA;
                tvKeySequence.append(getResources().getString(R.string.btnA_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // M/B
              case 0x0B:
                KeySeq_Buf[1] = 0xDB;
                tvKeySequence.append(getResources().getString(R.string.btnB_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Up
              case 0x0C:
                KeySeq_Buf[1] = 0xDC;
                tvKeySequence.append(getResources().getString(R.string.btnC_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Down
//                                case 0x0D       :  KeySecuence_value = 0xD0;  tvKeySequence.append(getResources().getString(R.string.btnD_ext_text));       tvKeySequence.append(" ");    break; // E/S
              case 0x0E:
                KeySeq_Buf[1] = 0xDE;
                tvKeySequence.append(getResources().getString(R.string.btnStar_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // *
              case 0x0F:
                KeySeq_Buf[1] = 0xDF;
                tvKeySequence.append(getResources().getString(R.string.btnSharp_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // #
              // если необхходимо подтверждение действий
              //                    case (byte)0xe1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_RESET, tag);     alertDialog.show();  break;
              //                    case (byte)0xd1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_LOG_RESET, tag); alertDialog.show();  break;
              default:
                break;
            }
            return;
          }

          if ((KeySecuence_count == 1) && (KeySeq_Buf[0] == 0x0E)) {
            switch (tag) {
//                                case 0x00       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn0_ext_text));       tvKeySequence.append(" ");    break; // 0
//                                case 0x01       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn1_ext_text));       tvKeySequence.append(" ");    break; // 1
//                                case 0x02       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn2_ext_text));       tvKeySequence.append(" ");    break; // 2
//                                case 0x03       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn3_ext_text));       tvKeySequence.append(" ");    break; // 3
//                                case 0x04       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn4_ext_text));       tvKeySequence.append(" ");    break; // 4
//                                case 0x05       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn5_ext_text));       tvKeySequence.append(" ");    break; // 5
//                                case 0x06       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn6_ext_text));       tvKeySequence.append(" ");    break; // 6
//                                case 0x07       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn7_ext_text));       tvKeySequence.append(" ");    break; // 7
//                                case 0x08       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn8_ext_text));       tvKeySequence.append(" ");    break; // 8
//                                case 0x09       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btn9_ext_text));       tvKeySequence.append(" ");    break; // 9
//                                case 0x0A       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btnA_ext_text));       tvKeySequence.append(" ");    break; // M/B
//                                case 0x0B       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btnB_ext_text));       tvKeySequence.append(" ");    break; // Up
//                                case 0x0C       :  KeySeq_Buf[1] = 0xE0; tvKeySequence.append(getResources().getString(R.string.btnC_ext_text));       tvKeySequence.append(" ");    break; // Down
              case 0x0D:
                KeySeq_Buf[1] = 0xE0;
                tvKeySequence.append(getResources().getString(R.string.btnD_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // E/S
//                                case 0x0E       :  KeySecuence_value = 0xDE;  tvKeySequence.append(getResources().getString(R.string.btnStar_ext_text));    tvKeySequence.append(" ");    break; // *
//                                case 0x0F       :  KeySeq_Buf[1] = 0xE0;  tvKeySequence.append(getResources().getString(R.string.btnSharp_ext_text));   tvKeySequence.append(" ");    break; // #
              // если необхходимо подтверждение действий
              //                    case (byte)0xe1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_RESET, tag);     alertDialog.show();  break;
              //                    case (byte)0xd1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_LOG_RESET, tag); alertDialog.show();  break;
              default:
                break;
            }
            return;
          }

          if ((KeySecuence_count == 2) && (KeySeq_Buf[1] == 0xE0)) {
            switch (tag) {
              case 0x00:
                KeySeq_Buf[2] = 0xE0;
                tvKeySequence.append(getResources().getString(R.string.btn0_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 0
              case 0x01:
                KeySeq_Buf[2] = 0xE1;
                tvKeySequence.append(getResources().getString(R.string.btn1_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 1
              case 0x02:
                KeySeq_Buf[2] = 0xE2;
                tvKeySequence.append(getResources().getString(R.string.btn2_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 2
              case 0x03:
                KeySeq_Buf[2] = 0xE3;
                tvKeySequence.append(getResources().getString(R.string.btn3_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 3
              case 0x04:
                KeySeq_Buf[2] = 0xE4;
                tvKeySequence.append(getResources().getString(R.string.btn4_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 4
              case 0x05:
                KeySeq_Buf[2] = 0xE5;
                tvKeySequence.append(getResources().getString(R.string.btn5_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 5
              case 0x06:
                KeySeq_Buf[2] = 0xE6;
                tvKeySequence.append(getResources().getString(R.string.btn6_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 6
              case 0x07:
                KeySeq_Buf[2] = 0xE7;
                tvKeySequence.append(getResources().getString(R.string.btn7_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 7
              case 0x08:
                KeySeq_Buf[2] = 0xE8;
                tvKeySequence.append(getResources().getString(R.string.btn8_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 8
              case 0x09:
                KeySeq_Buf[2] = 0xE9;
                tvKeySequence.append(getResources().getString(R.string.btn9_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // 9
              case 0x0A:
                KeySeq_Buf[2] = 0xEA;
                tvKeySequence.append(getResources().getString(R.string.btnA_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // M/B
              case 0x0B:
                KeySeq_Buf[2] = 0xEB;
                tvKeySequence.append(getResources().getString(R.string.btnB_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Up
              case 0x0C:
                KeySeq_Buf[2] = 0xEC;
                tvKeySequence.append(getResources().getString(R.string.btnC_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // Down
//                                case 0x0D       :  KeySecuence_value = 0xD0;  tvKeySequence.append(getResources().getString(R.string.btnD_ext_text));       tvKeySequence.append(" ");    break; // E/S
//                                case 0x0E       :  KeySecuence_value = 0xE0;  tvKeySequence.append(getResources().getString(R.string.btnStar_ext_text));    tvKeySequence.append(" ");    break; // *
              case 0x0F:
                KeySeq_Buf[2] = 0xEF;
                tvKeySequence.append(getResources().getString(R.string.btnSharp_ext_text));
                tvKeySequence.append(" ");
                KeySecuence_count++;
                break; // #
              // если необхходимо подтверждение действий
              //                    case (byte)0xe1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_RESET, tag);     alertDialog.show();  break;
              //                    case (byte)0xd1 : alertDialog = getDialog(getActivity(), IDD_CONFIRM_LOG_RESET, tag); alertDialog.show();  break;
              default:
                break;
            }
            return;
          }
        } // if (tvKeySequence_State == tvKeySequence_en.KeySeq_ENABLE)
      }
    }
  };

  public TerminalFragment() {
    btInputHandler = new InputHandler();
    btInputHandler.setDataArriveListener(this);
  }

  //----------------------------------------
  // Tap KeySequence
  //----------------------------------------
  public void Update_tvKeySequence_According_InputSate(tvKeySequence_en state) {
    //  https://stackoverflow.com/questions/7348150/android-why-setvisibilityview-gone-or-setvisibilityview-invisible-do-not
    //  View.GONE This view is invisible, and it doesn't take any space for layout purposes.
    //  View.INVISIBLE This view is invisible, but it still takes up space for layout purposes.
    switch (state) {
      case KeySeq_HIDE: {
        tvKeySequence_State = tvKeySequence_en.KeySeq_HIDE;
        tvKeySequence.setBackgroundResource(R.drawable.textview_shape_disable);
        tvKeySequence.setText(R.string.textView_KeySequence_Default);
        tvKeySequence.setVisibility(View.INVISIBLE);
        btSend.setVisibility(View.INVISIBLE);

        llKS.setVisibility(View.GONE);
        llBtnLinks.setVisibility(View.VISIBLE);

        if ((DEBUG_MODE & DEBUG_Show_Toast_KeySeq_bm) == DEBUG_Show_Toast_KeySeq_bm) {
          Toast toast = Toast
              .makeText(getContext(), R.string.toast_mes_keySeq_input_hide, Toast.LENGTH_LONG);
          toast.show();
        }
        return;
      }
      case KeySeq_SHOW: {
        tvKeySequence_State = tvKeySequence_en.KeySeq_DISABLE;
        tvKeySequence.setBackgroundResource(R.drawable.textview_shape_disable);
        tvKeySequence.setText(R.string.textView_KeySequence_Default);
        tvKeySequence.setVisibility(View.VISIBLE);
        btSend.setVisibility(View.INVISIBLE);

        llKS.setVisibility(View.VISIBLE);
        llBtnLinks.setVisibility(View.GONE);

        if ((DEBUG_MODE & DEBUG_Show_Toast_KeySeq_bm) == DEBUG_Show_Toast_KeySeq_bm) {
          Toast toast = Toast
              .makeText(getContext(), R.string.toast_mes_keySeq_input_show, Toast.LENGTH_LONG);
          toast.show();
        }
        return;

      }
      case KeySeq_DISABLE: {
        tvKeySequence_State = tvKeySequence_en.KeySeq_DISABLE;
        tvKeySequence.setBackgroundResource(R.drawable.textview_shape_disable);
        tvKeySequence.setText(R.string.textView_KeySequence_Default);
        tvKeySequence.setVisibility(View.VISIBLE);
        btSend.setVisibility(View.INVISIBLE);

        llKS.setVisibility(View.VISIBLE);
        llBtnLinks.setVisibility(View.GONE);

        if ((DEBUG_MODE & DEBUG_Show_Toast_KeySeq_bm) == DEBUG_Show_Toast_KeySeq_bm) {
          Toast toast = Toast
              .makeText(getContext(), R.string.toast_mes_keySeq_input_disabled, Toast.LENGTH_LONG);
          toast.show();
        }
        return;
      }
      case KeySeq_ENABLE: {

        tvKeySequence_State = tvKeySequence_en.KeySeq_ENABLE;
        tvKeySequence.setBackgroundResource(R.drawable.textview_shape_enable);
        tvKeySequence.setText(R.string.textView_KeySequence_On);
        tvKeySequence.setVisibility(View.VISIBLE);
        btSend.setVisibility(View.VISIBLE);

        llKS.setVisibility(View.VISIBLE);
        llBtnLinks.setVisibility(View.GONE);

        for (int i = 0; i < KeySeq_Buf.length; i++) {
          KeySeq_Buf[i] = -1;
        }
        KeySecuence_count = 0;
        KeySecuence_count_max = MAX_Key_Seq_Count;

        if ((DEBUG_MODE & DEBUG_Show_Toast_KeySeq_bm) == DEBUG_Show_Toast_KeySeq_bm) {
          Toast toast = Toast
              .makeText(getContext(), R.string.toast_mes_keySeq_input_enabled, Toast.LENGTH_LONG);
          toast.show();
        }
        return;

      }
      default:
        break;
    }
  }

  /*
   * Lifecycle
   */
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    setRetainInstance(true);
    deviceAddress = getArguments().getString("device");
  }

  @Override
  public void onDestroy() {
    if (connected != Connected.False) {
      disconnect();
    }
    getActivity().stopService(new Intent(getActivity(), SerialService.class));
    super.onDestroy();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (service != null) {
      service.attach(this);
    } else {
      getActivity().startService(new Intent(getActivity(),
          SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }
  }

  @Override
  public void onStop() {
    if (service != null && !getActivity().isChangingConfigurations()) {
      service.detach();
    }
    super.onStop();
  }

  @SuppressWarnings("deprecation")
  // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    getActivity().bindService(new Intent(getActivity(), SerialService.class), this,
        Context.BIND_AUTO_CREATE);
  }

  @Override
  public void onDetach() {
    try {
      getActivity().unbindService(this);
    } catch (Exception ignored) {
    }
    super.onDetach();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (initialStart && service != null) {
      initialStart = false;
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          TerminalFragment.this.connect();
        }
      });
    }
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder binder) {
    service = ((SerialService.SerialBinder) binder).getService();
    if (initialStart && isResumed()) {
      initialStart = false;
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          TerminalFragment.this.connect();
        }
      });
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    if (service != null) {
      service = null;
    }
  }

  /*
   * User Interface
   */
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_terminal, container, false);

    vibe = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

    tvTopLine = (TextView) view.findViewById(R.id.tvTopLine);
    tvBottomLine = (TextView) view.findViewById(R.id.tvBottomLine);
    tvKeySequence = (TextView) view.findViewById(R.id.tvKeySequence);
    llKS = (LinearLayout) view.findViewById(R.id.Line_KS);
    llBtnLinks = (LinearLayout) view.findViewById(R.id.Line_Links);

    tvTopLine.setMovementMethod(ScrollingMovementMethod.getInstance());
    tvBottomLine.setMovementMethod(ScrollingMovementMethod.getInstance());
    tvKeySequence.setMovementMethod(ScrollingMovementMethod.getInstance());

    bt0 = (Button) view.findViewById(R.id.bt0);
    bt0.setTag((byte) 0x00);
    bt1 = (Button) view.findViewById(R.id.bt1);
    bt1.setTag((byte) 0x01);
    bt2 = (Button) view.findViewById(R.id.bt2);
    bt2.setTag((byte) 0x02);
    bt3 = (Button) view.findViewById(R.id.bt3);
    bt3.setTag((byte) 0x03);
    bt4 = (Button) view.findViewById(R.id.bt4);
    bt4.setTag((byte) 0x04);
    bt5 = (Button) view.findViewById(R.id.bt5);
    bt5.setTag((byte) 0x05);
    bt6 = (Button) view.findViewById(R.id.bt6);
    bt6.setTag((byte) 0x06);
    bt7 = (Button) view.findViewById(R.id.bt7);
    bt7.setTag((byte) 0x07);
    bt8 = (Button) view.findViewById(R.id.bt8);
    bt8.setTag((byte) 0x08);
    bt9 = (Button) view.findViewById(R.id.bt9);
    bt9.setTag((byte) 0x09);
    btA = (Button) view.findViewById(R.id.btA);
    btA.setTag((byte) 0x0A);
    btB = (Button) view.findViewById(R.id.btB);
    btB.setTag((byte) 0x0B);
    btD = (Button) view.findViewById(R.id.btD);
    btD.setTag((byte) 0x0D);
    btC = (Button) view.findViewById(R.id.btC);
    btC.setTag((byte) 0x0C);
    btStar = (Button) view.findViewById(R.id.btStar);
    btStar.setTag((byte) 0x0E);
    btSharp = (Button) view.findViewById(R.id.btSharp);
    btSharp.setTag((byte) 0x0F);
    btSend = (Button) view.findViewById(R.id.btSend);

    imbt_link_video = (ImageButton) view.findViewById(R.id.bt_YouTubeLink);
    imbt_link_video.setTag((byte) 0x00);
    btLink_Support = (Button) view.findViewById(R.id.bt_SupportLink);
    btLink_Support.setTag((byte) 0x01);
    imbt_link_home = (ImageButton) view.findViewById(R.id.bt_HomeLink);
    imbt_link_home.setTag((byte) 0x02);

    // обработчик нажатия кнопок
    bt0.setOnClickListener(btListener);
    bt1.setOnClickListener(btListener);
    bt2.setOnClickListener(btListener);
    bt3.setOnClickListener(btListener);
    bt4.setOnClickListener(btListener);
    bt5.setOnClickListener(btListener);
    bt6.setOnClickListener(btListener);
    bt7.setOnClickListener(btListener);
    bt8.setOnClickListener(btListener);
    bt9.setOnClickListener(btListener);
    btA.setOnClickListener(btListener);
    btB.setOnClickListener(btListener);
    btC.setOnClickListener(btListener);
    btD.setOnClickListener(btListener);
    btStar.setOnClickListener(btListener);
    btSharp.setOnClickListener(btListener);
    btSend.setOnClickListener(btSendKeySequence_Send);
    tvKeySequence.setOnClickListener(tvKeySequence_Tap);
    tvBottomLine.setOnClickListener(tv_botline_tap);

    btLink_Support.setOnClickListener(bt_Link_Listener);

    imbt_link_video.setOnClickListener(bt_Link_Listener);
    imbt_link_home.setOnClickListener(bt_Link_Listener);

    // прячем ненужную строчку
    Update_tvKeySequence_According_InputSate(tvKeySequence_en.KeySeq_HIDE);

    //----------------------------------------
    // Side Slide
    //----------------------------------------
//        https://michiganlabs.com/2014/04/01/detecting-sliding-gestures-android/
//        https://gist.github.com/jkreiser/9916464
    tvTopLine.setOnTouchListener(new OnSlidingTouchListener(getContext()) {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//        https://stackoverflow.com/questions/24952312/ontouchlistener-warning-ontouch-should-call-viewperformclick-when-a-click-is-d
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        //some code....
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        v.performClick();
//                        break;
//                    default:
//                        break;
//                }
//                return true;
//            }

      @Override
      public boolean onSlideLeft() {
        // do something
        if (tvKeySequence_State == tvKeySequence_en.KeySeq_HIDE) {
          Update_tvKeySequence_According_InputSate(tvKeySequence_en.KeySeq_SHOW);
        }
        return true;
      }

      @Override
      public boolean onSlideRight() {
        //                Toast toast = Toast.makeText(getContext(), "right", Toast.LENGTH_LONG);
//                toast.show();

//                https://stackoverflow.com/questions/7348150/android-why-setvisibilityview-gone-or-setvisibilityview-invisible-do-not
//                View.GONE This view is invisible, and it doesn't take any space for layout purposes.
//                View.INVISIBLE This view is invisible, but it still takes up space for layout purposes.

        if (tvKeySequence_State != tvKeySequence_en.KeySeq_HIDE) {
          Update_tvKeySequence_According_InputSate(tvKeySequence_en.KeySeq_HIDE);
        }
        return true;
      }

      @Override
      public boolean onSlideUp() {
        // do something
        return true;
      }

      @Override
      public boolean onSlideDown() {
        // do something
        return true;
      }
    });

    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu,
      MenuInflater inflater) {   // отображаем заголовок приложения
    inflater.inflate(R.menu.menu_terminal, menu);
    this.menu = menu;
    menu.findItem(R.id.bt_apk_name).setEnabled(true);

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.bt_home_site) {
      String url = getString(R.string.home_site_link);
      try {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

        if ((DEBUG_MODE & DEBUG_Show_Toast_HomeSite_bm) == DEBUG_Show_Toast_HomeSite_bm) {
          Toast toast = Toast
              .makeText(getContext(), getText(R.string.home_site_link), Toast.LENGTH_LONG);
          toast.show();
        }
      } catch (Exception ignored) {
      }
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  /*
   * Serial + UI
   */
  private void connect() {
    try {
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
      String deviceName = device.getName() != null ? device.getName() : device.getAddress();
      status("connecting...");
      connected = Connected.Pending;
      socket = new SerialSocket();
      service.connect(this, "Connected to " + deviceName);
      socket.connect(getContext(), service, device);
    } catch (Exception e) {
      onSerialConnectError(e);
    }
  }

  private void disconnect() {
    if (connected != Connected.False) {
      connected = Connected.False;
    }
    if (service != null) {
      service.disconnect();
    }
    if (socket != null) {
      socket.disconnect();
      socket = null;
    }
  }

  private void send(byte b) {
//    if (connected != Connected.True) {
//      Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//
//      disconnect();
//      connect();
//      return;
//    }
    try {
      byte[] data = {b};
      socket = new SerialSocket();
      socket.write(data);

      if ((DEBUG_MODE & DEBUG_Show_Send_Toast_bm) == DEBUG_Show_Send_Toast_bm) {
        String s = "Send (0x" + String.format("%02X", 0xff & b) + ')';
        Toast toast = Toast.makeText(getContext(), s, s.length());
        toast.show();
      }
    } catch (Exception e) {
      onSerialIoError(e);
    }
  }

  private void send_array(byte[] data) {
//    if (connected != Connected.True) {
//      Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//      return;
//    }
    try {
      socket.write(data);
    } catch (Exception e) {
      onSerialIoError(e);
    }
  }

  private void status(String str) {
/*
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
*/
    // TODO: Connection status
  }

  /*
   * SerialListener
   */
  @Override
  public void onSerialConnect() {
    status("connected");
    connected = Connected.True;
  }

  @Override
  public void onSerialConnectError(Exception e) {
    status("connection failed: " + e.getMessage());
    disconnect();
  }

  @Override
  public void onSerialRead(byte[] data) {
    // receive(data);
    try {
      btInputHandler.handleInput(data);
    } catch (InputHandler.IllegalLineIndexException e) {
      // TODO: UI indication for invalid data
      Log.e(TAG,
          "Invalid line marker received (" + String.format("%02X", 0xff & e.invalidIndex()) + ')',
          e);
    }
  }

  @Override
  public void onSerialIoError(Exception e) {
    status("connection lost: " + e.getMessage());
    disconnect();
  }

  @Override
  public void onTopLineArrived(String topLine) {
    tvTopLine.setText(topLine);
  }

  @Override
  public void onBottomLineArrived(String bottomLine) {
    tvBottomLine.setText(bottomLine);
  }

  private enum Connected {False, Pending, True}

  // для организации отработки сообщений об ошибках из трафика
  public enum err_code_detect_state_en {
    ERR_CODE_NO, ERR_CODE_YES, ERR_CODE_1, ERR_CODE_2, ERR_CODE_3
  }

  // для переключения состяония строки расширенного ввода команд
  public enum tvKeySequence_en {
    KeySeq_DISABLE, KeySeq_ENABLE, KeySeq_SHOW, KeySeq_HIDE
  }

}
