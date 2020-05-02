# Latest Release

The latest available release of binjr is ${tagName}, released on ${releaseDate}.  
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
| ${version} | **Linux** (glibc v2.5 or higher)| x84 64-bit | [<button ><img alt="" src="../../assets/images/download.svg"> .tar.gz</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz.asc)[^1]  |
| ${version} | **macOS** (10.10 or later)| x84 64-bit | [<button ><img alt="" src="../../assets/images/download.svg"> .dmg</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg.asc)[^1]  |
| ${version} | **Windows** (7 or later)| x84 64-bit | [<button><img alt="" src="../../assets/images/download.svg"> .msi</button>](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi) | [(GPG signature)](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi.asc)[^1]  |


[^1]: [How to verify the integrity of the downloaded file?](../../documentation/verify-signature/)