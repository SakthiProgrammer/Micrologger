
import com.sakthivel.micrologger.Log;
import com.sakthivel.micrologger.LogConfig;

public class Main {
    public static void main(String[] args) {

        LogConfig config = new LogConfig()
                .setPrintToConsole(true)
                .setLogDirectory("log")
                .setRotateDaily(true)
                .setColorEnabled(true);

        Log.init(config);
        Log logger = Log.getInstance();
        logger.setUid("Hello");
        logger.statement("Main(+)");
        logger.error("Occured");
        logger.setReference("Sakthivel is Logging");
        System.out.println("Hello World");
        logger.info("Programm Started...");
        logger.details("Programm Endded");
        logger.statement("Main(-)");
        System.out.println("Hello World");
        caller(logger);

    }

    public static void caller(Log logger) {
        logger.error("error in caller method");
        logger.debug("Debugging..");
        logger.setReference("Ram is Logging");
        logger.setUid("Helo World");
        System.out.println("Hello World");
        logger = Log.getInstance();
        logger.removeReference();
        logger.error("222.error in caller method");
        System.out.println("Hello World");
        logger.debug("3333.Debugging..");
    }

}