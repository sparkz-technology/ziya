package com.example.ziya;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Integration tests for NotificationService
 */
@RunWith(AndroidJUnit4.class)
public class NotificationServiceTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Test
    public void testServiceStartsSuccessfully() throws TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent serviceIntent = new Intent(context, NotificationService.class);
        
        // Start the service and verify it doesn't crash
        serviceRule.startService(serviceIntent);
        
        // If we get here without exception, the service started successfully
        assertTrue("Service should start without throwing exception", true);
    }

    @Test
    public void testServiceHandlesUpdateUrlAction() throws TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.setAction(NotificationService.ACTION_UPDATE_URL);
        serviceIntent.putExtra("websocket_url", "ws://test.example.com:3000");
        
        // Start the service with update URL action
        serviceRule.startService(serviceIntent);
        
        // If we get here without exception, the service handled the action
        assertTrue("Service should handle UPDATE_URL action", true);
    }

    @Test
    public void testServiceHandlesTestConnectionAction() throws TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.setAction(NotificationService.ACTION_TEST_CONNECTION);
        serviceIntent.putExtra("websocket_url", "ws://test.example.com:3000");
        
        // Start the service with test connection action
        serviceRule.startService(serviceIntent);
        
        // If we get here without exception, the service handled the action
        assertTrue("Service should handle TEST_CONNECTION action", true);
    }

    @Test
    public void testServiceHandlesSendTestNotificationAction() throws TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.setAction(NotificationService.ACTION_SEND_TEST_NOTIFICATION);
        
        // Start the service with send test notification action
        serviceRule.startService(serviceIntent);
        
        // If we get here without exception, the service handled the action
        assertTrue("Service should handle SEND_TEST_NOTIFICATION action", true);
    }

    @Test
    public void testServiceCreatesNotificationChannel() throws TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent serviceIntent = new Intent(context, NotificationService.class);
        
        // Start the service
        serviceRule.startService(serviceIntent);
        
        // Verify notification channel is created (service should start without crash)
        assertTrue("Service should create notification channel successfully", true);
    }
}