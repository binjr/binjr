# Application Bundles

These bundles contain all dependencies and runtime components needed to run binjr.   
They also include support for the following data sources: 
[JRDS](https://github.com/fbacchella/jrds), [Netdata](https://www.netdata.cloud), RDD Files and CSV files.

The latest available version is ${version}, released on ${releaseDate}.  

Complete release information can be found in the [change log](CHANGELOG.md). 

<style>
  .md-typeset button {
    cursor: pointer;
    transition: opacity 250ms;
  }
  .md-typeset button:hover {
    opacity: 0.75;
  }
  .md-typeset button  {
    border-style: solid;
    border-width: 5px;   
    border-radius: 5px;
    padding: 0px 5px 0px 5px;

    border-color: var(--md-accent-fg-color); 
    display: block;
    color: var(--md-accent-bg-color);
    background-color: var(--md-accent-fg-color);
  }
</style>



|Version     | Operating System       |  Architecture        |  Download  | |
|----------|----------|----------|------|----|
| <p>binjr ${tagName}</p> | <p>**Linux** (glibc v2.5 or higher)</p>| <p>x84 64-bit</p> | <p>[<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz)</p> | <p>[Signature (GPG)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz.asc)[^1]</p>  |
| <p>binjr ${tagName}</p> | <p>**macOS** (10.10 or later)</p>| <p>x84 64-bit</p> |  <p>[<button ><img alt="" src="../../assets/images/download.svg"> .dmg</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg)</p> <p>[<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.tar.gz)</p> | <p>[Signature (GPG)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg.asc)[^1]</p> <p>[Signature (GPG)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.tar.gz.asc)[^1]</p> |
| <p>binjr ${tagName}</p> | <p>**Windows** (7 or later)</p>| <p>x84 64-bit</p> | <p>[<button><img alt="" src="../../assets/images/download.svg"> .msi</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi)</p> <p>[<button><img alt="" src="../../assets/images/download.svg"> .zip</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.zip)</p>  | <p>[Signature (GPG)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi.asc)[^1]</p> <p>[Signature (GPG)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.zip.asc)[^1]</p> |

[^1]: [How to verify the integrity of the downloaded file?](/documentation/verify-signature/)

!!! Warning "If you're having trouble launching binjr..."
    === "...on macOS"
        **binjr** might not be recognized by the Apple notary service, so you may get a warning when trying to run it the 
        first time.
        To override it, locate binjr in the Finder on your Mac, control-click the app icon, then choose `Open` from the
        shortcut menu and click `Open` ([see more](https://support.apple.com/guide/mac-help/mh40616/mac)).

    === "...on Windows"
        **binjr** might not be recognized by Microsoft Defender SmartScreen, so you might need to suppress a warning
        to install it on your system. To do so, click "more info" then "run anyway"
        ([see more](https://docs.microsoft.com/en-us/windows/security/threat-protection/microsoft-defender-smartscreen/microsoft-defender-smartscreen-overview)).
        
    To ensure that your download has not been tempered with,
    [you should verify its OpenGPG signature](/documentation/verify-signature).
           
