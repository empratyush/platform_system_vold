package grapheneos.test;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(DeviceJUnit4ClassRunner.class)
public class KeystoreDuressTest extends BaseHostJUnit4Test {

    public static final String KEYSTORE_LOG_TAG = "DuressKeystore";

    private ITestDevice device;

    @Before
    public void setup() throws DeviceNotAvailableException {
        device = getDevice();
        Assert.assertTrue(
                "non user-debug build",
                device.enableAdbRoot()
        );
    }

    @Test
    public void testKeystoreWipe() throws DeviceNotAvailableException {
        makeSureKeystoreBasedKeysAreAvailable();
        wipeKeystoreKeys();
        waitFor(Duration.ofSeconds(1));
        makeSureKeystoreBasedKeysAreGone();
    }

    private void waitFor(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignored) {
        }
    }

    private void makeSureKeystoreBasedKeysAreAvailable()
            throws DeviceNotAvailableException {

        long from = device.getDeviceDate();
        device.executeShellCommand("duressKeystoreTest");
        Assert.assertTrue(
                "unable to decrypted keys using keystore before wiping the keystore ",
                keysCanBeDecryptedUsingKeystore(from)
        );
    }

    private void wipeKeystoreKeys()
            throws DeviceNotAvailableException {

        var from = device.getDeviceDate();
        device.executeShellCommand("start KeystoreDuressService");
        Assert.assertTrue(
                "KeystoreDuressService time out",
                waitForKeystoreDuressServiceToFinish(from)
        );
    }

    private void makeSureKeystoreBasedKeysAreGone()
            throws DeviceNotAvailableException {

        long from = device.getDeviceDate();
        device.executeShellCommand("duressKeystoreTest");
        Assert.assertFalse(
                "able to decrypted keys using keystore after wiping the keystore ",
                keysCanBeDecryptedUsingKeystore(from)
        );
    }

    private boolean waitForKeystoreDuressServiceToFinish(long startedFrom) {
        var waitDuration = Duration.ofSeconds(30).toMillis();
        long stopTimeInMilli = System.currentTimeMillis() + waitDuration;

        while (System.currentTimeMillis() < stopTimeInMilli) {
            var lines = getLogsSince(startedFrom);
            var keystoreDuressServiceInitExitStatus = "keys in keystore erased successfully";

            for (String line : lines) {
                if (line.contains(KEYSTORE_LOG_TAG) &&
                        line.contains(keystoreDuressServiceInitExitStatus)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean keysCanBeDecryptedUsingKeystore(long startedFrom) {
        var waitDuration = Duration.ofSeconds(30).toMillis();
        long stopTimeInMilli = System.currentTimeMillis() + waitDuration;

        var success = "able to retrieveKey keys";
        var failed = "failed to retrieveKey keys";

        while (System.currentTimeMillis() < stopTimeInMilli) {
            var lines = getLogsSince(startedFrom);

            for (String line : lines) {
                if (!line.contains(KEYSTORE_LOG_TAG)) {
                    continue;
                }

                if (line.contains(success)) {
                    return true;
                } else if (line.contains(failed)) {
                    return false;
                }
            }
        }

        return false;
    }

    private List<String> getLogsSince(long since) {
        var logs = device.getLogcatSince(since);
        try (BufferedReader logsReader =
                     new BufferedReader(new InputStreamReader(logs.createInputStream()))) {
            return logsReader.lines().toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

}
