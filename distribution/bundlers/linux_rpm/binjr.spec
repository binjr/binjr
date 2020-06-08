Summary: A Time Series Browser
Name: binjr
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: Apache-2.0
Vendor: binjr
URL: https://binjr.eu
Prefix: %{dirname:/opt/binjr}
Provides: binjr
%if "xUtility" != x
Group: Utility
%endif

Autoprov: 0
Autoreq: 0
%if "xalsa-lib, at-spi2-atk, at-spi2-core, atk, bzip2-libs, cairo, cairo-gobject, dbus-libs, elfutils-libelf, elfutils-libs, expat, fontconfig, freetype, fribidi, gdk-pixbuf2, glib2, glibc, graphite2, gtk2, gtk3, harfbuzz, libX11, libXau, libXcomposite, libXcursor, libXdamage, libXext, libXfixes, libXi, libXinerama, libXrandr, libXrender, libXtst, libattr, libblkid, libcap, libepoxy, libffi, libgcc, libgcrypt, libglvnd, libglvnd-egl, libglvnd-glx, libgpg-error, libmount, libpng, libselinux, libthai, libuuid, libwayland-client, libwayland-cursor, libwayland-egl, libxcb, libxkbcommon, lz4, pango, pcre, pixman, systemd-libs, xdg-utils, xz-libs, zlib" != x || "x" != x
Requires: alsa-lib, at-spi2-atk, at-spi2-core, atk, bzip2-libs, cairo, cairo-gobject, dbus-libs, elfutils-libelf, elfutils-libs, expat, fontconfig, freetype, fribidi, gdk-pixbuf2, glib2, glibc, graphite2, gtk2, gtk3, harfbuzz, libX11, libXau, libXcomposite, libXcursor, libXdamage, libXext, libXfixes, libXi, libXinerama, libXrandr, libXrender, libXtst, libattr, libblkid, libcap, libepoxy, libffi, libgcc, libgcrypt, libglvnd, libglvnd-egl, libglvnd-glx, libgpg-error, libmount, libpng, libselinux, libthai, libuuid, libwayland-client, libwayland-cursor, libwayland-egl, libxcb, libxkbcommon, lz4, pango, pcre, pixman, systemd-libs, xdg-utils, xz-libs, zlib 
%endif

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

%description
binjr is a standalone time series browser.
it renders time series data produced by other
applications as dynamically editable charts
and provides advanced features to navigate
the data smoothly and efficiently.

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}/opt/binjr
cp -r %{_sourcedir}/opt/binjr/* %{buildroot}/opt/binjr
%if "x%{buildroot}/../../../../../LICENSE.md" != x
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:%{buildroot}/../../../../../LICENSE.md}
  install -d -m 755 %{buildroot}%{dirname:%{license_install_file}}
  install -m 644 %{buildroot}/../../../../../LICENSE.md %{buildroot}%{license_install_file}
%endif

%files
%if "x%{buildroot}/../../../../../LICENSE.md" != x
  %license %{license_install_file}
  %{dirname:%{license_install_file}}
%endif
# If installation directory for the application is /a/b/c, we want only root
# component of the path (/a) in the spec file to make sure all subdirectories
# are owned by the package.
%(echo /opt/binjr | sed -e "s|\(^/[^/]\{1,\}\).*$|\1|")

%post
xdg-desktop-menu install /opt/binjr/lib/binjr-binjr.desktop
xdg-mime install /opt/binjr/lib/binjr-binjr-MimeInfo.xml
xdg-icon-resource install --context mimetypes --size 128 /opt/binjr/lib/binjr.png application-x-binjr
# Symlink bin command to /usr/bin
ln -sf /opt/%{name}/bin/%{name} %{_bindir}/%{name}

%preun
#
# Remove $1 desktop file from the list of default handlers for $2 mime type
# in $3 file dumping output to stdout.
#
_filter_out_default_mime_handler ()
{
  local defaults_list="$3"

  local desktop_file="$1"
  local mime_type="$2"

  awk -f- "$defaults_list" <<EOF
  BEGIN {
    mime_type="$mime_type"
    mime_type_regexp="~" mime_type "="
    desktop_file="$desktop_file"
  }
  \$0 ~ mime_type {
    \$0 = substr(\$0, length(mime_type) + 2);
    split(\$0, desktop_files, ";")
    remaining_desktop_files
    counter=0
    for (idx in desktop_files) {
      if (desktop_files[idx] != desktop_file) {
        ++counter;
      }
    }
    if (counter) {
      printf mime_type "="
      for (idx in desktop_files) {
        if (desktop_files[idx] != desktop_file) {
          printf desktop_files[idx]
          if (--counter) {
            printf ";"
          }
        }
      }
      printf "\n"
    }
    next
  }

  { print }
EOF
}


#
# Remove $2 desktop file from the list of default handlers for $@ mime types
# in $1 file.
# Result is saved in $1 file.
#
_uninstall_default_mime_handler ()
{
  local defaults_list=$1
  shift
  [ -f "$defaults_list" ] || return 0

  local desktop_file="$1"
  shift

  tmpfile1=$(mktemp)
  tmpfile2=$(mktemp)
  cat "$defaults_list" > "$tmpfile1"

  local v
  local update=
  for mime in "$@"; do
    _filter_out_default_mime_handler "$desktop_file" "$mime" "$tmpfile1" > "$tmpfile2"
    v="$tmpfile2"
    tmpfile2="$tmpfile1"
    tmpfile1="$v"

    if ! diff -q "$tmpfile1" "$tmpfile2" > /dev/null; then
      update=yes
      trace Remove $desktop_file default handler for $mime mime type from $defaults_list file
    fi
  done

  if [ -n "$update" ]; then
    cat "$tmpfile1" > "$defaults_list"
    trace "$defaults_list" file updated
  fi

  rm -f "$tmpfile1" "$tmpfile2"
}


#
# Remove $1 desktop file from the list of default handlers for $@ mime types
# in all known system defaults lists.
#
uninstall_default_mime_handler ()
{
  for f in /usr/share/applications/defaults.list /usr/local/share/applications/defaults.list; do
    _uninstall_default_mime_handler "$f" "$@"
  done
}


trace ()
{
  echo "$@"
}
xdg-desktop-menu uninstall /opt/binjr/lib/binjr-binjr.desktop
xdg-mime uninstall /opt/binjr/lib/binjr-binjr-MimeInfo.xml
uninstall_default_mime_handler binjr-binjr.desktop application/x-binjr
xdg-icon-resource uninstall application-x-binjr --size 128
# Remove symlink in bin dir
if [ $1 = 0 ]; then
  rm -f %{_bindir}/%{name}
fi

%clean
