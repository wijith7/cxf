/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.systest.sts.itests;

import java.io.File;

import org.apache.cxf.testutil.common.TestUtil;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@ExamReactorStrategy(PerClass.class)
public class BasicSTSIntegrationTest {

    // Adding apache snapshots as cxf trunk may contain snapshot dependencies
//    private static final String REPOS = "https://repo1.maven.org/maven2@id=central,"
//        + "https://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases";

    @Configuration
    public Option[] getConfig() {
        String port = TestUtil.getPortNumber(BasicSTSIntegrationTest.class);
        System.setProperty("BasicSTSIntegrationTest.PORT", port);

        String localRepository = System.getProperty("localRepository");

        MavenArtifactUrlReference stsFeatures = maven() //
            .groupId("org.apache.cxf.services.sts.systests") //
            .artifactId("cxf-services-sts-systests-features") //
            .versionAsInProject() //
            .type("xml");

        final Option[] basicOptions = new Option[] {
             karafDistributionConfiguration()
                 .frameworkUrl(
                     maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz"))
                 .unpackDirectory(new File("target/paxexam/unpack/"))
                 .useDeployFolder(false),
             systemProperty("java.awt.headless").value("true"),
             systemProperty("BasicSTSIntegrationTest.PORT").value(port),
//             editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
//                 "org.ops4j.pax.url.mvn.repositories", REPOS),

             copy("clientKeystore.properties"),
             copy("clientstore.jks"),
             editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                                      "org.osgi.service.http.port", port),
             when(localRepository != null)
                 .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                      "org.ops4j.pax.url.mvn.localRepository",
                                                      localRepository)),
             features(stsFeatures, "cxf-sts-service"),
             configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
        };
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            final String karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
            return OptionUtils.combine(basicOptions,
                new VMOption("--add-reads=java.xml=java.logging"),
                new VMOption("--add-exports=java.base/"
                    + "org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                new VMOption("--patch-module"),
                new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + karafVersion + ".jar"),
                new VMOption("--patch-module"),
                new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + karafVersion + ".jar"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.security=ALL-UNNAMED"),
                new VMOption("--add-opens"), new VMOption("java.base/java.net=ALL-UNNAMED"),
                new VMOption("--add-opens"), new VMOption("java.base/java.lang=ALL-UNNAMED"),
                new VMOption("--add-opens"), new VMOption("java.base/java.util=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                new VMOption("-classpath"),
                new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
            );
        }
        return basicOptions;
    }

    protected static Option copy(String path) {
        return replaceConfigurationFile(path, new File("src/test/resources/" + path));
    }

}
