package com.Habib.calculator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvDisplay;

    private StringBuilder currentNumber = new StringBuilder();
    private double firstOperand   = 0;
    private String  pendingOperator = "";
    private boolean isNewEntry    = true;
    private boolean hasResult     = false;
    private String  expressionText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tv_expression);
        tvDisplay    = findViewById(R.id.tv_display);

        if (savedInstanceState != null) {
            currentNumber = new StringBuilder(savedInstanceState.getString("currentNumber", ""));
            firstOperand = savedInstanceState.getDouble("firstOperand");
            pendingOperator = savedInstanceState.getString("pendingOperator", "");
            isNewEntry = savedInstanceState.getBoolean("isNewEntry");
            hasResult = savedInstanceState.getBoolean("hasResult");
            expressionText = savedInstanceState.getString("expressionText", "");
            tvExpression.setText(expressionText);
            tvDisplay.setText(savedInstanceState.getString("displayText", "0"));
        }

        setupClickListeners();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentNumber", currentNumber.toString());
        outState.putDouble("firstOperand", firstOperand);
        outState.putString("pendingOperator", pendingOperator);
        outState.putBoolean("isNewEntry", isNewEntry);
        outState.putBoolean("hasResult", hasResult);
        outState.putString("expressionText", expressionText);
        outState.putString("displayText", tvDisplay.getText().toString());
    }

    // ─────────────────────────── Setup ───────────────────────────

    private void setupClickListeners() {

        // Digit buttons (0–9)
        int[] digitIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        for (int id : digitIds) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
            }
        }

        // Operator buttons
        setClickListener(R.id.btn_add, v -> setOperator("+"));
        setClickListener(R.id.btn_subtract, v -> setOperator("−"));
        setClickListener(R.id.btn_multiply, v -> setOperator("×"));
        setClickListener(R.id.btn_divide, v -> setOperator("÷"));

        // Action buttons
        setClickListener(R.id.btn_equals, v -> calculateResult());
        setClickListener(R.id.btn_clear, v -> clearAll());
        setClickListener(R.id.btn_ce, v -> clearEntry());
        setClickListener(R.id.btn_backspace, v -> backspace());
        setClickListener(R.id.btn_decimal, v -> addDecimal());
        setClickListener(R.id.btn_negate, v -> negate());
        setClickListener(R.id.btn_percent, v -> percent());
    }

    private void setClickListener(int id, android.view.View.OnClickListener listener) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }

    // ─────────────────────────── Digit input ───────────────────────────

    private void appendDigit(String digit) {
        if (hasResult) {
            currentNumber.setLength(0);
            expressionText = "";
            tvExpression.setText("");
            hasResult  = false;
            isNewEntry = false;
        } else if (isNewEntry) {
            currentNumber.setLength(0);
            isNewEntry = false;
        }
        
        if (currentNumber.length() >= 15) return;

        if (currentNumber.toString().equals("0") && !digit.equals(".")) {
            currentNumber.setLength(0);
        }
        currentNumber.append(digit);
        tvDisplay.setText(currentNumber.toString());
    }

    // ─────────────────────────── Operators ───────────────────────────

    private void setOperator(String operator) {
        // If nothing entered, treat as 0
        if (currentNumber.length() == 0 && !hasResult) {
            currentNumber.append("0");
        }

        if (!pendingOperator.isEmpty() && !isNewEntry) {
            double second = parseCurrentNumber();
            String chainResult = compute(firstOperand, second, pendingOperator);
            if (isErrorString(chainResult)) { showError(chainResult); return; }
            firstOperand = Double.parseDouble(chainResult);
            tvDisplay.setText(formatNumber(firstOperand));
            currentNumber.setLength(0);
            currentNumber.append(formatNumber(firstOperand));
        } else {
            firstOperand = parseCurrentNumber();
        }

        pendingOperator = operator;
        expressionText  = formatNumber(firstOperand) + "  " + operator;
        tvExpression.setText(expressionText);
        isNewEntry = true;
        hasResult  = false;
    }

    // ─────────────────────────── Equals ───────────────────────────

    private void calculateResult() {
        if (pendingOperator.isEmpty()) return;

        double second   = isNewEntry ? firstOperand : parseCurrentNumber();
        String fullExpr = formatNumber(firstOperand)
                + "  " + pendingOperator
                + "  " + formatNumber(second)
                + "  =";
        String result   = compute(firstOperand, second, pendingOperator);

        expressionText = fullExpr;
        tvExpression.setText(expressionText);

        if (isErrorString(result)) {
            showError(result);
            return;
        }

        tvDisplay.setText(result);
        firstOperand = Double.parseDouble(result);
        currentNumber.setLength(0);
        currentNumber.append(result);
        pendingOperator = "";
        isNewEntry = true;
        hasResult  = true;
    }

    // ─────────────────────────── Arithmetic core ───────────────────────────

    private String compute(double a, double b, String op) {
        double result;
        switch (op) {
            case "+": result = a + b; break;
            case "−": result = a - b; break;
            case "×": result = a * b; break;
            case "÷":
                if (b == 0) return "Cannot divide by 0";
                result = a / b;
                break;
            default:  return "Invalid operator";
        }
        if (Double.isInfinite(result)) return "Result too large";
        if (Double.isNaN(result))      return "Undefined result";
        return formatNumber(result);
    }

    // ─────────────────────────── Special buttons ───────────────────────────

    private void clearAll() {
        currentNumber.setLength(0);
        firstOperand    = 0;
        pendingOperator = "";
        isNewEntry      = true;
        hasResult       = false;
        expressionText  = "";
        tvExpression.setText("");
        tvDisplay.setText("0");
    }

    private void clearEntry() {
        currentNumber.setLength(0);
        isNewEntry = false;
        hasResult  = false;
        tvDisplay.setText("0");
    }

    private void backspace() {
        if (hasResult || isNewEntry) return;
        if (currentNumber.length() > 0) {
            currentNumber.deleteCharAt(currentNumber.length() - 1);
            String display = currentNumber.toString();
            if (display.isEmpty() || display.equals("-")) {
                currentNumber.setLength(0);
                tvDisplay.setText("0");
            } else {
                tvDisplay.setText(display);
            }
        }
    }

    private void addDecimal() {
        if (hasResult) {
            currentNumber.setLength(0);
            currentNumber.append("0");
            hasResult      = false;
            expressionText = "";
            tvExpression.setText("");
        }
        if (isNewEntry) {
            currentNumber.setLength(0);
            currentNumber.append("0");
            isNewEntry = false;
        }
        if (currentNumber.length() == 0) currentNumber.append("0");
        if (!currentNumber.toString().contains(".")) {
            currentNumber.append(".");
            tvDisplay.setText(currentNumber.toString());
        }
    }

    private void negate() {
        if (currentNumber.length() == 0 && !hasResult) {
            currentNumber.append("0");
        }
        if (currentNumber.length() == 0) return;
        
        String cur = currentNumber.toString();
        if (cur.equals("0") || cur.equals("0.")) return;
        if (cur.startsWith("-")) {
            currentNumber.deleteCharAt(0);
        } else {
            currentNumber.insert(0, "-");
        }
        tvDisplay.setText(currentNumber.toString());
        if (hasResult) firstOperand = parseCurrentNumber();
    }

    private void percent() {
        if (currentNumber.length() == 0 && !hasResult) {
             currentNumber.append("0");
        }
        if (currentNumber.length() == 0) return;
        
        double value = parseCurrentNumber();
        double pct   = (!pendingOperator.isEmpty())
                ? (firstOperand * value / 100.0)
                : (value / 100.0);
        String fmt   = formatNumber(pct);
        currentNumber.setLength(0);
        currentNumber.append(fmt);
        tvDisplay.setText(fmt);
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private void showError(String msg) {
        tvDisplay.setText(msg);
        currentNumber.setLength(0);
        pendingOperator = "";
        firstOperand    = 0;
        isNewEntry      = true;
        hasResult       = false;
    }

    private boolean isErrorString(String s) {
        return s.startsWith("Cannot") || s.startsWith("Invalid")
                || s.startsWith("Result") || s.startsWith("Undefined");
    }

    private double parseCurrentNumber() {
        try {
            if (currentNumber.length() == 0) return 0;
            return Double.parseDouble(currentNumber.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        DecimalFormat df = new DecimalFormat("#.##########");
        return df.format(value);
    }
}
