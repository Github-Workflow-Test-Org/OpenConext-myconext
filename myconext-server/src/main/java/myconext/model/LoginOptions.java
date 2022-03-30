package myconext.model;

public enum LoginOptions {

    APP("useApp"), FIDO("useWebAuthn"), MAGIC("useLink"), PASSWORD("usePassword");

    private final String value;

    LoginOptions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
