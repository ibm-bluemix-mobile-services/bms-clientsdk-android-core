/*
 *     Copyright 2015 IBM Corp.
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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.security;

import android.test.InstrumentationTestCase;

import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.certificate.CertificatesUtility;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by cirilla on 8/11/15.
 */
public class CertificateAuxiliaryTests extends InstrumentationTestCase {


    final String certificateInBase64 = "MIICbTCCAVWgAwIBAgIIG0w8ZSCfbeQwDQYJKoZIhvcNAQELBQAwYDELMAkGA1UEBhMCSUwxCzAJBgNVBAgTAklMMREwDwYDVQQHEwhTaGVmYXlpbTEMMAoGA1UEChMDSUJNMRIwEAYDVQQLEwlXb3JrbGlnaHQxDzANBgNVBAMTBldMIERldjAgFw0xNTA4MTcwNzQ5MDZaGA8yMDY1MDgxNzA3NDkwNlowUzEXMBUGCgmSJomT8ixkARkWB25vdGhpbmcxODA2BgoJkiaJk\\/IsZAEBEyg2ZmQ4YjViN2FhYzBmNDZkNGQwYzUzY2MyM2M2ZGQyZWMyYmMxOGIxMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMA0jApej21k0ztkgJ4LCrtfT64tvDBq37c5pwCDudyfQqVrySCIc42PuZbQujKMawqJytO168P7Gev\\/z4JlAjsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAA+0kgTMLNiOALF2T\\/w+4Tf\\/Vcs\\/TZ+5ztSV2xZmLj+cNt18vnAs+Zt\\/zs2v45WVFOE2t58XUIiE5XQ5wqsXdsMRPABwbNj1PGs1o0\\/yWeAHlas0JtJNlgQ4p8SkybnlKdUMzZqtUA0mBb5\\/GqVBrxdl9ci\\/Y1TP6jh+7eQM5Oyj31F7PjfowTxVuxjm7kz87B0MsrZlulTE2h02pFMdNanTdJS3Cx6RmerAXUgn90qKQsMjKTzrnF0lknO6cOSPbyLurJv2t5eJ65wyanpO38IaC31Uy4ARZUU5lPMoBpciCBdiJMfl2QQQFpfxrIQzQDWMNV5AEF1hSKZfTEvAWfQ==";
    final String expectedClientId = "6fd8b5b7aac0f46d4d0c53cc23c6dd2ec2bc18b1";


    public void testParamForNull() throws CertificateException, IOException {

        boolean isIlligalArgument = false;
        try{
            CertificatesUtility.base64StringToCertificate(null);
        }catch (IllegalArgumentException e){
            isIlligalArgument = true;
        } catch (Exception e){

        }

        assertTrue(isIlligalArgument);
    }

    public void testParamForNull2() throws CertificateException, IOException {

        boolean isIlligalArgument = false;
        try{
            CertificatesUtility.getClientIdFromCertificate(null);
        } catch (IllegalArgumentException e){
            isIlligalArgument = true;
        } catch (Exception e){

        }

        assertTrue(isIlligalArgument);
    }

    public void testBase64ToCertificate() throws CertificateException, IOException {
        X509Certificate x509Certificate = CertificatesUtility.base64StringToCertificate(certificateInBase64);
        assertNotNull(x509Certificate);
    }

    public void testClientIdExtraction() throws CertificateException, IOException {
        X509Certificate x509Certificate = CertificatesUtility.base64StringToCertificate(certificateInBase64);
        String clientIdFromCertificate = CertificatesUtility.getClientIdFromCertificate(x509Certificate);
        assertEquals(expectedClientId,clientIdFromCertificate);
    }

}
