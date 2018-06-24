package at.co.netconsulting.shutdownservers;

import android.content.Context;
import android.support.v7.app.AlertDialog;

/**
 * Created by bernd on 24.03.18.
 */

public class MyException extends Exception {
    public MyException(String message) {
        super(message);
    }

    public void alertUser(Context context){
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("WARNING");
        dialog.setMessage(this.toString());
        dialog.setNeutralButton("Ok", null);
        dialog.create().show();
    }
}
