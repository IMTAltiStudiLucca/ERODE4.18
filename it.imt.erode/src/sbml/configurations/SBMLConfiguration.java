package sbml.configurations;

public class SBMLConfiguration {

    public static final int LEVEL = 3;
    public static final int VERSION = 1;
    public static final String DEFAULT_COMPARTMENT = "default";

    private int level;
    private int version;
    private String defaultCompartment;

    private static final SBMLConfiguration CONFIG = new SBMLConfiguration();

    public static SBMLConfiguration getConfiguration() {
        return CONFIG;
    }

    public SBMLConfiguration() {
        this.level = LEVEL;
        this.version = VERSION;
        this.defaultCompartment = DEFAULT_COMPARTMENT;
    }

    public int getLevel() {
        return this.level;
    }

    public int getVersion() {
        return this.version;
    }

    public String getDefaultCompartment() {
        return this.defaultCompartment;
    }
}
