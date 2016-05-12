package regexcnp;

import java.time.LocalDate;
import java.time.Year;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This JavaFX application is used for CNP validation.
 */
public class CnpApp extends Application {
    // the regex used for validation: [1-9]\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])(0[1-9]|[1-3]\d|4[0-6]|5[12])\d{2}[1-9]\d
    private final String CNP_REGEX = "[1-9]\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])(0[1-9]|[1-3]\\d|4[0-6]|5[12])\\d{2}[1-9]\\d";
    // length of a CNP
    private final int CNP_LENGTH = 13;
    // length of the first part of the CNP containing the digits for sex and date of birth
    private final int CNP_SEX_DATE_LENGTH = 7;
    // numbers used in the last part of the verification algorithm; for details see: https://ro.wikipedia.org/wiki/Cod_numeric_personal
    private final int[] CONTROL_ARRAY = {2, 7, 9, 1, 4, 6, 3, 5, 8, 2, 7, 9};
    
    // the pattern used for validation
    private Pattern cnpPattern;
    // a copy of the last verified cnp
    private String cnpVerifiedLast;
    
    /**
     * Compiles the pattern used for validation.
     */
    @Override
    public void init() {
        cnpPattern = Pattern.compile(CNP_REGEX);
    }
    
    /**
     * Creates all the controls of the application.
     * @param primaryStage the primary stage of the application
     */
    @Override
    public void start(Stage primaryStage) {
        // a label with instructions for the user
        Label instructionsLabel = new Label("Introduceti CNP in campul de mai jos:");
        
        // an effect used to show a glow around the cnp field based on the validity
        // of the cnp: green for valid, red for invalid, invisible when no result to show
        DropShadow cnpGlow = new DropShadow();
        cnpGlow.setColor(Color.TRANSPARENT);
        
        // a text field where the CNP is entered
        TextField cnpField = new TextField();
        cnpField.setAlignment(Pos.CENTER);
        cnpField.setEffect(cnpGlow);
        
        // a button used for starting the validation algorithm
        Button validationButton = new Button("Valideaza");
        validationButton.setDisable(true);
        
        // a horizontal box layout for the cnp field and validation button
        HBox cnpFieldButtonBox = new HBox(10);
        cnpFieldButtonBox.setAlignment(Pos.CENTER);
        cnpFieldButtonBox.getChildren().addAll(cnpField, validationButton);
        
         // a checkbox for allowing/disallowing CNP with future dates to be considered valid
        CheckBox futureDateCheck = new CheckBox("Permiteti CNP din viitor");
        futureDateCheck.setSelected(true);
        
        // a label that will show the result of the validation algorithm
        Label resultLabel = new Label();
        resultLabel.setAlignment(Pos.CENTER);
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        
        // a vertical box layout for the controls
        VBox layoutBox = new VBox(10);
        layoutBox.setPadding(new Insets(20));
        layoutBox.getChildren().addAll(instructionsLabel, cnpFieldButtonBox, futureDateCheck, resultLabel);
        
        // create a scene
        Scene scene = new Scene(layoutBox);
        
        // prepare the stage
        primaryStage.setTitle("Validator CNP");
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(CnpApp.class.getResourceAsStream("images/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();
        primaryStage.centerOnScreen();
        
        // the text formatter ensures that max 13 digits can be entered in the text field and no other characters
        cnpField.setTextFormatter(new TextFormatter<>(new UnaryOperator<Change>() {
            @Override
            public Change apply(Change change) {
                // get the newly entered text and its length
                String newText = change.getControlNewText();
                int newTextLength = newText.length();
                
                // just allow no text
                if (newTextLength == 0)
                    return change;
                
                // only accept up to 13 digits
                if (containsOnlyDigits(newText) && (newTextLength <= CNP_LENGTH)) {
                    // if exactly 13 digits have been entered
                    if (newTextLength == CNP_LENGTH)
                        validationButton.setDisable(false); // enable the validation button
                    else
                        validationButton.setDisable(true); // otherwise, disable the button
                    
                    return change;
                }
                
                return null;
            }
        }));
        
        // the listener of the text property empties the result label whenever the contents of the cnp field changes
        // also makes the glow of the cnp field invisible and the last verified cnp null
        cnpField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldString, String newString) {
                resultLabel.setText(null);
                cnpGlow.setColor(Color.TRANSPARENT);
                cnpVerifiedLast = null;
            }
        });
        
