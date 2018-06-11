package es.gob.afirma.android.signfolder.proxy;

/**
 * Operaciones asociadas a los
 */
public enum ProxyServiceOperations {
    PRESIGN("0"),
    POSTSIGN("1"),
    REQUEST_LIST("2"),
    REJECT("3"),
    DETAIL("4"),
    PREVIEW_DOCUMENT("5"),
    APP_LIST("6"),
    APPROVE("7"),
    PREVIEW_SIGN("8"),
    PREVIEW_REPORT("9"),
    LOGIN_REQUEST("10"),
    LOGIN_VALIDATION("11"),
    LOGOUT("12"),
    SIGN_ON_NOTIFICATIONS("13"),
    CHANGE_NOTIFICATIONS_STATE("14"),
    CHECK_NOTIFICATIONS_STATE("15");
//	CLAVE_LOGIN_REQUEST
//  CLAVE_LOGIN_VALIDATION
//  PRESIGN_CLAVE_FIRMA
//  POSTSIGN_CLAVE_FIRMA

    private String code;

    ProxyServiceOperations(String code) {
        this.code = code;
    }

    String getCode() {
        return this.code;
    }
}
