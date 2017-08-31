Summary: ${project.name} - ${project.description}
Name: ${project.name}
Version: ${bundle.version}
Release: 1
License: ${license.shortName}
Vendor: ${project.organization.name}
Prefix: /opt
Provides: ${project.name}
Requires: ld-linux.so.2 libX11.so.6 libXext.so.6 libXi.so.6 libXrender.so.1 libXtst.so.6 libasound.so.2 libc.so.6 libdl.so.2 libgcc_s.so.1 libm.so.6 libpthread.so.0 libthread_db.so.1
Autoprov: 0
Autoreq: 0

#avoid ARCH subfolder
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but 
#build time will substantially increase and it may require unpack200/system java to install
#%define __jar_repack %{nil}

%description
${project.description}

%prep

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt
cp -r %{_sourcedir}/binjr %{buildroot}/opt

%files
%doc /opt/binjr/app/LICENSE
/opt/binjr

%post


xdg-desktop-menu install --novendor /opt/binjr/binjr.desktop
xdg-mime install /opt/binjr/FredericThevenet-binjr-MimeInfo.xml

if [ "false" = "true" ]; then
    cp /opt/binjr/binjr.init /etc/init.d/binjr
    if [ -x "/etc/init.d/binjr" ]; then
        /sbin/chkconfig --add binjr
        if [ "false" = "true" ]; then
            /etc/init.d/binjr start
        fi
    fi
fi

%preun

xdg-desktop-menu uninstall --novendor /opt/binjr/binjr.desktop
xdg-mime uninstall /opt/binjr/FredericThevenet-binjr-MimeInfo.xml

if [ "false" = "true" ]; then
    if [ -x "/etc/init.d/binjr" ]; then
        if [ "true" = "true" ]; then
            /etc/init.d/binjr stop
        fi
        /sbin/chkconfig --del binjr
        rm -f /etc/init.d/binjr
    fi
fi

%clean