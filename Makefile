SHELL := /bin/bash
GRADLE_VERSION ?= 8.14.3

b: buildw
buildw:
	./gradlew clean build test jacocoTestReport -i
	gradle
install-jacococli:
	wget https://search.maven.org/remotecontent\?filepath\=org/jacoco/jacoco/0.8.7/jacoco-0.8.7.zip
	unzip remotecontent\?filepath=org%2Fjacoco%2Fjacoco%2F0.8.7%2Fjacoco-0.8.7.zip
unpack-reports:
	mkdir -p jacoco
	java -jar lib/jacococli.jar report timezone-utility-app/build/jacoco/testReleaseUnitTest.exec --classfiles timezone-utility-app/build/.transforms/*/transformed/out/jars/classes.jar --xml jacoco/jacocoRelease.xml
	java -jar lib/jacococli.jar report timezone-utility-app/build/jacoco/testDebugUnitTest.exec --classfiles timezone-utility-app/build/.transforms/*/transformed/out/jars/classes.jar --xml jacoco/jacocoDebug.xml
upgrade:
	gradle wrapper --gradle-version $(GRADLE_VERSION) --validate-url
coverage:
	./gradlew clean build test jacocoTestReport
	./gradlew -i
dependencies:
	./gradlew androidDependencies
lint:
	./gradlew lint test --scan
local-pipeline: dependencies lint b
upgrade-gradle:
	sudo apt upgrade
	sudo apt update
	export SDKMAN_DIR="$(HOME)/.sdkman"; \
	[[ -s "$(HOME)/.sdkman/bin/sdkman-init.sh" ]]; \
	source "$(HOME)/.sdkman/bin/sdkman-init.sh"; \
	sdk update; \
	gradleOnlineVersion=$(shell curl -s https://services.gradle.org/versions/current | jq .version | xargs -I {} echo {}); \
	if [[ -z "$$gradleOnlineVersion" ]]; then \
		sdk install gradle $(GRADLE_VERSION); \
		sdk use gradle $(GRADLE_VERSION); \
	else \
		sdk install gradle $$gradleOnlineVersion; \
		sdk use gradle $$gradleOnlineVersion; \
		export GRADLE_VERSION=$$gradleOnlineVersion; \
	fi; \
	make upgrade
install-linux:
	sudo apt-get install jq
	sudo apt-get install curl
	curl https://services.gradle.org/versions/current
deps-plugins-update:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/pluginUpdatesOne.sh | bash -s -- $(PARAMS)
deps-compose-update:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/jetPackComposeUpdatesOne.sh | bash
deps-gradle-update:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/gradleUpdatesOne.sh | bash
deps-java-update:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/javaUpdatesOne.sh | bash
deps-quick-update: deps-compose-update deps-plugins-update deps-gradle-update
accept-prs:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/acceptPR.sh | bash
update-repo-prs:
	curl -sL https://raw.githubusercontent.com/jesperancinha/project-signer/master/update-all-repo-prs.sh | bash
