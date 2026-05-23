package com.Habib.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etExpression, etDisplay;

    private BigDecimal firstOperand = BigDecimal.ZERO;
    private BigDecimal lastResultValue = BigDecimal.ZERO;
    private String pendingOperator = "";
    private boolean isNewEntry = true;
    private boolean hasResult = false;
    private boolean isFullPrecisionShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etExpression = findViewById(R.id.et_expression);
        etDisplay = findViewById(R.id.et_display);

        // Professional behavior: allow cursor navigation/scrolling but suppress soft keyboard
        etExpression.setShowSoftInputOnFocus(false);
        etDisplay.setShowSoftInputOnFocus(false);
        etExpression.setShowSoftInputOnFocus(false);
        etDisplay.setShowSoftInputOnFocus(false);
        // Click logic for "Expandable Result" and "Copy" (Requirement 4)
        etDisplay.setOnClickListener(v -> {
            if (hasResult && !isErrorString(etDisplay.getText().toString())) {
                togglePrecision();
            }
            copyToClipboard();
        });

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        setupClickListeners();
    }

    private void togglePrecision() {
        if (!isFullPrecisionShown) {
            // Expand to full plain string (Requirement 4)
            etDisplay.setText(lastResultValue.stripTrailingZeros().toPlainString());
            isFullPrecisionShown = true;
        } else {
            // Contract to compact view (Requirement 3)
            etDisplay.setText(formatNumber(lastResultValue, true));
            isFullPrecisionShown = false;
        }
        etDisplay.setSelection(etDisplay.getText().length());
    }

    private void copyToClipboard() {
        String text = etDisplay.getText().toString();
        if (!TextUtils.isEmpty(text) && !text.equals("0") && !isErrorString(text)) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Calculator Result", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        try {
            firstOperand = new BigDecimal(savedInstanceState.getString("firstOperand", "0"));
            lastResultValue = new BigDecimal(savedInstanceState.getString("lastResultValue", "0"));
        } catch (Exception e) {
            firstOperand = BigDecimal.ZERO;
            lastResultValue = BigDecimal.ZERO;
        }
        pendingOperator = savedInstanceState.getString("pendingOperator", "");
        isNewEntry = savedInstanceState.getBoolean("isNewEntry");
        hasResult = savedInstanceState.getBoolean("hasResult");
        isFullPrecisionShown = savedInstanceState.getBoolean("isFullPrecisionShown");
        etExpression.setText(savedInstanceState.getString("expressionText", ""));
        etDisplay.setText(savedInstanceState.getString("displayText", "0"));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("firstOperand", firstOperand.toString());
        outState.putString("lastResultValue", lastResultValue.toString());
        outState.putString("pendingOperator", pendingOperator);
        outState.putBoolean("isNewEntry", isNewEntry);
        outState.putBoolean("hasResult", hasResult);
        outState.putBoolean("isFullPrecisionShown", isFullPrecisionShown);
        outState.putString("expressionText", etExpression.getText().toString());
        outState.putString("displayText", etDisplay.getText().toString());
    }

    private void setupClickListeners() {
        // Numbers
        int[] digitIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                          R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9};
        for (int id : digitIds) {
            findViewById(id).setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
        }

        // Operators
        setClickListener(R.id.btn_add, v -> setOperator("+"));
        setClickListener(R.id.btn_subtract, v -> setOperator("−"));
        setClickListener(R.id.btn_multiply, v -> setOperator("×"));
        setClickListener(R.id.btn_divide, v -> setOperator("÷"));

        // Actions
        setClickListener(R.id.btn_equals, v -> calculateResult(true));
        setClickListener(R.id.btn_clear, v -> clearAll());
        setClickListener(R.id.btn_decimal, v -> addDecimal());
        setClickListener(R.id.btn_percent, v -> percent());

        // Backspace (Requirement 1)
        ImageButton btnBackspace = findViewById(R.id.btn_backspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> backspace());
            btnBackspace.setOnLongClickListener(v -> {
                clearAll();
                return true;
            });
        }

        // Utility Icons
        setClickListener(R.id.btn_history, v -> Toast.makeText(this, R.string.history_desc, Toast.LENGTH_SHORT).show());
        setClickListener(R.id.btn_currency, v -> Toast.makeText(this, R.string.currency_desc, Toast.LENGTH_SHORT).show());
        setClickListener(R.id.btn_unit, v -> Toast.makeText(this, R.string.unit_converter_desc, Toast.LENGTH_SHORT).show());
        setClickListener(R.id.btn_mode, v -> Toast.makeText(this, R.string.mode_desc, Toast.LENGTH_SHORT).show());
    }

    private void setClickListener(int id, View.OnClickListener listener) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }

    private void appendDigit(String digit) {
        if (hasResult) {
            clearAll();
            hasResult = false;
        }
        if (isNewEntry) {
            etDisplay.setText("");
            isNewEntry = false;
        }

        int cursor = etDisplay.getSelectionStart();
        String current = etDisplay.getText().toString();

        if (current.equals("0") && !digit.equals(".")) {
            etDisplay.setText(digit);
            etDisplay.setSelection(1);
        } else {
            // Allows editing any digit in place (Requirement 2)
            StringBuilder sb = new StringBuilder(current);
            if (cursor >= 0) {
                sb.insert(cursor, digit);
                etDisplay.setText(sb.toString());
                etDisplay.setSelection(cursor + 1);
            } else {
                etDisplay.append(digit);
            }
        }
    }

    private void setOperator(String operator) {
        String currentText = etDisplay.getText().toString();
        if (isErrorString(currentText)) currentText = "0";

        if (!pendingOperator.isEmpty() && !isNewEntry) {
            calculateResult(false);
        } else {
            firstOperand = parseNumber(currentText);
        }

        pendingOperator = operator;
        etExpression.setText(getString(R.string.expression_format, formatNumber(firstOperand, true), operator));
        isNewEntry = true;
        hasResult = false;
        isFullPrecisionShown = false;
    }

    private void calculateResult(boolean isFinal) {
        if (pendingOperator.isEmpty()) return;

        BigDecimal secondOperand = parseNumber(etDisplay.getText().toString());
        BigDecimal result;
        
        try {
            result = compute(firstOperand, secondOperand, pendingOperator);
        } catch (ArithmeticException e) {
            showError("Cannot divide by 0");
            return;
        }

        lastResultValue = result;
        if (isFinal) {
            etExpression.setText(getString(R.string.result_format, formatNumber(firstOperand, true), pendingOperator, formatNumber(secondOperand, true)));
            etDisplay.setText(formatNumber(result, true));
            hasResult = true;
            pendingOperator = "";
            isFullPrecisionShown = false;
        } else {
            firstOperand = result;
            etDisplay.setText(formatNumber(result, true));
        }
        isNewEntry = true;
    }

    private BigDecimal compute(BigDecimal a, BigDecimal b, String op) {
        switch (op) {
            case "+": return a.add(b);
            case "−": return a.subtract(b);
            case "×": return a.multiply(b);
            case "÷":
                if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException();
                return a.divide(b, MathContext.DECIMAL128);
            default: return BigDecimal.ZERO;
        }
    }

    private void clearAll() {
        etDisplay.setText("0");
        etExpression.setText("");
        firstOperand = BigDecimal.ZERO;
        lastResultValue = BigDecimal.ZERO;
        pendingOperator = "";
        isNewEntry = true;
        hasResult = false;
        isFullPrecisionShown = false;
    }

    private void backspace() {
        if (hasResult) {
            etExpression.setText("");
            hasResult = false;
            return;
        }
        int cursor = etDisplay.getSelectionStart();
        if (cursor > 0) {
            StringBuilder sb = new StringBuilder(etDisplay.getText().toString());
            sb.deleteCharAt(cursor - 1);
            etDisplay.setText(sb.toString());
            etDisplay.setSelection(cursor - 1);
            if (etDisplay.getText().toString().isEmpty()) {
                etDisplay.setText("0");
                isNewEntry = true;
            }
        }
    }

    private void addDecimal() {
        if (hasResult || isNewEntry) {
            etDisplay.setText("0.");
            etDisplay.setSelection(2);
            hasResult = false;
            isNewEntry = false;
            return;
        }
        String text = etDisplay.getText().toString();
        if (!text.contains(".")) {
            int cursor = etDisplay.getSelectionStart();
            StringBuilder sb = new StringBuilder(text);
            sb.insert(cursor, ".");
            etDisplay.setText(sb.toString());
            etDisplay.setSelection(cursor + 1);
        }
    }

    private void percent() {
        BigDecimal value = parseNumber(etDisplay.getText().toString());
        BigDecimal result;
        
        // Professional logic: if operator pending (+/-), calculate percentage of firstOperand
        if (!pendingOperator.isEmpty() && (pendingOperator.equals("+") || pendingOperator.equals("−"))) {
            result = firstOperand.multiply(value).divide(new BigDecimal("100"), MathContext.DECIMAL128);
        } else {
            result = value.divide(new BigDecimal("100"), MathContext.DECIMAL128);
        }
        
        lastResultValue = result;
        etDisplay.setText(formatNumber(result, true));
        isNewEntry = false; // Allow continuing input
        isFullPrecisionShown = false;
    }

    private void showError(String msg) {
        etDisplay.setText(msg);
        pendingOperator = "";
        isNewEntry = true;
        hasResult = false;
    }

    private boolean isErrorString(String s) {
        return s.equals("Error") || s.contains("divide") || s.equals("Infinity") || s.equals("NaN");
    }

    private BigDecimal parseNumber(String s) {
        try {
            String sanitized = s.replace("−", "-").replace("×", "*").replace("÷", "/");
            return new BigDecimal(sanitized);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatNumber(BigDecimal value, boolean compact) {
        double dValue = value.doubleValue();
        // Requirement 3: Scientific notation for extremes
        if (compact && (Math.abs(dValue) >= 1e11 || (Math.abs(dValue) > 0 && Math.abs(dValue) < 1e-7))) {
            DecimalFormat df = new DecimalFormat("0.######E0", DecimalFormatSymbols.getInstance(Locale.US));
            return df.format(dValue);
        }

        // Limit digits in compact view
        if (compact && value.precision() > 14) {
            return value.round(new MathContext(12)).stripTrailingZeros().toPlainString();
        }

        return value.stripTrailingZeros().toPlainString();
    }
}
