.PHONY: all build test package clean verify reuse

# Java environment configuration
# Default to existing JAVA_HOME or auto-detect installed JDK 21
export JAVA_HOME ?= $(shell if [ -d "/opt/homebrew/opt/openjdk@21" ]; then echo "/opt/homebrew/opt/openjdk@21"; elif /usr/libexec/java_home -v 21 >/dev/null 2>&1; then /usr/libexec/java_home -v 21; fi)
ifneq ($(JAVA_HOME),)
export PATH := $(JAVA_HOME)/bin:$(PATH)
endif

MVN := ./mvnw

all: build

# Full build (compile + package)
build:
	$(MVN) clean install

# Build with tests / run all tests
test:
	$(MVN) clean verify

# Verify target
verify:
	$(MVN) clean verify

# Build p2 update site package
package:
	$(MVN) clean package -DskipTests

# Clean target
clean:
	$(MVN) clean

# REUSE license check
reuse:
	pipx run reuse lint