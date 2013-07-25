package com.dtolabs.rundeck.jetty.jaas;

import org.mortbay.jetty.plus.jaas.callback.ObjectCallback;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;

/**
 * A property file login module, that supports the PAM style of shared authentication to
 * enable chained authentication/authorization.
 */
public class AuthorizingPropertyFileLoginModule extends PropertyFileLoginModule {
    public static final String SHARED_LOGIN_NAME = "javax.security.auth.login.name";
    public static final String SHARED_LOGIN_PASSWORD= "javax.security.auth.login.password";
    private boolean useFirstPass;
    private boolean tryFirstPass;
    private boolean storePass;
    private boolean clearPass;
    private boolean authorizeOnly;
    private boolean debug;
    private String sharedUserName;
    private Object sharedUserPass;
    private boolean hasSharedAuth;
    private Map sharedState;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.sharedState=sharedState;
        if (options.get("useFirstPass") != null) {
            useFirstPass = Boolean.parseBoolean(options.get("useFirstPass").toString());
        }
        if (options.get("tryFirstPass") != null) {
            tryFirstPass = Boolean.parseBoolean(options.get("tryFirstPass").toString());
        }
        if (options.get("storePass") != null) {
            storePass = Boolean.parseBoolean(options.get("storePass").toString());
        }
        if (options.get("clearPass") != null) {
            clearPass = Boolean.parseBoolean(options.get("clearPass").toString());
        }
        if (options.get("authorizeOnly") != null) {
            authorizeOnly = Boolean.parseBoolean(options.get("authorizeOnly").toString());
        }
        if (options.get("debug") != null) {
            debug = Boolean.parseBoolean(options.get("debug").toString());
        }
        if(useFirstPass||tryFirstPass) {
            if (sharedState.get(SHARED_LOGIN_NAME) != null) {
                sharedUserName = sharedState.get(SHARED_LOGIN_NAME).toString();
            }
            if(authorizeOnly) {
                sharedUserPass = "-";
            }else if (sharedState.get(SHARED_LOGIN_PASSWORD) != null) {
                sharedUserPass = sharedState.get(SHARED_LOGIN_PASSWORD);
            }
            hasSharedAuth = null != sharedUserName && null != sharedUserPass;
        }
        System.out.println(this);
    }

    @Override
    public String toString() {
        return "AuthorizingPropertyFileLoginModule{" +
                "useFirstPass=" + useFirstPass +
                ", tryFirstPass=" + tryFirstPass +
                ", storePass=" + storePass +
                ", clearPass=" + clearPass +
                ", debug=" + debug +
                ", sharedUserName='" + sharedUserName + '\'' +
                ", sharedUserPass=" + sharedUserPass +
                ", hasSharedAuth=" + hasSharedAuth +
                ", sharedState=" + sharedState +
                ", authorizeOnly=" + authorizeOnly +
                '}';
    }

    @Override
    public boolean login() throws LoginException {
        //if there is a shared username/password, attempt to use it
        if((useFirstPass||tryFirstPass) && hasSharedAuth){
            if(debug) {
                System.out.println(String.format("AuthorizingPropertyFileLoginModule: login with shared auth, " +
                        "try? %s, use? %s", tryFirstPass, useFirstPass));
            }
            UserInfo userInfo = null;
            try {
                userInfo = getUserInfo(sharedUserName);
            } catch (Exception e) {
                log.error(e);
                if(useFirstPass){
                    throw new LoginException(e.toString());
                }
            }

            if (userInfo == null && useFirstPass) {
                setAuthenticated(false);
            }else{
                JAASUserInfo jaasUserInfo = new JAASUserInfo(userInfo);
                boolean authState = jaasUserInfo.checkCredential(sharedUserPass);
                if(authState){
                    this.currentUser = jaasUserInfo;
                    setAuthenticated(authState);
                }
            }
        }

        if(useFirstPass && hasSharedAuth){
            //finish with shared password auth attempt
            if (debug) {
                System.out.println(String.format("AuthorizingPropertyFileLoginModule: using login result: %s",
                        isAuthenticated()));
            }
            return isAuthenticated();
        }
        if (hasSharedAuth && debug) {
            System.out.println(String.format("AuthorizingPropertyFileLoginModule: shared auth failed, " +
                    "now trying callback auth."));
        }
        //try using callback authentication

        return callbackLogin();
    }

    private boolean callbackLogin() throws LoginException {
        log.debug("calling login");
        try {
            if (callbackHandler == null) {
                throw new LoginException("No callback handler");
            }

            Callback[] callbacks = configureCallbacks();
            callbackHandler.handle(callbacks);

            String webUserName = ((NameCallback) callbacks[0]).getName();
            Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

            if ((webUserName == null) || (webCredential == null)) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            UserInfo userInfo = getUserInfo(webUserName);

            if (userInfo == null) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            currentUser = new JAASUserInfo(userInfo);
            setAuthenticated(currentUser.checkCredential(webCredential));

            //store shared credentials if successful and not already stored
            if(isAuthenticated() && storePass && !hasSharedAuth) {
                sharedState.put(SHARED_LOGIN_NAME, webUserName);
                sharedState.put(SHARED_LOGIN_PASSWORD, webCredential);
            }

            return isAuthenticated();
        } catch (IOException e) {
            log.error(e);
            throw new LoginException(e.toString());
        } catch (UnsupportedCallbackException e) {
            log.error(e);
            throw new LoginException(e.toString());
        } catch (Exception e) {
            log.error(e);
            throw new LoginException(e.toString());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        boolean commit = super.commit();
        if(clearPass && hasSharedAuth) {
            sharedState.remove(SHARED_LOGIN_NAME);
            sharedState.remove(SHARED_LOGIN_PASSWORD);
        }
        return commit;
    }
}
