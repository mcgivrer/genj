package ${PACKAGE};

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Main class of the application.
 * 
 * @since ${PROJECT_YEAR}
 * @author ${AUTHOR_NAME}<${AUTHOR_EMAIL}>
 * @version ${PROJECT_VERSION}
 */
public class ${MAINCLASS}{

    ResourceBundle messages = ResourceBundle.getBundle("i18n/messages", Locale.getDefault());

    /**
     * Constructor for ${MAINCLASS}
     */
    public ${MAINCLASS}()
    {
        System.out.println("Starting ${MAINCLASS}...");
    }

    /**
     * 
     * @param args
     */
    public void run(String[] args) {
        System.out.println(messages.getString("app.main.welcome"));
    }

    /**
     * Main entry point of the application
     * @param args
     */
    public static void main(String[] args) {
        ${MAINCLASS} app = new ${MAINCLASS}();
        app.run(args);
    }
}
