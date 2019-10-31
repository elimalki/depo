package pvt.talinor.ptl8;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import butterknife.ButterKnife;
import java.util.Objects;

/**
 * <p> Created by Rubezhin Evgenij on 6/18/2019. <br>
 * Copyright (c) 2019 LineUp. <br> Project: bm71term, pvt.talinor.ptl8 </p>
 *
 * @author Rubezhin Evgenij
 * @version 1.0
 */
public class LoadingDialog extends DialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(
        Objects.requireNonNull(getActivity()));
    LayoutInflater inflater = getActivity().getLayoutInflater();

    View view = inflater.inflate(R.layout.progress_dialog, null);
    builder.setCancelable(false);

    ButterKnife.bind(this, view);

    AlertDialog dialog = builder.create();
    dialog.setView(view);
    return dialog;
  }

}
