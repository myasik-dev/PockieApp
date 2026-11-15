package com.pockie.app;

import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())
        || !Tracking.prefs(c).getBoolean("sms", false)) return;
    PendingResult result = goAsync();
    new Thread(
            () -> {
              try {
                SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (parts.length == 0) return;
                StringBuilder body = new StringBuilder();
                for (SmsMessage part : parts) body.append(part.getMessageBody());
                Tracking.importMessage(
                    c,
                    parts[0].getOriginatingAddress(),
                    body.toString(),
                    parts[0].getTimestampMillis(),
                    "SMS");
              } finally {
                result.finish();
              }
            },
            "pockie-sms")
        .start();
  }
}
