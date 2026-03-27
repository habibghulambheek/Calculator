package com.Habib.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

        setupClickListeners();
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
            btn.setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
        }

        // Operator buttons
        findViewById(R.id.btn_add)     .setOnClickListener(v -> setOperator("+"));
        findViewById(R.id.btn_subtract).setOnClickListener(v -> setOperator("−"));
        findViewById(R.id.btn_multiply).setOnClickListener(v -> setOperator("×"));
        findViewById(R.id.btn_divide)  .setOnClickListener(v -> setOperator("÷"));

        // Action buttons
        findViewById(R.id.btn_equals)   .setOnClickListener(v -> calculateResult());
        findViewById(R.id.btn_clear)    .setOnClickListener(v -> clearAll());
        findViewById(R.id.btn_ce)       .setOnClickListener(v -> clearEntry());
        findViewById(R.id.btn_backspace).setOnClickListener(v -> backspace());
        findViewById(R.id.btn_decimal)  .setOnClickListener(v -> addDecimal());
        findViewById(R.id.btn_negate)   .setOnClickListener(v -> negate());
        findViewById(R.id.btn_percent)  .setOnClickListener(v -> percent());
    }

    // ─────────────────────────── Digit input ───────────────────────────

    private void appendDigit(String digit) {
        if (hasResult) {
            // After a result: start a fresh number but keep result visible until first key
            currentNumber.setLength(0);
            expressionText = "";
            tvExpression.setText("");
            hasResult  = false;
            isNewEntry = false;
        } else if (isNewEntry) {
            currentNumber.setLength(0);
            isNewEntry = false;
        }
        // Limit display to 15 digits to prevent overflow
        if (currentNumber.length() >= 15) return;

        // Prevent multiple leading zeros  e.g. "007"
        if (currentNumber.toString().equals("0") && !digit.equals(".")) {
            currentNumber.setLength(0);
        }
        currentNumber.append(digit);
        tvDisplay.setText(currentNumber.toString());
    }

    // ─────────────────────────── Operators ───────────────────────────

    private void setOperator(String operator) {
        if (currentNumber.length() == 0 && !hasResult) return;

        // Chain operation: evaluate the pending one first
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

        tvExpression.setText(fullExpr);

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

    /** C — full reset */
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

    /** CE — clear only current entry */
    private void clearEntry() {
        currentNumber.setLength(0);
        isNewEntry = false;
        hasResult  = false;
        tvDisplay.setText("0");
    }

    /** ⌫ — delete last character */
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

    /** . — decimal point */
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

    /** +/− — toggle sign */
    private void negate() {
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

    /** % — percentage */
    private void percent() {
        if (currentNumber.length() == 0) return;
        double value = parseCurrentNumber();
        double pct   = (!pendingOperator.isEmpty())
                ? (firstOperand * value / 100.0)   // e.g. 200 + 10% → 20
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

    /**
     * Formats a double for display:
     *  – Whole numbers shown without decimal point (e.g. 42, not 42.0)
     *  – Up to 10 significant decimal places, trailing zeros stripped
     */
    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        DecimalFormat df = new DecimalFormat("#.##########");
        return df.format(value);
    }
}