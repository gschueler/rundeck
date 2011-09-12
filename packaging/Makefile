SHELL=/bin/bash

ifndef VERSION
VERSION=$(shell grep version.number= ${PWD}/../version.properties | cut -d= -f 2)
endif
ifndef RELEASE
RELEASE=$(shell grep version.release.number= ${PWD}/../version.properties | cut -d= -f 2)
endif
ifndef TAG
TAG=$(shell grep version.tag= ${PWD}/../version.properties | cut -d= -f 2)
endif

TAG?=snapshot

RPMBUILD_DEFINES = --define "_topdir ${PWD}/rpmdist" 
RPMBUILD_DEFINES+= --define "_version ${VERSION}"
RPMBUILD_DEFINES+= --define "_release ${RELEASE}"

ifndef BUILD_NUMBER
ALPHATAG?=$(TAG)-$(shell date "+%Y%m%d%H%M")
else
ALPHATAG?=$(TAG)
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"
endif

ifndef GA_RELEASE
RPMBUILD_DEFINES+= --define "_alphatag $(ALPHATAG)"
DEB_TAG="-$(ALPHATAG)"
endif

.PHONY: rpm deb dist msi

rpm:
	@mkdir -p rpmdist/RPMS/noarch
	@mkdir -p rpmdist/SOURCES
	@mkdir -p rpmdist/SOURCES/usr/bin
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p rpmdist/SOURCES/var/log/rundeck
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/var/tmp
	@mkdir -p rpmdist/SOURCES/tmp/rundeck
	@mkdir -p rpmdist/SOURCES/var/rundeck/projects
	@mkdir -p rpmdist/SOURCES/usr/share/man/man{1,5}
	@mkdir -p rpmdist/SOURCES/usr/share/man/en/man{1,5}
	@cp -R root/* rpmdist/SOURCES
	@cp ../docs/en/manpages/man1/*.1.gz rpmdist/SOURCES/usr/share/man/man1
	@cp ../docs/en/manpages/man5/*.5.gz rpmdist/SOURCES/usr/share/man/man5
	@cp ../docs/en/manpages/man1/*.1.gz rpmdist/SOURCES/usr/share/man/en/man1
	@cp ../docs/en/manpages/man5/*.5.gz rpmdist/SOURCES/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar rpmdist/SOURCES/var/lib/rundeck/bootstrap	
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar rpmdist/SOURCES/var/lib/rundeck/libext
	@find ../core/src/main/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z rpmdist/SOURCES/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d rpmdist/SOURCES/var/lib/rundeck/exp/webapp/
	@ln -s /etc/rundeck/log4j.properties; mv log4j.properties rpmdist/SOURCES/var/lib/rundeck/exp/webapp/WEB-INF/classes
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot ${PWD}/rpmdist/SOURCES rundeck.spec 


deb:
	@mkdir -p debdist/
	@mkdir -p debdist/usr/bin
	@mkdir -p debdist/var/lib/rundeck/exp/webapp
	@mkdir -p debdist/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p debdist/var/log/rundeck
	@mkdir -p debdist/var/lib/rundeck/var/tmp
	@mkdir -p debdist/tmp/rundeck
	@mkdir -p debdist/var/rundeck/projects
	@mkdir -p debdist/usr/share/man/man{1,5}
	@mkdir -p debdist/usr/share/man/en/man{1,5}
	@cp -R debroot/* debdist
	@# set version
	@cat debroot/DEBIAN/control | sed -e "s/%VERSION%/$(VERSION)/" > debdist/DEBIAN/control
	@cp ../docs/en/manpages/man1/*.1.gz debdist/usr/share/man/man1
	@cp ../docs/en/manpages/man5/*.5.gz debdist/usr/share/man/man5
	@cp ../docs/en/manpages/man1/*.1.gz debdist/usr/share/man/en/man1
	@cp ../docs/en/manpages/man5/*.5.gz debdist/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar debdist/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar debdist/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar debdist/var/lib/rundeck/bootstrap
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar debdist/var/lib/rundeck/libext
	@find ../core/src/main/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z debdist/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d debdist/var/lib/rundeck/exp/webapp/
	@ln -s /etc/rundeck/log4j.properties; mv log4j.properties debdist/var/lib/rundeck/exp/webapp/WEB-INF/classes
	fakeroot dpkg-deb --build debdist
	@mv debdist.deb rundeck-$(VERSION)-$(RELEASE)$(DEB_TAG).deb

checkwix:
	@if ! which candle.exe >&/dev/null; then \
          echo 'ERROR: Please put WIX-35 tools in your path.'; exit 1; \
	 fi

msi: checkwix msidist dist/SOURCES/stacktrace.log
	echo "java -jar \"%~dp0/rundeck-launcher-${VERSION}.jar\"" > dist/SOURCES/rundeck.cmd
	cd dist/SOURCES ;\
	heat.exe dir "." -nologo \
	  -var var.SOURCES \
	  -gg -scom -sreg -sfrag -srd \
	  -dr INSTALLDIR \
	  -cg RunDeckComponentGroup \
	  -out ../rundeck-files.wxs ;\
	candle.exe -nologo \
	  -dSOURCES=. \
	  -dVERSION=1.4.0 \
	  -out ../ \
      ../../rundeck.wxs \
      ../../rundeck-dialogs.wxs \
      ../rundeck-files.wxs ;\
	light.exe -nologo \
	  -dSOURCES=. \
	  -dVERSION=1.4.0 \
	  -out ../rundeck.msi \
	  -ext WixUIExtension -cultures:en-us \
      ../rundeck.wixobj \
      ../rundeck-dialogs.wixobj \
      ../rundeck-files.wixobj

msidist:
	@mkdir -p dist/SOURCES
	@cp ../rundeckapp/target/rundeck-launcher-*.jar dist/SOURCES

dist/SOURCES/stacktrace.log:
	@ while [ ! -f dist/SOURCES/stacktrace.log ] ; do \
	echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"; \
	echo "* In another window, please invoke the launcher using:      *"; \
	echo "*                                                           *"; \
	echo "*    # cd dist/SOURCES; java -jar rundeck-launcher-*.jar    *"; \
	echo "*                                                           *"; \
	echo "* Once the launcher is listening on HTTP port, press CTRL+C *"; \
	echo "* and press any key in this window to continue . . .        *"; \
	echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"; \
	read -n1 -s ;\
	done
	@echo Proceeding...
	@rm -f dist/SOURCES/server/config/jaas-loginmodule.conf
	@rm -f dist/SOURCES/server/config/realm.properties
	@rm -f dist/SOURCES/server/config/rundeck-config.properties
	@rm -f dist/SOURCES/server/config/ssl.properties
	@rm -f dist/SOURCES/etc/admin.aclpolicy
	@rm -f dist/SOURCES/etc/console-log4j.properties
	@rm -f dist/SOURCES/etc/framework.properties
	@rm -f dist/SOURCES/etc/log4j.properties
	@rm -f dist/SOURCES/etc/preferences.properties
	@rm -f dist/SOURCES/etc/profile
	@rm -f dist/SOURCES/etc/profile.bat
	@rm -f dist/SOURCES/etc/project.properties
	@rm -f dist/SOURCES/server/exp/webapp/WEB-INF/classes/log4j.properties
	@touch dist/SOURCES/server/exp/webapp/WEB-INF/classes/log4j.properties
	@find  dist/SOURCES -type f \
                            ! -path \*/server/data/\* \
                            ! -name rundeck-launcher-\*.jar \
	 | xargs -n 40 truncate --size=0


dist:
	@mkdir -p dist/RPMS/noarch
	@mkdir -p dist/SOURCES
	@mkdir -p dist/SOURCES/usr/bin
	@mkdir -p dist/SOURCES/var/lib/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p dist/SOURCES/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p dist/SOURCES/var/log/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/var/tmp
	@mkdir -p dist/SOURCES/var/rundeck/projects
	@mkdir -p dist/SOURCES/usr/share/man/man{1,5}
	@mkdir -p dist/SOURCES/usr/share/man/en/man{1,5}
	@cp -R root/* dist/SOURCES
	@-cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/man1
	@-cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/man5
	@-cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/en/man1
	@-cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar dist/SOURCES/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar dist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar dist/SOURCES/var/lib/rundeck/bootstrap	
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar dist/SOURCES/var/lib/rundeck/libext
	@find ../core/src/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z dist/SOURCES/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d dist/SOURCES/var/lib/rundeck/exp/webapp/
	ln -s /etc/rundeck/log4j.properties; mv log4j.properties dist/SOURCES/var/lib/rundeck/exp/webapp/WEB-INF/classes

clean: rpmclean debclean

rpmclean:
	rm -rf rpmdist

debclean:
	rm -rf debdist rundeck-*.deb

