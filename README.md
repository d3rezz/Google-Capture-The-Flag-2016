#Google-Capture-The-Flag-2016
##Ill lntentions
Challenge description: Do you have have ill intentions?

The illintentions.apk was provided.

#Inspecting the provided apk
After firing up the illintentions.apk in genymotion, we are greeted with an activity displaying: "Select the activity you wish to interact with.To-Do: Add buttons to select activity, for now use Send_to_Activity"

Not much to do here.

#Decompiling the apk
Dex2jar allows us to obtain the jar file

`d2j-dex2jar.sh BobbyApplication_CTF.apk`

Now to extract the code, we used jd-gui (Java Decompiler)

#Inspecting the source
The code for the MainActivity is:
```java
public class MainActivity extends Activity
{

    public MainActivity()
    {
    }

    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        bundle = new TextView(getApplicationContext());
        bundle.setText("Select the activity you wish to interact with.To-Do: Add buttons to select activity, for now use Send_to_Activity");
        setContentView(bundle);
        bundle = new IntentFilter();
        bundle.addAction("com.ctf.INCOMING_INTENT");
        registerReceiver(new Send_to_Activity(), bundle, "ctf.permission._MSG", null);
    }
}
```
The decompiled code is not perfect but gives us a very good idea of what's going on.

There is a Broadcast Receiver named Send_to_Activity being registered on the onCreate with the action `com.ctf.INCOMING_INTENT` and requires the permission `ctf.permission._MSG`.

The code for the Broadcast Receiver is:
```java
public class Send_to_Activity extends BroadcastReceiver
{

    public Send_to_Activity()
    {
    }

    public void onReceive(Context context, Intent intent)
    {
        intent = intent.getStringExtra("msg");
        if (intent.equalsIgnoreCase("ThisIsTheRealOne"))
        {
            context.startActivity(new Intent(context, com/example/application/ThisIsTheRealOne));
            return;
        }
        if (intent.equalsIgnoreCase("IsThisTheRealOne"))
        {
            context.startActivity(new Intent(context, com/example/application/IsThisTheRealOne));
            return;
        }
        if (intent.equalsIgnoreCase("DefinitelyNotThisOne"))
        {
            context.startActivity(new Intent(context, com/example/application/DefinitelyNotThisOne));
            return;
        } else
        {
            Toast.makeText(context, "Which Activity do you wish to interact with?", 1).show();
            return;
        }
    }
}
```

So if we send a broadcast, with the right action and "msg" extra, we can get to other 3 activities (ThisIsTheRealOne, IsThisTheRealOne, DefinitelyNotThisOne).

##Launching the Activities
The receiver is registered on the AndroidManifest.xml as:
```xml
<receiver 
	android:exported="true"
	android:name="com.example.application.Send_to_Activity"/>
```

`exported="true"` means the receiver can receive broadcasts from outside the application. This means good news as we can just use adb (Android Debug Bridge) to send broadcasts. The commands to open each Activity are:
```
./adb shell am broadcast -a com.ctf.INCOMING_INTENT --es "msg" "ThisIsTheRealOne"
./adb shell am broadcast -a com.ctf.INCOMING_INTENT --es "msg" "IsThisTheRealOne"
./adb shell am broadcast -a com.ctf.INCOMING_INTENT --es "msg" "DefinitelyNotThisOne"
```

The new activities are just a big button filling the whole screen as can been seen from each activity's code:
```java
public class ThisIsTheRealOne extends Activity
{
    public ThisIsTheRealOne()
    {
    }
    
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        (new TextView(this)).setText("Activity - This Is The Real One");
        bundle = new Button(this);
        bundle.setText("Broadcast Intent");
        setContentView(bundle);
        bundle.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(View view)
            {
                view = new Intent();
                view.setAction("com.ctf.OUTGOING_INTENT");
                String s = (new StringBuilder()).append(getResources().getString(0x7f030006)).append("YSmks").toString();
                String s1 = Utilities.doBoth(getResources().getString(0x7f030002));
                String s2 = Utilities.doBoth(getClass().getName());
                view.putExtra("msg", orThat(s, s1, s2));
                sendBroadcast(view, "ctf.permission._MSG");
            }
        });
    }
}
```

The button's onClick method sends a broadcast, with a "msg" extra containing a string with some operations applied on it.
Taking a look at strings.xml we find those strings contain only gibberish:
```xml
<string name="flag">Qvq lbh guvax vg jbhyq or gung rnfl?</string>
<string name="str1">`wTtqnVfxfLtxKB}YWFqqnXaOIck`</string>
<string name="str2">IIjsWa}iy</string>
<string name="str3">TRytfrgooq|F{i-JovFBungFk</string>
<string name="str4">H0l3kwjo1|+kdl^polr</string> 
```

The operations applied on those strings are described in Utilities.java. They seem however too complicate to undestand so we will just focus on capturing those broadcasts.

##Capturing the Broadcasts
For capturing the broadcasts, I just wrote an app that would register a BroadcastReceiver and listen for broadcasts with the action "com.ctf.OUTGOING_INTENT" and print the content os "msg".
The source code for the app is in this repo.

**Beware!** The illintentions.apk sends a broadcast requiring the permission "ctf.permission._MSG" so we need to add `    <uses-permission android:name="ctf.permission._MSG" />` to our AndroidManifest.xml

##Running the exploit
After deploying  the app to genymotion and pressing the button on one of those last activities, by looking at logcat we notice the app still isn't able to receive the broadcast due to *Permission Denial*.

This is because the permission `ctf.permission._MSG` defined on the illintentions.apk' manifest file has `android:protectionLevel="signature"` which means it can only be used within the application. The workaround is to remove this attribute from the AndroidManifest.xml and recompile the illintentions.apk again.

##Recompiling and signing the apk
I used this as reference https://www.reddit.com/r/Android/comments/11852r/how_to_modify_an_apk

Using apktool to recompile:
```
apktool b sourcefolder illintentions.apk
```

The apk now needs to be signed, otherwise it throws a Signature Error when installing. To resign the apk (see the reddit thread for the necessary files):
```
java -jar signapk.jar certificate.pem key.pk8 illintentions.apk illintentions2.apk
```

##Running the apk again
After deploying the recompiled illintentions.apk, I opened the IsThisTheRealOne activity with adb and pressed the button to fire the broadcast. My apk picked the broadcast and printed the flag `CTF{IDontHaveABadjokeSorry}`.
