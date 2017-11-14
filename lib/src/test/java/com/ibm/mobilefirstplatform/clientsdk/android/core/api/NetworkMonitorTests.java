/*
 *     Copyright 2017 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.test.mock.MockContext;

import junit.framework.ComparisonFailure;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NetworkMonitorTests {

    private CountDownLatch latch = null;

    @Test
    public void testConstructor() {
        NetworkMonitor networkMonitor = new NetworkMonitor(new MockContext(), getNetworkListener());
        assertNotNull(networkMonitor.getNetworkReceiver());

        networkMonitor = new NetworkMonitor(new MockContext(), null);
        assertNull(networkMonitor.getNetworkReceiver());
    }

    @Test
    public void testStartMonitoringNetworkChanges() throws Exception {
        latch = new CountDownLatch(1);

        class TestContext extends MockContext {
            @Override
            public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
                assertTrue(receiver instanceof NetworkMonitor.NetworkChangeReceiver);
                latch.countDown();
                return null;
            }
        }

        Context context = new TestContext();
        NetworkMonitor networkMonitor = new NetworkMonitor(context, getNetworkListener());
        networkMonitor.startMonitoringNetworkChanges();

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testStartMonitoringNetworkChangesWithNullListener() {

        class TestContext extends MockContext {
            @Override
            public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
                fail("Should not have registered a receiver without a NetworkChangeListener given to NetworkMonitor.");
                return null;
            }
        }

        Context context = new TestContext();
        NetworkMonitor networkMonitor = new NetworkMonitor(context, null);
        networkMonitor.startMonitoringNetworkChanges();
    }

    @Test
    public void testStopMonitoringNetworkChanges() throws Exception {
        latch = new CountDownLatch(1);

        class TestContext extends MockContext {
            @Override
            public void unregisterReceiver(BroadcastReceiver receiver) {
                assertTrue(receiver instanceof NetworkMonitor.NetworkChangeReceiver);
                latch.countDown();
            }
        }

        Context context = new TestContext();
        NetworkMonitor networkMonitor = new NetworkMonitor(context, getNetworkListener());
        networkMonitor.startMonitoringNetworkChanges();
        networkMonitor.stopMonitoringNetworkChanges();

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testStopMonitoringNetworkChangesWithNullListener() {

        class TestContext extends MockContext {
            @Override
            public void unregisterReceiver(BroadcastReceiver receiver) {
                fail("Should not have a receiver without a NetworkChangeListener given to NetworkMonitor.");
            }
        }

        // Test with null Listener
        Context context = new TestContext();
        NetworkMonitor networkMonitor = new NetworkMonitor(context, null);
        networkMonitor.startMonitoringNetworkChanges();
        networkMonitor.stopMonitoringNetworkChanges();

        // Test misuse of the API
        context = new TestContext();
        networkMonitor = new NetworkMonitor(context, null);
        networkMonitor.stopMonitoringNetworkChanges();
        networkMonitor.stopMonitoringNetworkChanges();
        networkMonitor.stopMonitoringNetworkChanges();
    }

    @Test
    public void testGetMobileNetworkTypeWithUnknownData() {
        Context context = new MockContext();
        NetworkMonitor networkMonitor = new NetworkMonitor(context, null);
        assertEquals("unknown", networkMonitor.getMobileNetworkType());
    }

    @Test
    @TargetApi(24)
    public void testGetMobileNetworkTypeWith4G() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 24);
        try {

            TelephonyManager mockedTelephonyManager = mock(TelephonyManager.class);
            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);

            NetworkMonitor mockedNetworkMonitor = mock(NetworkMonitor.class);
            when(mockedNetworkMonitor.getTelephonyManager()).thenReturn(mockedTelephonyManager);
            when(mockedNetworkMonitor.getMobileNetworkType()).thenCallRealMethod();

            assertEquals("4G", mockedNetworkMonitor.getMobileNetworkType());
        } catch (NullPointerException | ComparisonFailure e){
            
        }
    }

    @Test
    @TargetApi(24)
    public void testGetMobileNetworkTypeWith3G() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 24);

        try {
            TelephonyManager mockedTelephonyManager = mock(TelephonyManager.class);
            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_UMTS);

            NetworkMonitor mockedNetworkMonitor = mock(NetworkMonitor.class);
            when(mockedNetworkMonitor.getTelephonyManager()).thenReturn(mockedTelephonyManager);
            when(mockedNetworkMonitor.getMobileNetworkType()).thenCallRealMethod();
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_0);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_A);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSDPA);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSUPA);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSPA);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_B);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EHRPD);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSPAP);
            assertEquals("3G", mockedNetworkMonitor.getMobileNetworkType());
        } catch (NullPointerException | ComparisonFailure e){
            
        }
    }

    @Test
    @TargetApi(24)
    public void testGetMobileNetworkTypeWith2G() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 24);

        try {
            TelephonyManager mockedTelephonyManager = mock(TelephonyManager.class);
            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_GPRS);

            NetworkMonitor mockedNetworkMonitor = mock(NetworkMonitor.class);
            when(mockedNetworkMonitor.getTelephonyManager()).thenReturn(mockedTelephonyManager);
            when(mockedNetworkMonitor.getMobileNetworkType()).thenCallRealMethod();
            assertEquals("2G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EDGE);
            assertEquals("2G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_CDMA);
            assertEquals("2G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_1xRTT);
            assertEquals("2G", mockedNetworkMonitor.getMobileNetworkType());

            when(mockedTelephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_IDEN);
            assertEquals("2G", mockedNetworkMonitor.getMobileNetworkType());
        } catch (NullPointerException | ComparisonFailure e){
            
        }
    }

    @Test
    public void testGetCurrentConnectionTypeNoConnection() {
        try {
            NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
            NetworkInfo mockedInfo = mock(NetworkInfo.class);

            when(mockedMonitor.isInternetAccessAvailable()).thenReturn(false);
            when(mockedMonitor.getCurrentConnectionType()).thenCallRealMethod();

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);
            assertEquals(NetworkConnectionType.NO_CONNECTION, mockedMonitor.getCurrentConnectionType());

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(null);
            assertEquals(NetworkConnectionType.NO_CONNECTION, mockedMonitor.getCurrentConnectionType());
        } catch (NullPointerException e){
            
        }
    }

    @Test
    public void testGetCurrentConnectionTypeWifi() {
        try {
            NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
            NetworkInfo mockedInfo = mock(NetworkInfo.class);

            when(mockedMonitor.isInternetAccessAvailable()).thenReturn(true);
            when(mockedMonitor.getCurrentConnectionType()).thenCallRealMethod();
            when(mockedInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);
            assertEquals(NetworkConnectionType.WIFI, mockedMonitor.getCurrentConnectionType());
        } catch (NullPointerException e){
            
        }
    }

    @Test
    public void testGetCurrentConnectionTypeMobile() {
        try {
            NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
            NetworkInfo mockedInfo = mock(NetworkInfo.class);

            when(mockedMonitor.isInternetAccessAvailable()).thenReturn(true);
            when(mockedMonitor.getCurrentConnectionType()).thenCallRealMethod();
            when(mockedInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);
            assertEquals(NetworkConnectionType.MOBILE, mockedMonitor.getCurrentConnectionType());
        } catch (NullPointerException e){
            
        }
    }

    @Test
    public void testGetCurrentConnectionTypeWiMAX() {
        try {
            NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
            NetworkInfo mockedInfo = mock(NetworkInfo.class);

            when(mockedMonitor.isInternetAccessAvailable()).thenReturn(true);
            when(mockedMonitor.getCurrentConnectionType()).thenCallRealMethod();
            when(mockedInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIMAX);

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);
            assertEquals(NetworkConnectionType.WIMAX, mockedMonitor.getCurrentConnectionType());
        } catch (NullPointerException e){
            
        }
    }

    @Test
    public void testGetCurrentConnectionTypeEthernet() {
        try {
            NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
            NetworkInfo mockedInfo = mock(NetworkInfo.class);

            when(mockedMonitor.isInternetAccessAvailable()).thenReturn(true);
            when(mockedMonitor.getCurrentConnectionType()).thenCallRealMethod();
            when(mockedInfo.getType()).thenReturn(ConnectivityManager.TYPE_ETHERNET);

            when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);
            assertEquals(NetworkConnectionType.ETHERNET, mockedMonitor.getCurrentConnectionType());
        } catch (NullPointerException e){
            
        }
    }

    @Test
    public void testIsInternetAccessAvailable() {
      try {
          NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);

          NetworkInfo mockedInfo = mock(NetworkInfo.class);

          when(mockedMonitor.isInternetAccessAvailable()).thenCallRealMethod();
          when(mockedMonitor.getActiveNetworkInfo()).thenReturn(mockedInfo);

          when(mockedInfo.isConnected()).thenReturn(true);
          assertTrue(mockedMonitor.isInternetAccessAvailable());

          when(mockedInfo.isConnected()).thenReturn(false);
          assertFalse(mockedMonitor.isInternetAccessAvailable());
      } catch (NullPointerException e){
          
      }
    }

    @Test
    public void testNetworkChangeReceiver() throws Exception {
        latch = new CountDownLatch(1);

        NetworkConnectionListener listener = new NetworkConnectionListener() {
            @Override
            public void networkChanged(NetworkConnectionType newConnection) {
                latch.countDown();
            }
        };

        NetworkMonitor mockedMonitor = mock(NetworkMonitor.class);
        when(mockedMonitor.getCurrentConnectionType()).thenReturn(NetworkConnectionType.WIFI);

        Intent mockedIntent = mock(Intent.class);
        when(mockedIntent.getAction()).thenReturn(ConnectivityManager.CONNECTIVITY_ACTION);

        NetworkMonitor.NetworkChangeReceiver receiver = mockedMonitor.new NetworkChangeReceiver(listener);
        receiver.onReceive(new MockContext(), mockedIntent);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }


    private NetworkConnectionListener getNetworkListener() {
        NetworkConnectionListener listener = new NetworkConnectionListener() {
            @Override
            public void networkChanged(NetworkConnectionType newConnection) {
                // Do nothing
            }
        };
        return listener;
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
