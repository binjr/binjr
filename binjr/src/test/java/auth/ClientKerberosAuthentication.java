/*
 *    Copyright 2017 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package auth;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.Principal;

/**
 * Kerberos auth example.
 * <p>
 * <p><b>Information</b></p>
 * <p>For the best compatibility use Java >= 1.6 as it supports SPNEGO authentication more
 * completely.</p>
 * <p><em>NegotiateSchemeFactory</em></p>
 * <p>Has three custom methods</p>
 * <p><em>setStripPort(boolean)</em> - default is false, with strip the port off the Kerberos
 * service name if true. Found useful with JbossNegotiation. Java >= 1.5</p>
 * <p>
 * <p>Below are for Java 1.5.</p>
 * <p>
 * <p><em>setSpnegoCreate(boolean)</em> - defaults to false, try to create an SPNEGO token via
 * the token set in setSpengoGenerator. TODO - merge logic so just setSpengoGenerator</p>
 * <p>
 * <p><em>setSpengoGenerator(new SpnegoTokenGenerator())</em> - default is null, class to use to wrap
 * kerberos token. An example is in contrib - <em>org.apache.http.contrib.auth.BouncySpnegoTokenGenerator</em>.
 * Requires use of <a href="http://www.bouncycastle.org/java.html">bouncy castle libs</a>
 * </p>
 * <p>
 * <p><b>Addtional Config Files</b></p>
 * <p>Two files control how Java uses/configures Kerberos. Very basic examples are below. There
 * is a large amount of information on the web.</p>
 * <p><a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html">http://java.sun.com/j2se/1.5.0/docs/guide/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html</a>
 * <p><b>krb5.conf</b></p>
 * <pre>
 * [libdefaults]
 *     default_realm = AD.EXAMPLE.NET
 *     udp_preference_limit = 1
 * [realms]
 *     AD.EXAMPLE.NET = {
 *         kdc = AD.EXAMPLE.NET
 *     }
 *     DEV.EXAMPLE.NET = {
 *         kdc = DEV.EXAMPLE.NET
 *     }
 * [domain_realms]
 * .ad.example.net = AD.EXAMPLE.NET
 * ad.example.net = AD.EXAMPLE.NET
 * .dev.example.net = DEV.EXAMPLE.NET
 * dev.example.net = DEV.EXAMPLE.NET
 * gb.dev.example.net = DEV.EXAMPLE.NET
 * .gb.dev.example.net = DEV.EXAMPLE.NET
 * </pre>
 * <b>jaas_login.conf</b>
 * <pre>
 * com.sun.security.jgss.login {
 *   com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true;
 * };
 *
 * com.sun.security.jgss.initiate {
 *   com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true;
 * };
 *
 * com.sun.security.jgss.accept {
 *   com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true;
 * };
 * </pre>
 * <p><b>Windows specific configuration</b></p>
 * <p>
 * The registry key <em>allowtgtsessionkey</em> should be added, and set correctly, to allow
 * session keys to be sent in the Kerberos Ticket-Granting Ticket.
 * </p>
 * <p>
 * On the Windows Server 2003 and Windows 2000 SP4, here is the required registry setting:
 * </p>
 * <pre>
 * HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Lsa\Kerberos\Parameters
 *   Value Name: allowtgtsessionkey
 *   Value Type: REG_DWORD
 *   Value: 0x01
 * </pre>
 * <p>
 * Here is the location of the registry setting on Windows XP SP2:
 * </p>
 * <pre>
 * HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Lsa\Kerberos\
 *   Value Name: allowtgtsessionkey
 *   Value Type: REG_DWORD
 *   Value: 0x01
 * </pre>
 *
 * @since 4.1
 */
public class ClientKerberosAuthentication {

    public static void main(String[] args) throws Exception {

        System.setProperty("java.security.auth.login.config", ClientKerberosAuthentication.class.getResource("/jaas_login.conf").toExternalForm());
        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        String targetUrl;
        if (args.length < 1) {
            throw new IllegalArgumentException("Please specify a target URL");
        }
        targetUrl = args[0];
        /* ***********************************************************************************************/
        RegistryBuilder<AuthSchemeProvider> schemeProviderBuilder = RegistryBuilder.create();
        schemeProviderBuilder.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory());

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                new Credentials() {
                    @Override
                    public Principal getUserPrincipal() {
                        System.out.print("Get principal!");
                        return null;
                    }

                    @Override
                    public String getPassword() {
                        System.out.print("Get pwd!");
                        return null;
                    }
                });

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultAuthSchemeRegistry(schemeProviderBuilder.build())
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        doGet(httpClient, targetUrl);
        httpClient.close();
    }

    private static void doGet(HttpClient httpClient, String targetUrl) throws IOException {

        HttpUriRequest request = new HttpGet(targetUrl + "/jsontree?tab=hoststab");
        // Set user-agent pattern to workaround CAS server not proposing SPNEGO authentication unless it thinks agent can handle it.
        request.setHeader("User-Agent", "binjr/" + GlobalPreferences.getInstance().getManifestVersion() + " (Authenticates like: Firefox/Safari/Internet Explorer)");
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        System.out.println("----------------------------------------");
        if (entity != null) {
            //   System.out.println(EntityUtils.toString(entity));
        }
        System.out.println("----------------------------------------");

        // This ensures the connection gets released back to the manager
        if (entity != null) {
            EntityUtils.consume(entity);
        }

    }
}