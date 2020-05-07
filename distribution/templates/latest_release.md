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
    margin-bottom: 10px;
    border-color: var(--md-accent-fg-color); 
    display: block;
    color: var(--md-accent-bg-color);
    background-color: var(--md-accent-fg-color);
  }
</style>



|Version     | Operating System       |  Architecture        |  Download  | |
|----------|----------|----------|------|----|
| binjr ${tagName} | **Linux** (glibc v2.5 or higher)| x84 64-bit | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz.asc)[^1]  |
| binjr ${tagName} | **macOS** (10.10 or later)| x84 64-bit | [<button ><img alt="" src="../../assets/images/download.svg"> .dmg</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg.asc)[^1]  |
| binjr ${tagName} | **Windows** (7 or later)| x84 64-bit | [<button><img alt="" src="../../assets/images/download.svg"> .msi</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi.asc)[^1]  |


# Plugins

You can use the plugins below to further extend binjr's built-in functionalities:

1. Download and unpack the archive.
2. Extract and copy the `jar` files into the `plugins` folder of your binjr installation.
3. Restart binjr if it was running when you copied the plugin. 

|Version | Operating System | Architecture | Download |   |
|--------|------------------|--------------|----------|---|
| [Demo data adapter](https://github.com/binjr/binjr-adapter-demo) v1.1.0 | **All** | - | [<button ><img alt="" src="../../assets/images/download.svg"> .zip</button>](https://github.com/binjr/binjr-adapter-demo/releases/download/v1.1.0/binjr-adapter-demo-1.1.0.zip) | [(GPG signature)](https://github.com/binjr/binjr-adapter-demo/releases/download/v1.1.0/binjr-adapter-demo-1.1.0.zip.asc)[^1]  |



[^1]: [How to verify the integrity of the downloaded file?](../../documentation/verify-signature/)