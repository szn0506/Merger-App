package com.szn.merger.Utils.Signing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;

public class SigningActivity extends AppCompatActivity {
    private CustomSwitchItem signSwitch;
    TextInputEditText keystoreName, alias, password, confirmPassword, importPassword;
    private MaterialCardView signSchemes, importCard;
    private static MaterialCheckBox V1, V2, V3, V4;
    private TextView currentSchemes;
    private MaterialToolbar toolbar;
    private RecyclerView keystoreRecycler;
    private KeystoreAdapter adapter;
    private MaterialButton btnGenerate;
    private MaterialButton btnImport;
    private String name, aliasName, pass, confirm, importPass;
    private Uri selectedKeystoreUri;
    private String selectedKeystoreType, selectedKeystoreName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signing_layout);
        initView();
        setupListener();
        setupRecycler();
        loadState();
        updatePlaceholder();
    }

    private void initView() {
        signSwitch = findViewById(R.id.SignSwitch);
        signSchemes = findViewById(R.id.signSchemes);
        currentSchemes = findViewById(R.id.currentSchemes);
        toolbar = findViewById(R.id.toolbar);
        keystoreRecycler = findViewById(R.id.keystoreRecycler);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnImport = findViewById(R.id.btnImport);
    }
    private void setupListener() {
        signSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SigningManager.setSignEnabled(this, isChecked);
        });
        signSchemes.setOnClickListener(v -> {
            showSchemesBottomSheet();
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnGenerate.setOnClickListener(v -> showGenerateBottomSheet());
        btnImport.setOnClickListener( v-> showImportBottomSheet());
    }
    private void setupRecycler() {

        KeystoreManager manager =
                new KeystoreManager(this);

        adapter =
                new KeystoreAdapter(
                        this,
                        manager.getAll()
                );

        keystoreRecycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        keystoreRecycler.setAdapter(adapter);
    }

    private boolean validateGenerateInput() {

        if (name.isEmpty()) {
            keystoreName.setError("Required");
            return false;
        }

        if (aliasName.isEmpty()) {
            alias.setError("Required");
            return false;
        }

        if (pass.length() < 6) {
            password.setError("At least 6 characters");
            return false;
        }

        if (!pass.equals(confirm)) {
            confirmPassword.setError("Password doesn't match");
            return false;
        }

        return true;
    }
    private void generateKeystore(
            String name,
            String aliasName,
            String password
    ) {

        try {

            KeystoreManager manager =
                    new KeystoreManager(this);

            KeystoreGenerator generator =
                    new KeystoreGenerator(
                            name,
                            aliasName,
                            password
                    );

            KeystoreManager.Item item =
                    generator.generate(
                            manager.getFolder()
                    );
            Log.d(
                    "KEYSTORE",
                    "Saved: " + item.fileName);
            manager.save(item);

            adapter.reload(
                    manager.getAll()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showGenerateBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.keystore_generator_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        keystoreName = bottomSheetView.findViewById(R.id.keystoreName);
        alias = bottomSheetView.findViewById(R.id.alias);
        password = bottomSheetView.findViewById(R.id.password);
        confirmPassword = bottomSheetView.findViewById(R.id.confirmPassword);

        MaterialButton generateBtn = bottomSheetView.findViewById(R.id.btnGenerate);

        generateBtn.setOnClickListener(v -> {

            name = keystoreName.getText().toString().trim();
            aliasName = alias.getText().toString().trim();
            pass = password.getText().toString().trim();
            confirm = confirmPassword.getText().toString().trim();

            if (!validateGenerateInput()) {
                return;
            }

            generateKeystore(
                    name,
                    aliasName,
                    pass
            );

            bottomSheetDialog.dismiss();
        });
    }

    private final ActivityResultLauncher<Intent> keystorePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            selectedKeystoreUri = result.getData().getData();

                            selectedKeystoreName = getFileName(selectedKeystoreUri);

                            String path = selectedKeystoreUri.getPath();

                            if (path != null && path.endsWith(".jks")) {
                                selectedKeystoreType = "JKS";
                            } else {
                                selectedKeystoreType = "PKCS12";
                            }

                            Toast.makeText(
                                    this,
                                    "Keystore selected",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    private void openSAF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        keystorePicker.launch(intent);
    }

    private boolean validateImport() {
        if (selectedKeystoreUri == null) {
            Toast.makeText(
                    this,
                    "Select keystore first",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }


        String password =
                importPassword.getText()
                        .toString();


        if (password.isEmpty()) {
            importPassword.setError(
                    "Password required"
            );
            return false;
        }

        return true;
    }

    private void importKeystore() {
        try {
            KeystoreManager manager = new KeystoreManager(this);
            KeystoreImporter importer = new KeystoreImporter(this);
            KeystoreManager.Item item = importer.importKeystore(selectedKeystoreUri, selectedKeystoreName, importPass, selectedKeystoreType);
            manager.save(item);
            adapter.reload(manager.getAll());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         getContentResolver().query(
                                 uri,
                                 null,
                                 null,
                                 null,
                                 null
                         )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                    );

                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');

            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private void showImportBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.keystore_import_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        importPassword = bottomSheetDialog.findViewById(R.id.password);
        importCard = bottomSheetDialog.findViewById(R.id.importKeystore);
        MaterialButton importBtn = bottomSheetDialog.findViewById(R.id.btnImport);
        importCard.setOnClickListener( v -> openSAF());

        importBtn.setOnClickListener(v -> {
            importPass = importPassword.getText().toString().trim();
            if (!validateImport()) return;
            importKeystore();
            bottomSheetDialog.dismiss();
        });
    }

    private void showSchemesBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.sign_schemes_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        V1 = bottomSheetView.findViewById(R.id.checkV1);
        V2 = bottomSheetView.findViewById(R.id.checkV2);
        V3 = bottomSheetView.findViewById(R.id.checkV3);
        V4 = bottomSheetView.findViewById(R.id.checkV4);
        MaterialButton doneButton = bottomSheetView.findViewById(R.id.doneButton);
        restoreState();

        V1.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV1Enabled(this, isChecked));

        V2.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV2Enabled(this, isChecked));

        V3.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV3Enabled(this, isChecked));

        V4.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV4Enabled(this, isChecked));
        doneButton.setOnClickListener(v -> {
            updatePlaceholder();
            bottomSheetDialog.dismiss();
        });
    }
    private void restoreState() {
        V1.setChecked(SigningManager.isV1Enabled(this));
        V2.setChecked(SigningManager.isV2Enabled(this));
        V3.setChecked(SigningManager.isV3Enabled(this));
        V4.setChecked(SigningManager.isV4Enabled(this));
    }
    private void updatePlaceholder() {
        StringBuilder schemes = new StringBuilder();

        if (SigningManager.isV1Enabled(this)) schemes.append("V1");
        if (SigningManager.isV2Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V2");
        }
        if (SigningManager.isV3Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V3");
        }
        if (SigningManager.isV4Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V4");
        }

        currentSchemes.setText(schemes.length() > 0 ? schemes.toString() : "None");
    }
    private void loadState() {
        signSwitch.setChecked(SigningManager.isSignEnabled(this));
    }
}
