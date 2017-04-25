package github;

import eu.fthevenet.util.version.Version;

/**
 * Created by fred on 25/04/2017.
 */
public class VersionTest {

    public static void main(String[] args) {

        Version[] versions = new Version[]{
                new Version("1.0.0-SNAPSHOT"),
                new Version("1.0.0"),
                new Version("1.0.0.a-SNAPSHOT"),
                new Version("1.0.0.a"),
                new Version("1.0.0.b"),
                new Version("1.0.1-SNAPSHOT"),
                new Version("1.0.1"),
                new Version("1.0.1.a-SNAPSHOT"),
                new Version("1.0.1.a"),
                new Version("1.0.1.b")
        };


        for (int i = 0; i < versions.length; i++) {
            for (int j=0;j<versions.length;j++) {
                int cp = versions[i].compareTo(versions[j]);
                String sign = "==";
                if (cp > 0) {
                    sign = ">";
                }
                if (cp < 0) {
                    sign = "<";
                }
                System.out.println(versions[i].toString() + " " + sign + " " + versions[j].toString());
            }
        }

    }
}
