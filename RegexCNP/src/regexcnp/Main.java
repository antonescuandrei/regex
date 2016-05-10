package regexcnp;

import javafx.application.Application;

/**
 * This application will check if a user submitted CNP (Cod Numeric Personal -
 * Personal Numerical Code) is valid. The algorithm used for validation was
 * adapted from the instructions presented on the following page:
 * https://ro.wikipedia.org/wiki/Cod_numeric_personal
 */
class Main {
    /**
     * Starts program execution.
     * @param args command-line parameters (not used)
     */
    public static void main(String[] args) {
        // launch the JavaFX application
        Application.launch(CnpApp.class, args);
    }
}