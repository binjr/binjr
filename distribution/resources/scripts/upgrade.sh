#! /bin/sh

(
    echo "[$(date --iso-8601=seconds)] Updating binjr to version ${NEW_VERSION}" &&
        cp -rl "./${OLD_VERSION}" "./${NEW_VERSION}" &&
        while read file; do
            test -f "./${NEW_VERSION}/$file" &&
                rm "./${NEW_VERSION}/$file"
        done < "./${OLD_VERSION}/.installed" &&
        tar xzf "${PACKAGE}" "${NEW_VERSION}" &&
        ln -s "./${NEW_VERSION}/binjr" "./new_binjr" &&
        mv "./new_binjr" "./binjr" && # atomic upgrade
        rm -rf "./${OLD_VERSION}" &&
	rm "./upgrade" &&
        rm "${PACKAGE}" &&
        echo "[$(date --iso-8601=seconds)] binjr succesfully updated to version ${NEW_VERSION}" ||
        echo "[$(date --iso-8601=seconds)] Unable to update binjr to version ${NEW_VERSION}"
) >> "binjr-install.log" 2>&1

if "${RESTART}"; then
    exec "./binjr"
fi
