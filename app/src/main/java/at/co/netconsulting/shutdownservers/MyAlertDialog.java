package at.co.netconsulting.shutdownservers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by bernd on 24.03.18.
 */

public class MyAlertDialog {

    private AlertDialog alertDialog;
    private Context context;
    private String message;

    //-----------------------------------Constructors-----------------------------------//

    public MyAlertDialog(Context baseContext, String message) {
        setContext(baseContext);
        setMessage(message);
    }

    public void showAlertDialog(){

        alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(getMessage());
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    //-----------------------------------Getter / Setter-----------------------------------//

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAlertDialog(AlertDialog alertDialog) {
        this.alertDialog = alertDialog;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getMessage() {
        return message;
    }

    public AlertDialog getAlertDialog() {
        return alertDialog;
    }

    public Context getContext() {
        return context;
    }
}