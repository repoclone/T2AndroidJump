package com.janrain.android.utils;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.util.Log;

import com.janrain.android.engage.session.JRSession;
import com.janrain.android.utils.LogUtils;

public class T2CookieStore {

	private static final String TAG = T2CookieStore.class.getSimpleName();
    private static T2CookieStore sInstance;
    private static Cookie mSessionCookie;
    
    
    public static T2CookieStore getInstance() {
    	
        if (sInstance != null) {
        	return sInstance;
        }
        else {
        	sInstance = new T2CookieStore();
        	return sInstance;
        	
        }
	}
	
	public T2CookieStore() {

	}    
    
    public static Cookie getSessionCookie() {
        return mSessionCookie;
	}
    
    public static void setSessionCookie(Cookie cookie) {
    	mSessionCookie = cookie;
    }
    

}
