package gov.nih.nci.hpc.cli.util;

import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class CustomLowerCamelCase extends PropertyNamingStrategies.NamingBase {
    private static final Pattern REGEX = Pattern.compile("[A-Z]");

    @Override
    public String translate(String input) {
        if (input == null)
            return input; // garbage in, garbage out

        if (!input.isEmpty() && Character.isUpperCase(input.charAt(0)))
            input = input.substring(0, 1).toLowerCase() + input.substring(1);

        return input;
    }
}
