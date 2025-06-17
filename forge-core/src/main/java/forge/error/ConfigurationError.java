package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when configuration settings are invalid or missing.
 * This includes network settings, game settings, or other configuration issues.
 */
public class ConfigurationError extends ApplicationError {
    
    private static final long serialVersionUID = 1L;
    
    private final String configurationKey;
    private final String configurationValue;
    private final String expectedFormat;
    private final String configurationFile;
    
    /**
     * Creates a new ConfigurationError.
     * 
     * @param configurationKey Configuration key that is invalid
     * @param message Error description
     */
    public ConfigurationError(String configurationKey, String message) {
        this(configurationKey, null, null, null, message, null);
    }
    
    /**
     * Creates a new ConfigurationError with value information.
     * 
     * @param configurationKey Configuration key that is invalid
     * @param configurationValue Current value (may be invalid)
     * @param expectedFormat Expected format description
     * @param message Error description
     */
    public ConfigurationError(String configurationKey, String configurationValue, 
                             String expectedFormat, String message) {
        this(configurationKey, configurationValue, expectedFormat, null, message, null);
    }
    
    /**
     * Creates a new ConfigurationError with full details.
     * 
     * @param configurationKey Configuration key that is invalid
     * @param configurationValue Current value (may be invalid)
     * @param expectedFormat Expected format description
     * @param configurationFile Configuration file containing the error
     * @param message Error description
     * @param cause Underlying cause
     */
    public ConfigurationError(String configurationKey, String configurationValue, 
                             String expectedFormat, String configurationFile, 
                             String message, Throwable cause) {
        super(String.format("Configuration error for '%s': %s", configurationKey, message),
              true, cause, createContext(configurationKey, configurationValue, expectedFormat, configurationFile));
        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
        this.expectedFormat = expectedFormat;
        this.configurationFile = configurationFile;
    }
    
    private static Map<String, Object> createContext(String key, String value, String format, String file) {
        Map<String, Object> context = new HashMap<>();
        context.put("configurationKey", key);
        if (value != null) {
            context.put("configurationValue", value);
        }
        if (format != null) {
            context.put("expectedFormat", format);
        }
        if (file != null) {
            context.put("configurationFile", file);
        }
        return context;
    }
    
    /**
     * Gets the configuration key that has an error.
     * 
     * @return Configuration key
     */
    public String getConfigurationKey() {
        return configurationKey;
    }
    
    /**
     * Gets the current configuration value.
     * 
     * @return Configuration value or null if not available
     */
    public String getConfigurationValue() {
        return configurationValue;
    }
    
    /**
     * Gets the expected format description.
     * 
     * @return Expected format or null if not available
     */
    public String getExpectedFormat() {
        return expectedFormat;
    }
    
    /**
     * Gets the configuration file containing the error.
     * 
     * @return Configuration file or null if not available
     */
    public String getConfigurationFile() {
        return configurationFile;
    }
    
    @Override
    public String getUserMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration error: ");
        
        if (configurationFile != null) {
            sb.append("Please check the configuration file '").append(configurationFile).append("'. ");
        }
        
        sb.append("The setting '").append(configurationKey).append("'");
        
        if (configurationValue != null) {
            sb.append(" has value '").append(configurationValue).append("' which");
        }
        
        sb.append(" is invalid");
        
        if (expectedFormat != null) {
            sb.append(". Expected format: ").append(expectedFormat);
        }
        
        sb.append(".");
        
        return sb.toString();
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.USER_INTERVENTION; // User must fix configuration
    }
}