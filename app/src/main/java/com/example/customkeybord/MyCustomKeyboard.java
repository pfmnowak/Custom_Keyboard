package com.example.customkeybord;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Set;
import java.util.UUID;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;
    private boolean isCaps = false;
    static final int KEYCODE_PLAY = -1201;
    static final int KEYCODE_TOAST = -1202;
    static final int KEYCODE_CAMERA = -1203;
    static final int KEYCODE_SWITCH = -1204;
    static final int KEYCODE_SWITCH_BACK = -1205;
    static final int KEYCODE_TEXT_INSERT = -1206;
    static final int KEYCODE_TEXT_DROP = -1207;
    static final int KEYCODE_NFC = -1208;
    static final int KEYCODE_NFC_SWITCH = -1209;
    static final int KEYCODE_SWAP = -1210;
    static final int KEYCODE_BT = -1211;
    static final int KEYCODE_SCREENSHOT = -1212;
    String nfc = "";
    String bt = "";
    StringBuilder messages;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothConnectionService mBluetoothConnection;
    BluetoothDevice mBTDevice;

    private static final String TAG = "MainActivity";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    @Override
     public void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this,R.xml.buttons);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }

    public View onCreateInputView2() {
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this,R.xml.qwerty);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    @Override
    public void onKey(int i, int[] ints) {
        InputConnection ic = getCurrentInputConnection();
        playClick(i);
        switch (i)
        {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1,0);
                break;

            case KEYCODE_TEXT_DROP:
                ic.deleteSurroundingText(100,0);
                break;

            case KEYCODE_TEXT_INSERT:
                ic.commitText("Najlepszy custom keyboard", 1);
                break;

            case KEYCODE_PLAY:
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.sample);
                mp.start();
                break;

            case KEYCODE_TOAST:
                Toast.makeText(this, "Toast działa", Toast.LENGTH_SHORT).show();
                break;

            case KEYCODE_CAMERA:
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                startActivity(intent);
                break;

            case KEYCODE_SWITCH:
                setInputView(onCreateInputView2());
                break;

            case KEYCODE_SWITCH_BACK:
                setInputView(onCreateInputView());
                break;

            case KEYCODE_NFC:
                NfcManager manager = (NfcManager) getApplicationContext().getSystemService(Context.NFC_SERVICE);
                NfcAdapter adapter = manager.getDefaultAdapter();
                if (adapter != null && adapter.isEnabled()) {
                    nfc = "NFC/ON";
                } else {
                    nfc = "NFC/OFF";
                }
                Toast.makeText(this, nfc, Toast.LENGTH_SHORT).show();
                break;

            case KEYCODE_NFC_SWITCH:
                Intent intent2 = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent2);
                break;

            case KEYCODE_SWAP:
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
                break;

            case KEYCODE_BT:
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    bt = "BT don't supported!";
                } else if (!mBluetoothAdapter.isEnabled()) {
                    bt = "BT disabled!";
                } else {
                    bt = "BT enabled!";
                }
                Toast.makeText(this, bt, Toast.LENGTH_SHORT).show();
                // Nawiązanie połączenia i uruchomienie serwera
                startConnection();
                break;

            case KEYCODE_SCREENSHOT:
                // Maybe later
                break;

            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                kv.invalidateAllKeys();
                break;

            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                break;

            default:
                char code = (char)i;
                if(Character.isLetter(code) && isCaps)
                    code = Character.toUpperCase(code);
                ic.commitText(String.valueOf(code),1);
        }

    }

    // Odbieranie i wyświetlanie tekstu z drugiego pliku
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            messages.setLength(0);
            messages.append(text);
            // Wyświetlanie tekstu w polu wpisywania
            InputConnection ic = getCurrentInputConnection();
            ic.commitText(messages, 1);
        }
    };

    public void startConnection(){
        messages = new StringBuilder();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                // Jeśli urządzenie się zgadza to przypisz nazwę
                if(deviceName.equals("Redmi 4X")) {
                    mBTDevice = device;
                    Log.e(TAG, "ZNALEZIONO_Redmi 4X");
                    startBTConnection(mBTDevice,MY_UUID_INSECURE);
                }
            }
        }
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection = new BluetoothConnectionService(MyCustomKeyboard.this);
        mBluetoothConnection.startClient(device,uuid);
    }

    private void playClick(int i) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch(i)
        {
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}