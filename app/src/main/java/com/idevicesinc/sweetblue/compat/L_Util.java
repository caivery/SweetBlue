package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.utils.Interval;

import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class L_Util
{

    private L_Util() {}


    public interface ScanCallback
    {
        void onScanResult(int callbackType, ScanResult result);
        void onBatchScanResults(List<ScanResult> results);
        void onScanFailed(int errorCode);
    }

    public static class ScanResult
    {
        private BluetoothDevice device;
        private int rssi;
        private byte[] record;

        public BluetoothDevice getDevice() {
            return device;
        }

        public int getRssi() {
            return rssi;
        }

        public byte[] getRecord() {
            return record;
        }
    }

    private static ScanCallback m_UserCallback;

    private static ScanResult toLScanResult(android.bluetooth.le.ScanResult result) {
        ScanResult res = new ScanResult();
        res.device = result.getDevice();
        res.rssi = result.getRssi();
        res.record = result.getScanRecord().getBytes();
        return res;
    }

    private static List<ScanResult> toLScanResults(List<android.bluetooth.le.ScanResult> results) {
        int size = results.size();
        List<ScanResult> res = new ArrayList<ScanResult>(size);
        for (int i = 0; i < size; i++) {
            res.add(toLScanResult(results.get(i)));
        }
        return res;
    }

    private static android.bluetooth.le.ScanCallback m_callback = new android.bluetooth.le.ScanCallback()
    {
        @Override public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onScanResult(callbackType, toLScanResult(result));
            }
        }

        @Override public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onBatchScanResults(toLScanResults(results));
            }
        }

        @Override public void onScanFailed(int errorCode)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onScanFailed(errorCode);
            }
        }
    };

    static android.bluetooth.le.ScanCallback getNativeCallback() {
        return m_callback;
    }


    public static boolean requestMtu(BleDevice device, int mtu) {
        return device.getNativeGatt().requestMtu(mtu);
    }

    public static boolean isAdvertisingSupportedByChipset(BleManager mgr) {
        return mgr.getNativeAdapter().isMultipleAdvertisementSupported();
    }

    public static void stopNativeScan(BleManager mgr) {
        mgr.getNativeAdapter().getBluetoothLeScanner().stopScan(m_callback);
    }

    public static boolean requestConnectionPriority(BleDevice device, int mode) {
        return device.getNativeGatt().requestConnectionPriority(mode);
    }

    public static void startNativeScan(BleManager mgr, int scanMode, Interval scanReportDelay, ScanCallback listener) {

        final ScanSettings settings = buildSettings(mgr, scanMode, scanReportDelay).build();

        startScan(mgr, settings, listener);
    }

    static ScanSettings.Builder buildSettings(BleManager mgr, int scanMode, Interval scanReportDelay) {
        final ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);

        if( mgr.getNativeAdapter().isOffloadedScanBatchingSupported() )
        {
            final long scanReportDelay_millis = false == Interval.isDisabled(scanReportDelay) ? scanReportDelay.millis() : 0;
            builder.setReportDelay(scanReportDelay_millis);
        }
        else
        {
            builder.setReportDelay(0);
        }
        return builder;
    }

    static void startScan(BleManager mgr, ScanSettings scanSettings, ScanCallback listener) {
        m_UserCallback = listener;

        // Build some native filters
        List<android.bluetooth.le.ScanFilter> filters = null;

        try
        {
            filters = mgr.getConfig().defaultScanFilter.makeNativeScanFilters();
        }
        catch (Exception e)
        {
            // Oh well.  This can happen if anything was null or if we are on an OS version that doesnt support filters
        }

        mgr.getNativeAdapter().getBluetoothLeScanner().startScan(filters, scanSettings, m_callback);
    }

}