        // when the selected property of the future date checkbox changes, empty the result label
        // also make the glow of the cnp field invisible and the last verified cnp null
        futureDateCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasSelected, Boolean isSelected) {
                resultLabel.setText(null);
                cnpGlow.setColor(Color.TRANSPARENT);
                cnpVerifiedLast = null;
            }
        });
        
        // handle events created when the validation button is fired
        validationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ae) {
                // get the text from the field
                String cnpText = cnpField.getText();

                // if it's the same cnp, don't verify it again
                if (cnpText.equals(cnpVerifiedLast))
                    return;
                
                cnpVerifiedLast = cnpText;
                
                // create a matcher from the pattern and the text
                Matcher matcher = cnpPattern.matcher(cnpText);
                
                // if the CNP matches the pattern and the date is valid and the algorithm is valid
                if (matcher.matches() && dateIsValid(cnpText.substring(0, CNP_SEX_DATE_LENGTH), futureDateCheck.isSelected()) && algorithmIsValid(cnpText)) {
                    resultLabel.setText("CNP valid."); // show a positive result
                    cnpGlow.setColor(Color.GREEN); // make the cnp field glow green
                } else {
                    resultLabel.setText("CNP invalid."); // otherwise, show a negative result
                    cnpGlow.setColor(Color.RED); // make the cnp field glow red
                }
            }
        });
    }
    
    /**
     * Checks if some string contains only digits.
     * @param string the string to be checked
     * @return true if the string is composed only of digits, false otherwise
     */
    private boolean containsOnlyDigits(String string) {
        // extract the characters from the string to a char array
        char[] characters = string.toCharArray();
        
        // the Character.isDigit() method accepts non-latin digits, so checking is done through comparisons
        for (char c : characters) 
            if ((c < '0') || (c > '9'))
                return false;
        
        return true;
    }
    
    /**
     * Validates the date in the CNP.
     * @param cnpStart the first part of the CNP containing the sex and the date of birth
     * @param allowFutureDates indicates whether dates in the future are valid or not
     * @return true if the date is valid, false otherwise
     */
    private boolean dateIsValid(String cnpStart, boolean allowFutureDates) {
        int[] digits = convertStringToDigits(cnpStart); // get the first part of the cnp as an int array
        int sexDigit = digits[0]; // get the digit representing the sex
        int year = digits[1] * 10 + digits[2]; // get the last two digits of the year
        
        // calculate the full year based on the sex digit and the last two digits of the year
        switch (sexDigit) {
            case 1:
            case 2:
                year += 1900;
                
                break;
            case 3:
            case 4:
                year += 1800;
                
                break;
            case 5:
            case 6:
                year += 2000;
                
                break;
            case 7:
            case 8:
            case 9:
                if (year == 0)
                    year = 2000;
                else
                    year += 1900;
                
                break;
            default:
                return false;
        }
        
        int month = digits[3] * 10 + digits[4]; // get the month
        int day = digits[5] * 10 + digits[6]; // get the day
        
        // check if the date is valid based on the month, day and year
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                if (day > 30)
                    return false;
                
                break;
            case 2:
                int lastDayOfFebruary = Year.of(year).isLeap() ? 29 : 28;
                
                if (day > lastDayOfFebruary)
                    return false;
                
                break;
            default:
                return false;
        }
        
        // if future dates are not allowed, verify that the date in the CNP is not in the future
        if (!allowFutureDates) {
            LocalDate currentDate = LocalDate.now();
            LocalDate cnpDate = LocalDate.of(year, month, day);
            
            if (cnpDate.isAfter(currentDate))
                return false;
        }
        
        return true;
    }
    
    /**
     * Performs the final verification of the CNP.
     * See the following page for details: https://ro.wikipedia.org/wiki/Cod_numeric_personal
     * @param cnp the CNP entered by the user
     * @return true if the algorithm yields a valid result, false otherwise
     */
    private boolean algorithmIsValid(String cnp) {
        int[] digits = convertStringToDigits(cnp); // get the cnp as an int array
        int maxDigits = CNP_LENGTH - 1; // max number of digits used in the algorithm
        int lastDigit = digits[maxDigits]; // last digit in the CNP (control digit)
        int sum = 0; // initialize the sum
        
        // perform the necessary multiplications and additions
        for (int i = 0; i < maxDigits; i++)
            sum += digits[i] * CONTROL_ARRAY[i]; 
        
        // identify the actual control digit based on the remainder
        int remainder = sum % 11;
        int controlDigit = remainder;
        if (remainder == 10)
            controlDigit = 1;
        
        // check if the last digit in the CNP is the actual control digit
        return lastDigit == controlDigit;
    }
    
    /**
     * Converts a string containing only digits into an array of ints.
     * @param string the string to convert
     * @return an int array containing the digits of the string 
     */
    private int[] convertStringToDigits(String string) {
        // extract the digits from the string to a char array
        char[] digitChars = string.toCharArray();
        // transform the digit chars into ints and store them in an int array
        int[] digits = new int[digitChars.length];
        for (int i = 0; i < digits.length; i++)
            digits[i] = Character.getNumericValue(digitChars[i]);
        
        return digits;
    }
}