package at.co.netconsulting.at.co.netconsulting.general;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by bernd on 02.04.18.
 */

public class MessageToast {
    public static void Toast_msg(Context context, String message) {
        Toast.makeText(context, "ERROR in ShutdownServers: " + message, Toast.LENGTH_LONG).show();
    }
}
