# Application bundles

These bundles contain all dependencies and runtime components needed to run binjr.   
They also include support for the following data sources:
[JRDS](https://github.com/fbacchella/jrds), [Netdata](https://www.netdata.cloud), RRD, JFR, CSV and log files.

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

## Package managers
binjr can be installed and updated using various package managers, depending on you operating system.

| Version          | Operating System                                                 | Architecture         | Package manager                                              |
|------------------|------------------------------------------------------------------|----------------------|--------------------------------------------------------------|
| binjr ${tagName} | **Linux** ([supported distributions](https://flathub.org/setup)) | ` x86_64`, `aarch64` | [Flathub](https://flathub.org/apps/eu.binjr.binjr)           |
| binjr ${tagName} | **Debian** (10,11), **Ubuntu** (22.04+)                          | `x86_64`             | [binjr APT Repository](https://repos.binjr.eu/apt)           |
| binjr ${tagName} | **RHEL** (8,9), **Fedora** (36+)                                 | `x86_64`             | [binjr RPM Repository](https://repos.binjr.eu/rpm)           |
| binjr ${tagName} | **ArchLinux**                                                    | `x86_64`             | [Arch User Repository](https://repos.binjr.eu/aur)           |
| binjr ${tagName} | **Windows** (10[^2] or 11)                                       | `x86_64`             | [Winget Community Repository](https://repos.binjr.eu/winget) |


## Installable bundles

Installable bundles integrate with the host OS to provide menu shortcuts, file associations and per user settings.

| Version          | Operating System                        | Architecture | Download                                                                                                                                                                     | Signature[^1]                                                                                                                                                                     |
|------------------|-----------------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| binjr ${tagName} | **Debian** (10,11), **Ubuntu** (22.04+) | `x86_64`     | [<button ><img alt="" src="../../assets/images/download.svg"> .deb</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.deb)  | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.deb.asc)   |
| binjr ${tagName} | **Debian** (10,11), **Ubuntu** (22.04+) | `aarch64`    | [<button ><img alt="" src="../../assets/images/download.svg"> .deb</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.deb) | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.deb.asc)  |
| binjr ${tagName} | **RHEL** (8,9), **Fedora** (36+)        | `x86_64`     | [<button><img alt="" src="../../assets/images/download.svg"> .rpm</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.rpm)   | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.rpm.asc)   |
| binjr ${tagName} | **RHEL** (8,9), **Fedora** (36+)        | `aarch64`    | [<button><img alt="" src="../../assets/images/download.svg"> .rpm</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.rpm)  | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.rpm.asc)  |
| binjr ${tagName} | **macOS** (14.x or later)               | `x86_64`     | [<button ><img alt="" src="../../assets/images/download.svg"> .pkg</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.pkg)    | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.pkg.asc)     |
| binjr ${tagName} | **macOS** (14.x or later)               | `aarch64`    | [<button ><img alt="" src="../../assets/images/download.svg"> .pkg</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-aarch64.pkg)   | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-aarch64.pkg.asc)    |
| binjr ${tagName} | **Windows** (10 or later)               | `x86_64`     | [<button><img alt="" src="../../assets/images/download.svg"> .msi</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-x86_64.msi) | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-x86_64.msi.asc) |

## Portable bundles

Portable bundles can be unpacked to and used from a detachable drive or a file share.

| Version          | Operating System                 | Architecture | Download                                                                                                                                                                           | Signature[^1]                                                                                                                                                                       |
|------------------|----------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| binjr ${tagName} | **Linux** (glibc v2.5 or higher) | `x86_64`     | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.tar.gz)  | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-x86_64.tar.gz.asc)  |
| binjr ${tagName} | **Linux** (glibc v2.5 or higher) | `aarch64`    | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.tar.gz) | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-aarch64.tar.gz.asc) |
| binjr ${tagName} | **macOS** (14.x or later)        | `x86_64`     | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.tar.gz)    | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.tar.gz.asc)    |
| binjr ${tagName} | **macOS** (14.x or later)        | `aarch64`    | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-aarch64.tar.gz)   | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-aarch64.tar.gz.asc)   |
| binjr ${tagName} | **Windows** (10 or later)        | `x86_64`     | [<button><img alt="" src="../../assets/images/download.svg"> .zip</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-x86_64.zip)       | [<button ><img alt="" src="../../assets/images/download.svg"> .asc</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-x86_64.zip.asc)   |

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
    [you should verify its OpenPGP signature](/documentation/verify-signature).

[^1]: [How to verify the integrity of the downloaded file?](../documentation/verify-signature.md)
[^2]: The winget command line tool is only supported on Windows 10 1709 (build 16299) or later

