#! /bin/sh

(
    echo "[$(date -Iseconds)] Updating binjr to version ${NEW_VERSION}" &&
        tar xzf "${PACKAGE}" "${NEW_VERSION}" &&
        (
            cd "./${OLD_VERSION}" &&
                find . -mindepth 1 |
                    sed -n 's@^\./@@p' |
                    grep -vxFf "./.installed" |
                    while read file; do
                        if [ -f "$file" ] && ! [ -f "../${NEW_VERSION}/$file" ]; then
                            mkdir -p "../${NEW_VERSION}/$(dirname "$file")" &&
                                ln "$file" "../${NEW_VERSION}/$file" ||
                                    exit 1
                        fi
                    done
        ) &&
        ln -s "./${NEW_VERSION}/binjr" "./new_binjr" &&
        mv "./new_binjr" "./binjr" && # atomic upgrade
        rm -rf "./${OLD_VERSION}" &&
        rm "./upgrade" &&
        rm "${PACKAGE}" &&
        echo "[$(date -Iseconds)] binjr succesfully updated to version ${NEW_VERSION}" ||
        echo "[$(date -Iseconds)] Unable to update binjr to version ${NEW_VERSION}"
) >> "binjr-install.log" 2>&1

if "${RESTART}"; then
    exec "./binjr"
fi
