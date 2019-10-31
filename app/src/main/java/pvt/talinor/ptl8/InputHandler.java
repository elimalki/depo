package pvt.talinor.ptl8;

public class InputHandler {

  private static final int MAX_LINE_LENGTH = 16;
  // variable definition
  IOnDataArriveListener callback;
  byte[] cmd_buf;   /* command buff*/
  int max_size;  /* command max size*/
  int len;       /* current length*/
  State cmd_state; /* command state*/
  Line flag;      /* flag for line ident*/
  public InputHandler() {
    cmd_buf = new byte[MAX_LINE_LENGTH + 1];
    max_size = MAX_LINE_LENGTH;
    len = 0;
    cmd_state = State.INIT;
    flag = Line.Line_xx;
  }

  public void setDataArriveListener(IOnDataArriveListener callback) {
    this.callback = callback;
  }

  public void handleInput(byte[] data_in) throws IllegalLineIndexException {
    int i;
    byte test_byte;
    int data_len = data_in.length;
    int index = 0;

    while (data_len > 0) {
      switch (cmd_state) {

        case INIT: {
          len = 0;
          index = 0;
          cmd_state = State.START;
          flag = Line.Line_xx;

          for (i = 0; i < max_size; i++) {
            cmd_buf[i] = ' ';
          }    // заполняем все пробелами для избегания мигания
          cmd_buf[max_size] = 0;   // за границами допустимых символов MAX_LINE_LENGTH добиваем нулем
        }
        break;

        case START: {
          test_byte = data_in[index];

          data_len--;
          index++;

          // reset lines if there is special symbol
          if (test_byte == 0x01) {
            flag = Line.Line_01;
            len = 0;
            cmd_state = State.BODY;
            for (i = 0; i < max_size; i++) {
              cmd_buf[i] = ' ';
            }
            if (callback != null) {
              callback.onTopLineArrived(new String(cmd_buf));
            }
          }

          if (test_byte == 0x02) {
            flag = Line.Line_02;
            len = 0;
            cmd_state = State.BODY;
            for (i = 0; i < max_size; i++) {
              cmd_buf[i] = ' ';
            }
            if (callback != null) {
              callback.onBottomLineArrived(new String(cmd_buf));
            }
          }

        }
        break;

        case BODY: {
          test_byte = data_in[index];

          data_len--;
          index++;

          // reset lines if there is special symbol
          if (test_byte == 0x01) {
            flag = Line.Line_01;
            len = 0;
            for (i = 0; i < max_size; i++) {
              cmd_buf[i] = ' ';
            }
            if (callback != null) {
              callback.onTopLineArrived(new String(cmd_buf));
            }
            break;
          }

          if (test_byte == 0x02) {
            flag = Line.Line_02;
            len = 0;
            for (i = 0; i < max_size; i++) {
              cmd_buf[i] = ' ';
            }
            if (callback != null) {
              callback.onBottomLineArrived(new String(cmd_buf));
            }
            break;
          }

          // check if this is printable ASCII char
          if ((test_byte >= ' ') && (test_byte <= '~')) {
            cmd_buf[len] = test_byte;
            len++;
          }

          // plot the lines depend flag
          if (flag == Line.Line_01) {
            if (callback != null) {
              callback.onTopLineArrived(new String(cmd_buf));
            }
          }

          if (flag == Line.Line_02) {
            if (callback != null) {
              callback.onBottomLineArrived(new String(cmd_buf));
            }
          }

          // wait for new start symbol if len more than max allowed
          if (len >= max_size) {
            flag = Line.Line_xx;
            cmd_state = State.START;
          }

        }
        break;
      }
    }
  }
  private enum State {INIT, START, BODY}

  private enum Line {Line_01, Line_02, Line_xx}

  public interface IOnDataArriveListener {

    void onTopLineArrived(String topLine);

    void onBottomLineArrived(String topLine);
  }

  public static class IllegalLineIndexException extends Exception {

    byte invalidIndexByte;

    public IllegalLineIndexException(byte invalidIndexByte) {
      this.invalidIndexByte = invalidIndexByte;
    }

    byte invalidIndex() {
      return invalidIndexByte;
    }
  }

}
