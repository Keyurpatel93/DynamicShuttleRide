package edu.msu.cse.patelke6.DynamicShuttleRide;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;



public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback{


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    /** Alias for our key in the Android Key Store */
    private static final String KEY_NAME = "my_key";


    @Inject KeyguardManager mKeyguardManager;
    @Inject FingerprintManager mFingerprintManager;
    @Inject FingerprintAuthenticationDialogFragment mFragment;
    @Inject KeyStore mKeyStore;
    @Inject KeyGenerator mKeyGenerator;
    @Inject Cipher mCipher;
    @Inject SharedPreferences mSharedPreferences;

    private static final int MY_PERMISSIONS_REQUEST = 0;
    private static String[] PERMISSIONS_REQUEST = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((InjectedApplication) getApplication()).inject(this);

        setContentView(R.layout.activity_main);


        ImageView mainLogoImgView = (ImageView) findViewById(R.id.mainLogo);
        ImageView shuttleImgView = (ImageView) findViewById(R.id.shuttleImgView);

        //ToDo figure out why picasso does not display the image
//        Picasso.with(this)
//                .load(R.drawable.DynamicShuttleRide)
//                .fit()
//                .centerInside()
//                .into(shuttleImgView);

        Picasso.with(this)
                .load(R.drawable.smartmobilitylogo)
                .fit()
                .centerInside()
                .into(mainLogoImgView);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // todo Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(this, PERMISSIONS_REQUEST,
                        MY_PERMISSIONS_REQUEST);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, PERMISSIONS_REQUEST,
                        MY_PERMISSIONS_REQUEST);
            }
        }

    }

    private void init(){

        Button authBtn = (Button) findViewById(R.id.authenticate_button);
        if (!mKeyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            authBtn.setEnabled(false);
            return;
        }

        //noinspection ResourceType
        if (!mFingerprintManager.hasEnrolledFingerprints()) {
            authBtn.setEnabled(false);
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey();
        authBtn.setEnabled(true);
        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Set up the crypto object for later. The object will be authenticated by use
                // of the fingerprint.
                if (initCipher()) {

                    // Show the fingerprint dialog. The user has the option to use the fingerprint with
                    // crypto, or you can fall back to using a server-side verified password.
                    mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                    boolean useFingerprintPreference = mSharedPreferences
                            .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                    true);
                    if (useFingerprintPreference) {
                        mFragment.setStage(
                                FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                    } else {
                        mFragment.setStage(
                                FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                    }
                    mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                } else {
                    // This happens if the lock screen has been disabled or or a fingerprint got
                    // enrolled. Thus show the dialog to authenticate with their password first
                    // and ask the user if they want to authenticate with fingerprints in the
                    // future
                    mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                    mFragment.setStage(
                            FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                    mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                }
            }
        });
        

    }

    @Override
    public void onResume(){
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public void onAuth(boolean withFingerprint) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            tryEncrypt();

        } else {
            // Authentication happened with backup password. Just show the confirmation message.
            showConfirmation(null);
        }
        userAuthenticated();
    }

    // Show confirmation, if fingerprint was used show crypto information. Not used in current application
    private void showConfirmation(byte[] encrypted) {

        if (encrypted != null) {
            Log.i("Encrypted Message", "" + Base64.encodeToString(encrypted, 0 /* flags */));
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt() {
        try {
            byte[] encrypted = mCipher.doFinal(SECRET_MESSAGE.getBytes());
            showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the Authentication", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    public void createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            // Require the user to authenticate with a fingerprint to authorize every use
                            // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST) {

            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                init();
                Log.i("Permission", "Write and Read permission has now been granted.");

            } else {
                Log.i("Permission", "Write permission was NOT granted.");
                Toast.makeText(this, "Permission required to operate app", Toast.LENGTH_LONG).show();

                //Close App
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        System.exit(0);
                    }
                }, 2000);
            }

        }  else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //When fingerprint reader auth user start activity to view and book rides.
    void userAuthenticated(){
        Log.i("UserAuth", "Yes");
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.userRadioGroup);
        int radioBtnID = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) radioGroup.findViewById(radioBtnID);

        Intent myIntent = new Intent(this, DynamicShuttleRideActivity.class);
        myIntent.putExtra("Name", radioButton.getText().toString());
        this.startActivity(myIntent);
    }


}
