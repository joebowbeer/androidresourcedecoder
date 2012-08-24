package com.joebowbeer.resourcedecoder;

public class ResourceConfig {

    private static final int CONFIG_MCC = 0x0001;
    private static final int CONFIG_MNC = 0x0002;
    private static final int CONFIG_LOCALE = 0x0004;
    private static final int CONFIG_TOUCHSCREEN = 0x0008;
    private static final int CONFIG_KEYBOARD = 0x0010;
    private static final int CONFIG_KEYBOARD_HIDDEN = 0x0020;
    private static final int CONFIG_NAVIGATION = 0x0040;
    private static final int CONFIG_ORIENTATION = 0x0080;
    private static final int CONFIG_DENSITY = 0x0100;
    private static final int CONFIG_SCREEN_SIZE = 0x0200;
    private static final int CONFIG_VERSION = 0x0400;
    private static final int CONFIG_SCREEN_LAYOUT = 0x0800;
    private static final int CONFIG_UI_MODE = 0x1000;

    // Additional flag indicating an entry is public.
    private static final int SPEC_PUBLIC = 0x40000000;

    // imsi
    public final int mcc;
    public final int mnc;

    // locale
    public final String language;
    public final String country;

    // screen type
    public final int orientation;
    public final int touchscreen;
    public final int density;

    // input
    public final int keyboard;
    public final int navigation;
    public final int inputFlags;

    // screen size
    public final int screenWidth;
    public final int screenHeight;

    // version
    public final int sdkVersion;
    public final int minorVersion;

    // screen config
    public final int screenLayout;
    public final int uiMode;

    public ResourceConfig(int mcc, int mnc, String language, String country,
            int orientation, int touchscreen, int density,
            int keyboard, int navigation, int inputFlags,
            int screenWidth, int screenHeight,
            int sdkVersion, int minorVersion, int screenLayout, int uiMode) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.language = language;
        this.country = country;
        this.orientation = orientation;
        this.touchscreen = touchscreen;
        this.density = density;
        this.keyboard = keyboard;
        this.navigation = navigation;
        this.inputFlags = inputFlags;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.sdkVersion = sdkVersion;
        this.minorVersion = minorVersion;
        this.screenLayout = screenLayout;
        this.uiMode = uiMode;
    }

    public boolean isDefault() {
        if (!language.isEmpty() || !country.isEmpty()) {
            return false;
        }
        return (mcc | mnc | orientation | touchscreen | density
                | keyboard | navigation | inputFlags
                | screenWidth | screenHeight
                | sdkVersion | screenLayout | uiMode) == 0;
    }

    public String densityQualifier() {
        // TODO: tvdpi 
        switch (density) {
            case 0xffff:
                return "nodpi";
            case 120:
                return "ldpi";
            case 160:
                return "mdpi";
            case 240:
                return "hdpi";
            case 320:
                return "xhdpi";
            default:
                return String.valueOf(density);
        }
    }

    public String orientationQualifier() {
        switch (orientation) {
            case 1:
                return "port";
            case 2:
                return "land";
            default:
                return String.valueOf(orientation);
        }
    }
}
