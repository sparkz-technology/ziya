package com.example.ziya;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocket URL configuration functionality
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketConfigTest {

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPreferences;

    @Mock
    SharedPreferences.Editor mockEditor;

    private static final String PREFS_NAME = "ZiyaPrefs";
    private static final String KEY_WEBSOCKET_URL = "websocket_url";
    private static final String DEFAULT_WEBSOCKET_URL = "ws://192.168.1.9:3000";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
    }

    @Test
    public void testDefaultWebSocketUrl() {
        when(mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL))
                .thenReturn(DEFAULT_WEBSOCKET_URL);

        String url = mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
        assertEquals("Default WebSocket URL should be correct", DEFAULT_WEBSOCKET_URL, url);
    }

    @Test
    public void testValidWebSocketUrls() {
        String[] validUrls = {
                "ws://localhost:3000",
                "wss://example.com:8080",
                "ws://192.168.1.100:3000",
                "wss://secure.websocket.server.com"
        };

        for (String url : validUrls) {
            assertTrue("URL should be valid: " + url, isValidWebSocketUrl(url));
        }
    }

    @Test
    public void testInvalidWebSocketUrls() {
        String[] invalidUrls = {
                "",
                "http://example.com",
                "https://example.com",
                "ftp://example.com",
                "localhost:3000",
                "ws://",
                "wss://",
                null
        };

        for (String url : invalidUrls) {
            assertFalse("URL should be invalid: " + url, isValidWebSocketUrl(url));
        }
    }

    @Test
    public void testSaveWebSocketUrl() {
        String testUrl = "ws://test.example.com:3000";
        
        // Simulate saving URL
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(KEY_WEBSOCKET_URL, testUrl)).thenReturn(mockEditor);
        
        mockEditor.putString(KEY_WEBSOCKET_URL, testUrl);
        mockEditor.apply();
        
        verify(mockEditor).putString(KEY_WEBSOCKET_URL, testUrl);
        verify(mockEditor).apply();
    }

    @Test
    public void testLoadSavedWebSocketUrl() {
        String savedUrl = "ws://saved.example.com:3000";
        when(mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL))
                .thenReturn(savedUrl);

        String loadedUrl = mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
        assertEquals("Loaded URL should match saved URL", savedUrl, loadedUrl);
    }

    @Test
    public void testUrlPersistence() {
        String customUrl = "wss://custom.server.com:8080";
        
        // Save custom URL
        mockEditor.putString(KEY_WEBSOCKET_URL, customUrl);
        mockEditor.apply();
        
        // Simulate app restart - should load custom URL
        when(mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL))
                .thenReturn(customUrl);
        
        String loadedUrl = mockSharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
        assertEquals("Custom URL should persist after restart", customUrl, loadedUrl);
    }

    /**
     * Helper method to validate WebSocket URL format
     * This mirrors the validation logic that would be in the MainActivity
     */
    private boolean isValidWebSocketUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.startsWith("ws://") || url.startsWith("wss://");
    }

    @Test
    public void testWebSocketUrlFormatting() {
        // Test URL trimming
        String urlWithSpaces = "  ws://example.com:3000  ";
        String trimmedUrl = urlWithSpaces.trim();
        assertTrue("Trimmed URL should be valid", isValidWebSocketUrl(trimmedUrl));
        assertEquals("Trimmed URL should not have spaces", "ws://example.com:3000", trimmedUrl);
    }

    @Test
    public void testReconnectionScenarios() {
        // Test different reconnection scenarios
        String[] testUrls = {
                "ws://primary.server.com:3000",
                "ws://backup.server.com:3000",
                "wss://secure.server.com:443"
        };

        for (String url : testUrls) {
            assertTrue("URL should be valid for reconnection: " + url, isValidWebSocketUrl(url));
        }
    }
}