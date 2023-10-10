# Deprecation List

This article holds log of all removed features from plugin code base.

## Type-System Verification

Type-System-Verification (aka TSV) has been removed due to missing source-code for binary jar file, which was used as sub-process. There was plan for re-implementation with Open Source licence, in the meantime there were some moves on SAP to open source code. It was for some time available, but at the end disappeared from Github. Functionality has been removed from version [1.5.7](https://github.com/SAP/hybris-commerce-eclipse-plugin/releases/tag/v1.5.7). It was still working in [1.5.6](https://github.com/SAP/hybris-commerce-eclipse-plugin/releases/tag/v1.5.6).

## Copyright Extension

Copyright extension was responsible for bulk package comment placement to follow enterprise security rule to have comment with copyright added to all files. Functionality was available in version [22.08](https://github.com/SAP/hybris-commerce-eclipse-plugin/releases/tag/v22.8) and removed in **22.09** because it can be replaced by [Eclipse Copyright Tool](https://wiki.eclipse.org/Development_Resources/How_to_Use_Eclipse_Copyright_Tool).

