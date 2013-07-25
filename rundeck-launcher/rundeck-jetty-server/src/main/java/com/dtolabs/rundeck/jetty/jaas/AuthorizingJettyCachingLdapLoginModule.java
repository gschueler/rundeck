package com.dtolabs.rundeck.jetty.jaas;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.util.Map;

/**
 * Extends JettyCachingLdapLoginModule to enable PAM style shared login information for JAAS LoginModule chaining
 */
public class AuthorizingJettyCachingLdapLoginModule extends JettyCachingLdapLoginModule {
    public static final String SHARED_LOGIN_NAME = "javax.security.auth.login.name";
    public static final String SHARED_LOGIN_PASSWORD = "javax.security.auth.login.password";

    protected boolean storePass;
    protected boolean useFirstPass;
    protected boolean tryFirstPass;
    protected boolean clearPass;
    private String sharedUserName;
    private Object sharedUserPass;
    private boolean hasSharedAuth;
    private Map sharedState;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.sharedState = sharedState;
        useFirstPass = Boolean.parseBoolean(String.valueOf(getOption(options, "useFirstPass", Boolean
                .toString(useFirstPass))));
        tryFirstPass = Boolean.parseBoolean(String.valueOf(getOption(options, "tryFirstPass", Boolean
                .toString(tryFirstPass))));
        storePass = Boolean.parseBoolean(String.valueOf(getOption(options, "storePass", Boolean
                .toString(storePass))));
        clearPass = Boolean.parseBoolean(String.valueOf(getOption(options, "clearPass", Boolean
                .toString(clearPass))));

        if (useFirstPass || tryFirstPass) {
            if (sharedState.get(SHARED_LOGIN_NAME) != null) {
                sharedUserName = sharedState.get(SHARED_LOGIN_NAME).toString();
            }
            if (sharedState.get(SHARED_LOGIN_PASSWORD) != null) {
                sharedUserPass = sharedState.get(SHARED_LOGIN_PASSWORD);
            }
            hasSharedAuth = null != sharedUserName && null != sharedUserPass;
        }

    }

    @Override
    public boolean commit() throws LoginException {
        boolean commit = super.commit();
        if (clearPass && hasSharedAuth) {
            sharedState.remove(SHARED_LOGIN_NAME);
            sharedState.remove(SHARED_LOGIN_PASSWORD);
        }
        return commit;
    }

    @Override
    protected boolean performLogin(String webUserName, Object webCredential) throws Exception {
        boolean auth = super.performLogin(webUserName, webCredential);
        if (auth && storePass && !hasSharedAuth) {
            sharedState.put(SHARED_LOGIN_NAME, webUserName);
            sharedState.put(SHARED_LOGIN_PASSWORD, webCredential);
        }
        return auth;
    }

    @Override
    public boolean login() throws LoginException {
        if ((useFirstPass || tryFirstPass) && hasSharedAuth) {
            boolean auth = false;
            try {
                auth = performLogin(sharedUserName, sharedUserPass);
            } catch (Exception e) {
                if (_debug) {
                    e.printStackTrace();
                }
                if (useFirstPass) {
                    throw new LoginException("Error obtaining user info.");
                }
            }
            if (useFirstPass || auth) {
                setAuthenticated(auth);
                return isAuthenticated();
            }
        }
        return super.login();
    }
}
