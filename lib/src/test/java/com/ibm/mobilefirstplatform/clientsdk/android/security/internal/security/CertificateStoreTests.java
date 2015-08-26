package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.security;

import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateStore;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by cirilla on 8/6/15.
 */
public class CertificateStoreTests {



    final static String certificateAsHex = "3082026D30820155A00302010202081B4C3C65209F6DE4300D06092A864886F70D01010B05003060310B300906035504061302494C310B300906035504081302494C3111300F06035504071308536865666179696D310C300A060355040A130349424D31123010060355040B1309576F726B6C69676874310F300D06035504031306574C204465763020170D3135303831373037343930365A180F32303635303831373037343930365A305331173015060A0992268993F22C64011916076E6F7468696E6731383036060A0992268993F22C640101132836666438623562376161633066343664346430633533636332336336646432656332626331386231305C300D06092A864886F70D0101010500034B003048024100C0348C0A5E8F6D64D33B64809E0B0ABB5F4FAE2DBC306ADFB739A70083B9DC9F42A56BC92088738D8FB996D0BA328C6B0A89CAD3B5EBC3FB19EBFFCF8265023B0203010001300D06092A864886F70D01010B0500038201010003ED2481330B3623802C5D93FF0FB84DFFD572CFD367EE73B52576C5998B8FE70DB75F2F9C0B3E66DFF3B36BF8E56545384DADE7C5D42221395D0E70AAC5DDB0C44F001C1B363D4F1ACD68D3FC967801E56ACD09B49365810E29F129326E794A75433366AB540349816F9FC6A9506BC5D97D722FD8D533FA8E1FBB7903393B28F7D45ECF8DFA304F156EC639BB933F3B07432CAD996E953136874DA914C74D6A74DD252DC2C7A4667AB0175209FDD2A290B0C8CA4F3AE71749649CEE9C3923DBC8BBAB26FDADE5E27AE70C9A9E93B7F08682DF5532E00459514E653CCA01A5C88205D88931F976410405A5FC6B210CD00D630D5790041758522997D312F0167D";

    CertificateStore store;
    File tempFile;
    KeyPair keyPair;
    X509Certificate certificate;

    final String PASSWORD = "password";

    @Before
    public void setUp() throws Exception {
        tempFile = File.createTempFile("tempKeyStore", "tmp");
        store = new CertificateStore(tempFile,PASSWORD.toCharArray());
        keyPair = generateRandomKeyPair();

        InputStream inStream = new ByteArrayInputStream(hexStringToByteArray(certificateAsHex));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        certificate = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
    }

    @After
    public void tearDown() throws Exception {
        tempFile.delete();
    }

    @Test
    public void testEmpty(){
        Assert.assertFalse(store.isCertificateStored());
    }

    @Test
    public void testGetKeyPairFromEmpty() throws Exception {
        Assert.assertNull(store.getStoredKeyPair());
    }

    @Test
    public void testGetCertificateFromEmpty() throws Exception {
        Assert.assertNull(store.getCertificate());
    }

    @Test
    public void testSave() throws Exception {
        store.saveCertificate(keyPair,certificate);

        //store now contains certificate
        Assert.assertTrue(store.isCertificateStored());
        store = null;

        CertificateStore store2 = new CertificateStore(tempFile,PASSWORD.toCharArray());

        KeyPair savedKeyPair = store2.getStoredKeyPair();

        //store 2 should contain certificate
        Assert.assertTrue(store2.isCertificateStored());

        //check key pair
        Assert.assertNotNull(savedKeyPair.getPublic());
        Assert.assertTrue(keyPair.getPrivate().equals(savedKeyPair.getPrivate()));
        Assert.assertNotNull(store2.getStoredKeyPair());

        //the certificate should exist
        Assert.assertNotNull(store2.getCertificate());
    }


    private KeyPair generateRandomKeyPair() {
        KeyPair keyPair = null;

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            keyPair = kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return keyPair;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }




}
