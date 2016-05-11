package regexcnp;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
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
    // numbers used in the last part of the verification algorithm; for details see: https://ro.wikipedia.org/wiki/Cod_numeric_personal
    private final int[] CONTROL_ARRAY = {2, 7, 9, 1, 4, 6, 3, 5, 8, 2, 7, 9};
    // flag whether future dates in CNP should be allowed
    private final boolean ALLOW_FUTURE_DATES = true;
    
    /**
     * Creates all the controls of the application.
     * @param primaryStage the primary stage of the application
     */
    @Override
    public void start(Stage primaryStage) {
        // compile the pattern used for validation
        Pattern cnpPattern = Pattern.compile(CNP_REGEX);
        
        // a label with instructions for the user
        Label instructionsLabel = new Label("Introduceti CNP in campul de mai jos:");
        
        // a text field where the CNP is entered
        TextField cnpField = new TextField();
        cnpField.setAlignment(Pos.CENTER);
        
        // a button used for starting the validation algorithm
        Button validationButton = new Button("Valideaza CNP");
        validationButton.setDisable(true); // make the button initially disabled
        
        // a label that will show the result of the validation algorithm
        Label resultLabel = new Label();
        
        // a vertical box layout for the controls
        VBox layoutBox = new VBox(20);
        layoutBox.setAlignment(Pos.CENTER);
        layoutBox.setPadding(new Insets(20));
        layoutBox.getChildren().addAll(instructionsLabel, cnpField, validationButton, resultLabel);
        
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
        
        // the text formatter ensures that only digits can be entered in the text field
        cnpField.setTextFormatter(new TextFormatter<>(new UnaryOperator<Change>() {
            @Override
            public Change apply(Change change) {
                String newText = change.getControlNewText();
                
                // digits are allowed
                if (newText.matches("\\d{0,13}")) {
                    // if 13 digits have been entered, enable the validation button
                    if (newText.length() == 13)
                        validationButton.setDisable(false);
                    else
                        validationButton.setDisable(true); // otherwise, disable the button
                    
                    return change;
                }
                
                // reject any characters other than digits
                return null;
            }
        }));
        
        // the listener of the text property empties the result label whenever the contents of the cnp field changes
        cnpField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldString, String newString) {
                resultLabel.setText(null);
            }
        });
        
        // handle events created when the validation button is fired
        validationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ae) {
                // get the text from the field
                String text = cnpField.getText();
                
                // create a matcher from the pattern and the text
                Matcher matcher = cnpPattern.matcher(text);
                
                // if the CNP matches the pattern and the date is valid and the algorithm is valid
                if (matcher.matches() && dateIsValid(text.substring(0, 7)) && algorithmIsValid(text))
                    resultLabel.setText("CNP valid."); // show a positive result
                else
                    resultLabel.setText("CNP invalid."); // otherwise, show a negative result
            }
        });
    }
    
    /**
     * Validates the date in the CNP.
     * @param cnpStart the first part of the CNP containing the sex and the date of birth
     * @return true if the date is valid, false otherwise
     */
    private boolean dateIsValid(String cnpStart) {
        // extract the digits from the string to a char array
        char[] digitChars = cnpStart.toCharArray();
        // transform the digit chars into ints and store them in an int array
        int[] digits = new int[7];
        for (int i = 0; i < 7; i++)
            digits[i] = Character.getNumericValue(digitChars[i]);
        
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
                int maxDay;
                
                if ((year % 4 == 0) || ((year % 100 == 0) && (year % 400 == 0)))
                    maxDay = 29;
                else
                    maxDay = 28;
                
                if (day > maxDay)
                    return false;
                
                break;
            default:
                return false;
        }
        
        // if future dates are not allowed, check that the date in the CNP is not in the future
        if (!ALLOW_FUTURE_DATES) {
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
        // extract the digits from the string to a char array
        char[] digitChars = cnp.toCharArray();
        // transform the digits into ints and store them in an int array
        int[] digits = new int[CNP_LENGTH];
        for (int i = 0; i < CNP_LENGTH; i++)
            digits[i] = Character.getNumericValue(digitChars[i]);
        
        int maxDigits = CNP_LENGTH - 1; // maximum number of digits
        int lastDigit = digits[maxDigits]; // last digit in the CNP (control digit)
        
        int sum = 0; // initialie the sum
        // perform the necessary multiplications and additions
        for (int i = 0; i < maxDigits; i++)
            sum += digits[i] * CONTROL_ARRAY[i]; 
        
        // identify the required control digit based on the remainder
        int remainder = sum % 11;
        int controlDigit = remainder;
        if (remainder == 10)
            controlDigit = 1;
        
        // check if the last digit in the CNP is the actual control digit
        return lastDigit == controlDigit;
    }
}